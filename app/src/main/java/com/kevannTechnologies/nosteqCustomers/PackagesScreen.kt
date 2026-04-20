package com.kevannTechnologies.nosteqCustomers

import android.text.Html
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kevannTechnologies.nosteqCustomers.models.Plan
import com.kevannTechnologies.nosteqCustomers.models.UserDetail
import com.nosteq.provider.utils.PreferencesManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


fun String.decodeHtml(): String {
    return Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
}

fun isSubscriptionExpired(expiryDate: String?): Boolean {
    if (expiryDate.isNullOrBlank()) return false
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val expiry = sdf.parse(expiryDate) ?: return false
        val today = Calendar.getInstance().time
        today.after(expiry)
    } catch (e: Exception) {
        false
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
    var customPhoneNumber by remember { mutableStateOf("") }
    var useCustomPhone by remember { mutableStateOf(false) }
    var showConfigWarning by remember { mutableStateOf(false) }
    var paymentStatusType by remember { mutableStateOf<String?>(null) }
    var isVerifyingPayment by remember { mutableStateOf(false) }
    var pendingCheckoutId by remember { mutableStateOf<String?>(null) }
    var initialRechargeCount by remember { mutableStateOf(0) }
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabs = listOf("Home", "Business")

    val packagesActivity = remember {
        PackagesActivity(
            preferencesManager = preferencesManager,
            coroutineScope = coroutineScope,
            onStateUpdate = { errorMsg, statusType, processing, verifying, customPhone, phoneNum, checkoutId ->
                errorMessage = errorMsg
                paymentStatusType = statusType
                isProcessing = processing
                isVerifyingPayment = verifying
                useCustomPhone = customPhone
                customPhoneNumber = phoneNum
                pendingCheckoutId = checkoutId
            }
        )
    }

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

        packagesActivity.loadPlans(userId, "Bearer $token") { success, error, loadedPlans, loadedCurrency, loadedUserDetail ->
            isLoading = false
            if (success && loadedPlans != null) {
                plans = loadedPlans
                currency = loadedCurrency ?: "KES"
                userDetail = loadedUserDetail
                android.util.Log.d("PackagesScreen", "[v0] User detail loaded: ${userDetail?.userinfo?.phone}")
            } else {
                errorMessage = error ?: "Failed to load plans"
            }
        }
    }

    var numberOfMonths by remember { mutableStateOf(1) }

    if (showPaymentDialog && selectedPlan != null) {
        PaymentDialog(
            selectedPlan = selectedPlan!!,
            userDetail = userDetail,
            currency = currency,
            useCustomPhone = useCustomPhone,
            customPhoneNumber = customPhoneNumber,
            numberOfMonths = numberOfMonths,
            isProcessing = isProcessing,
            onUseCustomPhoneChange = { useCustomPhone = it },
            onPhoneNumberChange = { customPhoneNumber = it },
            onNumberOfMonthsChange = { numberOfMonths = it },
            onDismiss = {
                showPaymentDialog = false
                useCustomPhone = false
                customPhoneNumber = ""
                numberOfMonths = 1
            },
            onConfirmPayment = {
                // Capture values before resetting
                val useCustomPhoneValue = useCustomPhone
                val customPhoneValue = customPhoneNumber
                val numberOfMonthsValue = numberOfMonths

                // Close the dialog immediately so user can see status messages
                showPaymentDialog = false
                useCustomPhone = false
                customPhoneNumber = ""
                numberOfMonths = 1

                // Process payment in background with captured values
                packagesActivity.processPayment(
                    selectedPlan = selectedPlan,
                    userDetail = userDetail,
                    useCustomPhone = useCustomPhoneValue,
                    customPhoneNumber = customPhoneValue,
                    numberOfMonths = numberOfMonthsValue,
                    initialRechargeCount = initialRechargeCount
                ) { _, _, _, _ -> }
            }
        )
    }

    PackagesScreenContent(
        plans = plans,
        userDetail = userDetail,
        currency = currency,
        isLoading = isLoading,
        errorMessage = errorMessage,
        paymentStatusType = paymentStatusType,
        isVerifyingPayment = isVerifyingPayment,
        selectedTabIndex = selectedTabIndex,
        scrollState = scrollState,
        tabs = tabs,
        onTabChange = { selectedTabIndex = it },
        onSubscribeClick = { plan ->
            selectedPlan = plan
            showPaymentDialog = true
        }
    )
}

@Composable
fun PaymentDialog(
    selectedPlan: Plan,
    userDetail: UserDetail?,
    currency: String,
    useCustomPhone: Boolean,
    customPhoneNumber: String,
    numberOfMonths: Int,
    isProcessing: Boolean,
    onUseCustomPhoneChange: (Boolean) -> Unit,
    onPhoneNumberChange: (String) -> Unit,
    onNumberOfMonthsChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onConfirmPayment: () -> Unit
) {
    val decodedCurrency = currency.decodeHtml()
    val monthlyPrice = selectedPlan.customerCost.toDoubleOrNull() ?: 0.0
    val totalPrice = (monthlyPrice * numberOfMonths).toLong()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Payment") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Plan: ${selectedPlan.planName}")
                Text("Monthly Amount: $decodedCurrency ${selectedPlan.customerCost}")

                // Number of months section
                Text("Number of Months", fontWeight = FontWeight.Bold)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            if (numberOfMonths > 1) {
                                onNumberOfMonthsChange(numberOfMonths - 1)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("-", fontWeight = FontWeight.Bold)
                    }

                    OutlinedTextField(
                        value = numberOfMonths.toString(),
                        onValueChange = { value ->
                            val newValue = value.toIntOrNull() ?: 1
                            if (newValue > 0) {
                                onNumberOfMonthsChange(newValue)
                            }
                        },
                        modifier = Modifier
                            .width(80.dp),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
                    )

                    Button(
                        onClick = { onNumberOfMonthsChange(numberOfMonths + 1) },
                        modifier = Modifier.size(40.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("+", fontWeight = FontWeight.Bold)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Total Amount",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "$decodedCurrency $totalPrice",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text("Amount: $decodedCurrency ${selectedPlan.customerCost}")

                HorizontalDivider()

                Text("Payment Phone Number", fontWeight = FontWeight.Bold)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    RadioButton(
                        selected = !useCustomPhone,
                        onClick = { onUseCustomPhoneChange(false) }
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
                        onClick = { onUseCustomPhoneChange(true) }
                    )
                    Text(
                        text = "Different number",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (useCustomPhone) {
                    OutlinedTextField(
                        value = customPhoneNumber,
                        onValueChange = onPhoneNumberChange,
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
                onClick = onConfirmPayment,
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
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PackagesScreenContent(
    plans: List<Plan>,
    userDetail: UserDetail?,
    currency: String,
    isLoading: Boolean,
    errorMessage: String?,
    paymentStatusType: String?,
    isVerifyingPayment: Boolean,
    selectedTabIndex: Int,
    scrollState: androidx.compose.foundation.ScrollState,
    tabs: List<String>,
    onTabChange: (Int) -> Unit,
    onSubscribeClick: (Plan) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        val isDarkMode = isSystemInDarkTheme()
        val headerBgColor = Color(0xFF00BCD4)

        // Header section with gradient background
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(headerBgColor)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choose Your Plan",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Select the perfect package for your needs",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
        }

        // Tab section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val tabContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
            val tabBorderColor = if (isDarkMode) Color(0xFF404040) else Color(0xFFE0E0E0)
            val tabTextColorInactive = if (isDarkMode) Color(0xFF999999) else Color.Gray

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = tabContainerColor,
                contentColor = Color(0xFF00BCD4),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tabContainerColor, RoundedCornerShape(12.dp))
                    .border(1.dp, tabBorderColor, RoundedCornerShape(12.dp))
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { onTabChange(index) },
                        modifier = Modifier.padding(vertical = 12.dp),
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = if (selectedTabIndex == index) 16.sp else 14.sp,
                                color = if (selectedTabIndex == index) Color(0xFF00BCD4) else tabTextColorInactive
                            )
                        }
                    )
                }
            }
        }

        // Content section
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF00BCD4),
                            strokeWidth = 4.dp,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                errorMessage != null -> {
                    val isSuccess = paymentStatusType == "success"
                    val isLoadingState = paymentStatusType == "loading"

                    val (backgroundColor, textColor, borderColor) = when {
                        isLoadingState -> if (isDarkMode) {
                            Triple(Color(0xFF0D47A1).copy(alpha = 0.3f), Color(0xFF64B5F6), Color(0xFF1976D2))
                        } else {
                            Triple(Color(0xFFE3F2FD), Color(0xFF1565C0), Color(0xFF1976D2))
                        }
                        isSuccess -> if (isDarkMode) {
                            Triple(Color(0xFF1B5E20).copy(alpha = 0.3f), Color(0xFF81C784), Color(0xFF4CAF50))
                        } else {
                            Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), Color(0xFF4CAF50))
                        }
                        else -> if (isDarkMode) {
                            Triple(Color(0xFFB71C1C).copy(alpha = 0.3f), Color(0xFFEF5350), Color(0xFFF44336))
                        } else {
                            Triple(Color(0xFFFFEBEE), Color(0xFFC62828), Color(0xFFF44336))
                        }
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, borderColor, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = backgroundColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(20.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (isVerifyingPayment || isLoadingState) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(40.dp),
                                    color = textColor,
                                    strokeWidth = 3.dp
                                )
                            }
                            Text(
                                text = errorMessage,
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                else -> {
                    val filteredPlans = plans.filter { plan ->
                        when (selectedTabIndex) {
                            0 -> !plan.planName.contains("business", ignoreCase = true)
                            1 -> plan.planName.contains("business", ignoreCase = true)
                            else -> true
                        }
                    }

                    if (filteredPlans.isEmpty()) {
                        val emptyStateColor = if (isDarkMode) Color(0xFF2A2A2A) else Color(0xFFF5F5F5)
                        val emptyTextColor = if (isDarkMode) Color(0xFF999999) else Color.Gray

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(containerColor = emptyStateColor)
                        ) {
                            Text(
                                text = "No ${tabs[selectedTabIndex].lowercase()} packages available",
                                modifier = Modifier.padding(32.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = emptyTextColor
                            )
                        }
                    } else {
                        filteredPlans.forEach { plan ->
                            PackageCard(
                                plan = plan,
                                currency = currency,
                                isActive = plan.id == userDetail?.planId && !isSubscriptionExpired(userDetail?.expiryDate),
                                onSubscribe = { onSubscribeClick(plan) },
                                isDarkMode = isDarkMode
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
    onSubscribe: () -> Unit,
    isDarkMode: Boolean = false
) {
    val decodedCurrency = currency.decodeHtml()
    val isRecommended = plan.planName.contains("Business", ignoreCase = true)

    // Modern color scheme with dark mode support
    val primaryColor = Color(0xFF00BCD4)
    val accentColor = Color(0xFF00ACC1)

    val surfaceColor = when {
        isDarkMode && (isActive || isRecommended) -> Color(0xFF1A1A1A)
        isDarkMode -> Color(0xFF121212)
        isActive || isRecommended -> Color(0xFF1A237E).copy(alpha = 0.05f)
        else -> Color.White
    }

    val textColor = if (isDarkMode) Color.White else Color.Black
    val subtextColor = if (isDarkMode) Color(0xFFB0B0B0) else Color.Gray
    val borderColor = if (isRecommended) primaryColor else (if (isDarkMode) Color(0xFF404040) else Color(0xFFE0E0E0))
    val borderWidth = if (isRecommended) 2.dp else 1.dp
    val buttonDisabledColor = if (isDarkMode) Color(0xFF424242) else Color(0xFFE0E0E0)
    val buttonDisabledTextColor = if (isDarkMode) Color(0xFF999999) else Color.DarkGray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = surfaceColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isRecommended) 12.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with plan name and badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = plan.planName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                }

                if (isActive) {
                    Badge(
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Text("Active", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Price section - prominently displayed
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = decodedCurrency,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtextColor
                )
                Text(
                    text = plan.customerCost,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryColor
                )
                Text(
                    text = "/month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtextColor
                )
            }

            // Features list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Unlimited data access",
                    "24/7 customer support",
                    "Priority network speed"
                ).forEach { feature ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓",
                            color = Color(0xFF4CAF50),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall,
                            color = subtextColor
                        )
                    }
                }
            }

            // Subscribe/Current button
            Button(
                onClick = onSubscribe,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = true,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    disabledContainerColor = buttonDisabledColor,
                    disabledContentColor = buttonDisabledTextColor
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (isActive) "Current Package" else "Subscribe Now",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
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
