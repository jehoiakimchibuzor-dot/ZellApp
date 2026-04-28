package com.example.zell

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(
    phoneNumber: String = "",
    onBack: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {}
) {
    var otpCode by remember { mutableStateOf("") }

    // Automatically navigate when 6 digits are entered
    LaunchedEffect(otpCode) {
        if (otpCode.length == 6) {
            delay(500L)
            onNavigateToProfile()
        }
    }

    // Pulsing background for consistency with SignUp
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.05f, targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(2500), RepeatMode.Reverse), label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // RADIAL GLOW
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha), Color.Transparent),
                        radius = 1200f
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // HUD TOP BAR
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground)
                }
                Text(
                    text = "X_VERIFY_DECRYPT_v1.0",
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 2.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // BRANDING
            Text(
                text = "SECURITY PROTOCOL",
                fontSize = 12.sp,
                letterSpacing = 8.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SEQUENCE VERIFICATION",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = buildAnnotatedString {
                    append("Awaiting decryption key sent to terminal ")
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                        append(phoneNumber)
                    }
                },
                fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = Color.Gray,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(80.dp))

            // THE OTP INPUT: Massive digital slots
            BasicTextField(
                value = otpCode,
                onValueChange = { if (it.length <= 6) otpCode = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = {
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        repeat(6) { index ->
                            val char = if (index < otpCode.length) otpCode[index].toString() else ""
                            val isFocused = otpCode.length == index
                            Box(
                                modifier = Modifier
                                    .width(45.dp)
                                    .height(60.dp)
                                    .background(
                                        if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.03f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = char,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Light,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    letterSpacing = 0.sp
                                )
                                if (char.isEmpty() && !isFocused) {
                                    Box(
                                        modifier = Modifier
                                            .size(4.dp)
                                            .background(Color.Gray.copy(alpha = 0.3f), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "ENTER_SEQUENCE_NOW",
                fontSize = 10.sp,
                letterSpacing = 4.sp,
                color = Color.Gray.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.weight(1f))

            // ACTION PANEL
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "RESEND_KEY_REQUEST",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { /* Handle Resend */ }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "ENCRYPTION_STATUS: PENDING...",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
