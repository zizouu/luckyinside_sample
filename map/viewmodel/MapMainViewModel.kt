package com.lhbros.luckyinside.map.viewmodel

import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.LuckyInsideSharedPreference
import com.lhbros.luckyinside.common.api.AlarmApi
import com.lhbros.luckyinside.common.api.CityApi
import com.lhbros.luckyinside.common.exception.UnAuthorizedTokenException
import com.lhbros.luckyinside.common.viewmodel.BaseViewModel
import com.lhbros.luckyinside.launch.model.Landing
import com.lhbros.luckyinside.luckyu.api.LuckyUApi
import com.lhbros.luckyinside.map.api.*
import com.lhbros.luckyinside.popup.model.PopupNotice
import com.lhbros.luckyinside.map.model.Region
import com.lhbros.luckyinside.map.model.Venues
import com.lhbros.luckyinside.popup.api.PopupApi
import com.lhbros.luckyinside.signin.model.User
import com.lhbros.luckyinside.store.api.StoreApi
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class MapMainViewModel(
    val regionApi: RegionApi,
    val storeApi: StoreApi,
    val luckyUApi: LuckyUApi,
    val cityApi: CityApi,
    val alarmApi: AlarmApi,
    val popupApi: PopupApi,
    val preference: LuckyInsideSharedPreference
) : BaseViewModel() {
    lateinit var user: User
    var regions = PublishSubject.create<List<Region>>()
    var venues = PublishSubject.create<Venues>()
    val popupNotice = PublishSubject.create<PopupNotice>()
    var landing: Landing? = null

    var isRefreshFinished: Boolean? = null
    val isRefreshError = PublishSubject.create<Boolean>()

    val updateUser = BehaviorSubject.create<User>()

    fun requestRegions() =
        regionApi.getRegions()
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> regions.onNext(it.resultData ?: listOf())
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }
    fun requestVenues() =
        regionApi.getRegionVenues(0)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> venues.onNext(it.resultData ?: Venues(listOf(), listOf())
                    )
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestReadNotificationPush(pushIndex: Int) =
        alarmApi.updateNotificationAlarm(pushIndex)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> {
                        throw UnAuthorizedTokenException()
                    }
                }
            }) {
                if (it is UnAuthorizedTokenException) {
                    isUnAuthorizedToken.onNext(true)
                }
            }

    fun requestReadRegularPush(pushIndex: Int) =
        alarmApi.updateRegularAlarm(pushIndex)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> {
                        throw UnAuthorizedTokenException()
                    }
                }
            }) {
                if (it is UnAuthorizedTokenException) {
                    isUnAuthorizedToken.onNext(true)
                }
            }

    fun requestGetPopupNotice() =
        popupApi.getPopup()
            .filter { it.resultCode == LuckyInsideConstant.API_CODE_SUCCESS }
            .subscribe({
                val data = it.resultData
                if (data != null) {
                    popupNotice.onNext(data)
                }
            }) {
                // nothing
            }

    fun loadIsSawMapHelp() = preference.isSawMapHelp
    fun updateIsSawMapHelp() = preference.updateIsSawMapHelpView(true)
    fun loadIsSawMyStoreHelp() = preference.isSawMyStoreHelp
    fun updateIsSawMyStoreHelp() = preference.updateIsSawMyStoreHelpView(true)
    fun loadIsSawMyItemHelp() = preference.isSawMyItemHelp
    fun updateIsSawMyItemHelp() = preference.updateIsSawMyItemHelpView(true)
    fun loadIsSawLuckyUHelp() = preference.isSawLuckyUHelp
    fun updateIsSawLuckyUHelp() = preference.updateIsSawLuckyUHelpView(true)
    fun loadIsSawNavigationHelp() = preference.isSawNavigationHelp
    fun updateIsSawNavigationHelp() = preference.updateIsSawNavigationHelpView(true)
    fun loadIsSawStoreHelp() = preference.isSawStoreHelp
    fun updateIsSawStoreHelp() = preference.updateIsSawStoreHelpView(true)
    fun loadIsSawGetHelp() = preference.isSawGetHelp
    fun updateIsSawGetHelp() = preference.updateIsSawGetHelpView(true)

    fun removeTokens() = preference.removeTokens()
}