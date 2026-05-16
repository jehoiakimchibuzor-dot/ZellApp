package com.example.zell
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.util.*
/**
 * ? PRODUCTION-GRADE ERROR HANDLER v2.1
 * Improved specificity for Storage and Firestore errors
 */
enum class ErrorType {
    NETWORK, FIREBASE, PERMISSION, VALIDATION, TIMEOUT, AUTH, STORAGE, UNKNOWN
}
enum class ErrorSeverity {
    INFO, WARNING, ERROR, CRITICAL
}
data class AppError(
    val type: ErrorType = ErrorType.UNKNOWN,
    val severity: ErrorSeverity = ErrorSeverity.ERROR,
    val title: String = "Error Occurred",
    val message: String = "Something went wrong. Please try again.",
    val errorId: String = UUID.randomUUID().toString().take(8),
    val action: String? = null,
    val onAction: (() -> Unit)? = null,
    val retryCount: Int = 0,
    val maxRetries: Int = 3,
    val dismissible: Boolean = true,
    val autoDismissMs: Long = if (severity == ErrorSeverity.INFO) 3000 else -1
)
object ErrorHandler {
    fun classifyException(exception: Exception): AppError {
        FirebaseCrashlytics.getInstance().recordException(exception)
        Log.e("ErrorHandler", "Classifying exception: ${exception.message}", exception)
        
        return when (exception) {
            is java.net.ConnectException, is java.net.SocketException, is java.net.SocketTimeoutException -> AppError(type = ErrorType.NETWORK, severity = ErrorSeverity.WARNING, title = "Connection Problem", message = "Unable to connect. Check your internet.", action = "Retry")
            is java.net.UnknownHostException -> AppError(type = ErrorType.NETWORK, severity = ErrorSeverity.WARNING, title = "No Internet", message = "Check your internet connection.", action = "Retry")
            is java.util.concurrent.TimeoutException -> AppError(type = ErrorType.TIMEOUT, severity = ErrorSeverity.WARNING, title = "Request Timeout", message = "Request took too long.", action = "Retry")
            is SecurityException -> AppError(type = ErrorType.PERMISSION, severity = ErrorSeverity.WARNING, title = "Permission Denied", message = "This feature requires additional permissions.", action = "Grant")
            is com.google.firebase.auth.FirebaseAuthException -> when (exception.errorCode) {
                "ERROR_INVALID_EMAIL" -> AppError(type = ErrorType.VALIDATION, severity = ErrorSeverity.WARNING, title = "Invalid Email", message = "Enter a valid email.", action = "OK")
                "ERROR_WEAK_PASSWORD" -> AppError(type = ErrorType.VALIDATION, severity = ErrorSeverity.WARNING, title = "Weak Password", message = "Password needs 6+ characters.", action = "OK")
                "ERROR_USER_NOT_FOUND" -> AppError(type = ErrorType.AUTH, severity = ErrorSeverity.WARNING, title = "User Not Found", message = "No account with this email.", action = "OK")
                "ERROR_WRONG_PASSWORD" -> AppError(type = ErrorType.AUTH, severity = ErrorSeverity.WARNING, title = "Invalid Password", message = "Password is incorrect.", action = "Try Again")
                "ERROR_TOO_MANY_REQUESTS" -> AppError(type = ErrorType.AUTH, severity = ErrorSeverity.WARNING, title = "Too Many Attempts", message = "Try again later.", action = "OK", autoDismissMs = 5000)
                else -> AppError(type = ErrorType.FIREBASE, severity = ErrorSeverity.ERROR, title = "Auth Error", message = exception.message ?: "Auth failed.", action = "Retry")
            }
            is com.google.firebase.firestore.FirebaseFirestoreException -> when (exception.code) {
                com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError(type = ErrorType.PERMISSION, severity = ErrorSeverity.ERROR, title = "Access Denied", message = "Firebase Permission Denied. Check your Security Rules.", action = "OK")
                com.google.firebase.firestore.FirebaseFirestoreException.Code.UNAVAILABLE -> AppError(type = ErrorType.NETWORK, severity = ErrorSeverity.WARNING, title = "Server Down", message = "Service unavailable.", action = "Retry")
                else -> AppError(type = ErrorType.FIREBASE, severity = ErrorSeverity.ERROR, title = "Sync Error", message = exception.message ?: "Sync failed.", action = "Retry")
            }
            is com.google.firebase.storage.StorageException -> {
                val msg = when (exception.errorCode) {
                    com.google.firebase.storage.StorageException.ERROR_NOT_AUTHORIZED -> "Permission denied. Check Firebase Storage rules."
                    com.google.firebase.storage.StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> "Network limit exceeded. Try again."
                    else -> exception.message ?: "Upload failed."
                }
                AppError(type = ErrorType.STORAGE, severity = ErrorSeverity.ERROR, title = "Upload Error", message = msg, action = "Retry")
            }
            is IllegalArgumentException -> AppError(type = ErrorType.VALIDATION, severity = ErrorSeverity.WARNING, title = "Invalid Input", message = exception.message ?: "Input invalid.", action = "OK")
            else -> AppError(type = ErrorType.UNKNOWN, severity = ErrorSeverity.ERROR, title = "Error", message = exception.message ?: "Unknown error occurred.", action = "Retry")
        }
    }
}
@Composable
fun ErrorDialog(error: AppError?, onDismiss: () -> Unit, onRetry: (() -> Unit)? = null) {
    if (error != null) {
        if (error.autoDismissMs > 0) LaunchedEffect(error.errorId) { kotlinx.coroutines.delay(error.autoDismissMs); if (error.dismissible) onDismiss() }
        AlertDialog(onDismissRequest = { if (error.dismissible) onDismiss() }, icon = { Icon(getErrorIcon(error.type), null, tint = getErrorColor(error.severity), modifier = Modifier.size(32.dp)) }, title = { Text(error.title, fontWeight = FontWeight.Bold) }, text = { Column { Text(error.message); Spacer(Modifier.height(8.dp)); Text("ID: ${error.errorId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline) } }, confirmButton = { Button(onClick = { onRetry?.invoke() ?: error.onAction?.invoke(); onDismiss() }) { Text(error.action ?: "OK") } }, dismissButton = if (error.dismissible) { { TextButton(onClick = onDismiss) { Text("Dismiss") } } } else null)
    }
}
@Composable
fun ErrorBanner(error: AppError?, onDismiss: () -> Unit, onRetry: (() -> Unit)? = null) {
    if (error != null) {
        if (error.autoDismissMs > 0) LaunchedEffect(error.errorId) { kotlinx.coroutines.delay(error.autoDismissMs); if (error.dismissible) onDismiss() }
        AnimatedVisibility(visible = true, enter = slideInVertically() + fadeIn(), exit = slideOutVertically() + fadeOut()) {
            Surface(modifier = Modifier.fillMaxWidth(), color = getErrorColor(error.severity), shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp), shadowElevation = 8.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(getErrorIcon(error.type), null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Column {
                            Text(error.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(error.message, color = Color.White.copy(0.9f), fontSize = 12.sp)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onRetry != null) IconButton(onClick = { onRetry() }, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Refresh, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                        if (error.dismissible) IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                    }
                }
            }
        }
    }
}
fun getErrorIcon(type: ErrorType) = when (type) {
    ErrorType.NETWORK -> Icons.Default.CloudOff
    ErrorType.FIREBASE -> Icons.Default.Storage
    ErrorType.PERMISSION -> Icons.Default.Lock
    ErrorType.VALIDATION -> Icons.Default.Info
    ErrorType.TIMEOUT -> Icons.Default.Schedule
    ErrorType.AUTH -> Icons.Default.Person
    ErrorType.STORAGE -> Icons.Default.FolderOff
    ErrorType.UNKNOWN -> Icons.Default.Error
}
@Composable
fun getErrorColor(severity: ErrorSeverity) = when (severity) {
    ErrorSeverity.INFO -> MaterialTheme.colorScheme.secondary
    ErrorSeverity.WARNING -> Color(0xFFFF9800)
    ErrorSeverity.ERROR -> Color(0xFFD32F2F)
    ErrorSeverity.CRITICAL -> Color(0xFF1B1B1B)
}
