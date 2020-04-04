package com.lhbros.luckyinside.map.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lhbros.luckyinside.common.LuckyInsideSharedPreference
import com.lhbros.luckyinside.common.api.AlarmApi
import com.lhbros.luckyinside.common.api.CityApi
import com.lhbros.luckyinside.luckyu.api.LuckyUApi
import com.lhbros.luckyinside.map.api.*
import com.lhbros.luckyinside.popup.api.PopupApi
import com.lhbros.luckyinside.store.api.StoreApi

class MapMainViewModelFactory(
    val regionApi: RegionApi,
    val storeApi: StoreApi,
    val luckyUApi: LuckyUApi,
    val cityApi: CityApi,
    val alarmApi: AlarmApi,
    val popupApi: PopupApi,
    val preference: LuckyInsideSharedPreference
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return MapMainViewModel(regionApi, storeApi, luckyUApi, cityApi, alarmApi, popupApi, preference) as T
    }
}