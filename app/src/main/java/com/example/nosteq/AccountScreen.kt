package com.example.nosteq


import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nosteq.ui.theme.NosteqTheme
import com.nosteq.provider.utils.PreferencesManager

@Composable
fun AccountScreen() {
    val context = LocalContext.current
    val preferencesManager = PreferencesManager(context)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Account Details",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Personal Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { /* TODO: Edit profile */ }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                }

                AccountInfoRow("Full Name", "John Doe")
                AccountInfoRow("Account ID", "NST-2024-001")
                AccountInfoRow("Email", "john.doe@example.com")
                AccountInfoRow("Phone", "+254 712 345 678")
                AccountInfoRow("Installation Address", "123 Main Street, Nairobi")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Account Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                AccountInfoRow("Status", "Active")
                AccountInfoRow("Member Since", "January 2024")
                AccountInfoRow("Last Payment", "December 15, 2024")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Notification Preferences",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                NotificationToggle("Renewal Reminders", true)
                NotificationToggle("Promotional Offers", false)
                NotificationToggle("Service Updates", true)
                NotificationToggle("Outage Alerts", true)
            }
        }

        Button(
            onClick = {
                preferencesManager.clearLoginData()
                val intent = Intent(context, com.example.nosteq.LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            )
        ) {
            Icon(
                imageVector = Icons.Filled.ExitToApp,
                contentDescription = "Logout",
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Logout")
        }
    }
}

@Composable
fun AccountInfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun NotificationToggle(label: String, checked: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(
            checked = checked,
            onCheckedChange = { /* TODO: Update notification preference */ }
        )
    }
}

@Preview(showBackground = true)
@Composable
fun AccountScreenPreview() {
    NosteqTheme {
        AccountScreen()
    }
}
