package com.kevann.nosteq.models

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
    val confirmPassword: String
)

data class ChangePasswordResponse(
    val message: String
)

