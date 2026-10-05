package com.example.core.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseManager {
    private const val TAG = "FirebaseManager"

    @Volatile
    private var isInitialized: Boolean = false

    fun initialize(context: Context) {
        try {
            val apps = FirebaseApp.getApps(context)
            if (apps.isEmpty()) {
                val app = FirebaseApp.initializeApp(context)
                isInitialized = app != null
            } else {
                isInitialized = true
            }
            Log.d(TAG, "Firebase initialized state: $isInitialized")
        } catch (e: Exception) {
            Log.w(TAG, "Firebase initialization skipped or failed (offline/local mode active): ${e.message}")
            isInitialized = false
        }
    }

    fun isAvailable(): Boolean = isInitialized

    fun getAuth(): FirebaseAuth? {
        return try {
            if (isInitialized) FirebaseAuth.getInstance() else null
        } catch (e: Exception) {
            Log.w(TAG, "Unable to get FirebaseAuth: ${e.message}")
            null
        }
    }

    fun getFirestore(): FirebaseFirestore? {
        return try {
            if (isInitialized) FirebaseFirestore.getInstance() else null
        } catch (e: Exception) {
            Log.w(TAG, "Unable to get FirebaseFirestore: ${e.message}")
            null
        }
    }
}
