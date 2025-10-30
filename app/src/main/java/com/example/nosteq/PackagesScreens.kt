package com.example.nosteq

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.nosteq.provider.utils.PreferencesManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.google.gson.GsonBuilder


fun String.decodeHtml(): String {
    return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
}

fun String.formatForMpesa(): String {
    // Remove any spaces, dashes, or special characters
    var cleaned = this.replace(Regex("[\\s\\-\$$\$$\\+]"), "")

    // Remove leading zero if present
    if (cleaned.startsWith("0")) {
        cleaned = cleaned.substring(1)
    }

    // Add 254 country code if not present
    if (!cleaned.startsWith("254")) {
        cleaned = "254$cleaned"
    }

    return cleaned
}

private fun processRecharge(
    plan: Plan,
    userDetail: UserDetail,
    preferencesManager: PreferencesManager,
    context: Context,
    onProcessing: (Boolean) -> Unit,
    onError: (String) -> Unit
) {
    onProcessing(true)

    val token = preferencesManager.getToken()
    if (token == null) {
        onProcessing(false)
        onError("Authentication token not found")
        return
    }

    val rawPhone = userDetail.userinfo.phone ?: ""
    val formattedPhone = rawPhone.formatForMpesa()

    val rechargeRequest = RechargeRequest(
        rechargePlan = plan.id,
        quantity = 1,
        rechargeType = 1,
        resetRecharge = false,
        contactPerson = userDetail.userinfo.contactPerson ?: "",
        email = userDetail.userinfo.email ?: "",
        phone = formattedPhone, // Using formatted phone number
        city = userDetail.userinfo.billingCity ?: "",
        zip = userDetail.userinfo.billingZip ?: ""
    )

    Log.d("PackagesScreen", "=== Processing M-Pesa Payment ===")
    Log.d("PackagesScreen", "Plan: ${plan.planName} (ID: ${plan.id})")
    Log.d("PackagesScreen", "Amount: ${plan.customerCost}")
    Log.d("PackagesScreen", "[v0] Raw Phone: $rawPhone") // Added debug log
    Log.d("PackagesScreen", "[v0] Formatted Phone: $formattedPhone") // Added debug log

    PackagesApiClient.instance.processMpesaPayment("Bearer $token", rechargeRequest)
        .enqueue(object : Callback<MpesaResponse> {
            override fun onResponse(
                call: Call<MpesaResponse>,
                response: Response<MpesaResponse>
            ) {
                onProcessing(false)
                if (response.isSuccessful && response.body() != null) {
                    val mpesaResponse = response.body()!!
                    Log.d("PackagesScreen", "✓ M-Pesa payment initiated successfully")
                    Log.d("PackagesScreen", "Status: ${mpesaResponse.status}")
                    Log.d("PackagesScreen", "Transaction ID: ${mpesaResponse.transactionId}")

                    // Show success message
                    onError("M-Pesa payment initiated. Check your phone for the payment prompt.")
                } else {
                    val errorBody = response.errorBody()?.string()
                    onError("Payment failed: ${response.code()}")
                    Log.e("PackagesScreen", "✗ M-Pesa Error: ${response.code()}")
                    Log.e("PackagesScreen", "✗ Error Body: $errorBody")
                }
            }

            override fun onFailure(call: Call<MpesaResponse>, t: Throwable) {
                onProcessing(false)
                onError("Network error: ${t.message}")
                Log.e("PackagesScreen", "✗ M-Pesa Network Error: ${t.message}")
                t.printStackTrace()
            }
        })
}

@Composable
fun PackagesScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    var plans by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var userDetail by remember { mutableStateOf<UserDetail?>(null) }
    var currency by remember { mutableStateOf("KES") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPlan by remember { mutableStateOf<Plan?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val userId = preferencesManager.getUserId()
        val token = preferencesManager.getToken()

        if (userId == -1 || token == null) {
            errorMessage = "User not logged in"
            isLoading = false
            return@LaunchedEffect
        }

        Log.d("PackagesScreen", "=== Fetching Recharge Plans ===")
        Log.d("PackagesScreen", "User ID: $userId")
        Log.d("PackagesScreen", "Token: ${token.take(20)}...")
        Log.d("PackagesScreen", "Authorization Header: Bearer ${token.take(20)}...")

        PackagesApiClient.instance.getRechargePlans(userId, "Bearer $token")
            .enqueue(object : Callback<RechargePlansResponse> {
                override fun onResponse(
                    call: Call<RechargePlansResponse>,
                    response: Response<RechargePlansResponse>
                ) {
                    isLoading = false
                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        plans = data.plan
                        userDetail = data.user
                        currency = data.currency
                        Log.d("PackagesScreen", "✓ Successfully loaded ${plans.size} plans")
                        Log.d("PackagesScreen", "[v0] User's current planId: ${userDetail?.planId}")
                        plans.forEach { plan ->
                            val isActive = plan.id == userDetail?.planId
                            Log.d("PackagesScreen", "[v0] Plan: ${plan.planName} (ID: ${plan.id}) - Active: $isActive")
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        errorMessage = "Failed to load plans: ${response.code()}"
                        Log.e("PackagesScreen", "✗ Error Response Code: ${response.code()}")
                        Log.e("PackagesScreen", "✗ Error Message: ${response.message()}")
                        Log.e("PackagesScreen", "✗ Error Body: $errorBody")
                    }
                }

                override fun onFailure(call: Call<RechargePlansResponse>, t: Throwable) {
                    isLoading = false
                    errorMessage = "Network error: ${t.message}"
                    Log.e("PackagesScreen", "✗ Network Error: ${t.message}")
                    Log.e("PackagesScreen", "✗ Error Type: ${t.javaClass.simpleName}")
                    t.printStackTrace()
                }
            })
    }

    if (showPaymentDialog && selectedPlan != null) {
        val decodedCurrency = currency.decodeHtml()
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Confirm Recharge") },
            text = {
                Column {
                    Text("Plan: ${selectedPlan!!.planName}")
                    Text("Amount: $decodedCurrency ${selectedPlan!!.customerCost}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Proceed to payment?", fontWeight = FontWeight.Bold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPaymentDialog = false
                        processRecharge(
                            plan = selectedPlan!!,
                            userDetail = userDetail!!,
                            preferencesManager = preferencesManager,
                            context = context,
                            onProcessing = { isProcessing = it },
                            onError = { errorMessage = it }
                        )
                    },
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Confirm")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            plans.isEmpty() -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "No packages available",
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            else -> {
                plans.forEach { plan ->
                    PackageCard(
                        plan = plan,
                        currency = currency,
                        isActive = plan.id == userDetail?.planId,
                        onSubscribe = {
                            selectedPlan = plan
                            showPaymentDialog = true
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PackageCard(
    plan: Plan,
    currency: String,
    isActive: Boolean,
    onSubscribe: () -> Unit
) {
    val decodedCurrency = currency.decodeHtml()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (isActive) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = plan.planName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                if (isActive) {
                    Badge(
                        containerColor = Color(0xFF4CAF50), // Green
                        contentColor = Color.White
                    ) {
                        Text("Active")
                    }
                }
            }

            HorizontalDivider()

            Text(
                text = "$decodedCurrency ${plan.customerCost}",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF42A5F5), // Light blue
                fontWeight = FontWeight.Bold
            )

            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF42A5F5), // Light blue
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(if (isActive) "Current Package" else "Subscribe")
            }
        }
    }
}

@Composable
fun HorizontalDivider() {
    Divider(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}
