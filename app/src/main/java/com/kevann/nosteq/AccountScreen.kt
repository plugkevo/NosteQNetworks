package com.kevann.nosteq

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kevann.nosteq.models.ChangePasswordRequest
import com.kevann.nosteq.models.ChangePasswordResponse
import com.kevann.nosteq.models.UserResponse
import com.kevann.nosteq.ui.theme.NosteqTheme
import com.nosteq.provider.utils.PreferencesManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val preferencesManager = PreferencesManager(context)

    var userDetail by remember { mutableStateOf<UserResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var currentPasswordVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var isChangingPassword by remember { mutableStateOf(false) }
    var passwordChangeMessage by remember { mutableStateOf<String?>(null) }
    var passwordChangeError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = preferencesManager.getToken()
        if (token.isNullOrEmpty()) {
            errorMessage = "Please log in to view account details"
            isLoading = false
            return@LaunchedEffect
        }

        ApiClient.instance.getUserDetails("Bearer $token").enqueue(object : Callback<UserResponse> {
            override fun onResponse(call: Call<UserResponse>, response: Response<UserResponse>) {
                isLoading = false
                if (response.isSuccessful) {
                    userDetail = response.body()
                } else {
                    errorMessage = "Unable to load account details. Please try again."
                }
            }

            override fun onFailure(call: Call<UserResponse>, t: Throwable) {
                isLoading = false
                errorMessage = "Network error. Please check your connection and try again."
            }
        })
    }

    fun changePassword() {
        if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            passwordChangeError = "All fields are required"
            return
        }

        if (newPassword != confirmPassword) {
            passwordChangeError = "Passwords do not match"
            return
        }

        if (newPassword.length < 6) {
            passwordChangeError = "Password must be at least 6 characters"
            return
        }

        isChangingPassword = true
        passwordChangeError = null
        passwordChangeMessage = null

        val token = preferencesManager.getToken()
        if (token.isNullOrEmpty()) {
            passwordChangeError = "Please log in to change password"
            isChangingPassword = false
            return
        }

        val request = ChangePasswordRequest(
            currentPassword = currentPassword,
            newPassword = newPassword,
            confirmPassword = confirmPassword
        )

        ApiClient.instance.changePassword("Bearer $token", request).enqueue(object : Callback<ChangePasswordResponse> {
            override fun onResponse(call: Call<ChangePasswordResponse>, response: Response<ChangePasswordResponse>) {
                isChangingPassword = false
                if (response.isSuccessful) {
                    passwordChangeMessage = "Password changed successfully"
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                } else {
                    passwordChangeError = "Unable to change password. Please check your current password and try again."
                }
            }

            override fun onFailure(call: Call<ChangePasswordResponse>, t: Throwable) {
                isChangingPassword = false
                passwordChangeError = "Network error. Please check your connection and try again."
            }
        })
    }

    when {
        isLoading -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        errorMessage != null -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                    Button(onClick = {
                        isLoading = true
                        errorMessage = null
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Retry")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }
        userDetail != null -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Account Details",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                // Account Information Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Account Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        AccountInfoRow("Username", userDetail?.data?.username ?: "N/A")
                        AccountInfoRow("Account ID", userDetail?.data?.id?.toString() ?: "N/A")
                        AccountInfoRow("Current Plan", userDetail?.data?.planName ?: "N/A")
                        AccountInfoRow(
                            "Status",
                            when (userDetail?.data?.status) {
                                0 -> "Active"
                                1 -> "Inactive"
                                else -> "Unknown"
                            }
                        )
                        AccountInfoRow("Start Date", userDetail?.data?.startDate ?: "N/A")
                        AccountInfoRow("Expiry Date", userDetail?.data?.expiryDate ?: "N/A")
                    }
                }

                // Personal Information Card
                userDetail?.data?.userinfo?.let { userInfo ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Personal Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            AccountInfoRow("Contact Person", userInfo.contactPerson ?: "N/A")
                            AccountInfoRow("Company", userInfo.company ?: "N/A")
                            AccountInfoRow("Phone", userInfo.phone ?: "N/A")
                            AccountInfoRow("Email", userInfo.email ?: "N/A")
                        }
                    }
                }

                // Billing Information Card
                userDetail?.data?.userinfo?.let { userInfo ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Billing Information",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            AccountInfoRow("Address", userInfo.billingAddress1 ?: "N/A")
                            AccountInfoRow("City", userInfo.billingCity ?: "N/A")
                            AccountInfoRow("ZIP Code", userInfo.billingZip ?: "N/A")
                        }
                    }
                }

                // Change Password Card
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Change Password",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = currentPassword,
                            onValueChange = {
                                currentPassword = it
                                passwordChangeError = null
                            },
                            label = { Text("Current Password") },
                            visualTransformation = if (currentPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { currentPasswordVisible = !currentPasswordVisible }) {
                                    Icon(
                                        imageVector = if (currentPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (currentPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isChangingPassword
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                passwordChangeError = null
                            },
                            label = { Text("New Password") },
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (newPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isChangingPassword
                        )

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                passwordChangeError = null
                            },
                            label = { Text("Confirm New Password") },
                            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            trailingIcon = {
                                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                    Icon(
                                        imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isChangingPassword
                        )

                        if (passwordChangeError != null) {
                            Text(
                                text = passwordChangeError ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        if (passwordChangeMessage != null) {
                            Text(
                                text = passwordChangeMessage ?: "",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = { changePassword() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isChangingPassword
                        ) {
                            if (isChangingPassword) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text("Change Password")
                        }
                    }
                }

                // Logout Button
                Button(
                    onClick = {
                        preferencesManager.clearLoginData()
                        val intent = Intent(context, com.kevann.nosteq.LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ExitToApp,
                        contentDescription = "Logout",
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Logout")
                }
            }
        }
    }
}

@Composable
fun AccountInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {
    NosteqTheme {
        AccountScreen()
    }
}
