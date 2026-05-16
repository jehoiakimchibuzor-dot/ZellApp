package com.example.zell

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WelcomeScreen(onTimeout: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(8f) } 
    val alpha = remember { Animatable(0f) }
    val impactShake = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fast-track for returning users
        val currentUser = FirebaseAuth.getInstance().currentUser
        val isFirstTime = currentUser == null

        launch {
            rotation.animateTo(
                targetValue = 1080f,
                animationSpec = tween(durationMillis = 700, easing = LinearEasing)
            )
        }
        launch {
            alpha.animateTo(1f, tween(200))
        }
        
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 700, easing = FastOutLinearInEasing)
        )

        launch {
            repeat(5) {
                impactShake.animateTo(20f, tween(30))
                impactShake.animateTo(-20f, tween(30))
            }
            impactShake.animateTo(0f, tween(100))
        }
        
        // Dynamic delay: shorter if user is already known
        delay(if (isFirstTime) 1500L else 800L)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        brush = Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
                ) {
                    append("Zell")
                }
            },
            fontSize = 100.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-4).sp,
            lineHeight = 100.sp,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = impactShake.value.dp)
                .graphicsLayer {
                    rotationZ = rotation.value
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "from",
                fontSize = 14.sp,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Jay's Hub",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}
