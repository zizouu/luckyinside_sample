package com.lhbros.luckyinside.luckydraw.viewmodel

import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.viewmodel.BaseViewModel
import com.lhbros.luckyinside.daily.api.DailyApi
import com.lhbros.luckyinside.luckydraw.model.LuckyDraw
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class LuckyDrawDetailViewModel(
    val dailyApi: DailyApi
) : BaseViewModel(){
    var luckyDrawIndex: Int? = null
    var getProductIndex: Int? = null
    var isGetProcessing = false
    val product = BehaviorSubject.create<LuckyDraw>()
    val isGetProductGet = PublishSubject.create<Boolean>()
    val isGetProductSoldout = PublishSubject.create<Boolean>()

    fun requestGetProduct(luckyDrawIndex: Int, getProductIndex: Int) =
        dailyApi.getGetProduct(luckyDrawIndex, getProductIndex)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> product.onNext(it.resultData!!)
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }

    fun requestGetGetProduct(luckyDrawIndex: Int, getProductIndex: Int) =
        dailyApi.postGetProduct(luckyDrawIndex, getProductIndex)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_NOT_ENOUGH_PRODUCT -> isGetProductSoldout.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> isGetProductGet.onNext(true)
                    else -> isGetProductGet.onNext(false)
                }
            }) {
                isGetProductGet.onNext(false)
            }
}