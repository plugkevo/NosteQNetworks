package com.kevann.nosteq

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun UpdatePromptDialog(
    updateInfo: UpdateInfo,
    isForceUpdate: Boolean,
    isDownloading: Boolean = false,
    downloadProgress: DownloadProgress? = null,
    onUpdateClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isForceUpdate && !isDownloading) onSkipClick() },
        title = {
            Text(if (isDownloading) "Downloading Update..." else "App Update Available")
        },
        text = {
            Column {
                Text(updateInfo.updateMessage)

                if (isDownloading && downloadProgress != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    LinearProgressIndicator(
                        progress = { downloadProgress.progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${downloadProgress.progress}%",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = formatTimeRemaining(downloadProgress.timeRemainingSeconds),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onUpdateClick,
                enabled = !isDownloading
            ) {
                Text(if (isDownloading) "Downloading..." else "Update Now")
            }
        },
        dismissButton = {
            if (!isForceUpdate && !isDownloading) {
                Button(onClick = onSkipClick) {
                    Text("Later")
                }
            }
        }
    )
}

private fun formatTimeRemaining(seconds: Long): String {
    return when {
        seconds < 60 -> "${seconds}s left"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s left"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m left"
    }
}
