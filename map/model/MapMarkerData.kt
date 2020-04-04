package com.lhbros.luckyinside.map.model

import java.io.Serializable

open class MapMarkerData(
    val markerType: MarkerType
) : Serializable {
    enum class MarkerType {
        STORE, LUCKYU, REGION
    }
}