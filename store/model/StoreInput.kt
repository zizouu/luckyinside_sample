package com.lhbros.luckyinside.store.model

import java.io.Serializable

class StoreInput(
    val storeIndex: Int? = null,
    val userIndex: Int? = null,
    val regionIndex: Int? = null,
    val storeType: String? = null,
    val heart: Int? = null,
    val eventIndex: Int? = null,
    val productIndex: Int? = null,
    val acquisitionType: String? = null,
    val variation: String? = null,
    val type: String? = null
) : Serializable {
}