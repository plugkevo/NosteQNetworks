package com.example.nosteq

import android.content.Context
import android.content.SharedPreferences

data class MpesaConfig(
    val businessShortCode: String = "4047627",
    val consumerKey: String = "QjVwDxdGgaHrOqOcd42jIxmf67EH82xo",
    val consumerSecret: String = "engf86E6qZtjokva",
    val passkey: String = "467c2bfd8a1048ec4fff9c15151b181a6fb5de3b5498a0ea0ba9cd10fcdc788a",
    val callbackUrl: String = "https://nosteq.phpradius.com/index.php/api/c2bConfirmation",
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

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
            businessShortCode = prefs.getString(KEY_BUSINESS_SHORT_CODE, "4047627") ?: "4047627",
            consumerKey = prefs.getString(KEY_CONSUMER_KEY, "QjVwDxdGgaHrOqOcd42jIxmf67EH82xo") ?: "QjVwDxdGgaHrOqOcd42jIxmf67EH82xo",
            consumerSecret = prefs.getString(KEY_CONSUMER_SECRET, "engf86E6qZtjokva") ?: "engf86E6qZtjokva",
            passkey = prefs.getString(KEY_PASSKEY, "467c2bfd8a1048ec4fff9c15151b181a6fb5de3b5498a0ea0ba9cd10fcdc788a") ?: "467c2bfd8a1048ec4fff9c15151b181a6fb5de3b5498a0ea0ba9cd10fcdc788a",
            callbackUrl = prefs.getString(KEY_CALLBACK_URL, "https://nosteq.phpradius.com/index.php/api/c2bConfirmation") ?: "https://nosteq.phpradius.com/index.php/api/c2bConfirmation",
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
