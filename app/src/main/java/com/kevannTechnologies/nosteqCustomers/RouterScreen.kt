package com.kevannTechnologies.nosteqCustomers

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nosteq.provider.utils.PreferencesManager
import com.kevannTechnologies.nosteqCustomers.models.OnuDetails
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.layout.ContentScale
import com.kevannTechnologies.nosteqCustomers.repository.OnuRepository
import com.kevannTechnologies.nosteqCustomers.repository.OnuStatusRepository
import kotlinx.coroutines.launch




@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RouterScreen(
    username: String
) {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val onuRepository = remember { OnuRepository() }
    val onuStatusRepository = remember { OnuStatusRepository() }

    var onuList by remember { mutableStateOf<List<OnuDetails>>(emptyList()) }
    var selectedOnuIndex by remember { mutableStateOf(0) }
    var onuStatus by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var showWiFiDialog by remember { mutableStateOf(false) }
    var isChangingWiFi by remember { mutableStateOf(false) }
    var selectedGraphType by remember { mutableStateOf("daily") }
    var isGraphLoading by remember { mutableStateOf(false) }
    var graphImageUri by remember { mutableStateOf<String?>(null) }
    var uploadData by remember { mutableStateOf<Long>(0L) }
    var downloadData by remember { mutableStateOf<Long>(0L) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Fetch all ONUs for the user
    LaunchedEffect(username) {
        if (username.isBlank()) {
            errorMessage = "Please log in again"
            AppLogger.logError("RouterScreen: Username is blank")
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null

        scope.launch {
            val result = onuRepository.fetchAllOnusByUsername(username)
            result.onSuccess { onus ->
                onuList = onus
                selectedOnuIndex = 0
                if (onus.isNotEmpty()) {
                    preferencesManager.saveOnuExternalId(onus[0].uniqueExternalId)
                }
                isLoading = false
            }.onFailure { exception ->
                errorMessage = exception.message ?: "Error loading device information. Please try again"
                isLoading = false
            }
        }
    }

    // Fetch ONU status for the selected ONU
    LaunchedEffect(selectedOnuIndex, onuList) {
        if (onuList.isEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        val selectedOnu = onuList[selectedOnuIndex]
        android.util.Log.d("RouterScreen", "[v0] Fetching status for ONU: ${selectedOnu.name}, ExternalID: ${selectedOnu.uniqueExternalId}")

        scope.launch {
            val result = onuStatusRepository.fetchOnuStatus(
                onuExternalId = selectedOnu.uniqueExternalId
            )
            result.onSuccess { status ->
                android.util.Log.d("RouterScreen", "[v0] Status fetch success: $status")
                onuStatus = status
                isLoading = false
            }.onFailure { exception ->
                android.util.Log.e("RouterScreen", "[v0] Status fetch failure: ${exception.message}", exception)
                isLoading = false
            }
        }
    }

    // Fetch data usage for selected ONU
    LaunchedEffect(selectedOnuIndex, onuList) {
        if (onuList.isEmpty()) return@LaunchedEffect

        val selectedOnu = onuList[selectedOnuIndex]
        android.util.Log.d("RouterScreen", "[v0] Fetching ONU details for: ${selectedOnu.name}")

        scope.launch {
            try {
                val response = SmartOltClient.apiService.getOnuDetails(
                    onuExternalId = selectedOnu.uniqueExternalId,
                    apiKey = SmartOltConfig.API_KEY
                )

                android.util.Log.d("RouterScreen", "[v0] ONU details response code: ${response.code()}")

                if (response.isSuccessful) {
                    val details = response.body()?.onuDetails
                    android.util.Log.d("RouterScreen", "[v0] ONU details retrieved: $details")

                    val servicePorts = details?.servicePorts

                    if (!servicePorts.isNullOrEmpty()) {
                        val firstPort = servicePorts[0]

                        uploadData = parseNetworkSpeed(firstPort.uploadSpeed)
                        downloadData = parseNetworkSpeed(firstPort.downloadSpeed)

                        android.util.Log.d("RouterScreen", "[v0] Parsed speeds - Upload: ${formatBytes(uploadData)}, Download: ${formatBytes(downloadData)}")
                    } else {
                        uploadData = 0L
                        downloadData = 0L
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("RouterScreen", "[v0] Failed to fetch ONU details: ${response.code()}, Error: $errorBody")
                }
            } catch (e: Exception) {
                android.util.Log.e("RouterScreen", "[v0] Error fetching ONU details: ${e.message}", e)
            }
        }
    }

    // Fetch traffic graph when graph type or ONU changes
    LaunchedEffect(selectedGraphType, selectedOnuIndex, onuList) {
        if (onuList.isEmpty()) return@LaunchedEffect

        val selectedOnu = onuList[selectedOnuIndex]
        if (selectedOnu.uniqueExternalId.isNullOrBlank()) {
            android.util.Log.d("RouterScreen", "[v0] External ID is blank, skipping graph fetch")
            return@LaunchedEffect
        }

        isGraphLoading = true
        android.util.Log.d("RouterScreen", "[v0] Fetching graph - ExternalID: ${selectedOnu.uniqueExternalId}, Type: $selectedGraphType")

        scope.launch {
            try {
                val response = SmartOltClient.apiService.getOnuTrafficGraph(
                    onuExternalId = selectedOnu.uniqueExternalId ?: "",
                    graphType = selectedGraphType,
                    apiKey = SmartOltConfig.API_KEY
                )

                android.util.Log.d("RouterScreen", "[v0] Graph response code: ${response.code()}")

                if (response.isSuccessful) {
                    val bytes = response.body()?.bytes()
                    android.util.Log.d("RouterScreen", "[v0] Response body bytes: ${bytes?.size}")

                    if (bytes != null && bytes.isNotEmpty()) {
                        try {
                            val cacheDir = context.cacheDir
                            val graphFile = java.io.File(cacheDir, "traffic_graph_${selectedGraphType}.png")
                            graphFile.writeBytes(bytes)

                            graphImageUri = "file://${graphFile.absolutePath}"
                            android.util.Log.d("RouterScreen", "[v0] Graph image saved to: ${graphFile.absolutePath}")
                        } catch (e: Exception) {
                            android.util.Log.e("RouterScreen", "[v0] Error saving graph file: ${e.message}", e)
                        }
                    } else {
                        android.util.Log.e("RouterScreen", "[v0] Response body is empty or null")
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "No error details"
                    android.util.Log.e("RouterScreen", "[v0] Graph fetch failed: ${response.code()}, Error: $errorBody")
                }
                isGraphLoading = false
            } catch (e: Exception) {
                android.util.Log.e("RouterScreen", "[v0] Exception fetching graph: ${e.message}", e)
                isGraphLoading = false
            }
        }
    }

    fun rebootOnu() {
        if (onuList.isEmpty()) return

        scope.launch {
            try {
                val selectedOnu = onuList[selectedOnuIndex]
                AppLogger.logInfo("RouterScreen: Rebooting ONU",
                    mapOf("onuExternalId" to selectedOnu.uniqueExternalId) as Map<String, String>
                )

                val response = SmartOltClient.apiService.rebootOnu(
                    onuExternalId = selectedOnu.uniqueExternalId ?: "",
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

    val selectedOnu = if (onuList.isNotEmpty()) onuList[selectedOnuIndex] else null

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
                        Text(error, modifier = Modifier.weight(1f))
                    }
                }
            }

            // ONU Tabs - Show when multiple ONUs exist
            if (onuList.size > 1) {
                ScrollableTabRow(
                    selectedTabIndex = selectedOnuIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp)),
                    edgePadding = 0.dp
                ) {
                    onuList.forEachIndexed { index, onu ->
                        Tab(
                            selected = selectedOnuIndex == index,
                            onClick = { selectedOnuIndex = index },
                            text = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(8.dp)
                                ) {
                                    Text(
                                        text = onu.name ?: "Router ${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = "S/N: ${onu.sn?.take(8)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                }
                            }
                        )
                    }
                }
            }

            selectedOnu?.let { details ->
                // Device Info Card
                Card {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Router,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = details.name ?: "Unknown Device",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text(
                                    text = "S/N: ${details.sn}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (onuStatus == "Online") {
                                Badge(
                                    containerColor = Color(0xFF4CAF50)
                                ) {
                                    Text("Online", color = Color.White)
                                }
                            } else {
                                Badge(
                                    containerColor = Color(0xFFB71C1C)
                                ) {
                                    Text("Offline", color = Color.White)
                                }
                            }
                        }

                        Divider()

                        // Device Details Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DetailItem(label = "Type", value = details.onuTypeName ?: "Unknown", modifier = Modifier.weight(1f))
                            DetailItem(label = "Zone", value = details.zoneName ?: "N/A", modifier = Modifier.weight(1f))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DetailItem(label = "Username", value = details.username ?: "N/A", modifier = Modifier.weight(1f))
                            DetailItem(label = "Port", value = details.port ?: "N/A", modifier = Modifier.weight(1f))
                        }

                        // External ID and IP Address
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            DetailItem(label = "External ID", value = details.uniqueExternalId ?: "N/A", modifier = Modifier.weight(1f))
                            details.ipAddress?.let {
                                DetailItem(label = "IP Address", value = it, modifier = Modifier.weight(1f))
                            }
                        }

                        // Status Section
                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = if (onuStatus == "Online") Color(0x1F4CAF50) else Color(0x1FB71C1C),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Router Status",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = onuStatus ?: "Unknown",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (onuStatus == "Online") Color(0xFF4CAF50) else Color(0xFFB71C1C),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Divider()

                        // WiFi Credentials Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showWiFiDialog = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.VpnKey,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Change WiFi Credentials",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Update your router's WiFi SSID and password",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Reboot Device Button
                        Button(
                            onClick = { showRebootDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reboot Device")
                        }
                    }
                }

                // Traffic Graph Section
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Traffic Usage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf("Hourly", "Daily", "Weekly", "Monthly").forEach { period ->
                                FilterChip(
                                    selected = selectedGraphType == period.lowercase(),
                                    onClick = { selectedGraphType = period.lowercase() },
                                    label = { Text(period, maxLines = 1) }
                                )
                            }
                        }

                        if (isGraphLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text(
                                        text = "Loading traffic graph...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (!graphImageUri.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(graphImageUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Traffic graph for ${selectedGraphType}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No graph data available",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Reboot Dialog
    if (showRebootDialog) {
        AlertDialog(
            onDismissRequest = { showRebootDialog = false },
            title = { Text("Confirm Reboot") },
            text = { Text("Are you sure you want to reboot this device?") },
            confirmButton = {
                Button(
                    onClick = {
                        rebootOnu()
                        showRebootDialog = false
                    }
                ) {
                    Text("Yes, Reboot")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRebootDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // WiFi Credentials Dialog
    if (showWiFiDialog && onuList.isNotEmpty()) {
        ChangeWiFiCredentialsDialog(
            onuName = onuList[selectedOnuIndex].name ?: "Device",
            onDismiss = { showWiFiDialog = false },
            onConfirm = { ssid, password ->
                isChangingWiFi = true
                scope.launch {
                    try {
                        val selectedOnu = onuList[selectedOnuIndex]
                        val result = WiFiCredentialManager.changeWiFiCredentials(
                            onuExternalId = selectedOnu.uniqueExternalId,
                            newSSID = ssid,
                            newPassword = password
                        )

                        result.onSuccess { message ->
                            snackbarHostState.showSnackbar(message)
                            showWiFiDialog = false
                            android.util.Log.d("RouterScreen", "[v0] WiFi credentials changed successfully")
                        }.onFailure { error ->
                            snackbarHostState.showSnackbar("Error: ${error.message}")
                            android.util.Log.e("RouterScreen", "[v0] WiFi change failed: ${error.message}")
                        }
                    } finally {
                        isChangingWiFi = false
                    }
                }
            },
            isLoading = isChangingWiFi
        )
    }
}

@Composable
fun DetailItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TrafficStatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun parseNetworkSpeed(speedString: String): Long {
    val upperSpeed = speedString.uppercase().trim()

    val (value, unit) = if (upperSpeed.endsWith("G")) {
        Pair(upperSpeed.dropLast(1).toLongOrNull() ?: 0L, "G")
    } else if (upperSpeed.endsWith("M")) {
        Pair(upperSpeed.dropLast(1).toLongOrNull() ?: 0L, "M")
    } else if (upperSpeed.endsWith("K")) {
        Pair(upperSpeed.dropLast(1).toLongOrNull() ?: 0L, "K")
    } else {
        Pair(upperSpeed.toLongOrNull() ?: 0L, "")
    }

    return when (unit) {
        "G" -> value * 1_000_000_000
        "M" -> value * 1_000_000
        "K" -> value * 1_000
        else -> value
    }
}
