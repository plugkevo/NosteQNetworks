package com.kevannTechnologies.nosteqCustomers

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.kevannTechnologies.nosteqCustomers.models.LoginResponse
import com.kevannTechnologies.nosteqCustomers.ui.theme.NosteqTheme
import com.nosteq.provider.utils.PreferencesManager
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

                                    preferencesManager.saveLoginData(
                                        token = loginData.token,
                                        userId = loginData.user.id,
                                        username = loginData.user.username,
                                        ispName = loginData.ispDetail.isp_name,
                                        ispCurrency = loginData.ispDetail.isp_currency
                                    )

                                    navigateToMain()
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
                        onLoginClick = { performLogin() }
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
}
