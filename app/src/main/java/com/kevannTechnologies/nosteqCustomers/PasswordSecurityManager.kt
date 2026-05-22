package com.kevannTechnologies.nosteqCustomers

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.kevannTechnologies.nosteqCustomers.models.UserProfile
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

object PasswordSecurityManager {
    private const val TAG = "PasswordSecurityManager"
    private const val USERS_COLLECTION = "users"
    private const val PASSWORD_EXPIRY_DAYS = 90

    /**
     * Check if user needs to change password due to:
     * 1. First login (hasChangedPassword = false)
     * 2. Password expired (90 days since last change)
     */
    suspend fun shouldForcePasswordChange(userId: String): Boolean {
        return try {
            val userProfile = getUserProfile(userId)

            if (userProfile == null) {
                Log.w(TAG, "User profile not found for ID: $userId")
                return true // Force password change if no profile exists
            }

            // First login - password not changed yet
            if (!userProfile.hasChangedPassword) {
                Log.i(TAG, "First login detected for user: $userId")
                return true
            }

            // Check if password has expired
            val passwordChangedDate = userProfile.passwordChangedDate
            if (passwordChangedDate != null) {
                val isExpired = isPasswordExpired(passwordChangedDate)
                if (isExpired) {
                    Log.i(TAG, "Password expired for user: $userId")
                }
                return isExpired
            }

            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking password expiration", e)
            false
        }
    }

    /**
     * Create or update user profile after successful login
     */
    suspend fun createOrUpdateUserProfile(
        userId: String,
        username: String,
        phoneNumber: String
    ): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection(USERS_COLLECTION).document(userId)

            val existingProfile = userRef.get().await()

            val userProfile = if (existingProfile.exists()) {
                // Update last login date only
                existingProfile.toObject(UserProfile::class.java)?.copy(
                    lastLoginDate = Date()
                ) ?: UserProfile(
                    uid = userId,
                    username = username,
                    phoneNumber = phoneNumber,
                    lastLoginDate = Date()
                )
            } else {
                // Create new profile on first login
                UserProfile(
                    uid = userId,
                    username = username,
                    phoneNumber = phoneNumber,
                    hasChangedPassword = false,
                    lastLoginDate = Date()
                )
            }

            userRef.set(userProfile).await()
            Log.i(TAG, "User profile created/updated for: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error creating/updating user profile", e)
            false
        }
    }

    /**
     * Mark password as changed and store the date
     */
    suspend fun markPasswordAsChanged(userId: String): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            db.collection(USERS_COLLECTION).document(userId).update(
                mapOf(
                    "hasChangedPassword" to true,
                    "passwordChangedDate" to Date()
                )
            ).await()
            Log.i(TAG, "Password marked as changed for user: $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking password as changed", e)
            false
        }
    }

    /**
     * Get user profile from Firestore
     */
    private suspend fun getUserProfile(userId: String): UserProfile? {
        return try {
            val db = FirebaseFirestore.getInstance()
            val snapshot = db.collection(USERS_COLLECTION).document(userId).get().await()
            snapshot.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching user profile", e)
            null
        }
    }

    /**
     * Check if password has expired (more than 90 days since change)
     */
    private fun isPasswordExpired(passwordChangedDate: Date): Boolean {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -PASSWORD_EXPIRY_DAYS)
        val expiryDate = calendar.time

        return passwordChangedDate.before(expiryDate)
    }

    /**
     * Get days remaining until password expires
     */
    suspend fun getDaysUntilPasswordExpiry(userId: String): Int {
        return try {
            val userProfile = getUserProfile(userId) ?: return -1

            if (!userProfile.hasChangedPassword) {
                return 0 // Must change immediately on first login
            }

            val passwordChangedDate = userProfile.passwordChangedDate ?: return -1
            val calendar = Calendar.getInstance()
            calendar.time = passwordChangedDate
            calendar.add(Calendar.DAY_OF_YEAR, PASSWORD_EXPIRY_DAYS)
            val expiryDate = calendar.time

            val currentTime = Date()
            val diffInMillis = expiryDate.time - currentTime.time
            val daysRemaining = (diffInMillis / (1000 * 60 * 60 * 24)).toInt()

            maxOf(daysRemaining, 0)
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating password expiry days", e)
            -1
        }
    }
}
