package com.kevann.nosteq

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class DownloadProgress(
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progress: Int,
    val timeRemainingSeconds: Long,
    val isComplete: Boolean
)

object ApkDownloader {

    fun downloadApk(context: Context, downloadUrl: String): Flow<DownloadProgress> = flow {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
                setTitle("Nosteq Update")
                setDescription("Downloading app update...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalFilesDir(
                    context,
                    Environment.DIRECTORY_DOWNLOADS,
                    "nosteq_update.apk"
                )
            }

            val downloadId = downloadManager.enqueue(request)

            var downloading = true
            var lastProgress = 0
            var lastTime = System.currentTimeMillis()
            var lastBytes = 0L

            while (downloading) {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor = downloadManager.query(query)

                if (cursor.moveToFirst()) {
                    val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloadedBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    when (status) {
                        DownloadManager.STATUS_RUNNING -> {
                            if (totalBytes > 0) {
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt()

                                val currentTime = System.currentTimeMillis()
                                val timeDiff = (currentTime - lastTime) / 1000.0
                                val bytesDiff = downloadedBytes - lastBytes

                                val timeRemaining = if (timeDiff > 0 && bytesDiff > 0) {
                                    val speed = bytesDiff / timeDiff
                                    val remainingBytes = totalBytes - downloadedBytes
                                    (remainingBytes / speed).toLong()
                                } else {
                                    0L
                                }

                                if (progress != lastProgress) {
                                    emit(DownloadProgress(downloadedBytes, totalBytes, progress, timeRemaining, false))
                                    lastProgress = progress
                                    lastTime = currentTime
                                    lastBytes = downloadedBytes
                                }
                            }
                        }
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            emit(DownloadProgress(totalBytes, totalBytes, 100, 0, true))
                            downloading = false

                            val apkUri = downloadManager.getUriForDownloadedFile(downloadId)
                            installApk(context, apkUri)
                        }
                        DownloadManager.STATUS_FAILED -> {
                            val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            downloading = false
                            throw Exception("Download failed")
                        }
                    }
                }
                cursor.close()
                delay(500)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun installApk(context: Context, uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            throw e
        }
    }
}
