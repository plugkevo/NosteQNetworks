package com.kevannTechnologies.nosteqCustomers

import android.util.Log

object WiFiPortManager {

    suspend fun enableWiFi(
        onuExternalId: String,
        wifiPort: String = "wifi_0/1",
        ssid: String = "NosteqWiFi",
        password: String = "default123",
        authMode: String = "WPA2"
    ): Result<String> {
        return try {
            AppLogger.logInfo(
                "WiFiPortManager: Enabling WiFi",
                mapOf(
                    "onuExternalId" to onuExternalId,
                    "wifiPort" to wifiPort,
                    "ssid" to ssid
                )
            )

            val response = SmartOltClient.apiService.enableWiFiPort(
                onuExternalId = onuExternalId,
                wifiPort = wifiPort,
                dhcp = "No control",
                ssid = ssid,
                password = password,
                authMode = authMode,
                apiKey = SmartOltConfig.API_KEY
            )

            if (response.isSuccessful && response.body()?.status == true) {
                val message = response.body()?.response ?: "WiFi enabled successfully"
                AppLogger.logInfo("WiFiPortManager: Enable WiFi success", mapOf("message" to message))
                Result.success(message)
            } else {
                val errorMsg = "Failed to enable WiFi: ${response.code()} ${response.message()}"
                AppLogger.logError("WiFiPortManager: Enable WiFi failed", Exception(errorMsg))
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            AppLogger.logError("WiFiPortManager: Enable WiFi error", e)
            Result.failure(e)
        }
    }

    suspend fun disableWiFi(
        onuExternalId: String,
        wifiPort: String = "wifi_0/1"
    ): Result<String> {
        return try {
            AppLogger.logInfo(
                "WiFiPortManager: Disabling WiFi",
                mapOf(
                    "onuExternalId" to onuExternalId,
                    "wifiPort" to wifiPort
                )
            )

            val response = SmartOltClient.apiService.shutdownWiFiPort(
                onuExternalId = onuExternalId,
                wifiPort = wifiPort,
                apiKey = SmartOltConfig.API_KEY
            )

            if (response.isSuccessful && response.body()?.status == true) {
                val message = response.body()?.response ?: "WiFi disabled successfully"
                AppLogger.logInfo("WiFiPortManager: Disable WiFi success", mapOf("message" to message))
                Result.success(message)
            } else {
                val errorMsg = "Failed to disable WiFi: ${response.code()} ${response.message()}"
                AppLogger.logError("WiFiPortManager: Disable WiFi failed", Exception(errorMsg))
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            AppLogger.logError("WiFiPortManager: Disable WiFi error", e)
            Result.failure(e)
        }
    }
}
