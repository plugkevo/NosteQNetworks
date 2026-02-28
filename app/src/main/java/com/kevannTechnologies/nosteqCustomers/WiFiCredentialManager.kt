package com.kevannTechnologies.nosteqCustomers


import android.util.Log


object WiFiCredentialManager {
    suspend fun changeWiFiCredentials(
        onuExternalId: String?,
        newUsername: String,
        newPassword: String
    ): Result<String> {
        return try {
            // Validate inputs
            if (newUsername.length < 5 || newUsername.length > 16) {
                return Result.failure(Exception("Username must be 5-16 characters"))
            }
            if (newPassword.length < 8 || newPassword.length > 16) {
                return Result.failure(Exception("Password must be 8-16 characters"))
            }
            if (!newUsername.matches(Regex("^[a-zA-Z0-9]*$"))) {
                return Result.failure(Exception("Username must contain only alphanumeric characters"))
            }
            if (!newPassword.matches(Regex("^[a-zA-Z0-9]*$"))) {
                return Result.failure(Exception("Password must contain only alphanumeric characters"))
            }

            Log.d("WiFiCredentialManager", "[v0] Changing WiFi credentials for ONU: $onuExternalId")

            val response = SmartOltClient.apiService.changeWiFiCredentials(
                onuExternalId = onuExternalId,
                webUser = newUsername,
                webPassword = newPassword,
                apiKey = SmartOltConfig.API_KEY
            )

            Log.d("WiFiCredentialManager", "[v0] Response code: ${response.code()}")

            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d("WiFiCredentialManager", "[v0] Success: ${responseBody?.response}")
                Result.success(responseBody?.response ?: "Credentials updated successfully")
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Log.e("WiFiCredentialManager", "[v0] Error: $errorBody")
                Result.failure(Exception(errorBody))
            }
        } catch (e: Exception) {
            Log.e("WiFiCredentialManager", "[v0] Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
