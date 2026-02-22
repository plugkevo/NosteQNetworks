package com.kevannTechnologies.nosteqCustomers

import android.content.Context
import android.text.Html
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kevannTechnologies.nosteqCustomers.models.MpesaResponse
import com.kevannTechnologies.nosteqCustomers.models.Plan
import com.kevannTechnologies.nosteqCustomers.models.RechargeListResponse
import com.kevannTechnologies.nosteqCustomers.models.RechargePlansResponse
import com.kevannTechnologies.nosteqCustomers.models.RechargeRequest
import com.kevannTechnologies.nosteqCustomers.models.RechargeResponse
import com.kevannTechnologies.nosteqCustomers.models.UserDetail
import com.nosteq.provider.utils.PreferencesManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response




fun String.decodeHtml(): String {
    return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
}

@Composable
fun PackagesScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var plans by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var userDetail by remember { mutableStateOf<UserDetail?>(null) }
    var currency by remember { mutableStateOf("KES") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPlan by remember { mutableStateOf<Plan?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var customPhoneNumber by remember { mutableStateOf("") }
    var useCustomPhone by remember { mutableStateOf(false) }
    var showConfigWarning by remember { mutableStateOf(false) }
    var paymentStatusType by remember { mutableStateOf<String?>(null) } // "success", "error", or null
    var isVerifyingPayment by remember { mutableStateOf(false) }
    var pendingCheckoutId by remember { mutableStateOf<String?>(null) }
    var initialRechargeCount by remember { mutableStateOf(0) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Home", "Business")

    // Scroll to top when error message changes
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            coroutineScope.launch {
                scrollState.animateScrollTo(0)
            }
        }
    }

    LaunchedEffect(Unit) {
        val userId = preferencesManager.getUserId()
        val token = preferencesManager.getToken()

        if (userId == -1 || token == null) {
            errorMessage = "User not logged in"
            isLoading = false
            return@LaunchedEffect
        }

        fun loadUserPlans() {
            PackagesApiClient.instance.getRechargePlans(userId, "Bearer $token")
                .enqueue(object : Callback<RechargePlansResponse> {
                    override fun onResponse(
                        call: Call<RechargePlansResponse>,
                        response: Response<RechargePlansResponse>
                    ) {
                        isLoading = false
                        AppLogger.logApiCall(
                            endpoint = "getRechargePlans",
                            success = response.isSuccessful,
                            responseCode = response.code()
                        )

                        if (response.isSuccessful && response.body() != null) {
                            val data = response.body()!!
                            plans = data.plan
                            userDetail = data.user
                            currency = data.currency
                        } else {
                            errorMessage = "Failed to load plans"
                        }
                    }

                    override fun onFailure(call: Call<RechargePlansResponse>, t: Throwable) {
                        isLoading = false
                        errorMessage = "Network error. Please try again."
                        AppLogger.logError("PackagesScreen: Failed to load plans", t)
                    }
                })
        }

        loadUserPlans()
    }

    if (showPaymentDialog && selectedPlan != null) {
        val decodedCurrency = currency.decodeHtml()
        AlertDialog(
            onDismissRequest = {
                showPaymentDialog = false
                useCustomPhone = false
                customPhoneNumber = ""
            },
            title = { Text("Confirm Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Plan: ${selectedPlan!!.planName}")
                    Text("Amount: $decodedCurrency ${selectedPlan!!.customerCost}")

                    HorizontalDivider()

                    Text("Payment Phone Number", fontWeight = FontWeight.Bold)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = !useCustomPhone,
                            onClick = { useCustomPhone = false }
                        )
                        Text(
                            text = "My number: ${userDetail?.userinfo?.phone ?: "N/A"}",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = useCustomPhone,
                            onClick = { useCustomPhone = true }
                        )
                        Text(
                            text = "Different number",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    if (useCustomPhone) {
                        OutlinedTextField(
                            value = customPhoneNumber,
                            onValueChange = { customPhoneNumber = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("254712345678") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Text(
                            text = "Format: 254XXXXXXXXX",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "A payment request will be sent to the selected phone.",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val phoneToUse = if (useCustomPhone) {
                            if (customPhoneNumber.isBlank()) {
                                errorMessage = "Please enter a phone number"
                                return@Button
                            }
                            customPhoneNumber
                        } else {
                            userDetail?.userinfo?.phone ?: ""
                        }

                        showPaymentDialog = false
                        isProcessing = true
                        coroutineScope.launch {
                            try {
                                val token = preferencesManager.getToken() ?: ""

                                // First, fetch the current recharge list to get initial count
                                PackagesApiClient.instance.getRechargeList("Bearer $token")
                                    .enqueue(object : Callback<RechargeListResponse> {
                                        override fun onResponse(
                                            call: Call<RechargeListResponse>,
                                            response: Response<RechargeListResponse>
                                        ) {
                                            if (response.isSuccessful && response.body() != null) {
                                                val rechargeList = response.body()!!.data ?: emptyList()
                                                initialRechargeCount = rechargeList.size
                                                android.util.Log.d("PackagesScreen", "[v0] Initial recharge count set to: $initialRechargeCount")

                                                // Now proceed with M-Pesa payment
                                                val rechargeRequest = RechargeRequest(
                                                    rechargePlan = selectedPlan?.id ?: 0,
                                                    quantity = 1,
                                                    rechargeType = 1,
                                                    resetRecharge = false,
                                                    contactPerson = userDetail?.userinfo?.contactPerson ?: "",
                                                    email = userDetail?.userinfo?.email ?: "",
                                                    phone = phoneToUse,
                                                    city = userDetail?.userinfo?.billingCity ?: "",
                                                    zip = userDetail?.userinfo?.billingZip ?: ""
                                                )

                                                android.util.Log.d("PackagesScreen", "[v0] Initiating M-Pesa payment - Plan: ${selectedPlan?.id}, Phone: $phoneToUse")

                                                // Call the M-Pesa payment endpoint
                                                PackagesApiClient.instance.processMpesaPayment(
                                                    token = "Bearer $token",
                                                    rechargeRequest = rechargeRequest
                                                ).enqueue(object : Callback<MpesaResponse> {
                                                    override fun onResponse(
                                                        call: Call<MpesaResponse>,
                                                        response: Response<MpesaResponse>
                                                    ) {
                                                        isProcessing = false

                                                        if (response.isSuccessful && response.body() != null) {
                                                            val mpesaResponse = response.body()!!
                                                            android.util.Log.d("PackagesScreen", "[v0] M-Pesa full response: $mpesaResponse")
                                                            android.util.Log.d("PackagesScreen", "[v0] M-Pesa status: ${mpesaResponse.status}, CheckoutId: ${mpesaResponse.data?.checkoutId}, Data: ${mpesaResponse.data}")

                                                            // Consider success if we have checkoutId in data (payment initiated successfully)
                                                            if (mpesaResponse.data != null && mpesaResponse.data.checkoutId.isNotEmpty()) {
                                                                val checkoutId = mpesaResponse.data.checkoutId
                                                                val amount = mpesaResponse.data.amount
                                                                val txnRef = mpesaResponse.data.txnRef

                                                                errorMessage = "M-Pesa STK push sent. Verifying payment..."
                                                                paymentStatusType = "success"
                                                                isVerifyingPayment = true
                                                                pendingCheckoutId = checkoutId
                                                                android.util.Log.d("PackagesScreen", "[v0] M-Pesa STK push sent - CheckoutID: $checkoutId, TxnRef: $txnRef, Amount: $amount")

                                                                // Start polling recharge list to verify payment
                                                                var pollCount = 0
                                                                val maxPolls = 60

                                                                fun pollPaymentVerification() {
                                                                    if (pollCount >= maxPolls || !isVerifyingPayment) {
                                                                        if (pollCount >= maxPolls) {
                                                                            errorMessage = "Payment verification timeout. Please refresh to check payment status."
                                                                            paymentStatusType = "error"
                                                                            isVerifyingPayment = false
                                                                        }
                                                                        return
                                                                    }

                                                                    pollCount++
                                                                    val token = preferencesManager.getToken() ?: ""

                                                                    PackagesApiClient.instance.getRechargeList("Bearer $token")
                                                                        .enqueue(object : Callback<RechargeListResponse> {
                                                                            override fun onResponse(
                                                                                call: Call<RechargeListResponse>,
                                                                                response: Response<RechargeListResponse>
                                                                            ) {
                                                                                if (response.isSuccessful && response.body() != null) {
                                                                                    val data = response.body()!!
                                                                                    val rechargeList = data.data ?: emptyList()
                                                                                    val currentRechargeCount = rechargeList.size

                                                                                    android.util.Log.d("PackagesScreen", "[v0] Poll #$pollCount: Current recharges=$currentRechargeCount, Initial=$initialRechargeCount")

                                                                                    // Check if a new recharge was added
                                                                                    if (currentRechargeCount > initialRechargeCount) {
                                                                                        android.util.Log.d("PackagesScreen", "[v0] Payment verified! New recharge detected.")
                                                                                        isVerifyingPayment = false
                                                                                        errorMessage = "Payment successful! Your package has been activated."
                                                                                        paymentStatusType = "success"
                                                                                        useCustomPhone = false
                                                                                        customPhoneNumber = ""
                                                                                        pendingCheckoutId = null
                                                                                    } else {
                                                                                        // Payment not yet confirmed, continue polling
                                                                                        coroutineScope.launch {
                                                                                            kotlinx.coroutines.delay(3000)
                                                                                            pollPaymentVerification()
                                                                                        }
                                                                                    }
                                                                                } else {
                                                                                    // Continue polling on error
                                                                                    coroutineScope.launch {
                                                                                        kotlinx.coroutines.delay(3000)
                                                                                        pollPaymentVerification()
                                                                                    }
                                                                                }
                                                                            }

                                                                            override fun onFailure(call: Call<RechargeListResponse>, t: Throwable) {
                                                                                // Continue polling on network error
                                                                                android.util.Log.d("PackagesScreen", "[v0] Poll #$pollCount failed: ${t.message}")
                                                                                coroutineScope.launch {
                                                                                    kotlinx.coroutines.delay(3000)
                                                                                    pollPaymentVerification()
                                                                                }
                                                                            }
                                                                        })
                                                                }

                                                                // Start the polling
                                                                pollPaymentVerification()
                                                            } else {
                                                                errorMessage = "Payment initiation failed. Please try again."
                                                                paymentStatusType = "error"
                                                            }
                                                        } else {
                                                            errorMessage = "Payment initiation failed. Please try again."
                                                            paymentStatusType = "error"
                                                        }
                                                    }

                                                    override fun onFailure(call: Call<MpesaResponse>, t: Throwable) {
                                                        isProcessing = false
                                                        errorMessage = "Network error: ${t.message}"
                                                        paymentStatusType = "error"
                                                        android.util.Log.e("PackagesScreen", "[v0] M-Pesa payment error: ${t.message}", t)
                                                    }
                                                })
                                            } else {
                                                isProcessing = false
                                                errorMessage = "Failed to fetch recharge history"
                                                paymentStatusType = "error"
                                            }
                                        }

                                        override fun onFailure(call: Call<RechargeListResponse>, t: Throwable) {
                                            isProcessing = false
                                            errorMessage = "Network error: ${t.message}"
                                            paymentStatusType = "error"
                                            android.util.Log.e("PackagesScreen", "[v0] Failed to fetch initial recharge count: ${t.message}", t)
                                        }
                                    })
                            } catch (e: Exception) {
                                isProcessing = false
                                errorMessage = "Error: ${e.message}"
                                paymentStatusType = "error"
                                android.util.Log.e("PackagesScreen", "[v0] Exception: ${e.message}", e)
                                AppLogger.logError("PackagesScreen: Exception in payment flow", e)
                                useCustomPhone = false
                                customPhoneNumber = ""
                            }
                        }
                    },
                    enabled = !isProcessing
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Text("Pay with M-Pesa")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPaymentDialog = false
                    useCustomPhone = false
                    customPhoneNumber = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Available Packages",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (showConfigWarning) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3CD)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Payment System",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF856404)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "New payment method coming soon.",
                            color = Color(0xFF856404)
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color(0xFF42A5F5)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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
                    val isSuccess = paymentStatusType == "success"
                    val backgroundColor = if (isSuccess) Color(0xFFD4EDDA) else Color(0xFFF8D7DA)
                    val textColor = if (isSuccess) Color(0xFF155724) else Color(0xFF721C24)
                    val borderColor = if (isSuccess) Color(0xFF28A745) else Color(0xFFDC3545)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, borderColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (isVerifyingPayment) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = textColor
                                )
                            }
                            Text(
                                text = errorMessage!!,
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                else -> {
                    val filteredPlans = plans.filter { plan ->
                        when (selectedTabIndex) {
                            0 -> !plan.planName.contains("business", ignoreCase = true) // Home packages
                            1 -> plan.planName.contains("business", ignoreCase = true) // Business packages
                            else -> true
                        }
                    }

                    if (filteredPlans.isEmpty()) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "No ${tabs[selectedTabIndex].lowercase()} packages available",
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        filteredPlans.forEach { plan ->
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
                        containerColor = Color(0xFF4CAF50),
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
                color = Color(0xFF42A5F5),
                fontWeight = FontWeight.Bold,
            )

            Button(
                onClick = onSubscribe,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF42A5F5),
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
