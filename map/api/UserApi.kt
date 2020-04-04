package com.lhbros.luckyinside.map.api

import com.lhbros.luckyinside.common.model.LocalResponse
import com.lhbros.luckyinside.myitem.model.MyItem
import com.lhbros.luckyinside.myitem.model.MyItempage
import com.lhbros.luckyinside.setting.model.Change
import com.lhbros.luckyinside.setting.model.Leave
import com.lhbros.luckyinside.setting.model.LeaveReason
import io.reactivex.Flowable
import io.reactivex.Observable
import retrofit2.http.*

interface UserApi {
    @PUT("change/nickName")
    fun modifyNickname(@Body change: Change) : Observable<LocalResponse<Any>>

    @PUT("change/password")
    fun modifyPassword(@Body change: Change) : Observable<LocalResponse<Any>>

    @PUT("change/profileImage")
    fun modifyProfileImage(@Body change: Change) : Observable<LocalResponse<Any>>

    @GET("change/check/password")
    fun checkPassword(@Query("encPassword") password: String) : Observable<LocalResponse<Any>>

    @POST("change/password/email")
    fun sendCertEmail(@Body change: Change) : Observable<LocalResponse<Change>>

    @GET("change/check/validKey")
    fun checkValidKey(@Query("key") key: String) : Observable<LocalResponse<Change>>

    @POST("change/terms")
    fun modifyTerms(@Body change: Change): Flowable<LocalResponse<Any>>

    @GET("leave/reason")
    fun getLeaveReason() : Observable<LocalResponse<List<LeaveReason>>>

    @PUT("leave")
    fun leaveAccount(@Body leave: Leave) : Observable<LocalResponse<Any>>

    @POST("change/push/{pushReceptYn}")
    fun modifyPushReceptYn(@Path("pushReceptYn") pushReceptYn: String): Flowable<LocalResponse<Any>>
}