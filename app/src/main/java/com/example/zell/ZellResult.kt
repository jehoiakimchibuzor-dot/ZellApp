package com.example.zell

/**
 * The standard Result wrapper for the Zell App.
 * forces the UI to handle Loading, Success, and Error states.
 */
sealed class ZellResult<out T> {
    object Loading : ZellResult<Nothing>()
    data class Success<out T>(val data: T) : ZellResult<T>()
    data class Error(val exception: ZellException) : ZellResult<Nothing>()
}

/**
 * Custom exceptions to map technical errors to user-friendly messages.
 */
sealed class ZellException(val userMessage: String) : Exception() {
    object NetworkException : ZellException("No internet. Please check your connection.")
    object AuthException : ZellException("Your session expired. Please log in again.")
    object PermissionException : ZellException("You don't have permission to view this.")
    data class UnknownException(val msg: String?) : ZellException(msg ?: "An unexpected error occurred.")
}
