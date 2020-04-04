package com.lhbros.luckyinside.daily.model

import java.io.Serializable

data class DailyVenue(
    var storeIndex: Int? = null,
    var luckyuIndex: Int? = null,
    var division: String? = null,
    var openYn: String? = null,
    var name: String? = null,
    var title: String? = null,
    var startHour: String? = null,
    var bannerImageUrl1: String? = null,
    var bannerImageUrl2: String? = null,
    var bannerImageUrl3: String? = null,
    var bannerImageUrl4: String? = null,
    var regionIndex: Int? = null,
    var categoryName: String? = null,
    var categoryCode: String? = null,
    var myRegistrationYn: String? = null
) : Serializable {
}