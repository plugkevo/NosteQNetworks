package com.example.nosteq

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nosteq.ui.theme.ui.theme.NosteqTheme

@Preview(showBackground=true)
@Composable
fun PackagesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Available Packages",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        PackageCard(
            name = "Basic 10Mbps",
            speed = "10 Mbps",
            quota = "50 GB",
            price = "KES 1,500",
            validity = "30 Days",
            isActive = false
        )

        PackageCard(
            name = "Standard 25Mbps",
            speed = "25 Mbps",
            quota = "100 GB",
            price = "KES 2,500",
            validity = "30 Days",
            isActive = false
        )

        PackageCard(
            name = "Premium 50Mbps",
            speed = "50 Mbps",
            quota = "Unlimited",
            price = "KES 4,000",
            validity = "30 Days",
            isActive = true
        )

        PackageCard(
            name = "Ultra 100Mbps",
            speed = "100 Mbps",
            quota = "Unlimited",
            price = "KES 6,500",
            validity = "30 Days",
            isActive = false
        )
    }
}

@Composable
fun PackageCard(
    name: String,
    speed: String,
    quota: String,
    price: String,
    validity: String,
    isActive: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isActive) {
                    Badge { Text("Active") }
                }
            }

            Divider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Speed", style = MaterialTheme.typography.bodySmall)
                    Text(speed, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Quota", style = MaterialTheme.typography.bodySmall)
                    Text(quota, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Validity", style = MaterialTheme.typography.bodySmall)
                    Text(validity, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = price,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = { /* TODO: Implement package subscription */ },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActive
            ) {
                Text(if (isActive) "Current Package" else "Subscribe")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PackagesScreenPreview() {
    NosteqTheme {
        PackagesScreen()
    }
}
