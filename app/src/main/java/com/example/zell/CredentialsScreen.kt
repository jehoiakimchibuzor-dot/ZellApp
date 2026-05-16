package com.example.zell

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

/**
 * Credentials Screen - Password setup and account creation
 * Extracted from SignUpFlow for better maintainability
 */

data class CredentialsState(
    val password: String = "",
    val confirmPassword: String = "",
    val passwordError: String? = null,
    val appError: AppError? = null,
    val isProcessing: Boolean = false
)

@Composable
fun CredentialsScreen(
    email: String,
    state: CredentialsState,
    onStateChange: (CredentialsState) -> Unit,
    onCredentialsSet: () -> Unit
) {
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("credentials_step")
    ) {
        Text(
            text = "Credentials",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Secure your protocol with a strong password.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(40.dp))

        var passwordVisible by remember { mutableStateOf(false) }
        var confirmPasswordVisible by remember { mutableStateOf(false) }

        ElaraTextField(
            value = state.password,
            onValueChange = { onStateChange(state.copy(password = it, passwordError = null)) },
            label = "PASSWORD",
            placeholder = "Min. 6 chars + # + @",
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.passwordError != null,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            },
            testTag = "password_field"
        )

        Spacer(modifier = Modifier.height(24.dp))

        ElaraTextField(
            value = state.confirmPassword,
            onValueChange = { onStateChange(state.copy(confirmPassword = it, passwordError = null)) },
            label = "CONFIRM PASSWORD",
            placeholder = "Repeat your password",
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = state.passwordError != null,
            trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                    Icon(
                        if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        null,
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            },
            testTag = "confirm_password_field"
        )

        if (state.passwordError != null) {
            Text(
                state.passwordError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        PasswordStrengthMeter(state.password)
    }
}

@Composable
fun PasswordStrengthMeter(password: String) {
    val hasNumber = password.any { it.isDigit() }
    val hasSymbol = password.any { !it.isLetterOrDigit() }
    val strength = when {
        password.isEmpty() -> 0
        password.length < 6 || !hasNumber || !hasSymbol -> 2
        password.length < 10 -> 3
        else -> 4
    }
    val color = when (strength) {
        1, 2 -> MaterialTheme.colorScheme.error
        3 -> MaterialTheme.colorScheme.secondary
        4 -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }
    val labels = listOf("Too short", "Needs symbols/numbers", "Fair", "Strong")
    Column(modifier = Modifier.testTag("password_strength")) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(
                            if (index < strength) color
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                )
            }
        }
        if (password.isNotEmpty()) {
            Text(
                labels.getOrElse(strength - 1) { "" },
                fontSize = 12.sp,
                color = color,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .testTag("strength_label"),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

