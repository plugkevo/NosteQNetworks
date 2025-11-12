package com.example.nosteq

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// API Service interface for Packages endpoints
interface PackagesApiService {

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

    @POST("mpesa")
    fun processMpesaPayment(
        @Header("Authorization") token: String,
        @Body rechargeRequest: RechargeRequest
    ): Call<MpesaResponse>
}

// Packages API Client with dedicated base URL
object PackagesApiClient {

    private const val BASE_URL = "https://nosteq.phpradius.com/index.php/api/"

    val instance: PackagesApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .registerTypeAdapter(PaymentGateway::class.java, PaymentGatewayDeserializer())
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(PackagesApiService::class.java)
    }
}
