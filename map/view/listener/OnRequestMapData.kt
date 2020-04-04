package com.lhbros.luckyinside.map.view.listener

interface OnRequestMapData {
    fun requestIsLocatedInRegion(regionIndex: Int?) : Boolean
}