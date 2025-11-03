package com.example.nosteq.models

data class ReceiptResponse(
    val data: List<Receipt>
)

data class Receipt(
    val receiptNo: Int,
    val user_id: Int,
    val username: String,
    val contactPerson: String,
    val amount: String,
    val receiptDate: String,
    val createdBy: String,
    val receiptType: String,
    val id: Int,
    val zoneName: String
)