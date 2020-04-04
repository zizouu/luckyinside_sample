package com.lhbros.luckyinside.luckydraw.viewmodel

import com.lhbros.luckyinside.common.constant.LuckyInsideConstant
import com.lhbros.luckyinside.common.viewmodel.BaseViewModel
import com.lhbros.luckyinside.daily.api.DailyApi
import com.lhbros.luckyinside.luckydraw.model.LuckyDraw
import io.reactivex.subjects.BehaviorSubject

class LuckyDrawViewModel(
    val dailyApi: DailyApi
) : BaseViewModel() {
    var luckyDrawIndex: Int? = null
    val products = BehaviorSubject.create<List<LuckyDraw>>()

    fun requestProducts(luckyDrawIndex: Int) =
        dailyApi.getGetProducts(luckyDrawIndex)
            .subscribe({
                when (it.resultCode) {
                    LuckyInsideConstant.API_ACCESSTOKEN_UNATHORIZED -> isUnAuthorizedToken.onNext(true)
                    LuckyInsideConstant.API_CODE_SUCCESS -> products.onNext(it.resultData ?: listOf())
                    else -> error.onNext(true)
                }
            }) {
                error.onNext(true)
            }
}