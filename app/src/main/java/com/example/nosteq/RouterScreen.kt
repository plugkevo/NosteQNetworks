package com.example.nosteq

import android.util.Log
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
import com.example.nosteq.ui.theme.NosteqTheme
import com.nosteq.provider.utils.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouterScreen(
    username: String
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    var onuExternalId by remember { mutableStateOf<String?>(null) }
    var onuStatus by remember { mutableStateOf<OnuStatus?>(null) }
    var onuDetails by remember { mutableStateOf<OnuDetails?>(null) }
    var olts by remember { mutableStateOf<List<Olt>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var selectedGraphType by remember { mutableStateOf("daily") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(username) {
        // Validate username
        if (username.isBlank()) {
            errorMessage = "Username not found. Please log in again."
            Log.e("RouterScreen", "Username is blank")
            return@LaunchedEffect
        }

        // Validate SmartOLT configuration
        if (SmartOltConfig.SUBDOMAIN == "YOUR_SUBDOMAIN_HERE" ||
            SmartOltConfig.API_KEY == "YOUR_API_KEY_HERE") {
            errorMessage = "SmartOLT not configured. Please contact support."
            Log.e("RouterScreen", "SmartOLT credentials not configured")
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        Log.d("RouterScreen", "Starting ONU lookup for username: $username")

        try {
            // Check if we already have a cached ONU external ID
            val cachedId = preferencesManager.getOnuExternalId()

            if (cachedId != null) {
                onuExternalId = cachedId
                Log.d("RouterScreen", "Using cached ONU external ID: $cachedId")
            } else {
                Log.d("RouterScreen", "No cached ONU ID found, fetching from SmartOLT API...")

                // Fetch all ONUs and find the one matching the username
                val allOnusResponse = SmartOltClient.apiService.getAllOnusDetails(
                    apiKey = SmartOltConfig.API_KEY
                )

                if (allOnusResponse.isSuccessful && allOnusResponse.body()?.status == true) {
                    val allOnus = allOnusResponse.body()?.onus ?: emptyList()

                    Log.d("RouterScreen", "Fetched ${allOnus.size} ONUs from SmartOLT")
                    Log.d("RouterScreen", "Searching for username: $username")

                    // Log all usernames for debugging
                    allOnus.forEach { onu ->
                        Log.d("RouterScreen", "ONU: ${onu.name}, Username: ${onu.username}")
                    }

                    // Find ONU where username matches the logged-in user's username
                    val matchingOnu = allOnus.firstOrNull { onu ->
                        onu.username?.equals(username, ignoreCase = true) == true
                    }

                    if (matchingOnu != null) {
                        onuExternalId = matchingOnu.uniqueExternalId
                        Log.d("RouterScreen", "✓ Found matching ONU!")
                        Log.d("RouterScreen", "  - ONU Name: ${matchingOnu.name}")
                        Log.d("RouterScreen", "  - Username: ${matchingOnu.username}")
                        Log.d("RouterScreen", "  - ONU External ID: ${matchingOnu.uniqueExternalId}")
                        Log.d("RouterScreen", "  - ONU Serial: ${matchingOnu.sn}")

                        // Cache the external ID for future use
                        preferencesManager.saveOnuExternalId(matchingOnu.uniqueExternalId)
                        Log.d("RouterScreen", "Cached ONU external ID for future use")
                    } else {
                        errorMessage = "No ONU found for your account. Please contact support."
                        Log.e("RouterScreen", "✗ No matching ONU found for username: $username")
                        Log.e("RouterScreen", "Available usernames: ${allOnus.mapNotNull { it.username }.joinToString()}")
                        isLoading = false
                        return@LaunchedEffect
                    }
                } else {
                    val errorBody = allOnusResponse.errorBody()?.string()
                    val statusCode = allOnusResponse.code()
                    errorMessage = "Failed to fetch ONUs: ${allOnusResponse.message()}"

                    Log.e("RouterScreen", "API error: ${allOnusResponse.message()}")
                    Log.e("RouterScreen", "Status code: $statusCode")
                    Log.e("RouterScreen", "Error body: $errorBody")
                    Log.e("RouterScreen", "SmartOLT Config - Subdomain: ${SmartOltConfig.SUBDOMAIN}, Base URL: ${SmartOltConfig.getBaseUrl()}")

                    if (statusCode == 403) {
                        errorMessage = "Access forbidden. Please check:\n1. API key is correct\n2. Device IP is whitelisted in SmartOLT"
                    }

                    isLoading = false
                    return@LaunchedEffect
                }
            }
        } catch (e: Exception) {
            errorMessage = "Error finding ONU: ${e.message}"
            Log.e("RouterScreen", "Exception while finding ONU", e)
            isLoading = false
            return@LaunchedEffect
        }
    }

    LaunchedEffect(onuExternalId) {
        if (onuExternalId == null) return@LaunchedEffect

        Log.d("RouterScreen", "Fetching ONU data using external ID: $onuExternalId")

        isLoading = true
        errorMessage = null

        try {
            // Fetch ONU details first
            try {
                val detailsResponse = SmartOltClient.apiService.getOnuDetails(
                    onuExternalId = onuExternalId!!,
                    apiKey = SmartOltConfig.API_KEY
                )

                if (detailsResponse.isSuccessful && detailsResponse.body()?.status == true) {
                    onuDetails = detailsResponse.body()?.onuDetails
                    Log.d("RouterScreen", "Successfully fetched ONU details")

                    // Now fetch ONU status using the OLT ID, board, and port from onuDetails
                    onuDetails?.let { details ->
                        try {
                            val statusResponse = SmartOltClient.apiService.getOnuStatuses(
                                apiKey = SmartOltConfig.API_KEY,
                                oltId = details.oltId.toIntOrNull(),
                                board = details.board.toIntOrNull(),
                                port = details.port.toIntOrNull()
                            )

                            if (statusResponse.isSuccessful && statusResponse.body()?.status == true) {
                                onuStatus = statusResponse.body()?.response?.firstOrNull()
                                Log.d("RouterScreen", "Successfully fetched ONU status")
                            } else {
                                Log.w("RouterScreen", "Failed to fetch ONU status: ${statusResponse.message()}")
                            }
                        } catch (e: Exception) {
                            Log.e("RouterScreen", "Error fetching ONU status", e)
                        }
                    }
                } else {
                    Log.w("RouterScreen", "Failed to fetch ONU details: ${detailsResponse.message()}")
                }
            } catch (e: Exception) {
                Log.e("RouterScreen", "Error fetching ONU details", e)
            }

            // Fetch OLTs
            try {
                val oltsResponse = SmartOltClient.apiService.getOlts(SmartOltConfig.API_KEY)
                if (oltsResponse.isSuccessful && oltsResponse.body()?.status == true) {
                    olts = oltsResponse.body()?.response ?: emptyList()
                    Log.d("RouterScreen", "Successfully fetched ${olts.size} OLTs")
                } else {
                    Log.w("RouterScreen", "Failed to fetch OLTs: ${oltsResponse.message()}")
                }
            } catch (e: Exception) {
                Log.e("RouterScreen", "Error fetching OLTs", e)
            }

        } catch (e: Exception) {
            errorMessage = "Failed to load data: ${e.message}"
            Log.e("RouterScreen", "Exception while loading data", e)
        } finally {
            isLoading = false
        }
    }

    // Reboot function
    fun rebootOnu() {
        scope.launch {
            Log.d("RouterScreen", "Attempting to reboot ONU: $onuExternalId")

            try {
                val response = SmartOltClient.apiService.rebootOnu(
                    onuExternalId = onuExternalId!!,
                    apiKey = SmartOltConfig.API_KEY
                )

                if (response.isSuccessful && response.body()?.status == true) {
                    Log.d("RouterScreen", "✓ Reboot command sent successfully")
                    snackbarHostState.showSnackbar(
                        message = "Device reboot command sent successfully",
                        duration = SnackbarDuration.Short
                    )
                } else {
                    Log.e("RouterScreen", "✗ Reboot failed: ${response.message()}")
                    snackbarHostState.showSnackbar(
                        message = "Failed to reboot device",
                        duration = SnackbarDuration.Short
                    )
                }
            } catch (e: Exception) {
                Log.e("RouterScreen", "Exception during reboot", e)
                snackbarHostState.showSnackbar(
                    message = "Error: ${e.message}",
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

            // ONU Details Card
            onuDetails?.let { details ->
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "ONU Details",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Divider()

                        StatusRow("Name", details.name)
                        StatusRow("Serial Number", details.sn)
                        StatusRow("External ID", details.uniqueExternalId)
                        StatusRow("OLT", details.oltName)
                        StatusRow("ONU Type", details.onuTypeName)
                        StatusRow("Zone", details.zoneName)
                        StatusRow("Board/Port/ONU", "${details.board}/${details.port}/${details.onu}")
                        details.mode?.let { StatusRow("Mode", it) }
                        details.administrativeStatus?.let { StatusRow("Status", it) }
                        details.odbName?.let { StatusRow("ODB", it) }

                        // Service Ports
                        details.servicePorts?.let { ports ->
                            if (ports.isNotEmpty()) {
                                Divider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = "Service Ports",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                ports.forEach { port ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            StatusRow("Port", port.servicePort)
                                            StatusRow("VLAN", port.vlan)
                                            StatusRow("Upload", port.uploadSpeed)
                                            StatusRow("Download", port.downloadSpeed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ONU Status Card
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
                                    Color(0xFF4CAF50) // Green for online
                                else
                                    Color(0xFFEF5350) // Red for offline
                            ) {
                                Text(
                                    text = status.status.uppercase(),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Divider()

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
                            Text("Reboot Router")
                        }
                    }
                }
            }

            // Traffic Graph Card
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

                        Divider()

                        // Graph type selector
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("hourly","daily","weekly","monthly","yearly").forEach { type ->
                                FilterChip(
                                    selected = selectedGraphType == type,
                                    onClick = { selectedGraphType = type },
                                    label = { Text(type.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }

                        // Graph image
                        val graphUrl = "${SmartOltConfig.getBaseUrl()}onu/get_onu_traffic_graph/$externalId/$selectedGraphType"

                        Log.d("RouterScreen", "Loading traffic graph: $graphUrl")

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
                                modifier = Modifier.fillMaxSize(),
                                onError = { error ->
                                    Log.e("RouterScreen", "Failed to load traffic graph: ${error.result.throwable.message}")
                                },
                                onSuccess = {
                                    Log.d("RouterScreen", "Successfully loaded traffic graph")
                                }
                            )
                        }
                    }
                }
            }

            // OLT Information Card
            if (olts.isNotEmpty()) {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "OLT Information",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Divider()

                        olts.forEach { olt ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StatusRow("OLT ID", olt.id)
                                StatusRow("Name", olt.name)
                                StatusRow("Hardware", olt.oltHardwareVersion)
                                StatusRow("IP Address", olt.ip)
                                StatusRow("Telnet Port", olt.telnetPort)
                                StatusRow("SNMP Port", olt.snmpPort)

                                if (olts.indexOf(olt) < olts.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Reboot Confirmation Dialog
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
