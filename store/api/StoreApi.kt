package com.lhbros.luckyinside.store.api

import com.lhbros.luckyinside.common.model.LocalResponse
import com.lhbros.luckyinside.store.model.Store
import com.lhbros.luckyinside.store.model.StoreInput
import com.lhbros.luckyinside.store.model.Stores
import io.reactivex.Flowable
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface StoreApi {
    @GET("store/{storeIndex}")
    fun getStore(@Path("storeIndex") storeIndex: Int) : Flowable<LocalResponse<Store>>

    @GET("region/store/{regionIndex}")
    fun getRegionStore(@Path("regionIndex") regionIndex: Int) : Observable<LocalResponse<Stores>>

    @POST("store/get/product")
    fun getStoreProduct(@Body store: StoreInput) : Observable<LocalResponse<Any>>

}