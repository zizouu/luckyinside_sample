package com.lhbros.luckyinside.map.model

import com.lhbros.luckyinside.luckyu.model.LuckyU
import com.lhbros.luckyinside.store.model.Store
import java.io.Serializable

data class Venues(
    var storeList: List<Store> = listOf(),
    var luckyuList: List<LuckyU> = listOf()
) : Serializable