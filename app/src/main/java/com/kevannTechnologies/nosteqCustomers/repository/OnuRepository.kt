package com.kevannTechnologies.nosteqCustomers.repository

import android.util.Log
import com.kevannTechnologies.nosteqCustomers.AppLogger
import com.kevannTechnologies.nosteqCustomers.SmartOltClient
import com.kevannTechnologies.nosteqCustomers.SmartOltConfig
import com.kevannTechnologies.nosteqCustomers.models.OnuDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class OnuRepository {

    /**
     * Fetch all ONU details for a given username from the backend API
     * This ensures we always get fresh data, not cached data
     */
    suspend fun fetchAllOnusByUsername(username: String): Result<List<OnuDetails>> {
        return withContext(Dispatchers.IO) {
            try {
                if (username.isBlank()) {
                    return@withContext Result.failure(Exception("Username is blank"))
                }

                AppLogger.logInfo("OnuRepository: Fetching all ONUs from backend API", mapOf("username" to username))

                val response = SmartOltClient.apiService.getUserOnus(
                    username = username,
                    apiKey = SmartOltConfig.API_KEY
                )

                if (response.isSuccessful && response.body()?.status == true) {
                    val onuListData = response.body()?.onus ?: emptyList()
                    
                    val onuList = onuListData.map { onuData ->
                        OnuDetails(
                            name = onuData.name,
                            sn = onuData.sn,
                            uniqueExternalId = onuData.uniqueExternalId,
                            oltId = onuData.oltId,
                            oltName = onuData.oltName,
                            board = onuData.board,
                            port = onuData.port,
                            onu = onuData.onu,
                            onuTypeId = onuData.onuTypeId ?: "",
                            onuTypeName = onuData.onuTypeName ?: "Unknown",
                            zoneId = onuData.zoneId ?: "",
                            zoneName = onuData.zoneName,
                            address = onuData.address,
                            odbName = onuData.odbName,
                            mode = onuData.mode,
                            wanMode = onuData.wanMode,
                            ipAddress = onuData.ipAddress,
                            subnetMask = onuData.subnetMask,
                            defaultGateway = onuData.defaultGateway,
                            dns1 = onuData.dns1,
                            dns2 = onuData.dns2,
                            username = onuData.username,
                            password = null,
                            catv = onuData.catv,
                            administrativeStatus = onuData.administrativeStatus,
                            servicePorts = null
                        )
                    }

                    AppLogger.logInfo("OnuRepository: ${onuList.size} ONU(s) loaded from backend API", mapOf(
                        "username" to username,
                        "count" to onuList.size.toString()
                    ))

                    if (onuList.isEmpty()) {
                        val error = "No device found for your account. Please contact support"
                        AppLogger.logError("OnuRepository: No ONU found in backend for username", null, mapOf("username" to username))
                        Result.failure(Exception(error))
                    } else {
                        Result.success(onuList)
                    }
                } else {
                    val error = "Failed to fetch ONUs: HTTP ${response.code()}"
                    AppLogger.logError("OnuRepository: API call failed", null, mapOf("username" to username, "code" to response.code().toString()))
                    Result.failure(Exception(error))
                }
            } catch (e: Exception) {
                Log.e("OnuRepository", "[v0] Error fetching ONUs from backend: ${e.message}", e)
                AppLogger.logError("OnuRepository: Error fetching from backend API", e, mapOf("username" to username))
                Result.failure(e)
            }
        }
    }
}
