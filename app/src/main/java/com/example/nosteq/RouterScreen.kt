package com.example.nosteq

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
import com.example.nosteq.models.OnuDetails
import com.example.nosteq.models.OnuStatus
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
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRebootDialog by remember { mutableStateOf(false) }
    var selectedGraphType by remember { mutableStateOf("daily") }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(username) {
        if (username.isBlank()) {
            errorMessage = "Please log in again"
            return@LaunchedEffect
        }



        isLoading = true
        errorMessage = null

        try {
            val cachedId = preferencesManager.getOnuExternalId()

            if (cachedId != null) {
                onuExternalId = cachedId
            } else {
                val allOnusResponse = SmartOltClient.apiService.getAllOnusDetails(
                    apiKey = SmartOltConfig.API_KEY
                )

                if (allOnusResponse.isSuccessful && allOnusResponse.body()?.status == true) {
                    val allOnus = allOnusResponse.body()?.onus ?: emptyList()

                    val matchingOnu = allOnus.firstOrNull { onu ->
                        onu.username?.equals(username, ignoreCase = true) == true
                    }

                    if (matchingOnu != null) {
                        onuExternalId = matchingOnu.uniqueExternalId
                        preferencesManager.saveOnuExternalId(matchingOnu.uniqueExternalId)
                    } else {
                        errorMessage = "No device found for your account. Please contact support"
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
            isLoading = false
            return@LaunchedEffect
        }
    }

    LaunchedEffect(onuExternalId) {
        if (onuExternalId == null) return@LaunchedEffect

        isLoading = true
        errorMessage = null

        try {
            try {
                val detailsResponse = SmartOltClient.apiService.getOnuDetails(
                    onuExternalId = onuExternalId!!,
                    apiKey = SmartOltConfig.API_KEY
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

                            if (statusResponse.isSuccessful && statusResponse.body()?.status == true) {
                                onuStatus = statusResponse.body()?.response?.firstOrNull()
                            }
                        } catch (e: Exception) {
                            // Silently handle status fetch error
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently handle details fetch error
            }

        } catch (e: Exception) {
            errorMessage = "Unable to load device data. Please try again"
        } finally {
            isLoading = false
        }
    }

    fun rebootOnu() {
        scope.launch {
            try {
                val response = SmartOltClient.apiService.rebootOnu(
                    onuExternalId = onuExternalId!!,
                    apiKey = SmartOltConfig.API_KEY
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

                        HorizontalDivider()

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

                        HorizontalDivider()

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

                        HorizontalDivider()

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
