package com.example.nosteq

import com.google.gson.annotations.SerializedName
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

data class LoginRequest(
    val username: String,
    val loginType: String = "phone"
)

data class RechargePlansResponse(
    val user: com.example.nosteq.UserDetail,
    val plan: List<Plan>,
    val tax: Tax,
    val roundOff: Boolean,
    val currency: String,
    val rechargeConfig: RechargeConfig,
    val paymentGateway: PaymentGateway
)

data class Plan(
    val id: Int,
    val planName: String,
    val customerCost: String,
    val planType: Int,
    val planCategory: Int,
    @SerializedName("plangroup_id") val plangroupId: Int
)

data class Tax(
    @SerializedName("tax_type") val taxType: String,
    @SerializedName("tax_1") val tax1: List<String>?,
    @SerializedName("tax_2") val tax2: List<String>?,
    @SerializedName("tax_3") val tax3: List<String>?
)

data class RechargeConfig(
    @SerializedName("recharge_display_price") val displayPrice: Boolean,
    @SerializedName("recharge_outstandingAdd") val outstandingAdd: Boolean,
    @SerializedName("outstandingAmountDisable") val outstandingDisable: Boolean
)

class PaymentGatewayDeserializer : JsonDeserializer<PaymentGateway> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): PaymentGateway {
        val jsonObject = json.asJsonObject

        // Helper function to convert both boolean and int to int
        fun getIntValue(key: String): Int {
            val element = jsonObject.get(key)
            return when {
                element.isJsonPrimitive && element.asJsonPrimitive.isBoolean ->
                    if (element.asBoolean) 1 else 0
                element.isJsonPrimitive && element.asJsonPrimitive.isNumber ->
                    element.asInt
                else -> 0
            }
        }

        return PaymentGateway(
            payu = getIntValue("payu"),
            atom = getIntValue("atom"),
            onepay = getIntValue("onepay"),
            paypal = getIntValue("paypal"),
            esewa = getIntValue("esewa"),
            mpesa = getIntValue("mpesa"),
            kopokopo = getIntValue("kopokopo"),
            slydepay = getIntValue("slydepay"),
            paystack = getIntValue("paystack"),
            aggrepay = getIntValue("aggrepay"),
            razorpay = getIntValue("razorpay"),
            flutterwave = getIntValue("flutterwave"),
            paytm = getIntValue("paytm"),
            khalti = getIntValue("khalti"),
            cashfree = getIntValue("cashfree"),
            phonepe = getIntValue("phonepe"),
            waitnpay = getIntValue("waitnpay"),
            redde = getIntValue("redde")
        )
    }
}

data class PaymentGateway(
    val payu: Int,
    val atom: Int,
    val onepay: Int,
    val paypal: Int,
    val esewa: Int,
    val mpesa: Int,
    val kopokopo: Int,
    val slydepay: Int,
    val paystack: Int,
    val aggrepay: Int,
    val razorpay: Int,
    val flutterwave: Int,
    val paytm: Int,
    val khalti: Int,
    val cashfree: Int,
    val phonepe: Int,
    val waitnpay: Int,
    val redde: Int
)

data class RechargeRequest(
    val rechargePlan: Int,
    val quantity: Int = 1,
    val rechargeType: Int = 1,
    val resetRecharge: Boolean = false,
    val contactPerson: String,
    val email: String,
    val phone: String,
    val city: String,
    val zip: String
)

data class RechargeResponse(
    val status: String,
    @SerializedName("payment_url") val paymentUrl: String?,
    @SerializedName("transaction_id") val transactionId: String?,
    val data: RechargeData?
)

data class RechargeData(
    val txnRef: String,
    val amount: Int,
    val hotspotUrl: String
)

data class MpesaResponse(
    val status: String,
    val message: String?,
    @SerializedName("transaction_id") val transactionId: String?,
    @SerializedName("checkout_request_id") val checkoutRequestId: String?,
    val data: MpesaData?
)

data class MpesaData(
    val amount: Int,
    val phone: String,
    val reference: String?
)
