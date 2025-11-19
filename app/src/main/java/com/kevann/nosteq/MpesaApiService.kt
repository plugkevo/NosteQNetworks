package com.kevann.nosteq

import android.util.Base64
import com.google.gson.annotations.SerializedName
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// M-Pesa OAuth Token Response
data class MpesaTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("expires_in") val expiresIn: String
)

// M-Pesa STK Push Request
data class MpesaStkPushRequest(
    @SerializedName("BusinessShortCode") val BusinessShortCode: String,
    @SerializedName("Password") val Password: String,
    @SerializedName("Timestamp") val Timestamp: String,
    @SerializedName("TransactionType") val TransactionType: String = "CustomerPayBillOnline",
    @SerializedName("Amount") val Amount: String,
    @SerializedName("PartyA") val PartyA: String,
    @SerializedName("PartyB") val PartyB: String,
    @SerializedName("PhoneNumber") val PhoneNumber: String,
    @SerializedName("CallBackURL") val CallBackURL: String,
    @SerializedName("AccountReference") val AccountReference: String,
    @SerializedName("TransactionDesc") val TransactionDesc: String
)

// M-Pesa STK Push Response
data class MpesaStkPushResponse(
    @SerializedName("MerchantRequestID") val merchantRequestID: String?,
    @SerializedName("CheckoutRequestID") val checkoutRequestID: String?,
    @SerializedName("ResponseCode") val responseCode: String?,
    @SerializedName("ResponseDescription") val responseDescription: String?,
    @SerializedName("CustomerMessage") val customerMessage: String?,
    @SerializedName("errorCode") val errorCode: String?,
    @SerializedName("errorMessage") val errorMessage: String?
)

// M-Pesa STK Push Query Request
data class MpesaStkQueryRequest(
    @SerializedName("BusinessShortCode") val BusinessShortCode: String,
    @SerializedName("Password") val Password: String,
    @SerializedName("Timestamp") val Timestamp: String,
    @SerializedName("CheckoutRequestID") val CheckoutRequestID: String
)

// M-Pesa STK Push Query Response
data class MpesaStkQueryResponse(
    @SerializedName("ResponseCode") val responseCode: String?,
    @SerializedName("ResponseDescription") val responseDescription: String?,
    @SerializedName("MerchantRequestID") val merchantRequestID: String?,
    @SerializedName("CheckoutRequestID") val checkoutRequestID: String?,
    @SerializedName("ResultCode") val resultCode: String?,
    @SerializedName("ResultDesc") val resultDesc: String?,
    @SerializedName("errorCode") val errorCode: String?,
    @SerializedName("errorMessage") val errorMessage: String?
)

interface MpesaApiService {
    @GET("oauth/v1/generate?grant_type=client_credentials")
    suspend fun getAccessToken(
        @Header("Authorization") authorization: String
    ): Response<MpesaTokenResponse>

    @POST("mpesa/stkpush/v1/processrequest")
    suspend fun stkPush(
        @Header("Authorization") authorization: String,
        @Body request: MpesaStkPushRequest
    ): Response<MpesaStkPushResponse>

    @POST("mpesa/stkpushquery/v1/query")
    suspend fun stkPushQuery(
        @Header("Authorization") authorization: String,
        @Body request: MpesaStkQueryRequest
    ): Response<MpesaStkQueryResponse>
}

object MpesaApiClient {
    private var retrofit: Retrofit? = null

    fun getInstance(baseUrl: String): MpesaApiService {
        if (retrofit == null || retrofit?.baseUrl().toString() != baseUrl) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }

        return retrofit!!.create(MpesaApiService::class.java)
    }
}

class MpesaManager(private val config: MpesaConfig) {
    private val apiService = MpesaApiClient.getInstance(config.environment.baseUrl)

    private suspend fun getAccessToken(): String? {
        return try {
            val credentials = Credentials.basic(config.consumerKey, config.consumerSecret)
            val response = apiService.getAccessToken(credentials)

            if (response.isSuccessful) {
                response.body()?.accessToken
            } else {
                android.util.Log.e("MpesaManager", "Token Error: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MpesaManager", "Token Exception: ${e.message}")
            null
        }
    }

    private fun generatePassword(timestamp: String): String {
        val str = "${config.businessShortCode}${config.passkey}$timestamp"
        return Base64.encodeToString(str.toByteArray(), Base64.NO_WRAP)
    }

    private fun getTimestamp(): String {
        val dateFormat = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
        return dateFormat.format(Date())
    }

    suspend fun initiateStkPush(
        phoneNumber: String,
        amount: String,
        accountReference: String,
        transactionDesc: String
    ): MpesaStkPushResponse? {
        return try {
            val token = getAccessToken()
            if (token == null) {
                android.util.Log.e("MpesaManager", "Failed to get access token")
                return null
            }

            val timestamp = getTimestamp()
            val password = generatePassword(timestamp)

            val request = MpesaStkPushRequest(
                BusinessShortCode = config.businessShortCode,
                Password = password,
                Timestamp = timestamp,
                Amount = amount,
                PartyA = phoneNumber,
                PartyB = config.businessShortCode,
                PhoneNumber = phoneNumber,
                CallBackURL = config.callbackUrl,
                AccountReference = accountReference,
                TransactionDesc = transactionDesc
            )

            android.util.Log.d("MpesaManager", "[v0] Initiating STK Push:")
            android.util.Log.d("MpesaManager", "[v0] Phone: $phoneNumber")
            android.util.Log.d("MpesaManager", "[v0] Amount: $amount")
            android.util.Log.d("MpesaManager", "[v0] BusinessShortCode: ${config.businessShortCode}")

            val response = apiService.stkPush("Bearer $token", request)

            if (response.isSuccessful) {
                android.util.Log.d("MpesaManager", "[v0] STK Push Success: ${response.body()}")
                response.body()
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("MpesaManager", "[v0] STK Push Error: ${response.code()}")
                android.util.Log.e("MpesaManager", "[v0] Error Body: $errorBody")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MpesaManager", "[v0] STK Push Exception: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    suspend fun queryStkPushStatus(checkoutRequestID: String): MpesaStkQueryResponse? {
        return try {
            val token = getAccessToken()
            if (token == null) {
                android.util.Log.e("MpesaManager", "Failed to get access token for query")
                return null
            }

            val timestamp = getTimestamp()
            val password = generatePassword(timestamp)

            val request = MpesaStkQueryRequest(
                BusinessShortCode = config.businessShortCode,
                Password = password,
                Timestamp = timestamp,
                CheckoutRequestID = checkoutRequestID
            )

            android.util.Log.d("MpesaManager", "[v0] Querying STK Push Status:")
            android.util.Log.d("MpesaManager", "[v0] CheckoutRequestID: $checkoutRequestID")

            val response = apiService.stkPushQuery("Bearer $token", request)

            if (response.isSuccessful) {
                android.util.Log.d("MpesaManager", "[v0] Query Success: ${response.body()}")
                response.body()
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.e("MpesaManager", "[v0] Query Error: ${response.code()}")
                android.util.Log.e("MpesaManager", "[v0] Error Body: $errorBody")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MpesaManager", "[v0] Query Exception: ${e.message}")
            e.printStackTrace()
            null
        }
    }
}
