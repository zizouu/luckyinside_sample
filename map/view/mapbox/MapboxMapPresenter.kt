package com.lhbros.luckyinside.map.view.mapbox

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.lhbros.luckyinside.R
import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.util.UiUtil
import com.lhbros.luckyinside.luckyu.model.LuckyU
import com.lhbros.luckyinside.map.model.*
import com.lhbros.luckyinside.store.model.Store
import com.mapbox.android.gestures.StandardScaleGestureDetector
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.LocationComponentOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.*
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

class MapboxMapPresenter(
    val mapView: MapView,
    val styleUrl: String,
    val context: Context
) : MapPresenter,
    OnMapReadyCallback,
    OnSymbolClickListener,
    MapboxMap.OnScaleListener,
    MapboxMap.OnMapClickListener {
    companion object {
        // zoom, lat lng
        private const val REGION_CLICK_ZOOM = 17.0
        private const val MARKER_TOGGLE_ZOOM = 13.5
        private const val USER_LOCATION_ZOOM = 15.0
        private const val DEFAULT_ZOOM = 12.0
        private const val DEFAULT_LAT = 37.522
        private const val DEFAULT_LNG = 127.020
        // duration
        private const val CAMERA_ANIMATION_DURATION = 2000
        private const val REGION_CLICK_DURATION = 500
        // image name
        private const val ACTIVE_MARKER_IMAGE = "active_marker_image"
        private const val CLICKED_ACTIVE_MARKER_IMAGE = "clicked_active_marker_image"
        private const val INACTIVE_MARKER_IMAGE = "inactive_marker_image"
        private const val CLICKED_INACTIVE_MARKER_IMAGE = "clicked_inactive_marker_image"
        // region area
        private const val REGION_AREA_CENTER_LAT = 37.548723
        private const val REGION_AREA_CENTER_LNG = 127.043934
        private const val REGION_AREA_RADIUS = 600000.0
        private const val REGION_AREA_LAYER_ID = "region_area_layer_id"
        private const val REGION_AREA_SOURCE_ID = "region_area_source_id"
    }

    val disposable = CompositeDisposable()
    // map
    private var mapboxMap: MapboxMap? = null
    private var symbolManager: SymbolManager? = null
    private var lineManager: LineManager? = null
    // data
    private var regionSymbols: MutableList<Symbol> = mutableListOf()
    private var regionSymbolOptionsList: MutableList<SymbolOptions> = mutableListOf()
    private var venueSymbols: MutableList<Symbol> = mutableListOf()
    private var venueSymbolOptionsList: MutableList<SymbolOptions> = mutableListOf()
    private var lineOptionsList: MutableList<LineOptions> = mutableListOf()
    override var selectedMarker: Symbol? = null
    // rx
    override val mapReady: PublishSubject<Boolean> = PublishSubject.create()
    override val venueMarkerClicked: PublishSubject<Venue> = PublishSubject.create()
    override val mapClicked: PublishSubject<Boolean> = PublishSubject.create()
    override val regionClicked = PublishSubject.create<Boolean>()
    private val zoomChanged = PublishSubject.create<Double>()
    private val symbolClicked = PublishSubject.create<Symbol>()
    val error = PublishSubject.create<Boolean>()
    // bitmap
    override var activeMarker: Bitmap? = null
    override var inactiveMarker: Bitmap? = null
    override var clickedActiveMarker: Bitmap? = null
    override var clickedInactiveMarker: Bitmap? = null

    override fun drawRegionMarker() {
        mapClicked.onNext(true)
        if (isShowingRegionMarker()) return
        regionSymbols = symbolManager?.create(regionSymbolOptionsList) ?: mutableListOf()
    }

    override fun drawVenueMarker() {
        if (isShowingVenueMarker()) return
        venueSymbols = symbolManager?.create(venueSymbolOptionsList) ?: mutableListOf()
    }

    override fun drawRegionArea() {
        showRegionLine()
        showOuterLayer()
    }

    override fun removeVenueMarker() {
        val symbols = symbolManager?.annotations
        if (symbols == null || symbols.isEmpty) return
        for (venueSymbol in venueSymbols) {
            val id = venueSymbol.id
            symbols.remove(id)
        }
    }

    override fun removeRegionMarker() {
        val symbols = symbolManager?.annotations
        if (symbols == null || symbols.isEmpty) return
        for (regionSymbol in regionSymbols) {
            val id = regionSymbol.id
            symbols.remove(id)
        }
    }

    override fun removeRegionArea() {
        removeRegionLine()
        hideOuterLayer()
    }

    override fun isShowingRegionMarker(): Boolean {
        val symbols = symbolManager?.annotations
        if (symbols == null || symbols.isEmpty) return false
        for (regionSymbol in regionSymbols) {
            if (symbols.containsKey(regionSymbol.id)) {
                return true
            }
        }
        return false
    }

    override fun isShowingVenueMarker(): Boolean {
        val symbols = symbolManager?.annotations
        if (symbols == null || symbols.isEmpty) return false
        for (venueSymbol in venueSymbols) {
            if (symbols.containsKey(venueSymbol.id)) {
                return true
            }
        }
        return false
    }

    override fun isShowingRegionArea(): Boolean {
        val isLayerVisible = mapboxMap?.style?.getLayer(REGION_AREA_LAYER_ID)?.visibility?.getValue() == Property.VISIBLE
        val isLineVisible = lineManager?.annotations?.isEmpty == false
        return isLayerVisible || isLineVisible
    }

    override fun setRegions(regions: List<Region>) {
        addRegionImage(regions)
        val symbolOptionsList = mutableListOf<SymbolOptions>()
        lineOptionsList.clear()
        for (region in regions) {
            // 지역 반경 라인
            val lineOptions = makeAreaLineOptions(region.getLatLng(), region.distance?.toDouble())
            if (lineOptions != null) {
                lineOptionsList.add(lineOptions)
            }
            // 지역 아이콘 (라인옵션을 먼저 set 한 후 symbolOption 추가 해야됨 - data 때문)
            val symbolOptions =
                SymbolOptions()
                    .withData(GsonBuilder().create().toJsonTree(region))
                    .withIconImage(region.regionCode)
                    .withLatLng(region.getLatLng())
            symbolOptionsList.add(symbolOptions)
        }
        removeOuterLayer()
        removeOuterSource()
        addOuterSource(regions)
        addOuterLayer()
        regionSymbolOptionsList = symbolOptionsList
    }

    override fun setVenues(venues: Venues) {
        addVenueImages()
        val symbolOptions = mutableListOf<SymbolOptions>()
        for (luckyU in venues.luckyuList) {
            val latLng = luckyU.getLatLng()
            latLng ?: continue
            val isActive = luckyU.possibleGetYn == LuckyInsideConstant.COMMON_YN_Y
            val options =
                SymbolOptions()
                    .withData(GsonBuilder().create().toJsonTree(luckyU))
                    .withIconImage(if (isActive) ACTIVE_MARKER_IMAGE else INACTIVE_MARKER_IMAGE)
                    .withLatLng(latLng)
            symbolOptions.add(options)
        }
        for (store in venues.storeList) {
            val latLng = store.getLatLng()
            latLng ?: continue
            val isActive = store.possibleGetYn == LuckyInsideConstant.COMMON_YN_Y
            val options =
                SymbolOptions()
                    .withData(GsonBuilder().create().toJsonTree(store))
                    .withIconImage(if (isActive) ACTIVE_MARKER_IMAGE else INACTIVE_MARKER_IMAGE)
                    .withLatLng(latLng)
            symbolOptions.add(options)
        }
        venueSymbolOptionsList = symbolOptions
        drawMarker()
    }

    override fun refreshMapStart() {
        mapClicked.onNext(true)
        symbolManager?.deleteAll()
        lineManager?.deleteAll()
        regionSymbols.clear()
        regionSymbolOptionsList.clear()
        venueSymbols.clear()
        venueSymbolOptionsList.clear()
        lineOptionsList.clear()
        hideOuterLayer()
        selectedMarker = null
    }

    override fun isUserLocatedInRegion(regionIndex: Int?): Boolean {
        if (mapboxMap?.locationComponent?.isLocationComponentActivated == false) return false
        val userLocation = mapboxMap?.locationComponent?.lastKnownLocation
        userLocation ?: return false
        var result = false
        for (symbol in regionSymbols) {
            val region = getMapMarkerData(symbol) as Region
            if (region.regionIndex == regionIndex) {
                val radius = region.distance?.toDouble()
                if (radius == null) {
                    result = false
                    break
                }
                val distance = region.getLatLng()?.distanceTo(LatLng(userLocation.latitude, userLocation.longitude))
                if (distance == null) {
                    result = false
                    break
                }
                if (radius > distance || radius == distance) {
                    result = true
                    break
                }
            }
        }
        return result
    }

    override fun getUserLatLng(): LatLng? {
        if (mapboxMap?.locationComponent?.isLocationComponentActivated == false) return null
        val location = mapboxMap?.locationComponent?.lastKnownLocation
        location ?: return null
        return LatLng(location.latitude, location.longitude)
    }

    override fun setSelectedMarkerUnselected() {
        changeMarkerImage(selectedMarker, false)
    }

    override fun destroyMap() {
        disposable.clear()
    }

    override fun setUserImage() {

    }

    override fun activateLocationComponent() {
        val style = mapboxMap?.style
        style ?: return
        val locationComponentOptions = LocationComponentOptions.builder(context)
            .foregroundStaleName(LuckyInsideConstant.MAPBOX_USER_PROFILE_IMAGE)
            .foregroundName(LuckyInsideConstant.MAPBOX_USER_PROFILE_IMAGE)
            .bearingName(LuckyInsideConstant.MAPBOX_BEARING_IMAGE)
            .build()
        val activateOptions = LocationComponentActivationOptions.builder(context, style)
            .locationComponentOptions(locationComponentOptions)
            .build()
        val locationComponent = mapboxMap?.locationComponent
        locationComponent?.activateLocationComponent(activateOptions)
        locationComponent?.isLocationComponentEnabled = true
        locationComponent?.cameraMode = CameraMode.NONE
        locationComponent?.renderMode = RenderMode.COMPASS
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(Style.Builder().fromUri(styleUrl)) {
            this.mapboxMap?.addOnScaleListener(this)
            this.mapboxMap?.addOnMapClickListener(this)
            symbolManager = SymbolManager(mapView, mapboxMap, it)
            symbolManager?.iconAllowOverlap = true
            symbolManager?.addClickListener(this@MapboxMapPresenter)
            lineManager = LineManager(mapView, mapboxMap, it)
            subscribeRx()
            initializeMapUserDefaultImage()
            mapReady.onNext(true)
        }
    }

    override fun onAnnotationClick(t: Symbol?) {
        val mapMarker = getMapMarkerData(t)
        mapMarker ?: return
        when (mapMarker.markerType) {
            MapMarkerData.MarkerType.STORE -> {
                t ?: return
                setSelectedMarkerUnselected()
                selectedMarker = t
                symbolClicked.onNext(t)
                venueMarkerClicked.onNext(mapMarker as Store)
            }
            MapMarkerData.MarkerType.LUCKYU -> {
                t ?: return
                setSelectedMarkerUnselected()
                selectedMarker = t
                symbolClicked.onNext(t)
                venueMarkerClicked.onNext(mapMarker as LuckyU)
            }
            MapMarkerData.MarkerType.REGION -> { clickRegionMarker(mapMarker as Region) }
        }
    }

    override fun onMapClick(point: LatLng): Boolean {
        val pointF = mapboxMap?.projection?.toScreenLocation(point)
        pointF ?: return false
        val features = mapboxMap?.queryRenderedFeatures(pointF, symbolManager?.filter)
        if (features.isNullOrEmpty()) {
            mapClicked.onNext(true)
            return true
        } else {
            for (feature in features) {
                if (feature.hasProperty("custom_data")) {
                    return false
                }
            }
            mapClicked.onNext(true)
            return true
        }
    }

    override fun animateUserLocation() {
        val latLng = getUserLatLng()
        latLng ?: return
        val callback = object : MapboxMap.CancelableCallback {
            override fun onFinish() {
                mapboxMap?.locationComponent?.cameraMode = CameraMode.TRACKING
                mapboxMap?.locationComponent?.renderMode = RenderMode.COMPASS
                drawMarker()
            }
            override fun onCancel() {
                drawMarker()
            }
        }
        animateCamera(latLng, USER_LOCATION_ZOOM, CAMERA_ANIMATION_DURATION, callback)
    }

    override fun animateDefaultMap() {
        val latLng = LatLng(DEFAULT_LAT, DEFAULT_LNG)
        animateCamera(latLng, DEFAULT_ZOOM, CAMERA_ANIMATION_DURATION)
    }

    override fun animateRegion(regionIndex: Int?) {
        val region = getRegion(regionIndex)
        region ?: return
        clickRegionMarker(region)
    }

    override fun getLocatedInRegion(): Region? {
        var result: Region? = null
        for (symbolOptions in regionSymbolOptionsList) {
            val region = getMapMarkerData(symbolOptions)
            if (region is Region) {
                if (region.isLocatedInRegion(getUserLatLng())) {
                    result = region
                    break
                }
            }
        }
        return result
    }

    override fun getRegion(index: Int?): Region? {
        index ?: return null
        var result: Region? = null
        for (symbolOptions in regionSymbolOptionsList) {
            val region = getMapMarkerData(symbolOptions)
            if (region is Region) {
                if (region.regionIndex == index) {
                    result = region
                    break
                }
            }
        }
        return result
    }

    override fun onScaleBegin(detector: StandardScaleGestureDetector) { }
    override fun onScale(detector: StandardScaleGestureDetector) { }
    override fun onScaleEnd(detector: StandardScaleGestureDetector) {
        drawMarker()
    }

    private fun subscribeRx() {
        disposable.add(
            zoomChanged
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    drawMarkersByZoom(it)
                }) {
                    error.onNext(true)
                }
        )
        disposable.add(
            symbolClicked
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    changeMarkerImage(it, true)
                }) {
                    error.onNext(true)
                }
        )
    }

    private fun showRegionLine() {
        if (lineManager?.annotations?.isEmpty == true) {
            lineManager?.create(lineOptionsList)
        }
    }

    private fun showOuterLayer() {
        mapboxMap?.style?.getLayer(REGION_AREA_LAYER_ID)?.setProperties(
            PropertyFactory.visibility(Property.VISIBLE)
        )
    }

    private fun hideOuterLayer() {
        mapboxMap?.style?.getLayer(REGION_AREA_LAYER_ID)?.setProperties(
            PropertyFactory.visibility(Property.NONE)
        )
    }

    private fun addOuterLayer() {
        val layer = mapboxMap?.style?.getLayer(REGION_AREA_LAYER_ID)
        if (layer == null) {
            mapboxMap?.style?.addLayer(
                FillLayer(REGION_AREA_LAYER_ID, REGION_AREA_SOURCE_ID)
                    .withProperties(
                        PropertyFactory.fillColor(context.getColor(R.color.luckyinside_symbol_10)),
                        PropertyFactory.visibility(Property.NONE)
                    )
            )
        }
    }

    private fun removeOuterLayer() {
        val layer = mapboxMap?.style?.getLayer(REGION_AREA_LAYER_ID)
        if (layer != null) {
            mapboxMap?.style?.removeLayer(REGION_AREA_LAYER_ID)
        }
    }

    private fun addOuterSource(regions: List<Region>) {
        val source = mapboxMap?.style?.getSource(REGION_AREA_SOURCE_ID)
        if (source == null) {
            val outerLineString = getRegionAreaOuterLineString()
            val innerLineStrings = getRegionAreaInnerLineStrings(regions)
            mapboxMap?.style?.addSource(
                GeoJsonSource(
                    REGION_AREA_SOURCE_ID,
                    Feature.fromGeometry(Polygon.fromOuterInner(outerLineString, innerLineStrings))
                )
            )
        }
    }

    private fun removeOuterSource() {
        val source = mapboxMap?.style?.getSource(REGION_AREA_SOURCE_ID)
        if (source != null) {
            mapboxMap?.style?.removeSource(REGION_AREA_SOURCE_ID)
        }
    }

    private fun removeRegionLine() {
        lineManager?.deleteAll()
    }

    override fun addUserImage(bitmap: Bitmap) {
        val bitmapWithBorder = UiUtil.drawWhiteBorder(bitmap, 20f)
        mapboxMap?.style?.addImage(LuckyInsideConstant.MAPBOX_USER_PROFILE_IMAGE, bitmapWithBorder)
        bitmap.recycle()
        bitmapWithBorder.recycle()
    }

    private fun initializeMapUserDefaultImage() {
        // 디폴트 이미지 셋팅
        val userBitmap = getDefaultUserProfile()
        addUserImage(userBitmap)
        // 베어링 이미지 셋팅
        val bearingBitmap = getUserProfileBearing()
        mapboxMap?.style?.addImage(LuckyInsideConstant.MAPBOX_BEARING_IMAGE, bearingBitmap)
        bearingBitmap.recycle()
    }

    private fun getDefaultUserProfile() : Bitmap {
        val profileSize = context.resources.getDimension(R.dimen.map_profile_size)
        val defaultProfile = BitmapFactory.decodeResource(context.resources, R.drawable.ic_user)
        val bitmap = UiUtil.getResizedBitmap(defaultProfile,
            UiUtil.dpToPx(context, profileSize),
            UiUtil.dpToPx(context, profileSize))
        defaultProfile.recycle()
        return bitmap
    }

    private fun getUserProfileBearing() : Bitmap {
        val bearingSize = context.resources.getDimension(R.dimen.map_bearing_size)
        val defaultBearing = BitmapFactory.decodeResource(context.resources, R.drawable.ic_bearing)
        val bitmap = UiUtil.getResizedBitmap(defaultBearing,
            UiUtil.dpToPx(context, bearingSize),
            UiUtil.dpToPx(context, bearingSize))
        defaultBearing.recycle()
        return bitmap
    }

    private fun changeMarkerImage(symbol: Symbol?, isClicked: Boolean) {
        symbol ?: return
        val currentImage = symbol.iconImage
        if (currentImage == ACTIVE_MARKER_IMAGE || currentImage == CLICKED_ACTIVE_MARKER_IMAGE) {
            symbol.iconImage = if (isClicked) CLICKED_ACTIVE_MARKER_IMAGE else ACTIVE_MARKER_IMAGE
        } else {
            symbol.iconImage = if (isClicked) CLICKED_INACTIVE_MARKER_IMAGE else INACTIVE_MARKER_IMAGE
        }
        symbolManager?.update(symbol)
    }

    private fun drawMarker() {
        val zoom = getCurrentZoom()
        zoom ?: return
        zoomChanged.onNext(zoom)
    }

    private fun clickRegionMarker(region: Region) {
        val latLng = region.getLatLng()
        latLng ?: return
        animateCamera(latLng, REGION_CLICK_ZOOM, REGION_CLICK_DURATION)
        regionClicked.onNext(true)
    }

    private fun addVenueImages() {
        if (activeMarker != null) addImage(ACTIVE_MARKER_IMAGE, activeMarker!!)
        if (inactiveMarker != null) addImage(INACTIVE_MARKER_IMAGE, inactiveMarker!!)
        if (clickedActiveMarker != null) addImage(CLICKED_ACTIVE_MARKER_IMAGE, clickedActiveMarker!!)
        if (clickedInactiveMarker != null) addImage(CLICKED_INACTIVE_MARKER_IMAGE, clickedInactiveMarker!!)
    }

    private fun addRegionImage(regions: List<Region>) {
        for (region in regions) {
            val code = region.regionCode
            val bitmap = region.representativeImageBitmap
            if (!code.isNullOrBlank() && bitmap != null) {
                addImage(code, bitmap)
            }
        }
    }

    private fun addImage(name: String, bitmap: Bitmap) {
        mapboxMap?.style?.addImage(name, bitmap)
        bitmap.recycle()
    }

    private fun animateCamera(latLng: LatLng, zoom: Double, duration: Int = 2000, callback: MapboxMap.CancelableCallback? = null) {
        val animCallback: MapboxMap.CancelableCallback
        if (callback == null) {
            animCallback = object : MapboxMap.CancelableCallback {
                override fun onFinish() {
                    drawMarker()
                }
                override fun onCancel() {
                    drawMarker()
                }
            }
        } else {
            animCallback = callback
        }
        mapboxMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom), duration, animCallback)
    }

    private fun getMapMarkerData(symbol: Symbol?): MapMarkerData? {
        symbol ?: return null
        val mapMarker = Gson().fromJson(symbol.data, MapMarkerData::class.java)
        var result: MapMarkerData? = null
        when (mapMarker.markerType) {
            MapMarkerData.MarkerType.STORE -> {
                result = Gson().fromJson(symbol.data, Store::class.java)
            }
            MapMarkerData.MarkerType.LUCKYU -> {
                result = Gson().fromJson(symbol.data, LuckyU::class.java)
            }
            MapMarkerData.MarkerType.REGION -> {
                result = Gson().fromJson(symbol.data, Region::class.java)
            }
        }
        return result
    }

    private fun getMapMarkerData(symbolOptions: SymbolOptions?): MapMarkerData? {
        symbolOptions ?: return null
        val mapMarker = Gson().fromJson(symbolOptions.data, MapMarkerData::class.java)
        var result: MapMarkerData? = null
        when (mapMarker.markerType) {
            MapMarkerData.MarkerType.STORE -> {
                result = Gson().fromJson(symbolOptions.data, Store::class.java)
            }
            MapMarkerData.MarkerType.LUCKYU -> {
                result = Gson().fromJson(symbolOptions.data, LuckyU::class.java)
            }
            MapMarkerData.MarkerType.REGION -> {
                result = Gson().fromJson(symbolOptions.data, Region::class.java)
            }
        }
        return result
    }

    private fun getRegion(symbol: Symbol?): Region? {
        symbol ?: return null
        val mapMarker = Gson().fromJson(symbol.data, MapMarkerData::class.java)
        var result: Region? = null
        if (mapMarker.markerType == MapMarkerData.MarkerType.REGION) {
            result = Gson().fromJson(symbol.data, Region::class.java)
        }
        return result
    }

    private fun drawMarkersByZoom(zoom: Double) {
        // 지역
        if (zoom < MARKER_TOGGLE_ZOOM || zoom == MARKER_TOGGLE_ZOOM) {
            removeRegionArea()
            removeVenueMarker()
            drawRegionMarker()
        }
        // 베뉴
        else if (zoom > MARKER_TOGGLE_ZOOM) {
            removeRegionMarker()
            drawRegionArea()
            drawVenueMarker()
        }
    }

    private fun getCurrentZoom(): Double? {
        return mapboxMap?.cameraPosition?.zoom
    }

    fun makeAreaLineOptions(position: LatLng?, radiusMeters: Double?) : LineOptions? {
        position ?: return null
        radiusMeters ?: return null
        return LineOptions()
            .withLatLngs(getAreaCircleLatLngs(position, radiusMeters))
            .withLineColor("#ee4958")
            .withLineOpacity(0.4f)
            .withLineWidth(1.5f)
    }

    fun getAreaCircleLatLngs(position: LatLng, radius: Double) : ArrayList<LatLng> {
        val degreesBetweenPoints = 1 // change here for shape
        val numberOfPoints = floor(360.0 / degreesBetweenPoints).toInt()
        val distRadians = radius / 6371000.0 // earth radius in meters
        val centerLatRadians = position.latitude * Math.PI / 180
        val centerLonRadians = position.longitude * Math.PI / 180
        val polygons = arrayListOf<LatLng>() // array to hold all the points
        for (index in 0..numberOfPoints) {
            val degrees = index * degreesBetweenPoints
            val degreeRadians = degrees * Math.PI / 180
            val pointLatRadians = Math.asin(
                sin(centerLatRadians) * cos(distRadians)
                        + cos(centerLatRadians) * sin(distRadians) * cos(degreeRadians)
            )
            val pointLonRadians = centerLonRadians + Math.atan2(
                sin(degreeRadians)
                        * sin(distRadians) * cos(centerLatRadians),
                cos(distRadians) - sin(centerLatRadians) * sin(pointLatRadians)
            )
            val pointLat = pointLatRadians * 180 / Math.PI
            val pointLon = pointLonRadians * 180 / Math.PI
            val point = LatLng(pointLat, pointLon)
            polygons.add(point)
        }
        // add first point at end to close circle
        polygons.add(polygons[0])
        return polygons
    }

    fun getAreaCirclePoints(position: LatLng, radius: Double) : ArrayList<Point> {
        val degreesBetweenPoints = 1 // change here for shape
        val numberOfPoints = Math.floor(360.0 / degreesBetweenPoints).toInt()
        val distRadians = radius / 6371000.0 // earth radius in meters
        val centerLatRadians = position.latitude * Math.PI / 180
        val centerLonRadians = position.longitude * Math.PI / 180
        val polygons = arrayListOf<Point>() // array to hold all the points
        for (index in 0..numberOfPoints) {
            val degrees = index * degreesBetweenPoints
            val degreeRadians = degrees * Math.PI / 180
            val pointLatRadians = Math.asin(
                sin(centerLatRadians) * cos(distRadians)
                        + cos(centerLatRadians) * sin(distRadians) * cos(degreeRadians)
            )
            val pointLonRadians = centerLonRadians + Math.atan2(
                sin(degreeRadians)
                        * sin(distRadians) * cos(centerLatRadians),
                cos(distRadians) - sin(centerLatRadians) * sin(pointLatRadians)
            )
            val pointLat = pointLatRadians * 180 / Math.PI
            val pointLon = pointLonRadians * 180 / Math.PI
            val point = Point.fromLngLat(pointLon, pointLat)
            polygons.add(point)
        }
        // add first point at end to close circle
        polygons.add(polygons[0])
        return polygons
    }

    fun getRegionAreaOuterPoints() : List<Point> {
        val outerLatLng = LatLng(REGION_AREA_CENTER_LAT, REGION_AREA_CENTER_LNG)
        return getAreaCirclePoints(outerLatLng, REGION_AREA_RADIUS)
    }

    fun getLineString(points: List<Point>) = LineString.fromLngLats(points)

    fun getRegionAreaOuterLineString() = getLineString(getRegionAreaOuterPoints())

    fun getRegionAreaInnerLineStrings(regions: List<Region>) : List<LineString> {
        val innerLineStrings = mutableListOf<LineString>()

        for (region in regions) {
            val regionLatLng = region.getLatLng()
            val regionRadius = region.distance?.toDouble()
            if (regionLatLng != null && regionRadius != null) {
                val innerPoints = getAreaCirclePoints(regionLatLng, regionRadius)
                val innerLineString = getLineString(innerPoints)
                innerLineStrings.add(innerLineString)
            }
        }
        return innerLineStrings
    }
}