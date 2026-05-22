package com.kevannTechnologies.nosteqCustomers

import android.app.Application
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

class NosteqApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
            // Enable Firestore persistence for offline support
            val db = FirebaseFirestore.getInstance()
            db.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
