package com.lhbros.luckyinside.luckydraw.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lhbros.luckyinside.daily.api.DailyApi

class LuckyDrawViewModelFactory(
    val dailyApi: DailyApi
) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return LuckyDrawViewModel(dailyApi) as T
    }
}