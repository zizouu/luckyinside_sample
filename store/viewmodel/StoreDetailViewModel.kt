package com.lhbros.luckyinside.store.viewmodel

import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.api.LogApi
import com.lhbros.luckyinside.common.model.LuckyInsideLog
import com.lhbros.luckyinside.common.viewmodel.BaseViewModel
import com.lhbros.luckyinside.mystore.api.MyApi
import com.lhbros.luckyinside.store.api.StoreApi
import com.lhbros.luckyinside.store.model.Store
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import java.lang.Exception

class StoreDetailViewModel(
    val storeApi: StoreApi,
    val logApi: LogApi,
    val myApi: MyApi
) : BaseViewModel() {
    val storeDetail: BehaviorSubject<Store> = BehaviorSubject.create()
    val isMyStoreRegistSuccess = PublishSubject.create<Boolean>()
    val isMyStoreDeleteSuccess = PublishSubject.create<Boolean>()

    val unAuthorizedTokenException = PublishSubject.create<Boolean>()

    fun requestStoreDetail(storeIndex: Int) =
            storeApi.getStore(storeIndex)
                .subscribe({
                    when (it.resultCode) {
                        LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> unAuthorizedTokenException.onNext(true)
                        LuckyInsideConstant.API_CODE_SUCCESS -> storeDetail.onNext(it.resultData!!)
                        else -> throw Exception("${it.resultMsg}")
                    }
                }){
                    message.onNext("${it.message}")
                }

    fun requestLogNavigation(log: LuckyInsideLog) =
        logApi.loggingExceuteNavigation(log)
                .subscribe({
                    // nothing
                }){
                    // nothing
                }

    fun requestRegistMyStore() =
            myApi.postMyStore(storeDetail.value!!.getStoreInputModel())
                .subscribe({
                    when (it.resultCode) {
                        LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> unAuthorizedTokenException.onNext(true)
                        LuckyInsideConstant.API_CODE_SUCCESS -> isMyStoreRegistSuccess.onNext(true)
                        else -> throw Exception("${it.resultMsg}")
                    }
                }) {
                    message.onNext("${it.message}")
                    isMyStoreRegistSuccess.onNext(false)
                }

    fun requestDeleteMyStore() =
            myApi.deleteMyStore(storeDetail.value!!.storeIndex!!)
                .subscribe({
                    when (it.resultCode) {
                        LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> unAuthorizedTokenException.onNext(true)
                        LuckyInsideConstant.API_CODE_SUCCESS -> isMyStoreDeleteSuccess.onNext(true)
                        else -> throw Exception("${it.resultMsg}")
                    }
                }) {
                    message.onNext("${it.message}")
                    isMyStoreDeleteSuccess.onNext(false)
                }

    fun getIsMyStore() = storeDetail.value!!.myStoreRegistrationYn == LuckyInsideConstant.COMMON_YN_Y
}