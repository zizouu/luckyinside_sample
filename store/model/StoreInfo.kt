package com.lhbros.luckyinside.store.model

import java.io.Serializable

data class StoreInfo(
    var storeInfoName: String? = null,
    var description: String? = null,
    var storeInfoUrl: String? = null
) : Serializable