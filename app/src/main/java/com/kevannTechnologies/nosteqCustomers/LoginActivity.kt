package com.kevannTechnologies.nosteqCustomers

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.kevannTechnologies.nosteqCustomers.models.LoginResponse
import com.kevannTechnologies.nosteqCustomers.ui.theme.NosteqTheme
import com.nosteq.provider.utils.PreferencesManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesManager = PreferencesManager(this)

        if (preferencesManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        setContent {
            NosteqTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var username by remember { mutableStateOf("") }
                    var password by remember { mutableStateOf("") }
                    var passwordVisible by remember { mutableStateOf(false) }
                    var isLoading by remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf<String?>(null) }

                    fun performLogin() {
                        if (username.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter both username and password"
                            return
                        }

                        isLoading = true
                        errorMessage = null

                        ApiClient.instance.login(username, password).enqueue(object : Callback<LoginResponse> {
                            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                                isLoading = false

                                if (response.isSuccessful && response.body() != null) {
                                    val loginData = response.body()!!.data
                                    val userId = loginData.user.id

                                    preferencesManager.saveLoginData(
                                        token = loginData.token,
                                        userId = userId,
                                        username = loginData.user.username,
                                        ispName = loginData.ispDetail.isp_name,
                                        ispCurrency = loginData.ispDetail.isp_currency
                                    )

                                    // Create/update user profile in Firestore
                                    lifecycleScope.launch {
                                        PasswordSecurityManager.createOrUpdateUserProfile(
                                            userId = userId,
                                            username = username,
                                            phoneNumber = username  // Phone number is used as username
                                        )

                                        // Check if password needs to be changed
                                        val shouldChangePassword = PasswordSecurityManager.shouldForcePasswordChange(userId)
                                        
                                        if (shouldChangePassword) {
                                            navigateToChangePassword(userId, username, isFirstLogin = true)
                                        } else {
                                            navigateToMain()
                                        }
                                    }
                                } else {
                                    errorMessage = "Invalid username or password"
                                }
                            }

                            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                isLoading = false
                                errorMessage = "Network error"
                            }
                        })
                    }

                    fun sendWhatsAppMessage(inputUsername: String, dialogType: String) {
                        // Support team WhatsApp number (replace with actual number)
                        val supportPhoneNumber = "1234567890" // Format: country code without + (e.g., 254 for Kenya, 1 for USA)
                        
                        val message = if (dialogType == "forgot_password") {
                            "Hello, I need help resetting my password. My username is: $inputUsername"
                        } else {
                            "Hello, I need assistance with my account. My username is: $inputUsername"
                        }
                        
                        try {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_VIEW
                                data = Uri.parse("https://wa.me/$supportPhoneNumber?text=${Uri.encode(message)}")
                            }
                            startActivity(sendIntent)
                        } catch (e: Exception) {
                            errorMessage = "WhatsApp is not installed or could not open"
                        }
                    }

                    LoginScreen(
                        username = username,
                        password = password,
                        passwordVisible = passwordVisible,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onUsernameChange = {
                            username = it
                            errorMessage = null
                        },
                        onPasswordChange = {
                            password = it
                            errorMessage = null
                        },
                        onPasswordVisibleChange = { passwordVisible = it },
                        onLoginClick = { performLogin() },
                        onSendWhatsAppMessage = { inputUsername, dialogType ->
                            sendWhatsAppMessage(inputUsername, dialogType)
                        }
                    )
                }
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToChangePassword(userId: String, username: String, isFirstLogin: Boolean) {
        val intent = Intent(this@LoginActivity, ChangePasswordActivity::class.java).apply {
            putExtra("userId", userId)
            putExtra("username", username)
            putExtra("isFirstLogin", isFirstLogin)
        }
        startActivity(intent)
        finish()
    }
}
