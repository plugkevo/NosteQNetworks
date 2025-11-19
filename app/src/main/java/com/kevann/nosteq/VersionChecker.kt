package com.kevann.nosteq

import android.content.Context
import android.content.pm.PackageManager
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await

data class UpdateInfo(
    val currentVersion: String,
    val minRequiredVersion: String,
    val apkDownloadUrl: String,
    val forceUpdate: Boolean,
    val updateMessage: String
)

object VersionChecker {

    fun getInstalledVersion(context: Context): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "0.0.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "0.0.0"
        }
    }

    suspend fun getRemoteVersion(): UpdateInfo? {
        return try {
            val remoteConfig = FirebaseRemoteConfig.getInstance()

            // Set cache expiration to 1 hour
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()
            remoteConfig.setConfigSettingsAsync(configSettings)

            // Fetch from Remote Config
            remoteConfig.fetchAndActivate().await()

            UpdateInfo(
                currentVersion = remoteConfig.getString("currentVersion"),
                minRequiredVersion = remoteConfig.getString("minRequiredVersion"),
                apkDownloadUrl = remoteConfig.getString("apkDownloadUrl"),
                forceUpdate = remoteConfig.getBoolean("forceUpdate"),
                updateMessage = remoteConfig.getString("updateMessage")
            )
        } catch (e: Exception) {
            null
        }
    }

    fun isUpdateNeeded(installedVersion: String, minRequiredVersion: String): Boolean {
        return compareVersions(installedVersion, minRequiredVersion) < 0
    }

    private fun compareVersions(version1: String, version2: String): Int {
        val parts1 = version1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = version2.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val v1 = if (i < parts1.size) parts1[i] else 0
            val v2 = if (i < parts2.size) parts2[i] else 0

            when {
                v1 < v2 -> return -1
                v1 > v2 -> return 1
            }
        }
        return 0
    }
}
