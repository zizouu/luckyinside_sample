package com.lhbros.luckyinside.daily.view.listener

interface OnDailyVenueClickListener {
    fun onDailyVenueClick(type: String?, index: Int?)
    fun onDailyMyVenueClick(type: String?, index: Int?, isRegist: Boolean)
    fun onDailyFreeExperienceSeeAll()
}