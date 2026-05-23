package com.kevannTechnologies.nosteqCustomers

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object LANManager {

    suspend fun enableLan(
        onuExternalId: String,
        ethernetPort: String = "eth_0/1"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("LANManager", "Enabling LAN for ONU: $onuExternalId, port: $ethernetPort")

            val response = SmartOltClient.apiService.setEthernetPortLan(
                onuExternalId = onuExternalId,
                ethernetPort = ethernetPort,
                dhcp = "No control",
                apiKey = SmartOltConfig.API_KEY
            )

            if (response.isSuccessful && response.body()?.status == true) {
                val message = response.body()?.response ?: "LAN enabled successfully"
                Log.d("LANManager", "Enable LAN success: $message")
                Result.success(message)
            } else {
                val error = "HTTP ${response.code()}: ${response.message()}"
                Log.e("LANManager", "Enable LAN failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e("LANManager", "Error enabling LAN", e)
            Result.failure(e)
        }
    }

    suspend fun disableLan(
        onuExternalId: String,
        ethernetPort: String = "eth_0/1"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Log.d("LANManager", "Disabling LAN for ONU: $onuExternalId, port: $ethernetPort")

            val response = SmartOltClient.apiService.shutdownEthernetPort(
                onuExternalId = onuExternalId,
                ethernetPort = ethernetPort,
                apiKey = SmartOltConfig.API_KEY
            )

            if (response.isSuccessful && response.body()?.status == true) {
                val message = response.body()?.response ?: "LAN disabled successfully"
                Log.d("LANManager", "Disable LAN success: $message")
                Result.success(message)
            } else {
                val error = "HTTP ${response.code()}: ${response.message()}"
                Log.e("LANManager", "Disable LAN failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Log.e("LANManager", "Error disabling LAN", e)
            Result.failure(e)
        }
    }

    suspend fun enableAllLanPorts(
        onuExternalId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val failedPorts = mutableListOf<Int>()

            for (portNum in 1..4) {
                val result = enableLan(
                    onuExternalId = onuExternalId,
                    ethernetPort = "eth_0/$portNum"
                )

                result.onFailure {
                    failedPorts.add(portNum)
                }
            }

            if (failedPorts.isEmpty()) {
                Log.d("LANManager", "All LAN ports enabled successfully")
                Result.success("LAN ports 1-4 enabled successfully")
            } else {
                val errorMsg = "LAN ports enabled, but failed on ports: $failedPorts"
                Log.e("LANManager", errorMsg)
                Result.success(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("LANManager", "Error enabling all LAN ports", e)
            Result.failure(e)
        }
    }

    suspend fun disableAllLanPorts(
        onuExternalId: String
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val failedPorts = mutableListOf<Int>()

            for (portNum in 1..4) {
                val result = disableLan(
                    onuExternalId = onuExternalId,
                    ethernetPort = "eth_0/$portNum"
                )

                result.onFailure {
                    failedPorts.add(portNum)
                }
            }

            if (failedPorts.isEmpty()) {
                Log.d("LANManager", "All LAN ports disabled successfully")
                Result.success("LAN ports 1-4 disabled successfully")
            } else {
                val errorMsg = "LAN ports disabled, but failed on ports: $failedPorts"
                Log.e("LANManager", errorMsg)
                Result.success(errorMsg)
            }
        } catch (e: Exception) {
            Log.e("LANManager", "Error disabling all LAN ports", e)
            Result.failure(e)
        }
    }
}
