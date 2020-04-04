package com.lhbros.luckyinside.store.model

import com.lhbros.luckyinside.store.model.Store
import java.io.Serializable

data class Stores(
    val regionIndex: Int? = null,
    val storeList: List<Store> = listOf()
) : Serializable