package com.kevannTechnologies.nosteqCustomers


import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlin.jvm.java

object ApiClient {
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = GsonBuilder()
        .registerTypeAdapter(PaymentGateway::class.java, PaymentGatewayDeserializer())
        .create()

    private val retrofit = Retrofit.Builder()
        .baseUrl(ApiConfig.getBaseUrl())
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson)) // Use custom Gson instance
        .build()

    val instance: ApiService = retrofit.create(ApiService::class.java)
}
