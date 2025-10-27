package com.example.nosteq

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Brands
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.brands.Whatsapp
import compose.icons.fontawesomeicons.brands.Facebook
import compose.icons.fontawesomeicons.brands.Tiktok
import compose.icons.fontawesomeicons.solid.Phone as PhoneSolid
import compose.icons.fontawesomeicons.solid.Envelope
import compose.icons.fontawesomeicons.solid.Phone

@Composable
fun SupportScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Support & Help",
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
                Text(
                    text = "Contact Us",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                ClickableContactRow(
                    icon = FontAwesomeIcons.Brands.Whatsapp,
                    label = "WhatsApp",
                    value = "+254 743 101738",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/254743101738")
                        }
                        context.startActivity(intent)
                    }
                )

                ClickableContactRow(
                    icon = FontAwesomeIcons.Solid.Phone,
                    label = "Call Us",
                    value = "+254 743 101738",
                    onClick = {
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:+254743101738")
                        }
                        context.startActivity(intent)
                    }
                )

                ClickableContactRow(
                    icon = FontAwesomeIcons.Solid.Envelope,
                    label = "Email",
                    value = "Billing@nosteq.co.ke",
                    onClick = {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:Billing@nosteq.co.ke")
                        }
                        context.startActivity(intent)
                    }
                )

                ClickableContactRow(
                    icon = FontAwesomeIcons.Brands.Facebook,
                    label = "Facebook",
                    value = "Follow us on Facebook",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://www.facebook.com/share/1S8or4NUUR/")
                        }
                        context.startActivity(intent)
                    }
                )

                ClickableContactRow(
                    icon = FontAwesomeIcons.Brands.Tiktok,
                    label = "TikTok",
                    value = "@nosteq.network.li",
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://www.tiktok.com/@nosteq.network.li?_t=ZM-90iOkUNiOXA&_r=1")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        Text(
            text = "Frequently Asked Questions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        FAQCard(
            question = "How do I change my package?",
            answer = "Go to the Packages screen and select your desired package."
        )

        FAQCard(
            question = "How do I reset my router password?",
            answer = "Navigate to Router Management and use the change password option."
        )

        FAQCard(
            question = "What payment methods are accepted?",
            answer = "We accept M-Pesa, bank transfers, and card payments."
        )
    }
}

@Composable
fun ClickableContactRow(
    icon: ImageVector,
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
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
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "Open",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun FAQCard(question: String, answer: String) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = question,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = answer,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
