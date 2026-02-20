package com.kevannTechnologies.nosteqCustomers

import com.google.gson.GsonBuilder
import com.kevannTechnologies.nosteqCustomers.models.MpesaResponse
import com.kevannTechnologies.nosteqCustomers.models.PaymentGateway
import com.kevannTechnologies.nosteqCustomers.models.PaymentGatewayDeserializer
import com.kevannTechnologies.nosteqCustomers.models.RechargePlansResponse
import com.kevannTechnologies.nosteqCustomers.models.RechargeRequest
import com.kevannTechnologies.nosteqCustomers.models.RechargeResponse
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// API Service interface for Packages endpoints
interface PackagesApiService {

    @GET("recharge/{user_id}")
    fun getRechargePlans(
        @Path("user_id") userId: Int,
        @Header("Authorization") token: String
    ): Call<RechargePlansResponse>

    @FormUrlEncoded
    @POST("recharge/5") // Use the path from your docs
    fun processRecharge(
        @Header("Authorization") token: String,
        @Field("rechargePlan") rechargePlan: Int,
        @Field("quantity") quantity: Int = 1,
        @Field("rechargeType") rechargeType: Int = 1,
        @Field("resetRecharge") resetRecharge: Int = 0, // Using Int (0/1) often works better for PHP
        @Field("contactPerson") contactPerson: String,
        @Field("email") email: String,
        @Field("phone") phone: String,
        @Field("city") city: String,
        @Field("zip") zip: String
    ): Call<okhttp3.ResponseBody>

    @POST("mpesa")
    fun processMpesaActivation(
        @Header("Authorization") token: String,
        @Body rechargeRequest: RechargeRequest
    ): Call<okhttp3.ResponseBody>
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
            .setLenient() // <--- ADD THIS LINE HERE
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(PackagesApiService::class.java)
    }
}
