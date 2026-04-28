package com.example.zell

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.zell.R

data class OnBoardingPage(
    val title: String,
    val description: String,
    val videoRes: Int,
    val mainColor: Color
)

@Composable
fun OnBoardScreen(onNavigateToSignUp: () -> Unit) {
    val pages = listOf(
        OnBoardingPage(
            title = "Connect Globally",
            description = "Stay in touch with friends and family across the world with seamless messaging.",
            videoRes = R.raw.globe,
            mainColor = MaterialTheme.colorScheme.primary
        ),
        OnBoardingPage(
            title = "Express Yourself",
            description = "Share your thoughts, photos, and moments with your close circle effortlessly.",
            videoRes = R.raw.express, 
            mainColor = Color(0xFFFF0266)
        ),
        OnBoardingPage(
            title = "Secure & Private",
            description = "Your conversations are protected with end-to-end encryption for your peace of mind.",
            videoRes = R.raw.secure,
            mainColor = Color(0xFF03DAC6)
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Brand Title
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
            fontSize = 70.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-4).sp,
            lineHeight = 70.sp
        )

        Text(
            text = "Connect with people far and near",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = (-1).sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Horizontal Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { pageIndex ->
            OnBoardingContent(page = pages[pageIndex])
        }

        // Pager Indicators
        Row(
            modifier = Modifier
                .height(30.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) pages[iteration].mainColor else Color.LightGray.copy(alpha = 0.5f)
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(10.dp)
                        .background(color, shape = CircleShape)
                )
            }
        }

        // Space before legal text
        if (pagerState.currentPage == pages.size - 1) {
            val annotatedString = buildAnnotatedString {
                append("Read our ")
                pushStringAnnotation(tag = "URL", annotation = "https://www.google.com/search?q=privacy+policy")
                withStyle(SpanStyle(color = pages[pagerState.currentPage].mainColor, fontWeight = FontWeight.Bold)) {
                    append("Privacy Policy")
                }
                pop()
                append(". Tap \"Agree and Continue\" to accept the ")
                pushStringAnnotation(tag = "URL", annotation = "https://www.google.com/search?q=terms+of+service")
                withStyle(SpanStyle(color = pages[pagerState.currentPage].mainColor, fontWeight = FontWeight.Bold)) {
                    append("Terms of Service")
                }
                pop()
            }

            Spacer(modifier = Modifier.height(12.dp))
            ClickableText(
                text = annotatedString,
                style = TextStyle(
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = Color.Gray,
                    lineHeight = 18.sp
                ),
                modifier = Modifier.padding(horizontal = 40.dp),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            uriHandler.openUri(annotation.item)
                        }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        } else {
            Spacer(modifier = Modifier.height(40.dp))
        }

        // Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .height(80.dp),
            contentAlignment = Alignment.Center
        ) {
            if (pagerState.currentPage == pages.size - 1) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                
                // Animate the glow effect
                val glowRadius by animateDpAsState(
                    targetValue = if (isPressed) 16.dp else 0.dp,
                    animationSpec = tween(durationMillis = 100),
                    label = "glow"
                )
                
                val scale by animateFloatAsState(
                    targetValue = if (isPressed) 0.97f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
                    label = "scale"
                )

                Button(
                    onClick = { onNavigateToSignUp() },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .shadow(
                            elevation = glowRadius,
                            shape = RoundedCornerShape(16.dp),
                            clip = false,
                            ambientColor = pages[pagerState.currentPage].mainColor,
                            spotColor = pages[pagerState.currentPage].mainColor
                        ),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = pages[pagerState.currentPage].mainColor
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "Agree and continue",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun OnBoardingContent(page: OnBoardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Video Player Box
        Box(
            modifier = Modifier
                .size(320.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(page.mainColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            VideoPlayer(videoRes = page.videoRes)
        }

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = page.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            lineHeight = 38.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = page.description,
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(videoRes: Int) {
    val context = LocalContext.current
    
    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
            volume = 0f 
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> exoPlayer.play()
                Lifecycle.Event.ON_STOP -> exoPlayer.pause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            exoPlayer.release()
        }
    }

    LaunchedEffect(videoRes) {
        val path = "android.resource://${context.packageName}/$videoRes"
        exoPlayer.setMediaItem(MediaItem.fromUri(path))
        exoPlayer.prepare()
        exoPlayer.play()
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                player = exoPlayer
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
