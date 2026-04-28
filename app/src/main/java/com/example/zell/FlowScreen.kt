package com.example.zell

import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ReelVideo(
    val id: String = "",
    val url: String = "",
    val author: String = "",
    val description: String = "",
    val musicName: String = "Zell Original Audio",
    val likes: String = "0",
    val comments: String = "0",
    val avatarUrl: String = ""
)

@Composable
fun FlowScreen() {
    val db = FirebaseFirestore.getInstance()
    var videoList by remember { mutableStateOf<List<ReelVideo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        db.collection("reels")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { value, error ->
                if (value != null) {
                    videoList = value.toObjects(ReelVideo::class.java)
                }
                isLoading = false
            }
    }

    val pagerState = rememberPagerState { 
        if (videoList.isEmpty()) mockReelVideos.size else videoList.size 
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading && videoList.isEmpty()) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else {
            val listToUse = if (videoList.isEmpty()) mockReelVideos else videoList
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1
            ) { pageIndex ->
                ReelPlayerItem(
                    video = listToUse[pageIndex],
                    isCurrentPage = pagerState.currentPage == pageIndex
                )
            }
        }
        
        // Transparent Overlay Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Reels", color = Color.White, fontWeight = FontWeight.Black, fontSize = 24.sp, letterSpacing = (-1).sp)
            IconButton(onClick = {}) {
                Icon(Icons.Default.PhotoCamera, null, tint = Color.White)
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun ReelPlayerItem(video: ReelVideo, isCurrentPage: Boolean) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(video.url))
            prepare()
        }
    }

    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) {
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    player = exoPlayer
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Bottom Gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        startY = 1000f
                    )
                )
        )

        // UI Overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 24.dp, end = 16.dp, start = 20.dp)
        ) {
            // Right Side Actions
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Profile
                Box(modifier = Modifier.size(50.dp)) {
                    AsyncImage(
                        model = video.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape).border(2.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .align(Alignment.BottomCenter)
                            .offset(y = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
                
                ReelActionItem(Icons.Filled.Favorite, video.likes, color = if (video.likes.toIntOrNull() ?: 0 > 1000) Color.Red else Color.White)
                ReelActionItem(Icons.Filled.ChatBubble, video.comments)
                ReelActionItem(Icons.Filled.Share, "Share")
                ReelActionItem(Icons.Default.MoreHoriz, "")

                // Rotating Vinyl Effect
                RotatingVinyl(video.avatarUrl)
            }

            // Bottom Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.75f)
                    .padding(bottom = 8.dp)
            ) {
                Text(video.author, color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    video.description, 
                    color = Color.White, 
                    fontSize = 15.sp, 
                    maxLines = 3,
                    lineHeight = 20.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.MusicNote, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        video.musicName, 
                        color = Color.White, 
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.widthIn(max = 150.dp),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun RotatingVinyl(avatarUrl: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = Modifier
            .size(45.dp)
            .graphicsLayer { rotationZ = rotation }
            .clip(CircleShape)
            .background(Color.DarkGray)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun ReelActionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color = Color.White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

val mockReelVideos = listOf(
    ReelVideo(
        "1",
        "https://assets.mixkit.co/videos/preview/mixkit-tree-with-yellow-flowers-1173-large.mp4",
        "nature.vibes",
        "Golden hour hits different in the spring. 🌼 Just breathe. #nature #goldenhour #aesthetic",
        "Nature Sounds - Zell Audio",
        "12.4K",
        "1,240",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200"
    ),
    ReelVideo(
        "2",
        "https://assets.mixkit.co/videos/preview/mixkit-stunning-sunset-view-from-a-mountain-top-42456-large.mp4",
        "explorer.marcus",
        "Found this hidden gem in the mountains. Absolute peace. Zell is taking me places! 🏔️✨",
        "Mountain High - Zell",
        "45.2K",
        "3,412",
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200"
    ),
    ReelVideo(
        "3",
        "https://assets.mixkit.co/videos/preview/mixkit-man-working-on-his-laptop-308-large.mp4",
        "zell.builds",
        "Late night coding sessions for the new Zell update. Building the future one line at a time. 💻🔥",
        "Lofi Beats - Zell Originals",
        "8.1K",
        "567",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200"
    )
)
