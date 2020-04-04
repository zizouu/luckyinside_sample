package com.lhbros.luckyinside.store.model

import java.io.Serializable

data class StoreCaution(
    val storeIndex: Int? = null,
    val caution: String? = null
) : Serializable