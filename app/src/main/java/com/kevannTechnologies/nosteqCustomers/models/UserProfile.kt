package com.kevannTechnologies.nosteqCustomers.models

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val uid: String = "",
    val username: String = "",
    val phoneNumber: String = "",
    val hasChangedPassword: Boolean = false,
    @ServerTimestamp
    val passwordChangedDate: Date? = null,
    @ServerTimestamp
    val createdAt: Date? = null,
    val lastLoginDate: Date? = null
)
