package com.lhbros.luckyinside.luckydraw.model

import com.lhbros.luckyinside.common.model.ExchangePlaceInfo
import java.io.Serializable

data class LuckyDraw(
    var luckyDrawIndex: Int? = null,
    var productName: String? = null,
    var productIndex: Int? = null,
    var dailyGetCnt: Int? = null,
    var topShowYn: String? = null,
    var productImageUrl1: String? = null,
    var productImageUrl2: String? = null,
    var productImageUrl3: String? = null,
    var productImageUrl4: String? = null,
    var regionName: String? = null,
    var categoryIndex: Int? = null,
    var categoryName: String? = null,
    var categoryCode: String? = null,
    var introduceComment: String? = null,
    var address: String? = null,
    var addressDetail: String? = null,
    var exchangePlaceName: String? = null,
    var exchangePlaceIndex: Int? = null,
    var possibleGetYn: String? = null,
    var regionIndex: Int? = null,
    var regionImageUrl: String? = null,
    var categoryImageUrl: String? = null,
    var categoryFiveSensesIndex: Int? = null,
    var categoryFiveSensesName: String? = null,
    var categoryFiveSensesCode: String? = null,
    var categoryFiveSenseImageUrl: String? = null,
    var categoryEmotionIndex: Int? = null,
    var categoryEmotionName: String? = null,
    var categoryEmotionCode: String? = null,
    var categoryEmotionImageUrl: String? = null,
    var productCautionList: List<ProductCaution>? = null,
    var exchangePlaceInfo: List<ExchangePlaceInfo>? = null,
    var price: String? = null,
    var discountPrice: String? = null,
    var discountPercent: String? = null,
    var couponUseTime: String? = null,
    var couponType: String? = null,
    var useLimit: String? = null,
    var provisionMethod: String? = null,
    var provisionHour: String? = null
) : Serializable {
}