package com.lhbros.luckyinside.map.view.mapbox

import android.graphics.Bitmap
import com.lhbros.luckyinside.map.model.Region
import com.lhbros.luckyinside.map.model.Venue
import com.lhbros.luckyinside.map.model.Venues
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import io.reactivex.subjects.PublishSubject

interface MapPresenter {
    val mapReady: PublishSubject<Boolean>
    val venueMarkerClicked: PublishSubject<Venue>
    val mapClicked: PublishSubject<Boolean>
    val regionClicked: PublishSubject<Boolean>
    var activeMarker: Bitmap?
    var inactiveMarker: Bitmap?
    var clickedActiveMarker: Bitmap?
    var clickedInactiveMarker: Bitmap?
    var selectedMarker: Symbol?

    fun drawRegionMarker()
    fun drawVenueMarker()
    fun drawRegionArea()
    fun removeRegionMarker()
    fun removeVenueMarker()
    fun removeRegionArea()
    fun isShowingRegionMarker(): Boolean
    fun isShowingVenueMarker(): Boolean
    fun isShowingRegionArea(): Boolean
    fun setRegions(regions: List<Region>)
    fun setVenues(venues: Venues)
    fun refreshMapStart()
    fun destroyMap()
    fun setUserImage()
    fun isUserLocatedInRegion(regionIndex: Int?): Boolean
    fun getUserLatLng(): LatLng?
    fun setSelectedMarkerUnselected()
    fun activateLocationComponent()
    fun addUserImage(bitmap: Bitmap)
    fun animateUserLocation()
    fun animateDefaultMap()
    fun animateRegion(regionIndex: Int?)
    fun getLocatedInRegion(): Region?
    fun getRegion(index: Int?): Region?
}