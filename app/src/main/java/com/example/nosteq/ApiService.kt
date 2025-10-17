package com.example.nosteq


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
}