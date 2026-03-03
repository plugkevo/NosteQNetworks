package com.kevannTechnologies.nosteqCustomers

import android.util.Log


object WiFiCredentialManager {

    suspend fun changeWiFiCredentials(
        onuExternalId: String?,
        newSSID: String,
        newPassword: String
    ): Result<String> {
        return try {
            // Validate inputs
            if (newSSID.isBlank()) {
                return Result.failure(Exception("WiFi name (SSID) cannot be empty"))
            }
            if (newSSID.length > 32) {
                return Result.failure(Exception("WiFi name (SSID) must be 32 characters or less"))
            }
            if (newPassword.length < 8 || newPassword.length > 64) {
                return Result.failure(Exception("Password must be 8-64 characters"))
            }

            Log.d("WiFiCredentialManager", "[v0] Setting WiFi credentials for ONU: $onuExternalId")

            // WiFi ports are logical identifiers, independent of physical ports
            // wifi_0/1 = Radio 0 (2.4GHz), SSID 1
            // wifi_1/1 = Radio 1 (5GHz), SSID 1
            val wifiPorts = listOf("wifi_0/1", "wifi_1/1")

            Log.d("WiFiCredentialManager", "[v0] Trying WiFi ports: $wifiPorts")

            // Try each WiFi port until one succeeds
            for (wifiPort in wifiPorts) {
                Log.d("WiFiCredentialManager", "[v0] Attempting WiFi port: $wifiPort")

                try {
                    val response = SmartOltClient.apiService.setWiFiCredentials(
                        onuExternalId = onuExternalId,
                        wifiPort = wifiPort,
                        ssid = newSSID,
                        password = newPassword,
                        apiKey = SmartOltConfig.API_KEY
                    )

                    Log.d("WiFiCredentialManager", "[v0] Response code: ${response.code()}")

                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        Log.d("WiFiCredentialManager", "[v0] Success with port $wifiPort: ${responseBody?.response}")
                        return Result.success("WiFi credentials updated successfully")
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "Unknown error"
                        Log.d("WiFiCredentialManager", "[v0] Port $wifiPort failed: $errorBody")
                        // Continue trying next radio
                    }
                } catch (e: Exception) {
                    Log.d("WiFiCredentialManager", "[v0] Port $wifiPort timeout/error: ${e.message}")
                    // Continue trying next port
                    continue
                }
            }

            // If all ports failed, return error
            Result.failure(Exception("Unable to update WiFi credentials. Please contact support."))
        } catch (e: Exception) {
            Log.e("WiFiCredentialManager", "[v0] Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
