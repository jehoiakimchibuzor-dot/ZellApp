package com.example.zell

import android.util.Log
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase

/**
 * Centralized logging utility using Firebase Crashlytics
 * Logs both to Logcat and Crashlytics for crash reporting
 */
object CrashlyticsLogger {
    private const val TAG = "ZellApp"

    /**
     * Log debug message
     */
    fun d(tag: String = TAG, message: String, exception: Throwable? = null) {
        Log.d(tag, message, exception)
        Firebase.crashlytics.log("DEBUG: $tag - $message")
    }

    /**
     * Log info message
     */
    fun i(tag: String = TAG, message: String, exception: Throwable? = null) {
        Log.i(tag, message, exception)
        Firebase.crashlytics.log("INFO: $tag - $message")
    }

    /**
     * Log warning message
     */
    fun w(tag: String = TAG, message: String, exception: Throwable? = null) {
        Log.w(tag, message, exception)
        Firebase.crashlytics.log("WARNING: $tag - $message")
        if (exception != null) {
            Firebase.crashlytics.recordException(exception)
        }
    }

    /**
     * Log error message
     */
    fun e(tag: String = TAG, message: String, exception: Throwable? = null) {
        Log.e(tag, message, exception)
        Firebase.crashlytics.log("ERROR: $tag - $message")
        if (exception != null) {
            Firebase.crashlytics.recordException(exception)
        }
    }

    /**
     * Record a custom exception
     */
    fun recordException(exception: Throwable, context: String = "") {
        if (context.isNotEmpty()) {
            Firebase.crashlytics.log("Exception Context: $context")
        }
        Firebase.crashlytics.recordException(exception)
        Log.e(TAG, context, exception)
    }

    /**
     * Set custom key-value pairs for debugging
     */
    fun setCustomKey(key: String, value: String) {
        Firebase.crashlytics.setCustomKey(key, value)
    }

    /**
     * Set user ID for crash reports
     */
    fun setUserId(userId: String) {
        Firebase.crashlytics.setUserId(userId)
    }

    /**
     * Log user action
     */
    fun logUserAction(action: String, details: Map<String, String> = emptyMap()) {
        val logMessage = StringBuilder("User Action: $action")
        details.forEach { (key, value) ->
            logMessage.append(" | $key=$value")
        }
        Firebase.crashlytics.log(logMessage.toString())
        Log.i(TAG, logMessage.toString())
    }

    /**
     * Log Firebase operation
     */
    fun logFirebaseOperation(operation: String, status: String, message: String = "") {
        val logMessage = "Firebase Operation: $operation - Status: $status - $message"
        Firebase.crashlytics.log(logMessage)
        Log.i(TAG, logMessage)
    }

    /**
     * Log network operation
     */
    fun logNetworkOperation(endpoint: String, status: Int, duration: Long) {
        val logMessage = "Network: $endpoint - Status: $status - Duration: ${duration}ms"
        Firebase.crashlytics.log(logMessage)
        Log.i(TAG, logMessage)
    }
}

