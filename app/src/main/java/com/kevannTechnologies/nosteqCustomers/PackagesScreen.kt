package com.kevannTechnologies.nosteqCustomers

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
import com.kevannTechnologies.nosteqCustomers.models.Plan
import com.kevannTechnologies.nosteqCustomers.models.RechargePlansResponse
import com.kevannTechnologies.nosteqCustomers.models.RechargeRequest
import com.kevannTechnologies.nosteqCustomers.models.UserDetail
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
    username: String,
    context: Context
): Pair<Boolean, String?> {
    AppLogger.logInfo("PackagesScreen: Initiating payment", mapOf(
        "planId" to plan.id.toString(),
        "planName" to plan.planName,
        "amount" to plan.customerCost,
        "username" to username
    ))

    // Check M-Pesa configuration
    if (!MpesaConfigManager.isConfigured(context)) {
        AppLogger.logError("PackagesScreen: M-Pesa not configured")
        return Pair(false, "M-Pesa not configured. Please configure M-Pesa settings first.")
    }

    val mpesaConfig = MpesaConfigManager.getConfig(context)
    val formattedPhone = phoneNumber.formatForMpesa()
    val amount = plan.customerCost.toDouble().toInt()

    val mpesaManager = MpesaManager(mpesaConfig)
    val stkResponse = mpesaManager.initiateStkPush(
        phoneNumber = formattedPhone,
        amount = amount.toString(),
        accountReference = username,
        transactionDesc = plan.planName
    )

    val success = stkResponse != null && stkResponse.responseCode == "0"
    AppLogger.logApiCall(
        endpoint = "mpesa/stkpush",
        success = success,
        responseCode = stkResponse?.responseCode?.toIntOrNull(),
        errorMessage = if (!success) (stkResponse?.errorMessage ?: stkResponse?.responseDescription) else null
    )

    return if (success) {
        // Return the checkoutRequestID to use for payment verification
        Pair(true, stkResponse?.checkoutRequestID)
    } else {
        val error = stkResponse?.errorMessage ?: stkResponse?.responseDescription ?: "Payment failed"
        Pair(false, null)
    }
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
    var showConfigWarning by remember { mutableStateOf(false) }
    var customPhoneNumber by remember { mutableStateOf("") }
    var useCustomPhone by remember { mutableStateOf(false) }

    var checkoutRequestID by remember { mutableStateOf<String?>(null) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var isCheckingStatus by remember { mutableStateOf(false) }

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
        showConfigWarning = !MpesaConfigManager.isConfigured(context)

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
                            AppLogger.logInfo("PackagesScreen: Checking payment status", mapOf(
                                "checkoutRequestID" to checkoutRequestID!!
                            ))

                            val mpesaConfig = MpesaConfigManager.getConfig(context)
                            val mpesaManager = MpesaManager(mpesaConfig)
                            val queryResponse = mpesaManager.queryStkPushStatus(checkoutRequestID!!)

                            isCheckingStatus = false

                            AppLogger.logApiCall(
                                endpoint = "mpesa/stkquery",
                                success = queryResponse?.resultCode == "0",
                                responseCode = queryResponse?.resultCode?.toIntOrNull(),
                                errorMessage = queryResponse?.resultDesc
                            )

                            if (queryResponse != null) {
                                if (queryResponse.resultCode == "0") {
                                    // Payment successful - now update the user's plan in the system
                                    try {
                                        val token = preferencesManager.getToken() ?: ""
                                        val rechargeRequest = RechargeRequest(
                                            rechargePlan = selectedPlan?.id ?: 0,
                                            quantity = 1,
                                            rechargeType = 1,
                                            resetRecharge = false,
                                            contactPerson = userDetail?.userinfo?.contactPerson ?: "",
                                            email = userDetail?.userinfo?.email ?: "",
                                            phone = userDetail?.userinfo?.phone ?: "",
                                            city = userDetail?.userinfo?.billingCity ?: "",
                                            zip = userDetail?.userinfo?.billingZip ?: ""
                                        )

                                        val rechargeResponse = try {
                                            android.util.Log.d("PackagesScreen", "[v0] Sending recharge request - Plan: ${selectedPlan?.id}, Phone: ${rechargeRequest.phone}")
                                            PackagesApiClient.instance.processRecharge(
                                                token = "Bearer $token",
                                                rechargeRequest = rechargeRequest
                                            ).execute()
                                        } catch (e: Exception) {
                                            android.util.Log.e("PackagesScreen", "[v0] Exception during processRecharge: ${e.message}", e)
                                            AppLogger.logError("PackagesScreen: Exception updating plan", e)
                                            null
                                        }

                                        if (rechargeResponse?.isSuccessful == true && rechargeResponse?.body()?.status == "success") {
                                            errorMessage = "Payment successful! Your package has been activated."
                                            AppLogger.logInfo("PackagesScreen: Plan updated successfully", mapOf(
                                                "newPlanId" to (selectedPlan?.id ?: 0).toString()
                                            ))

                                            // Reload user data to show new plan
                                            val userId = preferencesManager.getUserId()
                                            val token = preferencesManager.getToken()
                                            if (userId != -1 && token != null) {
                                                PackagesApiClient.instance.getRechargePlans(userId, "Bearer $token")
                                                    .enqueue(object : Callback<RechargePlansResponse> {
                                                        override fun onResponse(
                                                            call: Call<RechargePlansResponse>,
                                                            response: Response<RechargePlansResponse>
                                                        ) {
                                                            if (response.isSuccessful && response.body() != null) {
                                                                val data = response.body()!!
                                                                plans = data.plan
                                                                userDetail = data.user
                                                                android.util.Log.d("PackagesScreen", "[v0] Updated user plan: ${data.user?.planName}")
                                                            }
                                                        }

                                                        override fun onFailure(call: Call<RechargePlansResponse>, t: Throwable) {
                                                            AppLogger.logError("PackagesScreen: Failed to reload user data", t)
                                                        }
                                                    })
                                            }
                                        } else {
                                            val responseStatus = rechargeResponse?.body()?.status ?: "null"
                                            val responseCode = rechargeResponse?.code() ?: -1
                                            val isSuccess = rechargeResponse?.isSuccessful ?: false
                                            android.util.Log.e("PackagesScreen", "[v0] Recharge failed - IsSuccessful: $isSuccess, Code: $responseCode, Status: $responseStatus")

                                            val errorBody = try {
                                                rechargeResponse?.errorBody()?.string() ?: "No error body"
                                            } catch (e: Exception) {
                                                "Could not read error body"
                                            }
                                            android.util.Log.e("PackagesScreen", "[v0] Error Response Body: $errorBody")

                                            errorMessage = "Payment verified but failed to activate plan (Code: $responseCode, Status: $responseStatus). Please contact support."
                                            AppLogger.logError("PackagesScreen: Failed to update plan", null, mapOf(
                                                "status" to responseStatus.toString(),
                                                "responseCode" to responseCode.toString(),
                                                "errorBody" to errorBody,
                                                "isSuccessful" to isSuccess.toString()
                                            ))
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("PackagesScreen", "[v0] Exception in plan update: ${e.message}", e)
                                        errorMessage = "Payment successful but error updating plan: ${e.message}. Please contact support."
                                        AppLogger.logError("PackagesScreen: Exception in plan update", e)
                                    }
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
                                showConfirmationDialog = false
                                checkoutRequestID = null
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
                            val (success, checkoutRequestId) = processRechargeWithPhone(
                                plan = selectedPlan!!,
                                phoneNumber = phoneToUse,
                                username = userDetail?.username ?: "",
                                context = context
                            )
                            isProcessing = false

                            if (success && checkoutRequestId != null) {
                                checkoutRequestID = checkoutRequestId
                                showConfirmationDialog = true
                            } else {
                                errorMessage = "Payment failed. Please try again."
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
