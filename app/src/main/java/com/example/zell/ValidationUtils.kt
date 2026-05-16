package com.example.zell

/**
 * Input validation utilities for critical data
 */

object ValidationUtils {
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_USERNAME_LENGTH = 3
    const val MAX_BIO_LENGTH = 500
    const val MAX_POST_LENGTH = 5000
    const val MAX_FILE_SIZE_MB = 100

    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() &&
               android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Validate phone number (basic)
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val cleanPhone = phone.replace(Regex("[^0-9]"), "")
        return cleanPhone.length >= 10
    }

    /**
     * Validate password strength
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LENGTH &&
               password.any { it.isUpperCase() } &&
               password.any { it.isDigit() }
    }

    /**
     * Validate username
     */
    fun isValidUsername(username: String): Boolean {
        return username.length >= MIN_USERNAME_LENGTH &&
               username.length <= 30 &&
               username.matches(Regex("^[a-zA-Z0-9_-]+$"))
    }

    /**
     * Validate post content
     */
    fun isValidPost(content: String): Boolean {
        return content.isNotBlank() &&
               content.trim().length <= MAX_POST_LENGTH
    }

    /**
     * Validate bio
     */
    fun isValidBio(bio: String): Boolean {
        return bio.length <= MAX_BIO_LENGTH
    }

    /**
     * Validate URL format
     */
    fun isValidUrl(url: String): Boolean {
        return url.isNotBlank() &&
               android.util.Patterns.WEB_URL.matcher(url).matches()
    }

    /**
     * Validate file size
     */
    fun isValidFileSize(sizeInBytes: Long): Boolean {
        return sizeInBytes <= (MAX_FILE_SIZE_MB * 1024 * 1024)
    }

    /**
     * Get error message for validation
     */
    fun getValidationError(type: String, value: String): String? {
        return when (type) {
            "email" -> if (!isValidEmail(value)) "Invalid email format" else null
            "phone" -> if (!isValidPhoneNumber(value)) "Invalid phone number" else null
            "password" -> when {
                value.length < MIN_PASSWORD_LENGTH -> "Password must be at least $MIN_PASSWORD_LENGTH characters"
                !value.any { it.isUpperCase() } -> "Password must contain uppercase letter"
                !value.any { it.isDigit() } -> "Password must contain a digit"
                else -> null
            }
            "username" -> when {
                value.length < MIN_USERNAME_LENGTH -> "Username must be at least $MIN_USERNAME_LENGTH characters"
                !value.matches(Regex("^[a-zA-Z0-9_-]+$")) -> "Username can only contain letters, numbers, hyphens, and underscores"
                else -> null
            }
            "bio" -> if (value.length > MAX_BIO_LENGTH) "Bio must be less than $MAX_BIO_LENGTH characters" else null
            "post" -> when {
                value.isBlank() -> "Post cannot be empty"
                value.trim().length > MAX_POST_LENGTH -> "Post exceeds maximum length"
                else -> null
            }
            "url" -> if (!isValidUrl(value)) "Invalid URL format" else null
            else -> null
        }
    }
}

