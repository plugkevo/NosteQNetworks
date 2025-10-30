package com.example.nosteq


import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
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
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
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
