package com.lhbros.luckyinside.daily.viewmodel

import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.viewmodel.BaseViewModel
import com.lhbros.luckyinside.daily.model.DailyVenue
import com.lhbros.luckyinside.daily.api.DailyApi
import com.lhbros.luckyinside.luckyu.model.LuckyUInput
import com.lhbros.luckyinside.mystore.api.MyApi
import com.lhbros.luckyinside.store.model.StoreInput
import io.reactivex.subjects.PublishSubject

class DailyVenueListViewModel(
    val dailyApi: DailyApi,
    val myApi: MyApi
) : BaseViewModel() {
    var regionIndex: Int? = null
    val venues = PublishSubject.create<List<DailyVenue>>()

    fun requestRegistMyStore(index: Int) =
        myApi.postMyStore(StoreInput(storeIndex = index))
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> {}
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestRegistMyLuckyU(index: Int) =
        myApi.postMyLuckyU(LuckyUInput(luckyuIndex = index))
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> {}
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestDeleteMyStore(index: Int) =
        myApi.deleteMyStore(index)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> {}
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestDeleteMyLuckyU(index: Int) =
        myApi.deleteMyLuckyU(index)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> {}
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestFreeExperienceVenues(regionIndex: Int) =
        dailyApi.getOnGoingVenues(regionIndex)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> venues.onNext(it.resultData ?: listOf())
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }
}