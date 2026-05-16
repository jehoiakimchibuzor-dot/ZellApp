package com.example.zell

import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
// 🔧 REFACTORED: Explicitly ensuring modular screens are available (same package, but for clarity)
import com.example.zell.PhoneAuthScreen
import com.example.zell.CredentialsScreen
import com.example.zell.PersonaSelectionScreen
import com.example.zell.InterestSelectionScreen
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

/**
 * SignUpFlow - Refactored coordinator for signup steps
 * 🔧 REFACTORED: Now serves as the single source of truth for the registration process
 * 🔗 CONNECTED: PhoneAuthScreen, CredentialsScreen, PersonaSelectionScreen, InterestSelectionScreen
 * 📝 NOTE: Handles Firebase orchestration and state management
 */

@Composable
@Suppress("DEPRECATION")
fun SignUpFlow(onSignInClick: () -> Unit, onComplete: () -> Unit) {
    // 🔧 REFACTORED: Centralized state management for all steps
    var currentStep by remember { mutableIntStateOf(1) }
    var isProcessing by remember { mutableStateOf(false) }
    var appError by remember { mutableStateOf<AppError?>(null) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val activity = context as? ComponentActivity

    // Step States
    var phoneAuthState by remember { mutableStateOf(PhoneAuthState()) }
    var credentialsState by remember { mutableStateOf(CredentialsState()) }
    var personaState by remember { mutableStateOf(PersonaState()) }
    var interestState by remember { mutableStateOf(InterestState()) }

    // 🔧 REFACTORED: Firebase Phone Auth Callbacks moved to coordinator
    val phoneAuthCallbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                isProcessing = true
                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                    isProcessing = false
                    if (task.isSuccessful) {
                        currentStep = 3
                        CrashlyticsLogger.i("SignUpFlow", "Auto-verification successful")
                    } else {
                        appError = ErrorHandler.classifyException(task.exception ?: Exception("Verification failed"))
                    }
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isProcessing = false
                appError = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("SignUpFlow", "Phone verification failed", e)
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                isProcessing = false
                phoneAuthState = phoneAuthState.copy(verificationId = verificationId)
                currentStep = 2
                CrashlyticsLogger.i("SignUpFlow", "OTP Code sent")
            }
        }
    }

    // Google Sign-In Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { idToken ->
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                isProcessing = true
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { authTask ->
                        isProcessing = false
                        if (authTask.isSuccessful) {
                            phoneAuthState = phoneAuthState.copy(
                                fullName = auth.currentUser?.displayName ?: "",
                                email = auth.currentUser?.email ?: ""
                            )
                            currentStep = 3
                            CrashlyticsLogger.i("SignUpFlow", "Google sign-in successful")
                        } else {
                            appError = ErrorHandler.classifyException(authTask.exception ?: Exception("Google Sign-In failed"))
                        }
                    }
            }
        } catch (e: ApiException) {
            appError = AppError(type = ErrorType.AUTH, title = "Google Error", message = e.message ?: "Failed")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("signup_flow")
    ) {
        BackgroundOrbs()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp)
        ) {
            CardHeader(currentStep = currentStep)
            Spacer(modifier = Modifier.height(32.dp))
            ProgressTrack(step = currentStep)
            Spacer(modifier = Modifier.height(48.dp))

            ErrorBanner(error = appError, onDismiss = { appError = null })

            Box(modifier = Modifier.weight(1f)) {
                if (isProcessing) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    AnimatedContent(
                        targetState = currentStep,
                        transitionSpec = {
                            (slideInVertically { it } + fadeIn()).togetherWith(slideOutVertically { -it } + fadeOut())
                        },
                        label = "step_transition"
                    ) { step ->
                        when (step) {
                            // 🔧 REFACTORED: Wiring modular screens with proper state and callbacks
                            1, 2 -> PhoneAuthScreen(
                                state = phoneAuthState,
                                onStateChange = { phoneAuthState = it }
                            )
                            3 -> CredentialsScreen(
                                email = phoneAuthState.email,
                                state = credentialsState,
                                onStateChange = { credentialsState = it },
                                onCredentialsSet = { currentStep = 4 }
                            )
                            4 -> PersonaSelectionScreen(
                                state = personaState,
                                onStateChange = { personaState = it }
                            )
                            5 -> InterestSelectionScreen(
                                state = interestState,
                                onStateChange = { interestState = it }
                            )
                            6 -> DoneStep(
                                name = phoneAuthState.fullName,
                                email = phoneAuthState.email
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (currentStep <= 1) {
                    GoogleButton(
                        onClick = {
                            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                                .requestIdToken("540355292191-0gcj7apssqj0mh4pf6va3c13002vvmu9.apps.googleusercontent.com")
                                .requestEmail()
                                .build()
                            launcher.launch(GoogleSignIn.getClient(context, gso).signInIntent)
                        },
                        enabled = !isProcessing
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 🔧 REFACTORED: Unified progression logic for the PrimaryButton
                val isNextEnabled = when (currentStep) {
                    1 -> phoneAuthState.phoneNumber.length >= 7 && phoneAuthState.fullName.isNotEmpty() && phoneAuthState.agreedToTerms
                    2 -> phoneAuthState.otpCode.length == 6
                    3 -> credentialsState.password.length >= 6 && credentialsState.password == credentialsState.confirmPassword
                    4 -> personaState.selected != null
                    5 -> interestState.selectedInterests.size >= 3
                    else -> true
                }

                PrimaryButton(
                    text = when (currentStep) {
                        1 -> "Verify Phone"
                        2 -> "Confirm OTP"
                        3 -> "Register Account"
                        5 -> "Finalize Account"
                        6 -> "Go to Dashboard"
                        else -> "Continue"
                    },
                    enabled = isNextEnabled && !isProcessing,
                    onClick = {
                        appError = null
                        when (currentStep) {
                            1 -> {
                                if (validatePhoneForm(phoneAuthState, { phoneAuthState = it }, { appError = it })) {
                                    isProcessing = true
                                    val fullNumber = phoneAuthState.countryCode + phoneAuthState.phoneNumber
                                    val options = PhoneAuthOptions.newBuilder(auth)
                                        .setPhoneNumber(fullNumber)
                                        .setTimeout(60L, TimeUnit.SECONDS)
                                        .setActivity(activity!!)
                                        .setCallbacks(phoneAuthCallbacks)
                                        .build()
                                    PhoneAuthProvider.verifyPhoneNumber(options)
                                }
                            }
                            2 -> {
                                isProcessing = true
                                val credential = PhoneAuthProvider.getCredential(phoneAuthState.verificationId!!, phoneAuthState.otpCode)
                                auth.signInWithCredential(credential).addOnCompleteListener { task ->
                                    isProcessing = false
                                    if (task.isSuccessful) currentStep = 3
                                    else appError = ErrorHandler.classifyException(task.exception ?: Exception("Invalid OTP"))
                                }
                            }
                            3 -> validateAndProceedCredentials(
                                phoneAuthState.email,
                                credentialsState,
                                { credentialsState = it },
                                { appError = it },
                                { currentStep = 4 }
                            )
                            in 4..5 -> currentStep++
                            6 -> onComplete()
                        }
                    }
                )

                if (currentStep < 6) {
                    Spacer(modifier = Modifier.height(16.dp))
                    SignInLink(onClick = onSignInClick)
                }
            }
        }
    }
}

// 🔧 REFACTORED: Helper validation functions moved from private class methods to top-level for cleaner access
private fun validatePhoneForm(
    state: PhoneAuthState,
    onStateChange: (PhoneAuthState) -> Unit,
    onError: (AppError) -> Unit
): Boolean {
    var hasError = false
    var nameError: String? = null
    var emailError: String? = null
    var phoneError: String? = null

    if (state.fullName.length < 2) { nameError = "Name is too short"; hasError = true }
    if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) { emailError = "Invalid email"; hasError = true }
    if (state.phoneNumber.length < 7) { phoneError = "Invalid phone"; hasError = true }
    if (!state.agreedToTerms) { 
        onError(AppError(type = ErrorType.VALIDATION, title = "Required", message = "Agree to terms")); 
        hasError = true 
    }

    onStateChange(state.copy(fullNameError = nameError, emailError = emailError, phoneError = phoneError))
    return !hasError
}

private fun validateAndProceedCredentials(
    email: String,
    state: CredentialsState,
    onStateChange: (CredentialsState) -> Unit,
    onError: (AppError) -> Unit,
    onSuccess: () -> Unit
) {
    // 🔧 REFACTORED: Credentials logic moved to coordinator for consistent error handling
    if (state.password.length < 6) {
        onStateChange(state.copy(passwordError = "Too short"))
        return
    }
    if (state.password != state.confirmPassword) {
        onStateChange(state.copy(passwordError = "No match"))
        return
    }

    val auth = FirebaseAuth.getInstance()
    auth.createUserWithEmailAndPassword(email, state.password).addOnCompleteListener { task ->
        if (task.isSuccessful) onSuccess()
        else onError(ErrorHandler.classifyException(task.exception ?: Exception("Failed")))
    }
}

// UI Components (Kept for consistency with user request not to remove existing code)

@Composable
fun GoogleButton(onClick: () -> Unit, enabled: Boolean) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text("Sign up with Google")
        }
    }
}

@Composable
fun CardHeader(currentStep: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        if (currentStep <= 5) {
            Text("ZELL", fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Text("0$currentStep / 05", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
        }
    }
}

@Composable
fun ProgressTrack(step: Int) {
    val progress by animateFloatAsState(targetValue = if (step > 5) 1f else (step - 1) / 4f, label = "progress")
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
}

@Composable
fun DoneStep(name: String, email: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(100.dp).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Welcome, ${name.split(" ").firstOrNull()}!", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Account authorized for $email.", textAlign = TextAlign.Center)
    }
}

@Composable
fun ElaraTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false,
    errorMessage: String? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    testTag: String = ""
) {
    // 🔧 REFACTORED: Restored missing visualTransformation and trailingIcon parameters to fix build errors (doneby Gemini)
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(0.4f))
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().testTag(testTag),
            placeholder = { Text(placeholder) },
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            isError = isError,
            shape = RoundedCornerShape(12.dp)
        )
        if (isError && errorMessage != null) {
            Text(errorMessage, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("primary_button"),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SignInLink(onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
        Text("Sign In", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
fun BackgroundOrbs() {
    // 🔧 REFACTORED: Simplified BackgroundOrbs for the final build integration
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(Color.Blue.copy(0.05f), 200.dp.toPx(), Offset(size.width * 0.2f, size.height * 0.2f))
    }
}

// 🔧 REFACTORED: Moved ErrorBanner to a standard composable here for visibility
@Composable
fun ErrorBanner(error: AppError?, onDismiss: () -> Unit) {
    if (error != null) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(8.dp)
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(error.message, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                Text("Dismiss", modifier = Modifier.clickable { onDismiss() }, fontWeight = FontWeight.Bold)
            }
        }
    }
}

val countryCodes = listOf("+234" to "🇳🇬", "+1" to "🇺🇸", "+44" to "🇬🇧")
