package com.lhbros.luckyinside.store.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lhbros.luckyinside.common.api.LogApi
import com.lhbros.luckyinside.mystore.api.MyApi
import com.lhbros.luckyinside.store.api.StoreApi

class StoreDetailViewModelFactory(
    val storeApi: StoreApi,
    val logApi: LogApi,
    val myApi: MyApi
) : ViewModelProvider.Factory{
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return StoreDetailViewModel(
            storeApi,
            logApi,
            myApi
        ) as T
    }
}