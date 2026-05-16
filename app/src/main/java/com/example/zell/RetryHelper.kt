package com.example.zell

import android.util.Log
import kotlinx.coroutines.delay

data class RetryConfig(
    val maxAttempts: Int = 3,
    val initialDelayMs: Long = 1000,  // 1 second
    val maxDelayMs: Long = 8000,      // 8 seconds
    val backoffMultiplier: Double = 2.0,
    val retryableErrors: Set<ErrorType> = setOf(
        ErrorType.NETWORK,
        ErrorType.TIMEOUT,
        ErrorType.FIREBASE
    )
)

object RetryHelper {
    @PublishedApi
    internal val tag = "RetryHelper"

    /**
     * Main retry function with exponential backoff
     * 
     * Usage:
     * ```
     * try {
     *     RetryHelper.retryWithBackoff {
     *         firebaseOperation()
     *     }
     * } catch (e: Exception) {
     *     val error = ErrorHandler.classifyException(e)
     * }
     * ```
     */
    suspend inline fun <T> retryWithBackoff(
        config: RetryConfig = RetryConfig(),
        crossinline block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        var delayMs = config.initialDelayMs

        repeat(config.maxAttempts) { attempt ->
            try {
                Log.d(tag, "Attempt ${attempt + 1}/${config.maxAttempts}: Starting operation")
                return block()
            } catch (e: Exception) {
                lastException = e
                val error = ErrorHandler.classifyException(e)

                // Only retry if error type is retryable
                if (!config.retryableErrors.contains(error.type)) {
                    Log.w(tag, "Error type ${error.type} is not retryable. Failing immediately.")
                    throw e
                }

                // If this was the last attempt, throw
                if (attempt == config.maxAttempts - 1) {
                    Log.e(tag, "All ${config.maxAttempts} attempts failed. Giving up.")
                    throw e
                }

                // Calculate backoff delay with jitter
                val jitter = (0..500).random()
                val nextDelayMs = minOf(
                    (delayMs * config.backoffMultiplier).toLong() + jitter,
                    config.maxDelayMs
                )

                Log.w(
                    tag,
                    "Attempt ${attempt + 1} failed (${error.type}). Retrying in ${nextDelayMs}ms"
                )

                delay(nextDelayMs)
                delayMs = nextDelayMs
            }
        }

        // Should not reach here, but just in case
        throw lastException ?: Exception("Retry failed with unknown error")
    }

    /**
     * Simplified retry for Firebase operations
     * Automatically handles network timeouts
     */
    suspend inline fun <T> firebaseRetry(
        crossinline operation: suspend () -> T
    ): T {
        return retryWithBackoff(
            config = RetryConfig(
                maxAttempts = 3,
                initialDelayMs = 1000,
                backoffMultiplier = 2.0
            )
        ) {
            operation()
        }
    }

    /**
     * Retry for network-only operations
     * Stricter retry policy
     */
    suspend inline fun <T> networkRetry(
        crossinline operation: suspend () -> T
    ): T {
        return retryWithBackoff(
            config = RetryConfig(
                maxAttempts = 3,
                initialDelayMs = 500,
                maxDelayMs = 4000,
                retryableErrors = setOf(ErrorType.NETWORK, ErrorType.TIMEOUT)
            )
        ) {
            operation()
        }
    }

    /**
     * Check if an error should be retried
     */
    fun shouldRetry(error: AppError, config: RetryConfig = RetryConfig()): Boolean {
        return config.retryableErrors.contains(error.type) && error.retryCount < error.maxRetries
    }

    /**
     * Get display message for retry attempt
     */
    fun getRetryMessage(error: AppError): String {
        return if (error.retryCount > 0) {
            "Attempt ${error.retryCount + 1} of ${error.maxRetries}"
        } else {
            "Retrying..."
        }
    }

    /**
     * Log retry attempt to Crashlytics
     */
    fun logRetryAttempt(error: AppError, attemptNumber: Int) {
        try {
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().log(
                "Retry attempt $attemptNumber for ${error.type}: ${error.message}"
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to log retry: ${e.message}")
        }
    }
}

/**
 * Extension function for easier retry syntax
 * 
 * Usage:
 * ```
 * val result = {
 *     firebaseDb.collection("users").get().await()
 * }.retryWithBackoff()
 * ```
 */
suspend inline fun <T> (suspend () -> T).retryWithBackoff(
    config: RetryConfig = RetryConfig()
): T {
    return RetryHelper.retryWithBackoff(config) { this() }
}

/**
 * Extension for AppError to update retry count
 */
fun AppError.incrementRetry(): AppError {
    return this.copy(retryCount = minOf(retryCount + 1, maxRetries))
}

/**
 * Extension for AppError to check if retryable
 */
fun AppError.isRetryable(config: RetryConfig = RetryConfig()): Boolean {
    return config.retryableErrors.contains(type) && retryCount < maxRetries
}
