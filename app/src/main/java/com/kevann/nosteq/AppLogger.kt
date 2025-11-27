package com.kevann.nosteq


import android.util.Log
import com.google.firebase.crashlytics.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics

object AppLogger {
    private const val TAG = "NosteqApp"

    fun logInfo(message: String, details: Map<String, String> = emptyMap()) {
        // Log locally in debug builds
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }

        // Send to Crashlytics in all builds
        FirebaseCrashlytics.getInstance().apply {
            log("INFO: $message")
            details.forEach { (key, value) ->
                setCustomKey(key, value)
            }
        }
    }

    fun logError(message: String, error: Throwable? = null, details: Map<String, String> = emptyMap()) {
        // Log locally in debug builds
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message, error)
        }

        // Send to Crashlytics in all builds
        FirebaseCrashlytics.getInstance().apply {
            log("ERROR: $message")
            details.forEach { (key, value) ->
                setCustomKey(key, value)
            }
            error?.let { recordException(it) }
        }
    }

    fun logApiCall(endpoint: String, success: Boolean, responseCode: Int? = null, errorMessage: String? = null) {
        val details = mutableMapOf(
            "endpoint" to endpoint,
            "success" to success.toString()
        )
        responseCode?.let { details["response_code"] = it.toString() }
        errorMessage?.let { details["error"] = it }

        val message = if (success) {
            "API Success: $endpoint"
        } else {
            "API Failed: $endpoint - $errorMessage"
        }

        if (success) {
            logInfo(message, details)
        } else {
            logError(message, null, details)
        }
    }
}
