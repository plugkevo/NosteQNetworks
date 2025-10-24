package com.example.nosteq



import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface SmartOltApiService {

    @POST("onu/reboot/{onu_external_id}")
    suspend fun rebootOnu(
        @Path("onu_external_id") onuExternalId: String,
        @Header("X-Token") apiKey: String
    ): Response<RebootResponse>

    @GET("onu/get_onus_statuses")
    suspend fun getOnuStatuses(
        @Header("X-Token") apiKey: String,
        @Query("olt_id") oltId: Int? = null,
        @Query("board") board: Int? = null,
        @Query("port") port: Int? = null,
        @Query("zone") zone: String? = null
    ): Response<OnuStatusResponse>

    @GET("system/get_speed_profiles")
    suspend fun getSpeedProfiles(
        @Header("X-Token") apiKey: String
    ): Response<SpeedProfilesResponse>

    @GET("system/get_olts")
    suspend fun getOlts(
        @Header("X-Token") apiKey: String
    ): Response<OltListResponse>


    @GET("onu/get_onu_details/{onu_external_id}")
    suspend fun getOnuDetails(
        @Path("onu_external_id") onuExternalId: String,
        @Header("X-Token") apiKey: String
    ): Response<OnuDetailsResponse>

    @GET("onu/get_all_onus_details")
    suspend fun getAllOnusDetails(
        @Header("X-Token") apiKey: String,
        @Query("olt_id") oltId: Int? = null,
        @Query("board") board: Int? = null,
        @Query("port") port: Int? = null,
        @Query("zone") zone: String? = null,
        @Query("odb") odb: String? = null
    ): Response<AllOnusDetailsResponse>

    @Streaming
    @GET("onu/get_onu_traffic_graph/{onu_external_id}/{graph_type}")
    suspend fun getOnuTrafficGraph(
        @Path("onu_external_id") onuExternalId: String,
        @Path("graph_type") graphType: String,
        @Header("X-Token") apiKey: String
    ): Response<ResponseBody>
}

object SmartOltClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl(SmartOltConfig.getBaseUrl())
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: SmartOltApiService = retrofit.create(SmartOltApiService::class.java)
}
