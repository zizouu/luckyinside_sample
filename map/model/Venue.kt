package com.lhbros.luckyinside.map.model

import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import java.io.Serializable

abstract class Venue(
    markerType: MarkerType
) : Serializable, MapMarkerData(markerType) {
    enum class VenueType : Serializable {
        STORE, LUCKYU, MYSTORE
    }

    abstract fun getIndex(): Int?
    abstract fun getMyRegistration(): String?
    abstract fun getPossibleYn(): String?
    abstract fun getLocationPoint(): Point?
    abstract fun getName(): String?
    abstract fun getLatLng(): LatLng?
    abstract fun getAddress(): String?
}