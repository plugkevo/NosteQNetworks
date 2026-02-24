package com.kevannTechnologies.nosteqCustomers.repository



import android.util.Log
import com.kevannTechnologies.nosteqCustomers.AppLogger
import com.kevannTechnologies.nosteqCustomers.SmartOltClient
import com.kevannTechnologies.nosteqCustomers.SmartOltConfig
import com.kevannTechnologies.nosteqCustomers.models.OnuStatus


class OnuStatusRepository {

    suspend fun fetchOnuStatus(onuExternalId: String?): Result<String?> {
        return try {
            if (onuExternalId.isNullOrBlank()) {
                return Result.failure(Exception("ONU External ID is required"))
            }

            Log.d("OnuStatusRepository", "[v0] Fetching ONU status for externalId=$onuExternalId")

            val statusResponse = SmartOltClient.apiService.getOnuStatus(
                onuExternalId = onuExternalId,
                apiKey = SmartOltConfig.API_KEY
            )

            Log.d("OnuStatusRepository", "[v0] getOnuStatus response code: ${statusResponse.code()}")

            AppLogger.logApiCall(
                endpoint = "getOnuStatus",
                success = statusResponse.isSuccessful,
                responseCode = statusResponse.code()
            )

            if (statusResponse.isSuccessful && statusResponse.body()?.status == true) {
                val onuStatus = statusResponse.body()?.onuStatus
                Log.d("OnuStatusRepository", "[v0] ONU status retrieved: $onuStatus")
                Result.success(onuStatus)
            } else {
                Log.e("OnuStatusRepository", "[v0] Failed to fetch ONU status: ${statusResponse.code()}")
                AppLogger.logError("OnuStatusRepository: ONU status fetch failed", null, mapOf("statusCode" to statusResponse.code().toString()))
                Result.failure(Exception("Failed to fetch ONU status - Code: ${statusResponse.code()}"))
            }
        } catch (e: Exception) {
            Log.e("OnuStatusRepository", "[v0] Failed to fetch ONU status: ${e.message}", e)
            AppLogger.logError("OnuStatusRepository: Failed to fetch ONU status", e)
            Result.failure(e)
        }
    }
}