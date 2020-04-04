package com.lhbros.luckyinside.daily.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lhbros.luckyinside.daily.api.DailyApi
import com.lhbros.luckyinside.mystore.api.MyApi

class DailyViewModelFactory(
    val dailyApi: DailyApi,
    val myApi: MyApi
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return DailyViewModel(dailyApi, myApi) as T
    }
}