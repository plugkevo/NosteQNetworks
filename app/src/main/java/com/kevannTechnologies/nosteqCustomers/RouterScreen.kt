package com.kevannTechnologies.nosteqCustomers

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.nosteq.provider.utils.PreferencesManager
import com.kevannTechnologies.nosteqCustomers.models.OnuDetails
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.graphics.vector.ImageVector
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
    var onuAdministrativeStatus by remember { mutableStateOf<String?>(null) }
    var currentOnuDetails by remember { mutableStateOf<OnuDetails?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var showWiFiDialog by remember { mutableStateOf(false) }
    var isChangingWiFi by remember { mutableStateOf(false) }
    var selectedGraphType by remember { mutableStateOf("daily") }
    var isGraphLoading by remember { mutableStateOf(false) }
    var graphImageUri by remember { mutableStateOf<String?>(null) }
    var uploadData by remember { mutableStateOf<Long>(0L) }
    var downloadData by remember { mutableStateOf<Long>(0L) }
    var isEnablingDisabling by remember { mutableStateOf(false) }
    var showOnuActionsDialog by remember { mutableStateOf(false) }
    var showOnuConfirmDialog by remember { mutableStateOf(false) }
    var onuStatusDialogType by remember { mutableStateOf("") } // "enable" or "disable"
    var showWiFiStatusDialog by remember { mutableStateOf(false) }
    var showWiFiActionsDialog by remember { mutableStateOf(false) }
    var showWiFiConfirmDialog by remember { mutableStateOf(false) }
    var wiFiStatusDialogType by remember { mutableStateOf("") } // "enable" or "disable"
    var showLanStatusDialog by remember { mutableStateOf(false) }
    var showLanActionsDialog by remember { mutableStateOf(false) }
    var showLanConfirmDialog by remember { mutableStateOf(false) }
    var lanStatusDialogType by remember { mutableStateOf("") } // "enable" or "disable"
    var wiFiAdministrativeStatus by remember { mutableStateOf<String?>(null) }
    var lanAdministrativeStatus by remember { mutableStateOf<String?>(null) }

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


    fun fetchOnuAdministrativeStatus() {
        if (onuList.isEmpty()) return

        scope.launch {
            try {
                val selectedOnu = onuList[selectedOnuIndex]
                val response = SmartOltClient.apiService.getOnuAdministrativeStatus(
                    onuExternalId = selectedOnu.uniqueExternalId ?: "",
                    apiKey = SmartOltConfig.API_KEY
                )

                if (response.isSuccessful && response.body()?.status == true) {
                    val adminStatus = response.body()?.administrativeStatus
                    onuAdministrativeStatus = adminStatus
                    AppLogger.logInfo("RouterScreen: Administrative status fetched", 
                        mapOf("status" to (adminStatus ?: "unknown")) as Map<String, String>
                    )
                } else {
                    AppLogger.logError("RouterScreen: Failed to fetch administrative status", Exception("HTTP ${response.code()}"))
                }
            } catch (e: Exception) {
                AppLogger.logError("RouterScreen: Error fetching administrative status", e)
            }
        }
    }

    fun fetchOnuDetailsWithStatus() {
        isRefreshing = true
        if (onuList.isEmpty()) {
            isRefreshing = false
            return
        }

        scope.launch {
            try {
                val selectedOnu = onuList[selectedOnuIndex]
                val response = SmartOltClient.apiService.getOnuDetails(
                    onuExternalId = selectedOnu.uniqueExternalId ?: "",
                    apiKey = SmartOltConfig.API_KEY
                )

                if (response.isSuccessful && response.body()?.status == true) {
                    val onuDetailsData = response.body()?.onuDetails
                    currentOnuDetails = onuDetailsData
                    
                    // Extract LAN status from ethernet_ports
                    val lanPortStatus = onuDetailsData?.ethernetPorts?.firstOrNull()?.adminState
                    
                    // Extract WiFi status from wifi_ports
                    val wifiPortStatus = onuDetailsData?.wifiPorts?.firstOrNull()?.adminState

                    wiFiAdministrativeStatus = wifiPortStatus ?: "Unknown"
                    lanAdministrativeStatus = lanPortStatus ?: "Unknown"

                    AppLogger.logInfo("RouterScreen: ONU details fetched",
                        mapOf(
                            "wifi" to (wifiPortStatus ?: "unknown"),
                            "lan" to (lanPortStatus ?: "unknown")
                        ) as Map<String, String>
                    )
                    
                    // Fetch administrative status separately
                    fetchOnuAdministrativeStatus()
                } else {
                    AppLogger.logError("RouterScreen: Failed to fetch ONU details", Exception("HTTP ${response.code()}"))
                }
                isRefreshing = false
            } catch (e: Exception) {
                AppLogger.logError("RouterScreen: Error fetching ONU details", e)
                isRefreshing = false
            }
        }
    }

    fun refetchOnuList() {
        scope.launch {
            val result = onuRepository.fetchAllOnusByUsername(username)
            result.onSuccess { onus ->
                onuList = onus
                AppLogger.logInfo("RouterScreen: ONU list refetched. New status: ${if (onuList.isNotEmpty()) onuList[selectedOnuIndex].administrativeStatus else "N/A"}")
                isEnablingDisabling = false
            }.onFailure { exception ->
                AppLogger.logError("RouterScreen: Refetch failed", exception)
                isEnablingDisabling = false
            }
        }
        // Refetch ONU details with all status information after list refresh with delay
        scope.launch {
            kotlinx.coroutines.delay(500)
            fetchOnuDetailsWithStatus()
        }
    }

    fun enableOnu() {
        if (onuList.isEmpty()) return

        isEnablingDisabling = true
        scope.launch {
            try {
                val selectedOnu = onuList[selectedOnuIndex]
                val result = DeviceManager.enableDevice(
                    onuExternalId = selectedOnu.uniqueExternalId ?: ""
                )

                result.onSuccess { message ->
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                    showOnuConfirmDialog = false
                    refetchOnuList()
                    fetchOnuDetailsWithStatus()
                }.onFailure { error ->
                    snackbarHostState.showSnackbar(
                        message = "Error: ${error.message}",
                        duration = SnackbarDuration.Short
                    )
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Error: ${e.message}",
                    duration = SnackbarDuration.Short
                )
            } finally {
                isEnablingDisabling = false
            }
        }
    }

    fun disableOnu() {
        if (onuList.isEmpty()) return

        isEnablingDisabling = true
        scope.launch {
            try {
                val selectedOnu = onuList[selectedOnuIndex]
                val result = DeviceManager.disableDevice(
                    onuExternalId = selectedOnu.uniqueExternalId ?: ""
                )

                result.onSuccess { message ->
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                    showOnuConfirmDialog = false
                    refetchOnuList()
                    fetchOnuDetailsWithStatus()
                }.onFailure { error ->
                    snackbarHostState.showSnackbar(
                        message = "Error: ${error.message}",
                        duration = SnackbarDuration.Short
                    )
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar(
                    message = "Error: ${e.message}",
                    duration = SnackbarDuration.Short
                )
            } finally {
                isEnablingDisabling = false
            }
        }
    }

    // Fetch ONU details with WiFi and LAN port status when ONU changes
    LaunchedEffect(selectedOnuIndex, onuList) {
        if (onuList.isEmpty()) return@LaunchedEffect
        fetchOnuDetailsWithStatus()
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
                            
                            // Administrative Status Badge
                            val adminStatusColor = when {
                                onuAdministrativeStatus?.lowercase() == "enabled" -> Color(0xFF2196F3)
                                onuAdministrativeStatus?.lowercase() == "disabled" -> Color(0xFFFF9800)
                                else -> Color(0xFF9E9E9E)
                            }
                            Badge(
                                containerColor = adminStatusColor
                            ) {
                                Text(onuAdministrativeStatus ?: "Unknown", color = Color.White)
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
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = Color(0x1F9C27B0),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Online/Offline Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Connection Status",
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
                                
                                // Administrative Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Device Status",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = onuAdministrativeStatus ?: "Unknown",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = when {
                                                onuAdministrativeStatus?.lowercase() == "enabled" -> Color(0xFF2196F3)
                                                onuAdministrativeStatus?.lowercase() == "disabled" -> Color(0xFFFF9800)
                                                else -> Color(0xFF9E9E9E)
                                            },
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
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

                        // Quick Actions Grid
                        if (onuList.isNotEmpty()) {
                            val isOnuOnline = onuStatus == "Online"

                            // Refresh Status Button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Quick Actions",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Button(
                                    onClick = { fetchOnuDetailsWithStatus() },
                                    enabled = !isRefreshing,
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                ) {
                                    if (isRefreshing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Refresh status",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refresh", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Device Enable/Disable Card
                                QuickActionCard(
                                    modifier = Modifier.weight(1f),
                                    title = "Device",
                                    status = onuStatus ?: "Unknown",
                                    isActive = isOnuOnline,
                                    icon = Icons.Default.Devices,
                                    isLoading = isEnablingDisabling,
                                    onClick = {
                                        showOnuActionsDialog = true
                                    }
                                )

                                // WiFi Control Card
                                QuickActionCard(
                                    modifier = Modifier.weight(1f),
                                    title = "WiFi",
                                    status = wiFiAdministrativeStatus ?: "Unknown",
                                    isActive = wiFiAdministrativeStatus?.lowercase() == "enabled",
                                    icon = Icons.Default.Wifi,
                                    onClick = {
                                        showWiFiActionsDialog = true
                                    }
                                )

                                // LAN Control Card
                                QuickActionCard(
                                    modifier = Modifier.weight(1f),
                                    title = "LAN",
                                    status = lanAdministrativeStatus ?: "Unknown",
                                    isActive = lanAdministrativeStatus?.lowercase() == "enabled",
                                    icon = Icons.Default.Language,
                                    onClick = {
                                        showLanActionsDialog = true
                                    }
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

    // ONU Actions Dialog - Shows Turn On and Turn Off buttons
    // ONU Control Dialogs
    OnuActionsDialog(
        showDialog = showOnuActionsDialog,
        onDismiss = { showOnuActionsDialog = false },
        onuStatus = onuStatus,
        onEnableClick = {
            onuStatusDialogType = "enable"
            showOnuActionsDialog = false
            showOnuConfirmDialog = true
        },
        onDisableClick = {
            onuStatusDialogType = "disable"
            showOnuActionsDialog = false
            showOnuConfirmDialog = true
        }
    )

    OnuConfirmDialog(
        showDialog = showOnuConfirmDialog,
        onDismiss = { showOnuConfirmDialog = false },
        isEnable = onuStatusDialogType == "enable",
        isLoading = isEnablingDisabling,
        onConfirm = {
            if (onuStatusDialogType == "enable") enableOnu() else disableOnu()
            showOnuConfirmDialog = false
        }
    )

    // WiFi Control Dialogs
    WiFiActionsDialog(
        showDialog = showWiFiActionsDialog,
        onDismiss = { showWiFiActionsDialog = false },
        wiFiAdministrativeStatus = wiFiAdministrativeStatus,
        onEnableClick = {
            wiFiStatusDialogType = "enable"
            showWiFiActionsDialog = false
            showWiFiConfirmDialog = true
        },
        onDisableClick = {
            wiFiStatusDialogType = "disable"
            showWiFiActionsDialog = false
            showWiFiConfirmDialog = true
        }
    )

    WiFiConfirmDialog(
        showDialog = showWiFiConfirmDialog,
        onDismiss = { showWiFiConfirmDialog = false },
        isEnable = wiFiStatusDialogType == "enable",
        isLoading = false,
        snackbarHostState = snackbarHostState,
        scope = scope,
        onuList = onuList,
        selectedOnuIndex = selectedOnuIndex,
        onFetchWiFiStatus = { fetchOnuDetailsWithStatus() },
        onConfirm = {
            // Close dialog immediately
            showWiFiConfirmDialog = false
            
            // Run operation in background
            scope.launch {
                try {
                    val selectedOnu = onuList[selectedOnuIndex]
                    val result = if (wiFiStatusDialogType == "enable") {
                        WiFiPortManager.enableAllWiFiPorts(
                            onuExternalId = selectedOnu.uniqueExternalId ?: "",
                            ssid = "NosteqWiFi",
                            password = "default123",
                            authMode = "WPA2"
                        )
                    } else {
                        WiFiPortManager.disableAllWiFiPorts(
                            onuExternalId = selectedOnu.uniqueExternalId ?: ""
                        )
                    }

                    result.onSuccess { message ->
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                        fetchOnuDetailsWithStatus()
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar(
                            message = "Error: ${error.message}",
                            duration = SnackbarDuration.Short
                        )
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = "Error: ${e.message}",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    )

    // LAN Control Dialogs
    LanActionsDialog(
        showDialog = showLanActionsDialog,
        onDismiss = { showLanActionsDialog = false },
        lanAdministrativeStatus = lanAdministrativeStatus,
        onEnableClick = {
            lanStatusDialogType = "enable"
            showLanActionsDialog = false
            showLanConfirmDialog = true
        },
        onDisableClick = {
            lanStatusDialogType = "disable"
            showLanActionsDialog = false
            showLanConfirmDialog = true
        }
    )

    LanConfirmDialog(
        showDialog = showLanConfirmDialog,
        onDismiss = { showLanConfirmDialog = false },
        isEnable = lanStatusDialogType == "enable",
        isLoading = false,
        snackbarHostState = snackbarHostState,
        scope = scope,
        onuList = onuList,
        selectedOnuIndex = selectedOnuIndex,
        onFetchLanStatus = { fetchOnuDetailsWithStatus() },
        onConfirm = {
            // Close dialog immediately
            showLanConfirmDialog = false
            
            // Run operation in background
            scope.launch {
                try {
                    val selectedOnu = onuList[selectedOnuIndex]
                    val result = if (lanStatusDialogType == "enable") {
                        LANManager.enableAllLanPorts(
                            onuExternalId = selectedOnu.uniqueExternalId ?: ""
                        )
                    } else {
                        LANManager.disableAllLanPorts(
                            onuExternalId = selectedOnu.uniqueExternalId ?: ""
                        )
                    }

                    result.onSuccess { message ->
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                        fetchOnuDetailsWithStatus()
                    }.onFailure { error ->
                        snackbarHostState.showSnackbar(
                            message = "Error: ${error.message}",
                            duration = SnackbarDuration.Short
                        )
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(
                        message = "Error: ${e.message}",
                        duration = SnackbarDuration.Short
                    )
                }
            }
        }
    )

    // WiFi Credentials Dialog
    if (showWiFiDialog && onuList.isNotEmpty()) {
        var ssid by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var showPassword by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showWiFiDialog = false },
            title = { Text("Change WiFi Credentials") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Update WiFi SSID and password for ${onuList[selectedOnuIndex].name ?: "Device"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    OutlinedTextField(
                        value = ssid,
                        onValueChange = { ssid = it },
                        label = { Text("WiFi SSID (Network Name)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isChangingWiFi
                    )
                    
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("WiFi Password") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !isChangingWiFi,
                        visualTransformation = if (showPassword)
                            VisualTransformation.None
                        else
                            PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(
                                onClick = { showPassword = !showPassword },
                                enabled = !isChangingWiFi
                            ) {
                                Icon(
                                    imageVector = if (showPassword)
                                        Icons.Default.Visibility
                                    else
                                        Icons.Default.VisibilityOff,
                                    contentDescription = if (showPassword)
                                        "Hide password"
                                    else
                                        "Show password"
                                )
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (ssid.isNotEmpty() && password.isNotEmpty()) {
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
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Short
                    )
                    showOnuConfirmDialog = false
                    refetchOnuList()
                    fetchOnuDetailsWithStatus()
                }.onFailure { error ->
                    snackbarHostState.showSnackbar(
                        message = "Error: ${error.message}",
                        duration = SnackbarDuration.Short
                    )
                }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar(
                                        message = "Error: ${e.message}",
                                        duration = SnackbarDuration.Short
                                    )
                                } finally {
                                    isChangingWiFi = false
                                }
                            }
                        } else {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Please fill in all fields",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    enabled = !isChangingWiFi,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    )
                ) {
                    if (isChangingWiFi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Update Credentials")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showWiFiDialog = false },
                    enabled = !isChangingWiFi
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun QuickActionCard(
    modifier: Modifier = Modifier,
    title: String,
    status: String,
    isActive: Boolean,
    icon: ImageVector,
    isLoading: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable(enabled = !isLoading) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                Color(0xFF4CAF50).copy(alpha = 0.1f)
            else
                Color(0xFFFF9800).copy(alpha = 0.1f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) Color(0xFF4CAF50) else Color(0xFFFF9800)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = if (isActive) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFFF9800).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = if (isActive) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = if (isActive) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
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
