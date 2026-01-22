package com.kevannTechnologies.nosteqCustomers

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging

//theme in use
import com.kevannTechnologies.nosteqCustomers.ui.theme.NosteqTheme
import com.nosteq.provider.utils.PreferencesManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        preferencesManager = PreferencesManager(this)

        lifecycleScope.launch {
            MpesaConfigManager.fetchConfigFromRemote(this@MainActivity)
        }

        retrieveFCMToken()

        if (preferencesManager.isSessionExpired()) {
            preferencesManager.clearLoginData()
            navigateToLogin()
            return
        }

        setContent {
            NosteqTheme(dynamicColor = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    HomeScreen(context = this@MainActivity)
                }
            }
        }
    }

    private fun retrieveFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                AppLogger.logError("FCM Token", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            AppLogger.logInfo("FCM Token Retrieved", mapOf("token" to token))
            Log.d("FCM_TOKEN", "Firebase Messaging Token: $token")
        }
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}


