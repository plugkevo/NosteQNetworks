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
}