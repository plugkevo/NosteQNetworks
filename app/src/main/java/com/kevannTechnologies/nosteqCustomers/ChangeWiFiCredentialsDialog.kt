package com.kevannTechnologies.nosteqCustomers


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp


@Composable
fun ChangeWiFiCredentialsDialog(
    onuName: String,
    onDismiss: () -> Unit,
    onConfirm: (ssid: String, password: String) -> Unit,
    isLoading: Boolean = false
) {
    var ssid by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var ssidError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = {
            if (!isLoading) onDismiss()
        },
        title = {
            Text("Change WiFi Credentials")
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Device: $onuName",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = ssid,
                    onValueChange = {
                        ssid = it
                        ssidError = null
                    },
                    label = { Text("WiFi Name (SSID)") },
                    placeholder = { Text("Up to 32 characters") },
                    enabled = !isLoading,
                    singleLine = true,
                    isError = ssidError != null,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (ssidError != null) {
                            Text(
                                text = ssidError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        passwordError = null
                    },
                    label = { Text("WiFi Password") },
                    placeholder = { Text("8-64 characters") },
                    enabled = !isLoading,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    isError = passwordError != null,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (passwordError != null) {
                            Text(
                                text = passwordError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                )

                Text(
                    text = "• SSID: Up to 32 characters\n• Password: 8-64 characters\n• Authentication: WPA2",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false

                    if (ssid.isBlank()) {
                        ssidError = "WiFi name cannot be empty"
                        hasError = true
                    }
                    if (ssid.length > 32) {
                        ssidError = "WiFi name must be 32 characters or less"
                        hasError = true
                    }

                    if (password.length < 8 || password.length > 64) {
                        passwordError = "Password must be 8-64 characters"
                        hasError = true
                    }

                    if (!hasError) {
                        onConfirm(ssid, password)
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                } else {
                    Text("Change")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}
