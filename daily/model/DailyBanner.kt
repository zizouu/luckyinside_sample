package com.lhbros.luckyinside.daily.model

import java.io.Serializable

data class DailyBanner(
    var luckyDrawIndex: Int? = null,
    var luckyuIndex: Int? = null,
    var storeIndex: Int? = null,
    var type: String? = null,
    var bannerImage: String? = null,
    var imageList: List<DailyBannerImage>,
    var title: String? = null
) : Serializable {
    companion object {
        const val TYPE_STORE = "S"
        const val TYPE_LUCKYU = "L"
        const val TYPE_DRAW = "D"
        const val TYPE_IMAGE = "I"
    }
}