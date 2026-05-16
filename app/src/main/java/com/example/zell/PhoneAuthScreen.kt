package com.example.zell

import android.util.Patterns
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

/**
 * Phone Authentication Screen - Handles UI for phone verification and OTP
 * 🔧 REFACTORED: Moved Firebase logic to SignUpFlow.kt for better orchestration
 * 🔗 CONNECTED: SignUpFlow.kt
 * 📝 NOTE: This screen is now a stateless UI component
 */

data class PhoneAuthState(
    val fullName: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val countryCode: String = "+234",
    val agreedToTerms: Boolean = false,
    val otpCode: String = "",
    val verificationId: String? = null,
    val isProcessing: Boolean = false,
    val currentStep: Int = 1, // 1 = Identity, 2 = OTP
    val fullNameError: String? = null,
    val emailError: String? = null,
    val phoneError: String? = null,
    val appError: AppError? = null
)

@Composable
fun PhoneAuthScreen(
    state: PhoneAuthState,
    onStateChange: (PhoneAuthState) -> Unit
) {
    // 🔧 REFACTORED: Removed internal Firebase callbacks to keep the screen purely UI-focused
    // Logic is now handled by the parent SignUpFlow via the PrimaryButton
    
    when (state.currentStep) {
        1 -> IdentityStep(
            state = state,
            onStateChange = onStateChange
        )
        2 -> OtpStep(
            otp = state.otpCode,
            onOtpChange = { onStateChange(state.copy(otpCode = it)) }
        )
    }
}

@Composable
fun IdentityStep(
    state: PhoneAuthState,
    onStateChange: (PhoneAuthState) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .testTag("identity_step")
    ) {
        // 🔧 REFACTORED: Added comments and ensured testTags are consistent
        Text(
            text = "Identity",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Tell us who you are.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(32.dp))

        ElaraTextField(
            value = state.fullName,
            onValueChange = { onStateChange(state.copy(fullName = it, fullNameError = null)) },
            label = "FULL NAME",
            placeholder = "Enter your name",
            isError = state.fullNameError != null,
            errorMessage = state.fullNameError,
            testTag = "fullname_field"
        )
        Spacer(modifier = Modifier.height(24.dp))

        ElaraTextField(
            value = state.email,
            onValueChange = { onStateChange(state.copy(email = it, emailError = null)) },
            label = "EMAIL ADDRESS",
            placeholder = "name@example.com",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            isError = state.emailError != null,
            errorMessage = state.emailError,
            testTag = "email_field"
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "PHONE NUMBER",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp)
                        .testTag("country_selector"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = countryCodes.find { it.first == state.countryCode }?.second ?: "",
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = state.countryCode,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    countryCodes.forEach { (code, flag) ->
                        DropdownMenuItem(
                            text = { Text("$flag $code") },
                            onClick = {
                                onStateChange(state.copy(countryCode = code))
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            BasicTextField(
                value = state.phoneNumber,
                onValueChange = { onStateChange(state.copy(phoneNumber = it, phoneError = null)) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        if (state.phoneError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp)
                    .testTag("phone_field"),
                textStyle = TextStyle(MaterialTheme.colorScheme.onSurface, 16.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                decorationBox = { inner ->
                    if (state.phoneNumber.isEmpty()) {
                        Text(
                            "810 000 0000",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            fontSize = 16.sp
                        )
                    }
                    inner()
                }
            )
        }
        if (state.phoneError != null) {
            Text(
                state.phoneError,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = state.agreedToTerms,
                onCheckedChange = { onStateChange(state.copy(agreedToTerms = it)) },
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("terms_checkbox")
            )
            Text(
                text = "I agree to the Terms of Service and Privacy Policy.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.clickable { onStateChange(state.copy(agreedToTerms = !state.agreedToTerms)) }
            )
        }
    }
}

@Composable
fun OtpStep(otp: String, onOtpChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().testTag("otp_step")) {
        // 🔧 REFACTORED: Simplified for better integration with parent button
        Text(
            text = "Verification",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Enter the 6-digit code sent to your phone.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(48.dp))

        ElaraTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) onOtpChange(it) },
            label = "OTP CODE",
            placeholder = "000 000",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            testTag = "otp_field"
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Didn't receive code? Resend in 54s",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}
