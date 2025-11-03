package com.example.nosteq


import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.nosteq.models.DashboardResponse
import com.example.nosteq.ui.theme.ui.theme.NosteqTheme
import com.nosteq.provider.utils.PreferencesManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }

    var dashboardData by remember { mutableStateOf<DashboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val token = prefsManager.getToken()
        Log.d("[v0]", "=== Dashboard API Call Debug ===")
        Log.d("[v0]", "Token retrieved: $token")
        Log.d("[v0]", "Token is null: ${token == null}")
        Log.d("[v0]", "Token is empty: ${token?.isEmpty()}")
        Log.d("[v0]", "Token length: ${token?.length}")

        if (token != null) {
            Log.d("[v0]", "Making API call to getDashboard with token: $token")
            ApiClient.instance.getDashboard("Bearer $token").enqueue(object : Callback<DashboardResponse> {
                override fun onResponse(call: Call<DashboardResponse>, response: Response<DashboardResponse>) {
                    isLoading = false
                    Log.d("[v0]", "=== API Response ===")
                    Log.d("[v0]", "Response code: ${response.code()}")
                    Log.d("[v0]", "Response message: ${response.message()}")
                    Log.d("[v0]", "Response successful: ${response.isSuccessful}")

                    if (response.isSuccessful) {
                        Log.d("[v0]", "Response body: ${response.body()}")
                        dashboardData = response.body()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("[v0]", "Response error body: $errorBody")
                        errorMessage = "Failed to load dashboard data (Code: ${response.code()})"
                    }
                }

                override fun onFailure(call: Call<DashboardResponse>, t: Throwable) {
                    isLoading = false
                    Log.e("[v0]", "API call failed: ${t.message}", t)
                    errorMessage = "Network error: ${t.message}"
                }
            })
        } else {
            isLoading = false
            Log.e("[v0]", "Token is null - user not authenticated")
            errorMessage = "Not authenticated"
        }
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (errorMessage != null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = errorMessage ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = {
                    isLoading = true
                    errorMessage = null
                    // Retry logic would go here
                }) {
                    Text("Retry")
                }
            }
        }
        return
    }

    val data = dashboardData?.data

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Account Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when (data?.status) {
                        0 -> "Active"
                        1 -> "Inactive"
                        2 -> "Suspended"
                        else -> "Unknown"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (data?.status == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Current Package", style = MaterialTheme.typography.bodySmall)
                        Text(
                            data?.planName ?: "N/A",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Validity", style = MaterialTheme.typography.bodySmall)
                        Text(
                            formatValidity(data?.currentRecharge?.validity),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Start Date", style = MaterialTheme.typography.bodySmall)
                        Text(
                            formatDate(data?.startDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Expires On", style = MaterialTheme.typography.bodySmall)
                        Text(
                            formatDate(data?.expiryDate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Used Data Card
        if (data?.usedData != null) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Data Usage",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Upload", style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatBytes(data.usedData.upload ?: 0),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Download", style = MaterialTheme.typography.bodySmall)
                            Text(
                                formatBytes(data.usedData.download ?: 0),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Quick Actions
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Filled.List,
                title = "Packages",
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("packages") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            QuickActionCard(
                icon = Icons.Filled.AccountBox,
                title = "Billing",
                modifier = Modifier.weight(1f),
                onClick = { navController.navigate("billing") }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                icon = Icons.Filled.Router,
                title = "Router",
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("router") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
            QuickActionCard(
                icon = Icons.Filled.Info,
                title = "Support",
                modifier = Modifier.weight(1f),
                onClick = {
                    navController.navigate("support") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun QuickActionCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    NosteqTheme {
        DashboardScreen(navController = rememberNavController())
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDate(dateString: String?): String {
    if (dateString == null) return "N/A"
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        dateString
    }
}

private fun formatValidity(validitySeconds: Long?): String {
    if (validitySeconds == null || validitySeconds <= 0) return "N/A"
    val days = validitySeconds / 86400
    return "$days days"
}
