package com.lhbros.luckyinside.map.api

import com.lhbros.luckyinside.common.model.LocalResponse
import com.lhbros.luckyinside.map.model.Region
import com.lhbros.luckyinside.map.model.Venues
import io.reactivex.Flowable
import retrofit2.http.GET
import retrofit2.http.Path

interface RegionApi {
    @GET("region")
    fun getRegions() : Flowable<LocalResponse<List<Region>>>
    @GET("region/{regionIndex}")
    fun getRegionVenues(@Path("regionIndex") regionIndex: Int) : Flowable<LocalResponse<Venues>>
}