package com.lhbros.luckyinside.daily.viewmodel

import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.viewmodel.BaseViewModel
import com.lhbros.luckyinside.daily.model.DailyBanner
import com.lhbros.luckyinside.daily.model.DailyVenue
import com.lhbros.luckyinside.daily.api.DailyApi
import com.lhbros.luckyinside.luckyu.model.LuckyUInput
import com.lhbros.luckyinside.mystore.api.MyApi
import com.lhbros.luckyinside.map.model.*
import com.lhbros.luckyinside.store.model.StoreInput
import io.reactivex.subjects.PublishSubject

class DailyViewModel(
    val dailyApi: DailyApi,
    val myApi: MyApi
) : BaseViewModel() {
    val banners = PublishSubject.create<List<DailyBanner>>()
    val venues = PublishSubject.create<List<DailyVenue>>()
    val regions = PublishSubject.create<List<Region>>()
    var selectedRegionIndex: Int? = null

    fun requestBanners() =
        dailyApi.getDailyBanners()
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> banners.onNext(it.resultData ?: listOf())
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestDailyVenues(regionIndex: Int) =
        dailyApi.getDailyVenues(regionIndex)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> venues.onNext(it.resultData ?: listOf())
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestDailyRegions() =
        dailyApi.getDailyRegions()
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> regions.onNext(it.resultData ?: listOf())
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestRegistMyStore(index: Int) =
        myApi.postMyStore(StoreInput(storeIndex = index))
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestRegistMyLuckyU(index: Int) =
        myApi.postMyLuckyU(LuckyUInput(luckyuIndex = index))
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestDeleteMyStore(index: Int) =
        myApi.deleteMyStore(index)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestDeleteMyLuckyU(index: Int) =
        myApi.deleteMyLuckyU(index)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                }
            }) {
                error.onNext(true)
            }

}