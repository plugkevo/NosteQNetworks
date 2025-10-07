package com.example.nosteq


import com.google.gson.annotations.SerializedName

data class DashboardResponse(
    @SerializedName("data")
    val data: DashboardData
)

data class DashboardData(
    @SerializedName("id")
    val id: Int,

    @SerializedName("username")
    val username: String,

    @SerializedName("plan_id")
    val planId: Int,

    @SerializedName("planName")
    val planName: String,

    @SerializedName("status")
    val status: Int,

    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("expiryDate")
    val expiryDate: String,

    @SerializedName("bill")
    val bill: String,

    @SerializedName("usedData")
    val usedData: UsedData?,

    @SerializedName("lastConnection")
    val lastConnection: LastConnection?,

    @SerializedName("current_recharge")
    val currentRecharge: CurrentRecharge?
)

data class UsedData(
    @SerializedName("upload")
    val upload: Long?,

    @SerializedName("download")
    val download: Long?
)

data class LastConnection(
    @SerializedName("acctstarttime")
    val acctStartTime: String?,

    @SerializedName("acctstoptime")
    val acctStopTime: String?,

    @SerializedName("acctsessiontime")
    val acctSessionTime: Int?
)

data class CurrentRecharge(
    @SerializedName("id")
    val id: Int,

    @SerializedName("planName")
    val planName: String,

    @SerializedName("validity")
    val validity: Long,

    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("expiryDate")
    val expiryDate: String,

    @SerializedName("downBW")
    val downBW: Int,

    @SerializedName("upBW")
    val upBW: Int,

    @SerializedName("data")
    val data: Long?,

    @SerializedName("fup")
    val fup: Int,

    @SerializedName("fupDownBW")
    val fupDownBW: String?,

    @SerializedName("fupUpBW")
    val fupUpBW: String?,

    @SerializedName("customerCost")
    val customerCost: String,

    @SerializedName("total")
    val total: String
)

