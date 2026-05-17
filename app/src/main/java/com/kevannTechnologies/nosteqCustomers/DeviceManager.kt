package com.kevannTechnologies.nosteqCustomers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DeviceManager {

    suspend fun enableDevice(
        onuExternalId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            AppLogger.logInfo(
                "DeviceManager: Enabling device",
                mapOf("onuExternalId" to onuExternalId)
            )

            val response = SmartOltClient.apiService.enableOnu(
                onuExternalId = onuExternalId,
                apiKey = SmartOltConfig.API_KEY
            )

            if (response.isSuccessful && response.body()?.status == true) {
                val message = response.body()?.response ?: "Device enabled successfully"
                AppLogger.logInfo("DeviceManager: Enable device success", mapOf("message" to message))
                Result.success(message)
            } else {
                val errorMsg = "Failed to enable device: ${response.code()} ${response.message()}"
                AppLogger.logError("DeviceManager: Enable device failed", Exception(errorMsg))
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            AppLogger.logError("DeviceManager: Enable device error", e)
            Result.failure(e)
        }
    }

    suspend fun disableDevice(
        onuExternalId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            AppLogger.logInfo(
                "DeviceManager: Disabling device",
                mapOf("onuExternalId" to onuExternalId)
            )

            val response = SmartOltClient.apiService.disableOnu(
                onuExternalId = onuExternalId,
                apiKey = SmartOltConfig.API_KEY
            )

            if (response.isSuccessful && response.body()?.status == true) {
                val message = response.body()?.response ?: "Device disabled successfully"
                AppLogger.logInfo("DeviceManager: Disable device success", mapOf("message" to message))
                Result.success(message)
            } else {
                val errorMsg = "Failed to disable device: ${response.code()} ${response.message()}"
                AppLogger.logError("DeviceManager: Disable device failed", Exception(errorMsg))
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            AppLogger.logError("DeviceManager: Disable device error", e)
            Result.failure(e)
        }
    }
}
