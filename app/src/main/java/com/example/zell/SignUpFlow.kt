package com.example.zell

import android.util.Patterns
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

// Data models
data class Persona(val title: String, val icon: String, val description: String)
data class Interest(val name: String, val category: String)

val personas = listOf(
    Persona("Builder", "⚡", "I build systems and solve complex problems."),
    Persona("Creator", "✦", "I design beautiful and intuitive experiences."),
    Persona("Analyst", "◎", "I find insights in data and drive decisions."),
    Persona("Explorer", "◈", "I discover new markets and opportunities.")
)

val interestsList = listOf(
    Interest("Technology", "Tech"), Interest("AI", "Tech"), Interest("Coding", "Tech"),
    Interest("Blockchain", "Tech"), Interest("Gadgets", "Tech"), Interest("Cybersecurity", "Tech"),
    Interest("Design", "Creative"), Interest("Art", "Creative"), Interest("Photography", "Creative"),
    Interest("UI/UX", "Creative"), Interest("Architecture", "Creative"), Interest("Fashion", "Creative"),
    Interest("Finance", "Business"), Interest("Investing", "Business"), Interest("Startup", "Business"),
    Interest("Marketing", "Business"), Interest("Economy", "Business"), Interest("E-commerce", "Business"),
    Interest("Gaming", "Lifestyle"), Interest("Fitness", "Lifestyle"), Interest("Travel", "Lifestyle"),
    Interest("Cooking", "Lifestyle"), Interest("Health", "Lifestyle"), Interest("Sports", "Lifestyle"),
    Interest("Music", "Entertainment"), Interest("Movies", "Entertainment"), Interest("Anime", "Entertainment"),
    Interest("Books", "Entertainment"), Interest("Podcasts", "Entertainment")
)

val countryCodes = listOf(
    "+234" to "🇳🇬",
    "+1" to "🇺🇸",
    "+44" to "🇬🇧",
    "+233" to "🇬🇭",
    "+254" to "🇰🇪",
    "+27" to "🇿🇦",
    "+91" to "🇮🇳",
    "+86" to "🇨🇳"
)

@Composable
fun SignUpFlow(onSignInClick: () -> Unit, onComplete: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(1) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var agreedToTerms by remember { mutableStateOf(false) }
    
    // State storage for user input
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var countryCode by remember { mutableStateOf("+234") }
    var otpCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedPersona by remember { mutableStateOf<Persona?>(null) }
    val selectedInterests = remember { mutableStateListOf<Interest>() }

    // Validation States
    var fullNameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val activity = context as? android.app.Activity

    // Phone Auth Callbacks
    val callbacks = remember {
        object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verify if Android detects the SMS
                auth.signInWithCredential(credential)
                    .addOnCompleteListener { task ->
                        isProcessing = false
                        if (task.isSuccessful) {
                            currentStep = 3 
                        } else {
                            errorMessage = task.exception?.localizedMessage
                        }
                    }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                isProcessing = false
                errorMessage = e.localizedMessage ?: "Verification failed"
            }

            override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                isProcessing = false
                verificationId = id
                currentStep = 2 // Move to OTP entry step
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
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            isProcessing = true
            auth.signInWithCredential(credential)
                .addOnCompleteListener { authTask ->
                    isProcessing = false
                    if (authTask.isSuccessful) {
                        fullName = auth.currentUser?.displayName ?: ""
                        email = auth.currentUser?.email ?: ""
                        currentStep = 4 // Skip to Persona selection
                    } else {
                        errorMessage = authTask.exception?.localizedMessage ?: "Google Sign-In failed"
                    }
                }
        } catch (e: ApiException) {
            errorMessage = "Google Sign-In error: ${e.message}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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

            // Error Display
            if (errorMessage != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        fontSize = 13.sp
                    )
                }
            }

            // Main screen content with transitions
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
                            1 -> IdentityStep(
                                fullName, { fullName = it; fullNameError = null }, 
                                email, { email = it; emailError = null },
                                phoneNumber, { phoneNumber = it; phoneError = null },
                                countryCode, { countryCode = it },
                                agreedToTerms, { agreedToTerms = it },
                                fullNameError, emailError, phoneError
                            )
                            2 -> OtpStep(otpCode) { otpCode = it }
                            3 -> CredentialsStep(
                                password, { password = it; passwordError = null }, 
                                confirmPassword, { confirmPassword = it; passwordError = null },
                                passwordError
                            )
                            4 -> PersonaStep(selectedPersona, { selectedPersona = it })
                            5 -> InterestsStep(selectedInterests)
                            6 -> DoneStep(name = fullName, email = email)
                        }
                    }
                }
            }

            // Bottom action area
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
                            val client = GoogleSignIn.getClient(context, gso)
                            launcher.launch(client.signInIntent)
                        },
                        enabled = !isProcessing
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                val isNextEnabled = when (currentStep) {
                    2 -> otpCode.length == 6
                    4 -> selectedPersona != null
                    5 -> selectedInterests.size >= 3
                    else -> true
                }

                PrimaryButton(
                    text = when(currentStep) {
                        1 -> "Verify Phone"
                        2 -> "Confirm OTP"
                        3 -> "Register Account"
                        5 -> "Finalize Account"
                        6 -> "Go to Dashboard"
                        else -> "Continue"
                    },
                    enabled = isNextEnabled && !isProcessing,
                    onClick = {
                        errorMessage = null
                        when (currentStep) {
                            1 -> {
                                // Validation
                                var hasError = false
                                if (fullName.length < 2) {
                                    fullNameError = "Name is too short"
                                    hasError = true
                                }
                                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                    emailError = "Invalid email format"
                                    hasError = true
                                }
                                if (phoneNumber.length < 7) {
                                    phoneError = "Invalid phone number"
                                    hasError = true
                                }
                                if (!agreedToTerms) {
                                    errorMessage = "Please agree to the Terms of Service"
                                    hasError = true
                                }

                                if (!hasError) {
                                    // SEND OTP
                                    isProcessing = true
                                    if (activity != null) {
                                        val fullNumber = countryCode + phoneNumber
                                        val options = PhoneAuthOptions.newBuilder(auth)
                                            .setPhoneNumber(fullNumber)
                                            .setTimeout(60L, TimeUnit.SECONDS)
                                            .setActivity(activity)
                                            .setCallbacks(callbacks)
                                            .build()
                                        PhoneAuthProvider.verifyPhoneNumber(options)
                                    } else {
                                        errorMessage = "Internal Error: No Activity found"
                                        isProcessing = false
                                    }
                                }
                            }
                            2 -> {
                                // REAL OTP VERIFICATION
                                if (verificationId != null) {
                                    isProcessing = true
                                    val credential = PhoneAuthProvider.getCredential(verificationId!!, otpCode)
                                    auth.signInWithCredential(credential)
                                        .addOnCompleteListener { task ->
                                            isProcessing = false
                                            if (task.isSuccessful) {
                                                currentStep++
                                            } else {
                                                errorMessage = task.exception?.localizedMessage ?: "Invalid OTP"
                                            }
                                        }
                                } else {
                                    errorMessage = "Verification ID missing. Please resend."
                                }
                            }
                            3 -> {
                                // Validation
                                if (password.length < 6) {
                                    passwordError = "Password must be at least 6 characters"
                                } else if (password != confirmPassword) {
                                    passwordError = "Passwords do not match"
                                } else {
                                    // LINK EMAIL/PASSWORD TO THE VERIFIED PHONE ACCOUNT
                                    isProcessing = true
                                    val user = auth.currentUser
                                    if (user != null) {
                                        val credential = EmailAuthProvider.getCredential(email, password)
                                        user.linkWithCredential(credential)
                                            .addOnCompleteListener { task ->
                                                isProcessing = false
                                                if (task.isSuccessful) {
                                                    currentStep++
                                                } else {
                                                    errorMessage = task.exception?.localizedMessage ?: "Account creation failed"
                                                }
                                            }
                                    } else {
                                        // Fallback to simple creation if phone step was skipped somehow
                                        auth.createUserWithEmailAndPassword(email, password)
                                            .addOnCompleteListener { task ->
                                                isProcessing = false
                                                if (task.isSuccessful) {
                                                    currentStep++
                                                } else {
                                                    errorMessage = task.exception?.localizedMessage ?: "Registration failed"
                                                }
                                            }
                                    }
                                }
                            }
                            in 4..5 -> currentStep++
                            else -> onComplete()
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

@Composable
fun OtpStep(otp: String, onOtpChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Verification", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = "Enter the 6-digit code sent to your phone.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(48.dp))
        
        ElaraTextField(
            value = otp,
            onValueChange = { if (it.length <= 6) onOtpChange(it) },
            label = "OTP CODE",
            placeholder = "000 000",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
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

@Composable
fun GoogleButton(onClick: () -> Unit, enabled: Boolean) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google Logo",
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text("Sign up with Google", fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun CardHeader(currentStep: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (currentStep <= 5) {
            Text(
                text = "ZELL",
                style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp, color = MaterialTheme.colorScheme.onBackground)
            )
            Text(
                text = "0$currentStep / 05",
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            )
        }
    }
}

@Composable
fun ProgressTrack(step: Int) {
    val progress by animateFloatAsState(
        targetValue = if (step > 5) 1f else (step - 1) / 4f,
        animationSpec = tween(500),
        label = "progress"
    )
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

    Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
        val width = size.width
        val centerY = size.height / 2
        drawLine(color = outline, start = Offset(0f, centerY), end = Offset(width, centerY), strokeWidth = 4.dp.toPx())
        drawLine(color = primary, start = Offset(0f, centerY), end = Offset(width * progress, centerY), strokeWidth = 4.dp.toPx())
        for (i in 0..4) {
            val isReached = if (step > 5) true else (step - 1) >= i
            drawCircle(color = if (isReached) primary else outline, radius = 6.dp.toPx(), center = Offset(width * (i / 4f), centerY))
        }
    }
}

@Composable
fun IdentityStep(
    name: String, 
    onNameChange: (String) -> Unit, 
    email: String, 
    onEmailChange: (String) -> Unit,
    phoneNumber: String,
    onPhoneNumberChange: (String) -> Unit,
    countryCode: String,
    onCountryCodeChange: (String) -> Unit,
    agreedToTerms: Boolean,
    onAgreedChange: (Boolean) -> Unit,
    nameError: String? = null,
    emailError: String? = null,
    phoneError: String? = null
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Text(text = "Identity", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = "Tell us who you are.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(32.dp))
        
        ElaraTextField(name, onNameChange, "FULL NAME", "Enter your name", isError = nameError != null, errorMessage = nameError)
        Spacer(modifier = Modifier.height(24.dp))
        
        ElaraTextField(email, onEmailChange, "EMAIL ADDRESS", "name@example.com", keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), isError = emailError != null, errorMessage = emailError)
        Spacer(modifier = Modifier.height(24.dp))
        
        Text("PHONE NUMBER", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(
                    modifier = Modifier
                        .height(56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = countryCodes.find { it.first == countryCode }?.second ?: "",
                        fontSize = 20.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = countryCode,
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
                                onCountryCodeChange(code)
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            BasicTextField(
                value = phoneNumber,
                onValueChange = onPhoneNumberChange,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, if(phoneError != null) Color.Red else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                textStyle = TextStyle(MaterialTheme.colorScheme.onSurface, 16.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                decorationBox = { inner ->
                    if (phoneNumber.isEmpty()) {
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
        if (phoneError != null) {
            Text(phoneError, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = agreedToTerms,
                onCheckedChange = onAgreedChange,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
            )
            Text(
                text = "I agree to the Terms of Service and Privacy Policy.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.clickable { onAgreedChange(!agreedToTerms) }
            )
        }
    }
}

@Composable
fun CredentialsStep(password: String, onPasswordChange: (String) -> Unit, confirmPassword: String, onConfirmPasswordChange: (String) -> Unit, error: String? = null) {
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Credentials", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = "Secure your protocol with a strong password.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(40.dp))
        
        ElaraTextField(
            password, onPasswordChange, "PASSWORD", "Min. 6 chars + # + @",
            if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = error != null
        ) {
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        ElaraTextField(
            confirmPassword, onConfirmPasswordChange, "CONFIRM PASSWORD", "Repeat your password",
            if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = error != null
        ) {
            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                Icon(if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
        
        if (error != null) {
            Text(error, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        PasswordStrengthMeter(password)
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
        1, 2 -> Color(0xFFE53935)
        3 -> Color(0xFFFFB300)
        4 -> Color(0xFF43A047)
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }
    val labels = listOf("Too short", "Needs symbols/numbers", "Fair", "Strong")
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) { index -> Box(modifier = Modifier.weight(1f).height(4.dp).clip(CircleShape).background(if (index < strength) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))) }
        }
        if (password.isNotEmpty()) Text(labels.getOrElse(strength - 1) { "" }, fontSize = 12.sp, color = color, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PersonaStep(selected: Persona?, onSelect: (Persona) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Persona", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = "Choose your starting class.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(32.dp))
        LazyVerticalGrid(columns = GridCells.Fixed(2), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            items(personas) { PersonaCard(it, selected == it, { onSelect(it) }) }
        }
    }
}

@Composable
fun PersonaCard(persona: Persona, isSelected: Boolean, onClick: () -> Unit) {
    val borderColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), label = "border")
    val bgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent, label = "bg")
    Box(modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(bgColor).border(1.dp, borderColor, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(16.dp).height(140.dp)) {
        Column {
            Text(persona.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(persona.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(4.dp))
            Text(persona.description, fontSize = 12.sp, lineHeight = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InterestsStep(selectedInterests: MutableList<Interest>) {
    var searchQuery by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = "Interests", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Text(text = "Pick 3+ topics to build your feed.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(24.dp))
        ElaraTextField(searchQuery, { searchQuery = it }, "SEARCH TOPICS", "e.g. Design, Crypto...", trailingIcon = { Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary) })
        Spacer(modifier = Modifier.height(24.dp))
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            val categories = interestsList.filter { it.name.contains(searchQuery, ignoreCase = true) }.groupBy { it.category }
            if (categories.isEmpty()) Text("No topics found for \"$searchQuery\"", color = Color.Gray, modifier = Modifier.padding(16.dp))
            categories.forEach { (category, items) ->
                Text(category.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 12.dp))
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.forEach { interest ->
                        val isSelected = selectedInterests.contains(interest)
                        val bgColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, label = "chip_bg")
                        val textColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, label = "chip_text")
                        Box(Modifier.clip(CircleShape).background(bgColor).border(1.dp, if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape).clickable { if (isSelected) selectedInterests.remove(interest) else selectedInterests.add(interest) }.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(interest.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun DoneStep(name: String, email: String) {
    val firstName = name.split(" ").firstOrNull() ?: name
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); animateIn = true }
    val scale by animateFloatAsState(if (animateIn) 1f else 0f, spring(0.6f, Spring.StiffnessLow), label = "done_scale")
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(modifier = Modifier.size(100.dp).scale(scale).background(MaterialTheme.colorScheme.primary, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Welcome, $firstName!", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Your account has been authorized for $email. Protocol activation complete.", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), textAlign = TextAlign.Center)
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
    trailingIcon: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        if (isError) Color.Red 
        else if (isFocused) MaterialTheme.colorScheme.primary 
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), 
        label = "border"
    )
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = if (isError) Color.Red else if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(8.dp))
        BasicTextField(value, onValueChange, Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }.border(1.dp, borderColor, RoundedCornerShape(12.dp)).padding(16.dp), textStyle = TextStyle(MaterialTheme.colorScheme.onSurface, 16.sp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) { if (value.isEmpty()) Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), fontSize = 16.sp); inner() }
                if (trailingIcon != null) trailingIcon()
            }
        })
        if (isError && errorMessage != null) {
            Text(errorMessage, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "btn_scale")
    Button(onClick, enabled = enabled, interactionSource = interactionSource, modifier = Modifier.fillMaxWidth().height(56.dp).scale(scale), colors = ButtonDefaults.buttonColors(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, MaterialTheme.colorScheme.primary.copy(0.3f), MaterialTheme.colorScheme.onPrimary.copy(0.5f)), shape = RoundedCornerShape(16.dp), elevation = ButtonDefaults.buttonElevation(0.dp)) {
        Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
fun SignInLink(onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), Arrangement.Center) {
        Text("Already have an account? ", color = MaterialTheme.colorScheme.onSurface.copy(0.6f), fontSize = 14.sp)
        Text("Sign In", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onClick))
    }
}

@Composable
fun BackgroundOrbs() {
    val transition = rememberInfiniteTransition(label = "orbs")
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val o1x by transition.animateFloat(0.2f, 0.8f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), "o1x")
    val o1y by transition.animateFloat(0.1f, 0.4f, infiniteRepeatable(tween(15000, easing = LinearEasing), RepeatMode.Reverse), "o1y")
    val o2x by transition.animateFloat(0.7f, 0.1f, infiniteRepeatable(tween(12000, easing = LinearEasing), RepeatMode.Reverse), "o2x")
    val o2y by transition.animateFloat(0.8f, 0.5f, infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Reverse), "o2y")
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(primary.copy(0.05f), 200.dp.toPx(), Offset(size.width * o1x, size.height * o1y))
        drawCircle(secondary.copy(0.05f), 250.dp.toPx(), Offset(size.width * o2x, size.height * o2y))
    }
}
