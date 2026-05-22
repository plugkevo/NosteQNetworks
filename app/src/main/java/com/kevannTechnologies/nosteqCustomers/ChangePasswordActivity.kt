package com.kevannTechnologies.nosteqCustomers

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.kevannTechnologies.nosteqCustomers.models.ChangePasswordResponse
import com.kevannTechnologies.nosteqCustomers.ui.theme.NosteqTheme
import com.nosteq.provider.utils.PreferencesManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChangePasswordActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesManager = PreferencesManager(this)

        val isFirstLogin = intent.getBooleanExtra("isFirstLogin", false)
        val username = intent.getStringExtra("username") ?: ""
        val userId = intent.getStringExtra("userId") ?: ""

        setContent {
            NosteqTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isLoading by remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf<String?>(null) }

                    fun performPasswordChange(newPassword: String) {
                        if (newPassword.isBlank()) {
                            errorMessage = "Password cannot be empty"
                            return
                        }

                        isLoading = true
                        errorMessage = null

                        // Call your backend API to change password (don't touch the API)
                        ApiClient.instance.changePassword(username, newPassword).enqueue(
                            object : Callback<ChangePasswordResponse> {
                                override fun onResponse(
                                    call: Call<ChangePasswordResponse>,
                                    response: Response<ChangePasswordResponse>
                                ) {
                                    isLoading = false

                                    if (response.isSuccessful && response.body()?.status == true) {
                                        // Mark password as changed in Firestore
                                        lifecycleScope.launch {
                                            PasswordSecurityManager.markPasswordAsChanged(userId)
                                            // Navigate to main activity
                                            navigateToMain()
                                        }
                                    } else {
                                        errorMessage = response.body()?.message
                                            ?: "Failed to change password"
                                    }
                                }

                                override fun onFailure(
                                    call: Call<ChangePasswordResponse>,
                                    t: Throwable
                                ) {
                                    isLoading = false
                                    errorMessage = "Network error: ${t.message}"
                                }
                            }
                        )
                    }

                    fun skipPasswordChange() {
                        // Only allow skip on first login
                        if (isFirstLogin) {
                            lifecycleScope.launch {
                                // Still mark that user has been prompted
                                PasswordSecurityManager.markPasswordAsChanged(userId)
                                navigateToMain()
                            }
                        }
                    }

                    ChangePasswordScreen(
                        isFirstLogin = isFirstLogin,
                        username = username,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onPasswordChange = { newPassword ->
                            performPasswordChange(newPassword)
                        },
                        onSkip = { skipPasswordChange() }
                    )
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this@ChangePasswordActivity, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
