package com.kevann.nosteq

import android.content.Context
import android.text.Html
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
import com.kevann.nosteq.models.UserDetail
import com.nosteq.provider.utils.PreferencesManager
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

fun String.decodeHtml(): String {
    return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
}

fun String.formatForMpesa(): String {
    var cleaned = this.replace(Regex("[\\s\\-()\\+]"), "")
    if (cleaned.startsWith("0")) {
        cleaned = cleaned.substring(1)
    }
    if (!cleaned.startsWith("254")) {
        cleaned = "254$cleaned"
    }
    return cleaned
}

private suspend fun processRechargeWithPhone(
    plan: Plan,
    phoneNumber: String,
    context: Context
): Pair<Boolean, String> {
    val mpesaConfig = MpesaConfigManager.getConfig(context)

    if (!MpesaConfigManager.isConfigured(context)) {
        return Pair(false, "M-Pesa not configured. Please configure M-Pesa settings first.")
    }

    val formattedPhone = phoneNumber.formatForMpesa()
    val amount = plan.customerCost.toDouble().toInt()

    val mpesaManager = MpesaManager(mpesaConfig)
    val response = mpesaManager.initiateStkPush(
        phoneNumber = formattedPhone,
        amount = amount.toString(),
        accountReference = "Plan-${plan.id}",
        transactionDesc = plan.planName
    )

    return if (response != null && response.responseCode == "0") {
        Pair(true, "Payment request sent! Check your phone to complete the payment.")
    } else {
        val error = response?.errorMessage ?: response?.responseDescription ?: "Payment failed"
        Pair(false, "Payment failed: $error")
    }
}

@Composable
fun PackagesScreen() {
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }
    val coroutineScope = rememberCoroutineScope()

    var plans by remember { mutableStateOf<List<Plan>>(emptyList()) }
    var userDetail by remember { mutableStateOf<UserDetail?>(null) }
    var currency by remember { mutableStateOf("KES") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedPlan by remember { mutableStateOf<Plan?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var showConfigWarning by remember { mutableStateOf(false) }
    var customPhoneNumber by remember { mutableStateOf("") }
    var useCustomPhone by remember { mutableStateOf(false) }

    var checkoutRequestID by remember { mutableStateOf<String?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var isCheckingStatus by remember { mutableStateOf(false) }

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Home", "Business")

    LaunchedEffect(Unit) {
        showConfigWarning = !MpesaConfigManager.isConfigured(context)

        val userId = preferencesManager.getUserId()
        val token = preferencesManager.getToken()

        if (userId == -1 || token == null) {
            errorMessage = "User not logged in"
            isLoading = false
            return@LaunchedEffect
        }

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
                    } else {
                        errorMessage = "Failed to load plans"
                    }
                }

                override fun onFailure(call: Call<RechargePlansResponse>, t: Throwable) {
                    isLoading = false
                    errorMessage = "Network error. Please try again."
                }
            })
    }

    if (showConfirmationDialog && checkoutRequestID != null) {
        AlertDialog(
            onDismissRequest = {
                showConfirmationDialog = false
                checkoutRequestID = null
            },
            title = { Text("Payment Sent") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("An M-Pesa payment request has been sent to your phone.")
                    Text("Please check your phone and enter your M-Pesa PIN to complete the payment.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "After completing the payment, click 'Confirm Payment' to verify.",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isCheckingStatus = true
                        coroutineScope.launch {
                            val mpesaConfig = MpesaConfigManager.getConfig(context)
                            val mpesaManager = MpesaManager(mpesaConfig)
                            val queryResponse = mpesaManager.queryStkPushStatus(checkoutRequestID!!)

                            isCheckingStatus = false

                            if (queryResponse != null) {
                                if (queryResponse.resultCode == "0") {
                                    errorMessage = "Payment successful! Your package has been activated."
                                    showConfirmationDialog = false
                                    checkoutRequestID = null
                                } else {
                                    val failureReason = queryResponse.resultDesc ?: "Payment was not completed"
                                    errorMessage = "Payment failed: $failureReason. Please try again."
                                    showConfirmationDialog = false
                                    checkoutRequestID = null
                                }
                            } else {
                                errorMessage = "Could not verify payment status. Please check your M-Pesa messages or try again."
                            }
                        }
                    },
                    enabled = !isCheckingStatus
                ) {
                    if (isCheckingStatus) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Confirm Payment")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        checkoutRequestID = null
                    },
                    enabled = !isCheckingStatus
                ) {
                    Text("Cancel")
                }
            }
        )
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
                        "An M-Pesa payment request will be sent to the selected phone.",
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
                            val (success, message) = processRechargeWithPhone(
                                plan = selectedPlan!!,
                                phoneNumber = phoneToUse,
                                context = context
                            )
                            isProcessing = false

                            if (success) {
                                checkoutRequestID = "CheckoutRequestID"
                                showConfirmationDialog = true
                            } else {
                                errorMessage = message
                            }

                            useCustomPhone = false
                            customPhoneNumber = ""
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
                            text = "M-Pesa Not Configured",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF856404)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please configure M-Pesa settings to enable payments.",
                            color = Color(0xFF856404)
                        )
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,

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
                .verticalScroll(rememberScrollState())
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
                else -> {
                    val filteredPlans = plans.filter { plan ->
                        when (selectedTabIndex) {
                            0 -> !plan.planName.contains("Business", ignoreCase = true) // Home packages
                            1 -> plan.planName.contains("Business", ignoreCase = true) // Business packages
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
