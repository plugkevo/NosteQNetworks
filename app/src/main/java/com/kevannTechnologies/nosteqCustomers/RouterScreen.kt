package com.kevannTechnologies.nosteqCustomers

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.kevannTechnologies.nosteqCustomers.ui.theme.NosteqTheme
import com.nosteq.provider.utils.PreferencesManager
import com.kevannTechnologies.nosteqCustomers.models.OnuDetails
import com.kevannTechnologies.nosteqCustomers.models.OnuStatus
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RouterScreen(
    username: String
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    var onuExternalId by remember { mutableStateOf<String?>(null) }
    var onuSerialNumber by remember { mutableStateOf<String?>(null) }
    var onuStatus by remember { mutableStateOf<OnuStatus?>(null) }
    var onuDetails by remember { mutableStateOf<OnuDetails?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var selectedGraphType by remember { mutableStateOf("daily") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(username) {
        if (username.isBlank()) {
            errorMessage = "Please log in again"
            AppLogger.logError("RouterScreen: Username is blank")
            return@LaunchedEffect
        }



        isLoading = true
        errorMessage = null

        try {
            val cachedId = preferencesManager.getOnuExternalId()

            if (cachedId != null) {
                onuExternalId = cachedId
                AppLogger.logInfo("RouterScreen: Using cached ONU ID", mapOf("onuExternalId" to cachedId))
            } else {
                AppLogger.logInfo("RouterScreen: Fetching ONU list", mapOf("username" to username))

                val allOnusResponse = SmartOltClient.apiService.getAllOnusDetails(
                    apiKey = SmartOltConfig.API_KEY
                )

                AppLogger.logApiCall(
                    endpoint = "getAllOnusDetails",
                    success = allOnusResponse.isSuccessful,
                    responseCode = allOnusResponse.code(),
                    errorMessage = if (!allOnusResponse.isSuccessful) "Failed to fetch ONUs" else null
                )

                if (allOnusResponse.isSuccessful && allOnusResponse.body()?.status == true) {
                    val allOnus = allOnusResponse.body()?.onus ?: emptyList()

                    val matchingOnu = allOnus.firstOrNull { onu ->
                        onu.username?.equals(username, ignoreCase = true) == true
                    }

                    if (matchingOnu != null) {
                        onuExternalId = matchingOnu.uniqueExternalId
                        onuSerialNumber = matchingOnu.sn
                        preferencesManager.saveOnuExternalId(matchingOnu.uniqueExternalId)
                        AppLogger.logInfo("RouterScreen: ONU found", mapOf(
                            "username" to username,
                            "onuExternalId" to matchingOnu.uniqueExternalId,
                            "onuSerialNumber" to matchingOnu.sn
                        ))
                    } else {
                        errorMessage = "No device found for your account. Please contact support"
                        AppLogger.logError("RouterScreen: No ONU found for username", null, mapOf("username" to username))
                        isLoading = false
                        return@LaunchedEffect
                    }
                } else {
                    val statusCode = allOnusResponse.code()
                    errorMessage = if (statusCode == 403) {
                        "Access denied. Please contact support"
                    } else {
                        "Unable to load device information. Please try again"
                    }

                    isLoading = false
                    return@LaunchedEffect
                }
            }
        } catch (e: Exception) {
            errorMessage = "Network error. Please check your connection and try again"
            AppLogger.logError("RouterScreen: Exception fetching ONU list", e, mapOf("username" to username))
            isLoading = false
            return@LaunchedEffect
        }
    }

    LaunchedEffect(onuSerialNumber) {
        if (onuSerialNumber == null) return@LaunchedEffect

        isLoading = true
        errorMessage = null

        try {
            try {
                android.util.Log.d("RouterScreen", "[v0] Fetching ONU details by SN: $onuSerialNumber")
                AppLogger.logInfo("RouterScreen: Fetching ONU details by SN", mapOf("onuSerialNumber" to onuSerialNumber!!))

                val detailsResponse = SmartOltClient.apiService.getOnuDetailsBySn(
                    sn = onuSerialNumber!!,
                    apiKey = SmartOltConfig.API_KEY
                )

                android.util.Log.d("RouterScreen", "[v0] getOnuDetailsBySn response code: ${detailsResponse.code()}")
                AppLogger.logApiCall(
                    endpoint = "getOnuDetailsBySn",
                    success = detailsResponse.isSuccessful,
                    responseCode = detailsResponse.code()
                )

                if (detailsResponse.isSuccessful && detailsResponse.body()?.status == true) {
                    val onuList = detailsResponse.body()?.onus ?: emptyList()
                    onuDetails = onuList.firstOrNull()?.let { onu ->
                        OnuDetails(
                            name = onu.name,
                            sn = onu.sn,
                            uniqueExternalId = onu.uniqueExternalId,
                            oltId = onu.oltId,
                            oltName = onu.oltName,
                            board = onu.board,
                            port = onu.port,
                            onu = onu.onu,
                            onuTypeId = "",
                            onuTypeName = onu.onuTypeName ?: "Unknown",
                            zoneId = "",
                            zoneName = onu.zoneName,
                            address = onu.address,
                            odbName = onu.odbName,
                            mode = null,
                            wanMode = null,
                            ipAddress = null,
                            subnetMask = null,
                            defaultGateway = null,
                            dns1 = null,
                            dns2 = null,
                            username = onu.username,
                            password = null,
                            catv = null,
                            administrativeStatus = null,
                            servicePorts = null
                        )
                    }
                    android.util.Log.d("RouterScreen", "[v0] ONU details retrieved successfully")

                    onuDetails?.let { details ->
                        try {
                            android.util.Log.d("RouterScreen", "[v0] Fetching ONU status for olt_id=${details.oltId}, board=${details.board}, port=${details.port}")

                            val statusResponse = SmartOltClient.apiService.getOnuStatuses(
                                apiKey = SmartOltConfig.API_KEY,
                                oltId = details.oltId.toIntOrNull(),
                                board = details.board.toIntOrNull(),
                                port = details.port.toIntOrNull()
                            )

                            android.util.Log.d("RouterScreen", "[v0] getOnuStatuses response code: ${statusResponse.code()}")
                            AppLogger.logApiCall(
                                endpoint = "getOnuStatuses",
                                success = statusResponse.isSuccessful,
                                responseCode = statusResponse.code()
                            )

                            if (statusResponse.isSuccessful && statusResponse.body()?.status == true) {
                                onuStatus = statusResponse.body()?.response?.firstOrNull()
                                android.util.Log.d("RouterScreen", "[v0] ONU status retrieved: ${onuStatus?.status}")
                            } else {
                                android.util.Log.e("RouterScreen", "[v0] Failed to fetch ONU status: ${statusResponse.code()}")
                                // Status fetch failure is not critical - continue with details only
                                if (statusResponse.code() != 403 && statusResponse.code() != 401) {
                                    // Only log, don't set error for non-auth failures
                                    AppLogger.logError("RouterScreen: ONU status fetch failed (non-critical)", null, mapOf("statusCode" to statusResponse.code().toString()))
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("RouterScreen", "[v0] Failed to fetch ONU status: ${e.message}", e)
                            AppLogger.logError("RouterScreen: Failed to fetch ONU status", e)
                        }
                    }
                } else {
                    val statusCode = detailsResponse.code()
                    android.util.Log.e("RouterScreen", "[v0] Failed to fetch ONU details: $statusCode")
                    android.util.Log.e("RouterScreen", "[v0] Response body: ${detailsResponse.errorBody()?.string()}")

                    errorMessage = when (statusCode) {
                        403 -> {
                            AppLogger.logError("RouterScreen: API returned 403 Forbidden", null,
                                mapOf(
                                    "endpoint" to "getOnuDetailsBySn",
                                    "sn" to onuSerialNumber!!,
                                    "apiKeyLength" to SmartOltConfig.API_KEY.length.toString()
                                )
                            )
                            "Access denied. Please verify your API configuration and try again"
                        }
                        400 -> "Invalid device serial number. Please contact support"
                        401 -> {
                            AppLogger.logError("RouterScreen: API returned 401 Unauthorized", null,
                                mapOf(
                                    "endpoint" to "getOnuDetailsBySn",
                                    "apiKeyConfigured" to (SmartOltConfig.API_KEY != "YOUR_API_KEY_HERE").toString()
                                )
                            )
                            "Authentication failed. Please contact support"
                        }
                        else -> "Unable to load device information. Please try again"
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("RouterScreen", "[v0] Exception fetching ONU details: ${e.message}", e)
                android.util.Log.e("RouterScreen", "[v0] Exception type: ${e.javaClass.simpleName}")
                errorMessage = "Network error. Please check your connection and try again"
                AppLogger.logError("RouterScreen: Exception fetching ONU details by SN", e, mapOf("onuSerialNumber" to onuSerialNumber!!))
            }
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(onuExternalId) {
        if (onuExternalId == null) return@LaunchedEffect

        // Skip if we already fetched details via serial number
        if (onuDetails != null) {
            android.util.Log.d("RouterScreen", "[v0] Skipping external ID fetch - already have details from serial number")
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        try {
            try {
                AppLogger.logInfo("RouterScreen: Fetching ONU details (fallback)", mapOf("onuExternalId" to onuExternalId!!))

                val detailsResponse = SmartOltClient.apiService.getOnuDetails(
                    onuExternalId = onuExternalId!!,
                    apiKey = SmartOltConfig.API_KEY
                )

                AppLogger.logApiCall(
                    endpoint = "getOnuDetails",
                    success = detailsResponse.isSuccessful,
                    responseCode = detailsResponse.code()
                )

                if (detailsResponse.isSuccessful && detailsResponse.body()?.status == true) {
                    onuDetails = detailsResponse.body()?.onuDetails

                    onuDetails?.let { details ->
                        try {
                            val statusResponse = SmartOltClient.apiService.getOnuStatuses(
                                apiKey = SmartOltConfig.API_KEY,
                                oltId = details.oltId.toIntOrNull(),
                                board = details.board.toIntOrNull(),
                                port = details.port.toIntOrNull()
                            )

                            AppLogger.logApiCall(
                                endpoint = "getOnuStatuses",
                                success = statusResponse.isSuccessful,
                                responseCode = statusResponse.code()
                            )

                            if (statusResponse.isSuccessful && statusResponse.body()?.status == true) {
                                onuStatus = statusResponse.body()?.response?.firstOrNull()
                            }
                        } catch (e: Exception) {
                            AppLogger.logError("RouterScreen: Failed to fetch ONU status (fallback)", e)
                        }
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("RouterScreen: Failed to fetch ONU details (fallback)", e, mapOf("onuExternalId" to onuExternalId!!))
            }
        } finally {
            isLoading = false
        }
    }

    fun rebootOnu() {
        scope.launch {
            try {
                AppLogger.logInfo("RouterScreen: Rebooting ONU", mapOf("onuExternalId" to onuExternalId!!))

                val response = SmartOltClient.apiService.rebootOnu(
                    onuExternalId = onuExternalId!!,
                    apiKey = SmartOltConfig.API_KEY
                )

                AppLogger.logApiCall(
                    endpoint = "rebootOnu",
                    success = response.isSuccessful && response.body()?.status == true,
                    responseCode = response.code()
                )

                if (response.isSuccessful && response.body()?.status == true) {
                    snackbarHostState.showSnackbar(
                        message = "Device reboot command sent successfully",
                        duration = SnackbarDuration.Short
                    )
                } else {
                    snackbarHostState.showSnackbar(
                        message = "Unable to reboot device. Please try again",
                        duration = SnackbarDuration.Short
                    )
                }
            } catch (e: Exception) {
                AppLogger.logError("RouterScreen: Reboot failed", e)
                snackbarHostState.showSnackbar(
                    message = "Network error. Please check your connection",
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Text(error)
                    }
                }
            }

            onuDetails?.let { details ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Device Details",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        StatusRow("Name", details.name)
                        StatusRow("Serial Number", details.sn)
                        StatusRow("External ID", details.uniqueExternalId)
                    }
                }
            }

            onuStatus?.let { status ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Router Status",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp)),
                                color = if (status.status.equals("online", ignoreCase = true))
                                    Color(0xFF4CAF50)
                                else
                                    Color(0xFFEF5350)
                            ) {
                                Text(
                                    text = status.status.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        StatusRow("Name", status.name)
                        status.signal?.let { StatusRow("Signal", it) }
                        status.rxPower?.let { StatusRow("RX Power", it) }
                        status.txPower?.let { StatusRow("TX Power", it) }
                        status.distance?.let { StatusRow("Distance", it) }
                        status.temperature?.let { StatusRow("Temperature", it) }
                        status.voltage?.let { StatusRow("Voltage", it) }

                        Button(
                            onClick = { showRebootDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Reboot Device")
                        }
                    }
                }
            }

            onuExternalId?.let { externalId ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Traffic Graph",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Divider(
                            modifier = Modifier.fillMaxWidth(),
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("hourly", "daily", "weekly", "monthly", "yearly").forEach { type ->
                                FilterChip(
                                    selected = selectedGraphType == type,
                                    onClick = { selectedGraphType = type },
                                    label = { Text(type.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }

                        val graphUrl = "${SmartOltConfig.getBaseUrl()}onu/get_onu_traffic_graph/$externalId/$selectedGraphType"

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(graphUrl)
                                    .addHeader("X-Token", SmartOltConfig.API_KEY)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "$selectedGraphType traffic graph",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text("Confirm Reboot") },
            text = { Text("Are you sure you want to reboot your device? This will temporarily disconnect your internet connection.") },
            confirmButton = {
                Button(
                    onClick = {
                        showRebootDialog = false
                        rebootOnu()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reboot")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRebootDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusRow(label: String, value: String?) {
    if (value == null) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RouterScreenPreview() {
    NosteqTheme {
        RouterScreen(
            username = "testuser"
        )
    }
}
