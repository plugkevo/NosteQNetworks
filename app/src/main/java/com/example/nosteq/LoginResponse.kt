package com.example.nosteq


// LoginResponse.kt
data class LoginResponse(
    val data: LoginData
)

data class LoginData(
    val user: UserInfo,
    val token: String,
    val abilities: Abilities,
    val ispDetail: IspDetail,
    val ispLogo: String
)

data class UserInfo(
    val id: Int,
    val username: String,
    val operator_id: Int,
    val portalLogin: Int,
    val plan_id: Int
)

data class Abilities(
    val portal_client_password: Boolean,
    val portal_client_session: Boolean,
    val portal_client_invoice: Boolean,
    val portal_client_receipt: Boolean,
    val portal_client_traffic: Boolean,
    val portal_client_sla: Boolean
)

data class IspDetail(
    val isp_name: String,
    val isp_timezone: String,
    val isp_timezone_diff: Int,
    val isp_currency: String
)

// UserResponse for getUserDetails endpoint
data class UserResponse(
    val data: UserDetail
)

data class UserDetail(
    val id: Int,
    val username: String,
    val planName: String,
    val status: Int,
    val startDate: String,
    val expiryDate: String,
    val userinfo: UserInfoDetail,
    val documents: List<Any>?
)

data class UserInfoDetail(
    val contactPerson: String?,
    val company: String?,
    val phone: String?,
    val email: String?,
    val billingAddress1: String?,
    val billingCity: String?,
    val billingZip: String?
)
