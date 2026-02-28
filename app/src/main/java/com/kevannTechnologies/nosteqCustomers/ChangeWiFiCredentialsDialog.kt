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
    onConfirm: (username: String, password: String) -> Unit,
    isLoading: Boolean = false
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var usernameError by remember { mutableStateOf<String?>(null) }
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
                    value = username,
                    onValueChange = {
                        username = it
                        usernameError = null
                    },
                    label = { Text("WiFi Username") },
                    placeholder = { Text("5-16 alphanumeric") },
                    enabled = !isLoading,
                    singleLine = true,
                    isError = usernameError != null,
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = {
                        if (usernameError != null) {
                            Text(
                                text = usernameError ?: "",
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
                    placeholder = { Text("8-16 alphanumeric") },
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
                    text = "• Username: 5-16 alphanumeric characters\n• Password: 8-16 alphanumeric characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false

                    if (username.length < 5 || username.length > 16) {
                        usernameError = "Username must be 5-16 characters"
                        hasError = true
                    }
                    if (!username.matches(Regex("^[a-zA-Z0-9]*$"))) {
                        usernameError = "Only alphanumeric characters allowed"
                        hasError = true
                    }

                    if (password.length < 8 || password.length > 16) {
                        passwordError = "Password must be 8-16 characters"
                        hasError = true
                    }
                    if (!password.matches(Regex("^[a-zA-Z0-9]*$"))) {
                        passwordError = "Only alphanumeric characters allowed"
                        hasError = true
                    }

                    if (!hasError) {
                        onConfirm(username, password)
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
