package com.lhbros.luckyinside.daily.api

import com.lhbros.luckyinside.common.model.LocalResponse
import com.lhbros.luckyinside.daily.model.DailyBanner
import com.lhbros.luckyinside.daily.model.DailyVenue
import com.lhbros.luckyinside.luckydraw.model.LuckyDraw
import com.lhbros.luckyinside.map.model.*
import com.lhbros.luckyinside.signin.model.TodayLuckyDraw
import io.reactivex.Flowable
import io.reactivex.Observable
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface DailyApi {
    @GET("luckyDraw")
    fun getDailyBanners(): Flowable<LocalResponse<List<DailyBanner>>>
    @GET("luckyDraw/{luckyDrawIndex}")
    fun getGetProducts(@Path("luckyDrawIndex") luckyDrawIndex: Int): Flowable<LocalResponse<List<LuckyDraw>>>
    @GET("luckyDraw/{luckyDrawIndex}/{productIndex}")
    fun getGetProduct(@Path("luckyDrawIndex") luckyDrawIndex: Int, @Path("productIndex") productIndex: Int): Flowable<LocalResponse<LuckyDraw>>
    @GET("luckyDraw/list/{regionIndex}")
    fun getDailyVenues(@Path("regionIndex") regionIndex: Int): Flowable<LocalResponse<List<DailyVenue>>>
    @GET("luckyDraw/regionList")
    fun getDailyRegions(): Flowable<LocalResponse<List<Region>>>
    @POST("luckyDraw/{luckyDrawIndex}/{productIndex}")
    fun postGetProduct(@Path("luckyDrawIndex") luckyDrawIndex: Int, @Path("productIndex") productIndex: Int): Flowable<LocalResponse<Any>>
    @GET("luckyDraw/onGoingList/{regionIndex}")
    fun getOnGoingVenues(@Path("regionIndex") regionIndex: Int): Flowable<LocalResponse<List<DailyVenue>>>
    @GET("luckyDraw/todayGetYn")
    fun getTodayGetYn(): Observable<LocalResponse<TodayLuckyDraw>>
}
