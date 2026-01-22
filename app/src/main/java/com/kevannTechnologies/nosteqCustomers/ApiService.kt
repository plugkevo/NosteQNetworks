package com.kevannTechnologies.nosteqCustomers


import com.kevannTechnologies.nosteqCustomers.models.ChangePasswordRequest
import com.kevannTechnologies.nosteqCustomers.models.ChangePasswordResponse
import com.kevannTechnologies.nosteqCustomers.models.DashboardResponse
import com.kevannTechnologies.nosteqCustomers.models.InvoiceResponse
import com.kevannTechnologies.nosteqCustomers.models.LoginResponse
import com.kevannTechnologies.nosteqCustomers.models.OltListResponse
import com.kevannTechnologies.nosteqCustomers.models.OnuStatusResponse
import com.kevannTechnologies.nosteqCustomers.models.RebootResponse
import com.kevannTechnologies.nosteqCustomers.models.ReceiptResponse

import com.kevannTechnologies.nosteqCustomers.models.SpeedProfilesResponse
import com.kevannTechnologies.nosteqCustomers.models.UserResponse
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