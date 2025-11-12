package com.example.nosteq

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.Firebase
import com.google.firebase.remoteconfig.remoteConfig

import kotlinx.coroutines.tasks.await

data class MpesaConfig(
    val businessShortCode: String = "",
    val consumerKey: String = "",
    val consumerSecret: String = "",
    val passkey: String = "",
    val callbackUrl: String = "",
    val environment: MpesaEnvironment = MpesaEnvironment.PRODUCTION
)

enum class MpesaEnvironment(val baseUrl: String) {
    SANDBOX("https://sandbox.safaricom.co.ke"),
    PRODUCTION("https://api.safaricom.co.ke")
}

object MpesaConfigManager {
    private const val PREFS_NAME = "mpesa_config"
    private const val KEY_BUSINESS_SHORT_CODE = "business_short_code"
    private const val KEY_CONSUMER_KEY = "consumer_key"
    private const val KEY_CONSUMER_SECRET = "consumer_secret"
    private const val KEY_PASSKEY = "passkey"
    private const val KEY_CALLBACK_URL = "callback_url"
    private const val KEY_ENVIRONMENT = "environment"

    private const val REMOTE_KEY_BUSINESS_SHORT_CODE = "mpesa_business_short_code"
    private const val REMOTE_KEY_CONSUMER_KEY = "mpesa_consumer_key"
    private const val REMOTE_KEY_CONSUMER_SECRET = "mpesa_consumer_secret"
    private const val REMOTE_KEY_PASSKEY = "mpesa_passkey"
    private const val REMOTE_KEY_CALLBACK_URL = "mpesa_callback_url"
    private const val REMOTE_KEY_ENVIRONMENT = "mpesa_environment"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    suspend fun fetchConfigFromRemote(context: Context): MpesaConfig? {
        return try {
            val remoteConfig = Firebase.remoteConfig

            remoteConfig.setConfigSettingsAsync(
                com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings.Builder()
                    .setMinimumFetchIntervalInSeconds(0)
                    .build()
            ).await()

            remoteConfig.fetchAndActivate().await()

            val config = MpesaConfig(
                businessShortCode = remoteConfig.getString(REMOTE_KEY_BUSINESS_SHORT_CODE),
                consumerKey = remoteConfig.getString(REMOTE_KEY_CONSUMER_KEY),
                consumerSecret = remoteConfig.getString(REMOTE_KEY_CONSUMER_SECRET),
                passkey = remoteConfig.getString(REMOTE_KEY_PASSKEY),
                callbackUrl = remoteConfig.getString(REMOTE_KEY_CALLBACK_URL),
                environment = try {
                    MpesaEnvironment.valueOf(remoteConfig.getString(REMOTE_KEY_ENVIRONMENT))
                } catch (e: Exception) {
                    MpesaEnvironment.PRODUCTION
                }
            )

            if (config.businessShortCode.isNotEmpty()) {
                saveConfig(context, config)
            }
            config
        } catch (e: Exception) {
            null
        }
    }

    fun saveConfig(context: Context, config: MpesaConfig) {
        getPrefs(context).edit().apply {
            putString(KEY_BUSINESS_SHORT_CODE, config.businessShortCode)
            putString(KEY_CONSUMER_KEY, config.consumerKey)
            putString(KEY_CONSUMER_SECRET, config.consumerSecret)
            putString(KEY_PASSKEY, config.passkey)
            putString(KEY_CALLBACK_URL, config.callbackUrl)
            putString(KEY_ENVIRONMENT, config.environment.name)
            apply()
        }
    }

    fun getConfig(context: Context): MpesaConfig {
        val prefs = getPrefs(context)

        return MpesaConfig(
            businessShortCode = prefs.getString(KEY_BUSINESS_SHORT_CODE, "") ?: "",
            consumerKey = prefs.getString(KEY_CONSUMER_KEY, "") ?: "",
            consumerSecret = prefs.getString(KEY_CONSUMER_SECRET, "") ?: "",
            passkey = prefs.getString(KEY_PASSKEY, "") ?: "",
            callbackUrl = prefs.getString(KEY_CALLBACK_URL, "") ?: "",
            environment = try {
                MpesaEnvironment.valueOf(prefs.getString(KEY_ENVIRONMENT, "PRODUCTION") ?: "PRODUCTION")
            } catch (e: Exception) {
                MpesaEnvironment.PRODUCTION
            }
        )
    }

    fun isConfigured(context: Context): Boolean {
        val config = getConfig(context)
        return config.businessShortCode.isNotEmpty() &&
                config.consumerKey.isNotEmpty() &&
                config.consumerSecret.isNotEmpty() &&
                config.passkey.isNotEmpty()
    }
}
