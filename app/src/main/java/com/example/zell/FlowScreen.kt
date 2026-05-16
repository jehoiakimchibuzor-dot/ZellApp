package com.example.zell

import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Data Model ────────────────────────────────────────────────────────────────

data class ReelVideo(
    val id: String = "",
    val url: String = "",
    val author: String = "",
    val authorId: String = "",
    val description: String = "",
    val musicName: String = "Zell Original Audio",
    val likes: Long = 0L,
    val comments: Long = 0L,
    val avatarUrl: String = "",
    // Map of uid → true so we can track per-user likes without a subcollection
    val likedBy: Map<String, Boolean> = emptyMap()
)

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * FlowScreen — Immersive, full-screen vertical video feed (TikTok / Instagram Reels style).
 *
 * Features:
 *   • Real-time Firestore data with mock fallback
 *   • Auto-play / pause based on current page visibility
 *   • Tap to pause / resume
 *   • Double-tap to like (heart burst animation, Instagram style)
 *   • Mute / unmute volume
 *   • Like toggle that persists to Firestore
 *   • Proper ExoPlayer lifecycle (created once per item, released on dispose)
 */
@Composable
fun FlowScreen() {
    val db  = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    var videoList by remember { mutableStateOf<List<ReelVideo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var appError  by remember { mutableStateOf<AppError?>(null) }

    // Global mute state — persists as you swipe (same behaviour as TikTok)
    var isMuted by remember { mutableStateOf(false) }

    // Real-time listener — keep a reference so we can remove it on dispose
    DisposableEffect(Unit) {
        val reg: ListenerRegistration = db.collection("reels")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                isLoading = false
                if (error != null) {
                    appError = ErrorHandler.classifyException(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    videoList = snapshot.toObjects(ReelVideo::class.java)
                }
            }
        onDispose { reg.remove() }
    }

    val listToUse = if (videoList.isEmpty()) mockReelVideos else videoList

    // key() forces the pager to reset when we switch from mock → real data
    key(listToUse.size) {
        val pagerState = rememberPagerState { listToUse.size }

        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else {
                VerticalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1          // pre-load one item ahead
                ) { pageIndex ->
                    ReelPlayerItem(
                        video        = listToUse[pageIndex],
                        isCurrentPage = pagerState.currentPage == pageIndex,
                        isMuted      = isMuted,
                        currentUserId = currentUserId,
                        onToggleMute  = { isMuted = !isMuted },
                        onLike        = { reelId, isLiked ->
                            toggleLike(db, reelId, currentUserId, isLiked)
                        }
                    )
                }
            }

            // Error banner
            if (appError != null) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(top = 60.dp)
                        .align(Alignment.TopCenter)
                ) {
                    ErrorBanner(error = appError, onDismiss = { appError = null })
                }
            }

            // Top bar — floats above the pager
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Flow",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = (-1).sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Mute button in the top bar so it's always visible
                    IconButton(onClick = { isMuted = !isMuted }) {
                        Icon(
                            if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { /* camera / create reel */ }) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = "Create Reel", tint = Color.White)
                    }
                }
            }
        }
    }
}

// ── Single Reel Item ──────────────────────────────────────────────────────────

@OptIn(UnstableApi::class)
@Composable
fun ReelPlayerItem(
    video: ReelVideo,
    isCurrentPage: Boolean,
    isMuted: Boolean,
    currentUserId: String,
    onToggleMute: () -> Unit,
    onLike: (reelId: String, isCurrentlyLiked: Boolean) -> Unit
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current
    val scope   = rememberCoroutineScope()

    // ── Player setup ──────────────────────────────────────────────────────────
    val exoPlayer = remember(video.url) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            volume     = if (isMuted) 0f else 1f
            setMediaItem(MediaItem.fromUri(video.url))
            prepare()
        }
    }

    // Sync play/pause with page focus
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage) exoPlayer.play() else exoPlayer.pause()
    }

    // Sync mute with global mute toggle
    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    DisposableEffect(video.url) {
        onDispose { exoPlayer.release() }
    }

    // ── Local UI state ────────────────────────────────────────────────────────
    var isPaused         by remember { mutableStateOf(false) }
    var showHeartBurst   by remember { mutableStateOf(false) }
    val isLiked = video.likedBy[currentUserId] == true
    val likeScale by animateFloatAsState(
        targetValue = if (isLiked) 1.3f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "likeScale"
    )

    // ── Heart-burst animation (double-tap like) ───────────────────────────────
    val heartScale by animateFloatAsState(
        targetValue = if (showHeartBurst) 1f else 0f,
        animationSpec = tween(200),
        label = "heartBurst"
    )
    val heartAlpha by animateFloatAsState(
        targetValue = if (showHeartBurst) 1f else 0f,
        animationSpec = tween(200),
        label = "heartAlpha"
    )
    LaunchedEffect(showHeartBurst) {
        if (showHeartBurst) {
            delay(700)
            showHeartBurst = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        // Single tap → pause / resume
                        isPaused = !isPaused
                        if (isPaused) exoPlayer.pause() else exoPlayer.play()
                    },
                    onDoubleTap = {
                        // Double tap → like + heart burst
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showHeartBurst = true
                        if (!isLiked) onLike(video.id, false)
                    }
                )
            }
    ) {
        // ── Video surface ─────────────────────────────────────────────────────
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = false
                    resizeMode    = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    player        = exoPlayer
                }
            },
            update = { it.player = exoPlayer },
            modifier = Modifier.fillMaxSize()
        )

        // ── Bottom gradient ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors  = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)),
                        startY  = 900f
                    )
                )
        )

        // ── Pause icon (centre) ───────────────────────────────────────────────
        AnimatedVisibility(
            visible = isPaused,
            enter   = scaleIn(initialScale = 0.6f) + fadeIn(),
            exit    = scaleOut(targetScale  = 0.6f) + fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Pause, null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }

        // ── Heart burst (double-tap like) ─────────────────────────────────────
        if (showHeartBurst || heartScale > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .scale(heartScale * 3f)
                    .graphicsLayer { alpha = heartAlpha },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        // ── Right-side actions + bottom info ──────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 24.dp, end = 16.dp, start = 20.dp)
        ) {
            // Right column — avatar + actions
            Column(
                modifier = Modifier.align(Alignment.BottomEnd),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Profile avatar with follow (+) badge
                Box(modifier = Modifier.size(50.dp)) {
                    AsyncImage(
                        model              = video.avatarUrl,
                        contentDescription = video.author,
                        modifier           = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentScale       = ContentScale.Crop
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

                // Like button — scale animation + real toggle
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLike(video.id, isLiked)
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint     = if (isLiked) Color.Red else Color.White,
                        modifier = Modifier
                            .size(32.dp)
                            .scale(likeScale)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        formatCount(video.likes),
                        color      = Color.White,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Comments
                ReelActionIcon(Icons.Filled.ChatBubble, formatCount(video.comments))

                // Share
                ReelActionIcon(Icons.Filled.Share, "Share")

                // More options
                ReelActionIcon(Icons.Default.MoreHoriz, "")

                // Rotating vinyl disc
                RotatingVinyl(video.avatarUrl)
            }

            // Bottom-left — author + caption + music
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.75f)
                    .padding(bottom = 8.dp)
            ) {
                Text(
                    "@${video.author}",
                    color      = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize   = 17.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    video.description,
                    color      = Color.White,
                    fontSize   = 14.sp,
                    maxLines   = 3,
                    lineHeight = 20.sp,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(14.dp))
                // Music chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.MusicNote, null,
                        tint     = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        video.musicName,
                        color      = Color.White,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                        modifier   = Modifier.widthIn(max = 160.dp)
                    )
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/** Rotates the creator's avatar like a vinyl disc — TikTok style */
@Composable
fun RotatingVinyl(avatarUrl: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 360f,
        animationSpec = infiniteRepeatable(
            animation  = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylRotation"
    )
    Box(
        modifier = Modifier
            .size(42.dp)
            .graphicsLayer { rotationZ = rotation }
            .clip(CircleShape)
            .background(Color(0xFF1A1A1A))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model              = avatarUrl,
            contentDescription = null,
            modifier           = Modifier.fillMaxSize().clip(CircleShape),
            contentScale       = ContentScale.Crop
        )
    }
}

@Composable
fun ReelActionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(30.dp))
        if (label.isNotEmpty()) {
            Spacer(Modifier.height(5.dp))
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** Friendly number formatting: 12400 → "12.4K", 1200000 → "1.2M" */
private fun formatCount(count: Long): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0).trimEnd('0').trimEnd('.')
    count >= 1_000     -> "%.1fK".format(count / 1_000.0).trimEnd('0').trimEnd('.')
    else               -> count.toString()
}

/** Toggle like in Firestore using a map field so it's concurrency-safe */
private fun toggleLike(
    db: FirebaseFirestore,
    reelId: String,
    userId: String,
    isCurrentlyLiked: Boolean
) {
    if (reelId.isBlank() || userId.isBlank()) return
    val ref = db.collection("reels").document(reelId)
    if (isCurrentlyLiked) {
        ref.update(
            mapOf(
                "likedBy.$userId" to FieldValue.delete(),
                "likes"           to FieldValue.increment(-1)
            )
        )
    } else {
        ref.update(
            mapOf(
                "likedBy.$userId" to true,
                "likes"           to FieldValue.increment(1)
            )
        )
    }
}

// ── Mock data (debug-only fallback) ──────────────────────────────────────────

val mockReelVideos = listOf(
    ReelVideo(
        id          = "mock_1",
        url         = "https://assets.mixkit.co/videos/preview/mixkit-tree-with-yellow-flowers-1173-large.mp4",
        author      = "nature.vibes",
        authorId    = "mock_author_1",
        description = "Golden hour hits different in the spring. 🌼 Just breathe. #nature #goldenhour #aesthetic",
        musicName   = "Nature Sounds – Zell Audio",
        likes       = 12400L,
        comments    = 1240L,
        avatarUrl   = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200"
    ),
    ReelVideo(
        id          = "mock_2",
        url         = "https://assets.mixkit.co/videos/preview/mixkit-stunning-sunset-view-from-a-mountain-top-42456-large.mp4",
        author      = "explorer.marcus",
        authorId    = "mock_author_2",
        description = "Found this hidden gem in the mountains. Absolute peace. Zell is taking me places! 🏔️✨",
        musicName   = "Mountain High – Zell",
        likes       = 45200L,
        comments    = 3412L,
        avatarUrl   = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200"
    ),
    ReelVideo(
        id          = "mock_3",
        url         = "https://assets.mixkit.co/videos/preview/mixkit-man-working-on-his-laptop-308-large.mp4",
        author      = "zell.builds",
        authorId    = "mock_author_3",
        description = "Late night coding sessions for the new Zell update. Building the future one line at a time 💻🔥",
        musicName   = "Lofi Beats – Zell Originals",
        likes       = 8100L,
        comments    = 567L,
        avatarUrl   = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200"
    )
)
