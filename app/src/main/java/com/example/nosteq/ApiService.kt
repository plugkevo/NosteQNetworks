package com.example.nosteq


import com.example.nosteq.models.ChangePasswordRequest
import com.example.nosteq.models.ChangePasswordResponse
import com.example.nosteq.models.DashboardResponse
import com.example.nosteq.models.InvoiceResponse
import com.example.nosteq.models.LoginResponse
import com.example.nosteq.models.OltListResponse
import com.example.nosteq.models.OnuStatusResponse
import com.example.nosteq.models.RebootResponse
import com.example.nosteq.models.ReceiptResponse

import com.example.nosteq.models.SpeedProfilesResponse
import com.example.nosteq.models.UserResponse
import retrofit2.Call
import retrofit2.http.*

interface ApiService {

    @FormUrlEncoded
    @POST("api/login")
    fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): Call<LoginResponse>

    @GET("api/userDetail")
    fun getUserDetails(
        @Header("Authorization") token: String
    ): Call<UserResponse>

    @GET("api/dashboard/1")
    fun getDashboard(
        @Header("Authorization") token: String
    ): Call<DashboardResponse>

    @POST("api/password")
    fun changePassword(
        @Header("Authorization") token: String,
        @Body request: ChangePasswordRequest
    ): Call<ChangePasswordResponse>

    @GET("api/invoice")
    fun getInvoices(
        @Header("Authorization") token: String,
        @Query("from") fromDate: String,
        @Query("to") toDate: String
    ): Call<InvoiceResponse>

    @GET("api/receipt")
    fun getReceipts(
        @Header("Authorization") token: String,
        @Query("from") fromDate: String,
        @Query("to") toDate: String
    ): Call<ReceiptResponse>

    //  Added SmartOLT router management endpoints
    @POST("api/router/reboot")
    fun rebootRouter(
        @Header("Authorization") token: String
    ): Call<RebootResponse>

    @GET("api/router/status")
    fun getRouterStatus(
        @Header("Authorization") token: String
    ): Call<OnuStatusResponse>

    @GET("api/router/speed-profiles")
    fun getSpeedProfiles(
        @Header("Authorization") token: String
    ): Call<SpeedProfilesResponse>

    @GET("api/router/olts")
    fun getOlts(
        @Header("Authorization") token: String
    ): Call<OltListResponse>

    @POST("loginPhone")
    @Headers("Accept: application/json")
    fun login(
        @Body loginRequest: LoginRequest
    ): Call<LoginResponse>

    @GET("recharge/{user_id}")
    fun getRechargePlans(
        @Path("user_id") userId: Int,
        @Header("Authorization") token: String
    ): Call<RechargePlansResponse>

    @POST("processRecharge")
    fun processRecharge(
        @Header("Authorization") token: String,
        @Body rechargeRequest: RechargeRequest
    ): Call<RechargeResponse>
}