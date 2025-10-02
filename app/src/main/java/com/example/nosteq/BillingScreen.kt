package com.example.nosteq

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Preview(showBackground=true)
@Composable
fun BillingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Billing & Payments",
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
                    text = "Current Balance",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "KES 0.00",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { /* TODO: Make payment */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Make Payment")
                }
            }
        }

        Text(
            text = "Recent Invoices",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        InvoiceCard(
            invoiceNumber = "INV-2024-001",
            date = "December 15, 2024",
            amount = "KES 4,000",
            status = "Paid"
        )

        InvoiceCard(
            invoiceNumber = "INV-2024-002",
            date = "November 15, 2024",
            amount = "KES 4,000",
            status = "Paid"
        )

        InvoiceCard(
            invoiceNumber = "INV-2024-003",
            date = "October 15, 2024",
            amount = "KES 4,000",
            status = "Paid"
        )
    }
}

@Composable
fun InvoiceCard(
    invoiceNumber: String,
    date: String,
    amount: String,
    status: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = invoiceNumber,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Badge(
                    containerColor = if (status == "Paid")
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(status)
                }
            }

            IconButton(onClick = { /* TODO: Download invoice */ }) {
                Icon(Icons.Filled.Download, contentDescription = "Download")
            }
        }
    }
}