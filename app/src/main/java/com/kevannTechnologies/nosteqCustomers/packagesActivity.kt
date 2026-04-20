package com.kevannTechnologies.nosteqCustomers

import android.util.Log
import com.kevannTechnologies.nosteqCustomers.models.MpesaResponse
import com.kevannTechnologies.nosteqCustomers.models.Plan
import com.kevannTechnologies.nosteqCustomers.models.RechargeListResponse
import com.kevannTechnologies.nosteqCustomers.models.RechargeRequest
import com.kevannTechnologies.nosteqCustomers.models.UserDetail
import com.nosteq.provider.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PackagesActivity(
    private val preferencesManager: PreferencesManager,
    private val coroutineScope: CoroutineScope,
    private val onStateUpdate: (
        errorMessage: String?,
        paymentStatusType: String?,
        isProcessing: Boolean,
        isVerifyingPayment: Boolean,
        useCustomPhone: Boolean,
        customPhoneNumber: String,
        pendingCheckoutId: String?
    ) -> Unit
) {

    fun processPayment(
        selectedPlan: Plan?,
        userDetail: UserDetail?,
        useCustomPhone: Boolean,
        customPhoneNumber: String,
        numberOfMonths: Int = 1,
        initialRechargeCount: Int,
        onPaymentStateChange: (Boolean, String?, String?, Boolean) -> Unit
    ) {
        val phoneToUse = if (useCustomPhone) {
            if (customPhoneNumber.isBlank()) {
                onStateUpdate("Please enter a phone number", "error", false, false, false, "", null)
                return
            }
            customPhoneNumber
        } else {
            userDetail?.userinfo?.phone ?: ""
        }

        onStateUpdate("Processing payment...", "loading", true, false, false, "", null)

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
                                val currentInitialCount = rechargeList.size
                                Log.d("PackagesActivity", "[v0] Initial recharge count set to: $currentInitialCount")

                                // Now proceed with M-Pesa payment
                                // Calculate total amount based on number of months
                                val monthlyPrice = selectedPlan?.customerCost?.toDoubleOrNull() ?: 0.0
                                val totalAmount = (monthlyPrice * numberOfMonths).toLong()
                                
                                val rechargeRequest = RechargeRequest(
                                    rechargePlan = selectedPlan?.id ?: 0,
                                    quantity = numberOfMonths,
                                    rechargeType = 1,
                                    resetRecharge = false,
                                    contactPerson = userDetail?.userinfo?.contactPerson ?: "",
                                    email = userDetail?.userinfo?.email ?: "",
                                    phone = phoneToUse,
                                    city = userDetail?.userinfo?.billingCity ?: "",
                                    zip = userDetail?.userinfo?.billingZip ?: ""
                                )

                                Log.d("PackagesActivity", "[v0] Initiating M-Pesa payment - Plan: ${selectedPlan?.id}, Months: $numberOfMonths, Total: $totalAmount, Phone: $phoneToUse")

                                // Call the M-Pesa payment endpoint
                                PackagesApiClient.instance.processMpesaPayment(
                                    token = "Bearer $token",
                                    rechargeRequest = rechargeRequest
                                ).enqueue(object : Callback<MpesaResponse> {
                                    override fun onResponse(
                                        call: Call<MpesaResponse>,
                                        response: Response<MpesaResponse>
                                    ) {
                                        onStateUpdate(null, null, false, false, false, "", null)

                                        if (response.isSuccessful && response.body() != null) {
                                            val mpesaResponse = response.body()!!
                                            Log.d("PackagesActivity", "[v0] M-Pesa full response: $mpesaResponse")
                                            Log.d("PackagesActivity", "[v0] M-Pesa status: ${mpesaResponse.status}, CheckoutId: ${mpesaResponse.data?.checkoutId}, Data: ${mpesaResponse.data}")

                                            // Consider success if we have checkoutId in data (payment initiated successfully)
                                            if (mpesaResponse.data != null && mpesaResponse.data.checkoutId.isNotEmpty()) {
                                                val checkoutId = mpesaResponse.data.checkoutId
                                                val amount = mpesaResponse.data.amount
                                                val txnRef = mpesaResponse.data.txnRef

                                                Log.d("PackagesActivity", "[v0] M-Pesa checkoutId received: $checkoutId, TxnRef: $txnRef, Amount: $amount")

                                                // STK push was sent successfully
                                                onStateUpdate(
                                                    "STK PUSH SUCCESS CHECK YOUR PHONE",
                                                    "success",
                                                    false,
                                                    true,
                                                    false,
                                                    "",
                                                    checkoutId
                                                )
                                                Log.d("PackagesActivity", "[v0] STK push sent successfully")

                                                // Now start polling recharge list to verify payment
                                                pollPaymentVerification(
                                                    currentInitialCount,
                                                    token,
                                                    checkoutId,
                                                    0
                                                )
                                            } else {
                                                onStateUpdate("Payment initiation failed. Please try again.", "error", false, false, false, "", null)
                                            }
                                        } else {
                                            onStateUpdate("Payment initiation failed. Please try again.", "error", false, false, false, "", null)
                                        }
                                    }

                                    override fun onFailure(call: Call<MpesaResponse>, t: Throwable) {
                                        onStateUpdate(
                                            "Network error: ${t.message}",
                                            "error",
                                            false,
                                            false,
                                            false,
                                            "",
                                            null
                                        )
                                        Log.e("PackagesActivity", "[v0] M-Pesa payment error: ${t.message}", t)
                                    }
                                })
                            } else {
                                onStateUpdate("Failed to fetch recharge history", "error", false, false, false, "", null)
                            }
                        }

                        override fun onFailure(call: Call<RechargeListResponse>, t: Throwable) {
                            onStateUpdate(
                                "Network error: ${t.message}",
                                "error",
                                false,
                                false,
                                false,
                                "",
                                null
                            )
                            Log.e("PackagesActivity", "[v0] Failed to fetch initial recharge count: ${t.message}", t)
                        }
                    })
            } catch (e: Exception) {
                onStateUpdate(
                    "Error: ${e.message}",
                    "error",
                    false,
                    false,
                    false,
                    "",
                    null
                )
                Log.e("PackagesActivity", "[v0] Exception: ${e.message}", e)
                AppLogger.logError("PackagesActivity: Exception in payment flow", e)
            }
        }
    }

    private fun pollPaymentVerification(
        initialRechargeCount: Int,
        token: String,
        checkoutId: String,
        pollCount: Int
    ) {
        val maxPolls = 60

        if (pollCount >= maxPolls) {
            onStateUpdate(
                "Payment verification timeout. Please refresh to check payment status.",
                "error",
                false,
                false,
                false,
                "",
                null
            )
            return
        }

        val newPollCount = pollCount + 1

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

                        Log.d("PackagesActivity", "[v0] Poll #$newPollCount: Current recharges=$currentRechargeCount, Initial=$initialRechargeCount")

                        // Check if a new recharge was added
                        if (currentRechargeCount > initialRechargeCount) {
                            Log.d("PackagesActivity", "[v0] Payment verified! New recharge detected.")
                            onStateUpdate(
                                "Payment successful! Your package has been activated.",
                                "success",
                                false,
                                false,
                                false,
                                "",
                                null
                            )
                        } else {
                            // Payment not yet confirmed, continue polling
                            coroutineScope.launch {
                                delay(3000)
                                pollPaymentVerification(initialRechargeCount, token, checkoutId, newPollCount)
                            }
                        }
                    } else {
                        // Continue polling on error
                        coroutineScope.launch {
                            delay(3000)
                            pollPaymentVerification(initialRechargeCount, token, checkoutId, newPollCount)
                        }
                    }
                }

                override fun onFailure(call: Call<RechargeListResponse>, t: Throwable) {
                    // Continue polling on network error
                    Log.d("PackagesActivity", "[v0] Poll #$newPollCount failed: ${t.message}")
                    coroutineScope.launch {
                        delay(3000)
                        pollPaymentVerification(initialRechargeCount, token, checkoutId, newPollCount)
                    }
                }
            })
    }

    fun loadPlans(
        userId: Int,
        token: String,
        onPlansLoaded: (Boolean, String?, List<Plan>?, String?, UserDetail?) -> Unit
    ) {
        PackagesApiClient.instance.getRechargePlans(userId, "Bearer $token")
            .enqueue(object : Callback<com.kevannTechnologies.nosteqCustomers.models.RechargePlansResponse> {
                override fun onResponse(
                    call: Call<com.kevannTechnologies.nosteqCustomers.models.RechargePlansResponse>,
                    response: Response<com.kevannTechnologies.nosteqCustomers.models.RechargePlansResponse>
                ) {
                    AppLogger.logApiCall(
                        endpoint = "getRechargePlans",
                        success = response.isSuccessful,
                        responseCode = response.code()
                    )

                    if (response.isSuccessful && response.body() != null) {
                        val data = response.body()!!
                        onPlansLoaded(true, null, data.plan, data.currency, data.user)
                    } else {
                        onPlansLoaded(false, "Failed to load plans", null, null, null)
                    }
                }

                override fun onFailure(
                    call: Call<com.kevannTechnologies.nosteqCustomers.models.RechargePlansResponse>,
                    t: Throwable
                ) {
                    onPlansLoaded(false, "Network error. Please try again.", null, null, null)
                    AppLogger.logError("PackagesActivity: Failed to load plans", t)
                }
            })
    }
}
