package com.kevannTechnologies.nosteqCustomers.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kevannTechnologies.nosteqCustomers.AppLogger
import com.kevannTechnologies.nosteqCustomers.models.OnuDetails
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class OnuRepository {
    private val firestore = FirebaseFirestore.getInstance()

    /**
     * Fetch all ONU details for a given username
     * Handles multiple ONUs per user
     */
    suspend fun fetchAllOnusByUsername(username: String): Result<List<OnuDetails>> {
        return suspendCoroutine { continuation ->
            if (username.isBlank()) {
                continuation.resume(Result.failure(Exception("Username is blank")))
                return@suspendCoroutine
            }

            AppLogger.logInfo("OnuRepository: Fetching all ONUs from Firebase", mapOf("username" to username))

            firestore.collection("onus_cache")
                .whereEqualTo("username", username)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    try {
                        if (querySnapshot.documents.isNotEmpty()) {
                            val onuList = querySnapshot.documents.map { onuDocument ->
                                val externalId = onuDocument.getString("unique_external_id")
                                    ?: onuDocument.getString("uniqueExternalId")

                                Log.d("OnuRepository", "[v0] Mapping ONU - Name: ${onuDocument.getString("name")}, ExternalId: $externalId")

                                OnuDetails(
                                    name = onuDocument.getString("name"),
                                    sn = onuDocument.getString("sn"),
                                    uniqueExternalId = externalId,
                                    oltId = onuDocument.getString("olt_id") ?: onuDocument.getString("oltId"),
                                    oltName = onuDocument.getString("olt_name") ?: onuDocument.getString("oltName"),
                                    board = onuDocument.getString("board"),
                                    port = onuDocument.getString("port"),
                                    onu = onuDocument.getString("onu"),
                                    onuTypeId = onuDocument.getString("onu_type_id") ?: onuDocument.getString("onuTypeId") ?: "",
                                    onuTypeName = onuDocument.getString("onu_type_name") ?: onuDocument.getString("onuTypeName") ?: "Unknown",
                                    zoneId = onuDocument.getString("zone_id") ?: onuDocument.getString("zoneId") ?: "",
                                    zoneName = onuDocument.getString("zone_name") ?: onuDocument.getString("zoneName"),
                                    address = onuDocument.getString("address"),
                                    odbName = onuDocument.getString("odb_name") ?: onuDocument.getString("odbName"),
                                    mode = onuDocument.getString("mode"),
                                    wanMode = onuDocument.getString("wan_mode") ?: onuDocument.getString("wanMode"),
                                    ipAddress = onuDocument.getString("ip_address") ?: onuDocument.getString("ipAddress"),
                                    subnetMask = onuDocument.getString("subnet_mask") ?: onuDocument.getString("subnetMask"),
                                    defaultGateway = onuDocument.getString("default_gateway") ?: onuDocument.getString("defaultGateway"),
                                    dns1 = onuDocument.getString("dns1"),
                                    dns2 = onuDocument.getString("dns2"),
                                    username = onuDocument.getString("username"),
                                    password = null,
                                    catv = onuDocument.getString("catv"),
                                    administrativeStatus = onuDocument.getString("administrative_status") ?: onuDocument.getString("administrativeStatus"),
                                    servicePorts = null
                                )
                            }

                            AppLogger.logInfo("OnuRepository: ${onuList.size} ONU(s) loaded from Firebase", mapOf(
                                "username" to username,
                                "count" to onuList.size.toString()
                            ))

                            continuation.resume(Result.success(onuList))
                        } else {
                            val error = "No device found for your account. Please contact support"
                            AppLogger.logError("OnuRepository: No ONU found in Firebase for username", null, mapOf("username" to username))
                            continuation.resume(Result.failure(Exception(error)))
                        }
                    } catch (e: Exception) {
                        Log.e("OnuRepository", "[v0] Error mapping Firebase documents: ${e.message}", e)
                        AppLogger.logError("OnuRepository: Error mapping Firebase documents", e)
                        continuation.resume(Result.failure(e))
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("OnuRepository", "[v0] Firebase query error: ${exception.message}", exception)
                    AppLogger.logError("OnuRepository: Firebase query failed", exception, mapOf("username" to username))
                    continuation.resume(Result.failure(exception))
                }
        }
    }
}
