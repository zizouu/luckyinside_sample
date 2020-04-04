package com.lhbros.luckyinside.map.model

import android.graphics.Bitmap
import android.location.Location
import com.lhbros.luckyinside.luckyu.model.LuckyU
import com.lhbros.luckyinside.store.model.Store
import com.mapbox.mapboxsdk.annotations.PolylineOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import java.io.Serializable
import java.util.*

data class Region(
    var regionIndex: Int? = null,
    var cityIndex: Int? = null,
    var cityCode: String? = null,
    var cityName: String? = null,
    var regionCode: String? = null,
    var regionName: String? = null,
    var registId: String? = null,
    var registDatetime: Date? = null,
    var modifyId: String? = null,
    var modifyDatetime: String? = null,
    var userIndex: Int? = null,
    var luckyuCount: Int? = null,
    var myStoreCount: Int? = null,
    var storeCount: Int? = null,
    var centerCoordinates: String? = null,
    var distance: Int? = null,
    var representativeImage: Int? = null,
    var gifImage: Int? = null,
    var representativeImageUrl: String? = null,
    var gifImageUrl: String? = null,
    var repConversionName: String? = null,
    var repConversionPath: String? = null,
    var gifConversionName: String? = null,
    var gifConversionPath: String? = null,
    var iconImageUrl: String? = null,
    @Transient
    var representativeImageBitmap: Bitmap? = null,
    @Transient
    var stores: MutableList<Store>? = null,
    @Transient
    var luckyUs: MutableList<LuckyU>? = null,
    @Transient
    var areaPolylineOptions: PolylineOptions? = null

) : Serializable, MapMarkerData(MarkerType.REGION) {
    val lat: Double?
        get() = centerCoordinates?.split(",")?.get(0)?.trim()?.toDouble()

    val lng: Double?
        get() = centerCoordinates?.split(",")?.get(1)?.trim()?.toDouble()

    fun getLatLng() : LatLng? {
        var latLng: LatLng? = null
        if (lat != null && lng != null) {
            latLng = LatLng(lat!!, lng!!)
        }
        return latLng
    }


    fun distanceTo(destinationLatLng: LatLng) : Double {
        val regionLatLng = LatLng(lat!!, lng!!)
        return regionLatLng.distanceTo(destinationLatLng)
    }

    fun isLocatedInRegion(position: LatLng?) : Boolean {
        position ?: return false
        val distance = distanceTo(position)
        return this.distance!!.toDouble() > distance || this.distance!!.toDouble() == distance
    }

    fun distanceTo(destinationLocation: Location) : Double {
        val regionLatLng = LatLng(lat!!, lng!!)
        val destinationLatLng = LatLng(destinationLocation.latitude, destinationLocation.longitude)
        return regionLatLng.distanceTo(destinationLatLng)
    }

    fun isLocatedInRegion(position: Location) : Boolean {
        val distance = distanceTo(position)
        return this.distance!!.toDouble() > distance || this.distance!!.toDouble() == distance
    }
}