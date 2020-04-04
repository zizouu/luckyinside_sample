package com.lhbros.luckyinside.luckydraw.model

import java.io.Serializable

data class ProductCaution(
    var productIndex: Int? = null,
    var caution: String? = null
) : Serializable{
}