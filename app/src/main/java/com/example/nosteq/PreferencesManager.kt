package com.nosteq.provider.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nosteq_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
        private const val KEY_ISP_NAME = "isp_name"
        private const val KEY_ISP_CURRENCY = "isp_currency"
        private const val KEY_LOGIN_TIMESTAMP = "login_timestamp"
        private const val SESSION_TIMEOUT_MS = 20 * 60 * 1000L

        private const val KEY_ONU_EXTERNAL_ID = "onu_external_id"
    }

    fun saveLoginData(
        token: String,
        userId: Int,
        username: String,
        ispName: String,
        ispCurrency: String
    ) {
        prefs.edit().apply {
            putString(KEY_TOKEN, token)
            putInt(KEY_USER_ID, userId)
            putString(KEY_USERNAME, username)
            putString(KEY_ISP_NAME, ispName)
            putString(KEY_ISP_CURRENCY, ispCurrency)
            putLong(KEY_LOGIN_TIMESTAMP, System.currentTimeMillis())
            apply()
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserId(): Int = prefs.getInt(KEY_USER_ID, -1)

    fun getUsername(): String? = prefs.getString(KEY_USERNAME, null)

    fun getIspName(): String? = prefs.getString(KEY_ISP_NAME, null)

    fun getIspCurrency(): String? = prefs.getString(KEY_ISP_CURRENCY, null)



    fun clearLoginData() {
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean = getToken() != null

    fun isSessionExpired(): Boolean {
        val loginTimestamp = prefs.getLong(KEY_LOGIN_TIMESTAMP, 0L)
        if (loginTimestamp == 0L) return true

        val currentTime = System.currentTimeMillis()
        val elapsedTime = currentTime - loginTimestamp

        return elapsedTime >= SESSION_TIMEOUT_MS
    }

    fun saveOnuExternalId(externalId: String) {
        prefs.edit().putString(KEY_ONU_EXTERNAL_ID, externalId).apply()
    }

    fun getOnuExternalId(): String? {
        return prefs.getString(KEY_ONU_EXTERNAL_ID, null)
    }

    fun clearOnuExternalId() {
        prefs.edit().remove(KEY_ONU_EXTERNAL_ID).apply()
    }


    fun saveUsername(username: String) {
        prefs.edit().putString(KEY_USERNAME, username).apply()
    }



    fun clearUsername() {
        prefs.edit().remove(KEY_USERNAME).apply()
    }

}
