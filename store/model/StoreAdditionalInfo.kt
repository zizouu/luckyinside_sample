package com.lhbros.luckyinside.store.model

import java.io.Serializable

data class StoreAdditionalInfo(
    val storeIndex: Int? = null,
    val additionInfo: String? = null
) : Serializable