package com.example.nosteq.models

data class InvoiceResponse(
    val data: List<Invoice>
)

data class Invoice(
    val invoiceNo: Int,
    val prefix: String,
    val status: String,
    val user_id: Int,
    val username: String,
    val invoiceDate: String,
    val total: String,
    val receiveAmount: String,
    val createdBy: String,
    val id: Int
)
