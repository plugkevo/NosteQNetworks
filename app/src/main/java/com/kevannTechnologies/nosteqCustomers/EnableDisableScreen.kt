package com.kevannTechnologies.nosteqCustomers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kevannTechnologies.nosteqCustomers.models.OnuDetails
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun OnuActionsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onuStatus: String?,
    onEnableClick: () -> Unit,
    onDisableClick: () -> Unit
) {
    if (showDialog) {
        val isOnuOnline = onuStatus == "Online"

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Device Control") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onEnableClick,
                            modifier = Modifier.weight(1f),
                            enabled = !isOnuOnline,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Turn On")
                        }

                        Button(
                            onClick = onDisableClick,
                            modifier = Modifier.weight(1f),
                            enabled = isOnuOnline,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB71C1C),
                                disabledContainerColor = Color(0xFFB71C1C).copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Turn Off")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun OnuConfirmDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    isEnable: Boolean,
    isLoading: Boolean,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        val titleText = if (isEnable) "Turn On Device?" else "Turn Off Device?"
        val messageText = if (isEnable)
            "Turning on your device will bring it online and restore service."
        else
            "Turning off your device will take it offline and stop service."
        val buttonText = if (isEnable) "Turn On" else "Turn Off"

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(titleText) },
            text = { Text(messageText) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnable) Color(0xFF4CAF50) else Color(0xFFB71C1C)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(buttonText)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun WiFiActionsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    wiFiAdministrativeStatus: String?,
    onEnableClick: () -> Unit,
    onDisableClick: () -> Unit
) {
    if (showDialog) {
        val isWiFiEnabled = wiFiAdministrativeStatus?.lowercase() == "enabled"

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("WiFi Control") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onEnableClick,
                            modifier = Modifier.weight(1f),
                            enabled = !isWiFiEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Turn On")
                        }

                        Button(
                            onClick = onDisableClick,
                            modifier = Modifier.weight(1f),
                            enabled = isWiFiEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB71C1C),
                                disabledContainerColor = Color(0xFFB71C1C).copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Turn Off")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun WiFiConfirmDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    isEnable: Boolean,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onuList: List<OnuDetails>,
    selectedOnuIndex: Int,
    onFetchWiFiStatus: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        val titleText = if (isEnable) "Turn On WiFi?" else "Turn Off WiFi?"
        val messageText = if (isEnable)
            "Enabling WiFi will allow wireless devices to connect to your network."
        else
            "Disabling WiFi will disconnect all wireless devices from your network."
        val buttonText = if (isEnable) "Turn On" else "Turn Off"

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(titleText) },
            text = { Text(messageText) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnable) Color(0xFF4CAF50) else Color(0xFFB71C1C)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(buttonText)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun LanActionsDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    lanAdministrativeStatus: String?,
    onEnableClick: () -> Unit,
    onDisableClick: () -> Unit
) {
    if (showDialog) {
        val isLanEnabled = lanAdministrativeStatus?.lowercase() == "enabled"

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("LAN Control") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onEnableClick,
                            modifier = Modifier.weight(1f),
                            enabled = !isLanEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                disabledContainerColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Turn On")
                        }

                        Button(
                            onClick = onDisableClick,
                            modifier = Modifier.weight(1f),
                            enabled = isLanEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFB71C1C),
                                disabledContainerColor = Color(0xFFB71C1C).copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Turn Off")
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                OutlinedButton(onClick = onDismiss) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun LanConfirmDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    isEnable: Boolean,
    isLoading: Boolean,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    onuList: List<OnuDetails>,
    selectedOnuIndex: Int,
    onFetchLanStatus: () -> Unit,
    onConfirm: () -> Unit
) {
    if (showDialog) {
        val titleText = if (isEnable) "Turn On LAN?" else "Turn Off LAN?"
        val messageText = if (isEnable)
            "Enabling LAN will allow wired devices to connect to your network."
        else
            "Disabling LAN will disconnect all wired devices from your network."
        val buttonText = if (isEnable) "Turn On" else "Turn Off"

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(titleText) },
            text = { Text(messageText) },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnable) Color(0xFF4CAF50) else Color(0xFFB71C1C)
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(buttonText)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isLoading
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
