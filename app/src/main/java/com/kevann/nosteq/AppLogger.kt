package com.kevann.nosteq

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

object AppLogger {
    private const val TAG = "NosteqApp"

    private val isDebug = try {
        Class.forName("${AppLogger::class.java.`package`?.name}.BuildConfig")
            .getField("DEBUG")
            .getBoolean(null)
    } catch (e: Exception) {
        false
    }

    fun logInfo(message: String, details: Map<String, String> = emptyMap()) {
        // Log locally in debug builds
        if (isDebug) {
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
        if (isDebug) {
            if (error != null) {
                Log.e(TAG, message, error)
            } else {
                Log.e(TAG, message)
            }
        }

        // Send to Crashlytics in all builds
        FirebaseCrashlytics.getInstance().apply {
            log("ERROR: $message")
            details.forEach { (key, value) ->
                setCustomKey(key, value)
            }
            if (error != null) {
                recordException(error)
            }
        }
    }

    fun logApiCall(endpoint: String, success: Boolean, responseCode: Int? = null, errorMessage: String? = null) {
        val details = mutableMapOf(
            "endpoint" to endpoint,
            "success" to success.toString()
        )
        if (responseCode != null) {
            details["response_code"] = responseCode.toString()
        }
        if (errorMessage != null) {
            details["error"] = errorMessage
        }

        val message = if (success) {
            "API Success: $endpoint"
        } else {
            "API Failed: $endpoint${if (errorMessage != null) " - $errorMessage" else ""}"
        }

        if (success) {
            logInfo(message, details)
        } else {
            logError(message, null, details)
        }
    }
}
