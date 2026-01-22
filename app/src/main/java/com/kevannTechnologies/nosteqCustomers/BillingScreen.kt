package com.kevannTechnologies.nosteqCustomers

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kevannTechnologies.nosteqCustomers.models.Invoice
import com.kevannTechnologies.nosteqCustomers.models.InvoiceResponse
import com.kevannTechnologies.nosteqCustomers.models.Receipt
import com.kevannTechnologies.nosteqCustomers.models.ReceiptResponse
import com.nosteq.provider.utils.PreferencesManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BillingScreen() {
    val context = LocalContext.current
    val prefsManager = remember { PreferencesManager(context) }

    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Invoices", "Receipts")

    var invoices by remember { mutableStateOf<List<Invoice>>(emptyList()) }
    var receipts by remember { mutableStateOf<List<Receipt>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val toDate = dateFormat.format(Date())
    val calendar = Calendar.getInstance()
    calendar.add(Calendar.MONTH, -12)
    val fromDate = dateFormat.format(calendar.time)

    LaunchedEffect(Unit) {
        val token = prefsManager.getToken()
        if (token.isNullOrEmpty()) {
            errorMessage = "Please log in to view billing information"
            isLoading = false
            return@LaunchedEffect
        }

        // Fetch invoices
        ApiClient.instance.getInvoices("Bearer $token", fromDate, toDate)
            .enqueue(object : Callback<InvoiceResponse> {
                override fun onResponse(
                    call: Call<InvoiceResponse>,
                    response: Response<InvoiceResponse>
                ) {
                    if (response.isSuccessful) {
                        invoices = response.body()?.data ?: emptyList()
                    } else {
                        errorMessage = "Unable to load invoices. Please try again."
                    }
                }

                override fun onFailure(call: Call<InvoiceResponse>, t: Throwable) {
                    errorMessage = "Network error. Please check your connection."
                }
            })

        // Fetch receipts
        ApiClient.instance.getReceipts("Bearer $token", fromDate, toDate)
            .enqueue(object : Callback<ReceiptResponse> {
                override fun onResponse(
                    call: Call<ReceiptResponse>,
                    response: Response<ReceiptResponse>
                ) {
                    isLoading = false
                    if (response.isSuccessful) {
                        receipts = response.body()?.data ?: emptyList()
                    } else {
                        errorMessage = "Unable to load receipts. Please try again."
                    }
                }

                override fun onFailure(call: Call<ReceiptResponse>, t: Throwable) {
                    isLoading = false
                    errorMessage = "Network error. Please check your connection."
                }
            })
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Billing & Payments",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Unable to Load Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(errorMessage ?: "Something went wrong. Please try again.")
                        Button(
                            onClick = {
                                isLoading = true
                                errorMessage = null
                            }
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            Text(
                                text = "Invoices (Last 12 Months)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (invoices.isEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No invoices found")
                                    }
                                }
                            } else {
                                invoices.forEach { invoice ->
                                    InvoiceCard(
                                        invoice = invoice,
                                        onDownload = { generateInvoicePdf(context, invoice) }
                                    )
                                }
                            }
                        }
                        1 -> {
                            Text(
                                text = "Receipts (Last 12 Months)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (receipts.isEmpty()) {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No receipts found")
                                    }
                                }
                            } else {
                                receipts.forEach { receipt ->
                                    ReceiptCard(
                                        receipt = receipt,
                                        onDownload = { generateReceiptPdf(context, receipt) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun InvoiceCard(
    invoice: Invoice,
    onDownload: () -> Unit
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
                    text = "${invoice.invoiceNo}${invoice.prefix}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = invoice.invoiceDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "KES ${invoice.total}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Badge(
                    containerColor = when (invoice.status.lowercase()) {
                        "paid" -> MaterialTheme.colorScheme.primaryContainer
                        "partial" -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer
                    }
                ) {
                    Text(invoice.status.uppercase())
                }
            }

            IconButton(onClick = onDownload) {
                Icon(Icons.Filled.Download, contentDescription = "Download PDF")
            }
        }
    }
}

@Composable
fun ReceiptCard(
    receipt: Receipt,
    onDownload: () -> Unit
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
                    text = "Receipt #${receipt.receiptNo}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = receipt.receiptDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "KES ${receipt.amount}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0CCFEA)
                )
                Text(
                    text = receipt.receiptType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDownload) {
                Icon(Icons.Filled.Download, contentDescription = "Download PDF")
            }
        }
    }
}

fun generateInvoicePdf(context: Context, invoice: Invoice) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("INVOICE", 50f, 80f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        var yPos = 140f

        canvas.drawText("Invoice Number: ${invoice.invoiceNo}${invoice.prefix}", 50f, yPos, paint)
        yPos += 30f
        canvas.drawText("Date: ${invoice.invoiceDate}", 50f, yPos, paint)
        yPos += 30f
        canvas.drawText("Status: ${invoice.status.uppercase()}", 50f, yPos, paint)
        yPos += 30f
        canvas.drawText("Username: ${invoice.username}", 50f, yPos, paint)
        yPos += 60f

        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Total Amount: KES ${invoice.total}", 50f, yPos, paint)
        yPos += 30f
        canvas.drawText("Amount Received: KES ${invoice.receiveAmount}", 50f, yPos, paint)
        yPos += 60f

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Created by: ${invoice.createdBy}", 50f, yPos, paint)

        pdfDocument.finishPage(page)

        val fileName = "Invoice_${invoice.invoiceNo}${invoice.prefix}.pdf"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                Toast.makeText(context, "Invoice saved to Downloads", Toast.LENGTH_LONG).show()
            } else {
                throw Exception("Unable to save file")
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            Toast.makeText(context, "Invoice saved to Downloads", Toast.LENGTH_LONG).show()
        }

    } catch (e: Exception) {
        Toast.makeText(context, "Unable to save invoice. Please try again.", Toast.LENGTH_LONG).show()
    }
}

fun generateReceiptPdf(context: Context, receipt: Receipt) {
    try {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)

        val canvas = page.canvas
        val paint = Paint()

        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("RECEIPT", 50f, 80f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        var yPos = 140f

        canvas.drawText("Receipt Number: ${receipt.receiptNo}", 50f, yPos, paint)
        yPos += 30f
        canvas.drawText("Date: ${receipt.receiptDate}", 50f, yPos, paint)
        yPos += 30f
        canvas.drawText("Payment Type: ${receipt.receiptType}", 50f, yPos, paint)
        yPos += 30f
        canvas.drawText("Username: ${receipt.username}", 50f, yPos, paint)
        yPos += 30f
        canvas.drawText("Contact Person: ${receipt.contactPerson}", 50f, yPos, paint)
        yPos += 30f
        canvas.drawText("Zone: ${receipt.zoneName}", 50f, yPos, paint)
        yPos += 60f

        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("Amount Paid: KES ${receipt.amount}", 50f, yPos, paint)
        yPos += 60f

        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Created by: ${receipt.createdBy}", 50f, yPos, paint)

        pdfDocument.finishPage(page)

        val fileName = "Receipt_${receipt.receiptNo}.pdf"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (uri != null) {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                pdfDocument.close()
                Toast.makeText(context, "Receipt saved to Downloads", Toast.LENGTH_LONG).show()
            } else {
                throw Exception("Unable to save file")
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val file = File(downloadsDir, fileName)

            FileOutputStream(file).use { outputStream ->
                pdfDocument.writeTo(outputStream)
            }
            pdfDocument.close()

            Toast.makeText(context, "Receipt saved to Downloads", Toast.LENGTH_LONG).show()
        }

    } catch (e: Exception) {
        Toast.makeText(context, "Unable to save receipt. Please try again.", Toast.LENGTH_LONG).show()
    }
}
