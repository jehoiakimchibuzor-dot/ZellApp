package com.example.zell

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class Story(val id: String, val imageUrl: String, val duration: Long = 5000L)
data class StoryGroup(
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val timeAgo: String = "now",
    val stories: List<Story>
)

// ─────────────────────────────────────────────────────────────────────────────
// StoryViewer — swipeable horizontal pager, one group per page
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StoryViewer(
    storyGroups: List<StoryGroup>,
    initialGroupIndex: Int = 0,
    onDismiss: () -> Unit,
    chatViewModel: ChatViewModel = viewModel()
) {
    if (storyGroups.isEmpty()) { onDismiss(); return }

    val pagerState    = rememberPagerState(initialPage = initialGroupIndex) { storyGroups.size }
    val coroutineScope = rememberCoroutineScope()

    HorizontalPager(
        state                = pagerState,
        modifier             = Modifier.fillMaxSize().background(Color.Black),
        beyondViewportPageCount = 1
    ) { pageIndex ->
        StoryGroupContent(
            group          = storyGroups[pageIndex],
            isActiveGroup  = pagerState.currentPage == pageIndex,
            onGroupFinished = {
                if (pageIndex < storyGroups.size - 1)
                    coroutineScope.launch { pagerState.animateScrollToPage(pageIndex + 1) }
                else onDismiss()
            },
            onPreviousGroup = {
                if (pageIndex > 0)
                    coroutineScope.launch { pagerState.animateScrollToPage(pageIndex - 1) }
            },
            onDismiss  = onDismiss,
            onReplySend = { text, storyImageUrl ->
                chatViewModel.sendStoryReply(storyGroups[pageIndex].userId, text, storyImageUrl)
            },
            onStoryViewed = { storyId ->
                chatViewModel.recordStoryView(storyGroups[pageIndex].userId, storyId)
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// StoryGroupContent — the full-screen story for one user
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StoryGroupContent(
    group: StoryGroup,
    isActiveGroup: Boolean,
    onGroupFinished: () -> Unit,
    onPreviousGroup: () -> Unit,
    onDismiss: () -> Unit,
    onReplySend: (text: String, storyImageUrl: String) -> Unit,
    onStoryViewed: (storyId: String) -> Unit = {}
) {
    var currentStoryIndex by remember { mutableIntStateOf(0) }
    var isPaused          by remember { mutableStateOf(false) }
    var replyText         by remember { mutableStateOf("") }
    var isLiked           by remember { mutableStateOf(false) }
    val scope             = rememberCoroutineScope()

    val progressList = remember(group.userId) {
        List(group.stories.size) { Animatable(0f) }
    }

    val currentStory = group.stories.getOrNull(currentStoryIndex) ?: run { onGroupFinished(); return }

    // Record view when this story becomes active
    LaunchedEffect(currentStoryIndex, isActiveGroup) {
        if (isActiveGroup) onStoryViewed(currentStory.id)
    }

    // Auto-advance timer
    LaunchedEffect(currentStoryIndex, isPaused, isActiveGroup) {
        if (!isPaused && isActiveGroup) {
            val progress  = progressList[currentStoryIndex]
            val remaining = ((1f - progress.value) * currentStory.duration).roundToInt()
            progress.animateTo(1f, tween(remaining, easing = LinearEasing))
            if (currentStoryIndex < group.stories.size - 1) currentStoryIndex++
            else onGroupFinished()
        }
    }

    fun goToStory(target: Int) = scope.launch {
        if (target > currentStoryIndex) progressList[currentStoryIndex].snapTo(1f)
        else { progressList[currentStoryIndex].snapTo(0f); progressList[target].snapTo(0f) }
        currentStoryIndex = target
    }

    // ── Top and bottom gradient scrims (same as Instagram) ───────────────────
    val topScrim = remember {
        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.60f), Color.Transparent))
    }
    val bottomScrim = remember {
        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f)))
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ── Full-screen story image ───────────────────────────────────────────
        AsyncImage(
            model              = currentStory.imageUrl,
            contentDescription = null,
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.Crop
        )

        // ── Bottom scrim ──────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter)
                .background(bottomScrim)
        )

        // ── Top scrim ─────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .align(Alignment.TopCenter)
                .background(topScrim)
        )

        // ── Tap zones: left 1/3 = back, right 2/3 = forward ──────────────────
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .pointerInput(currentStoryIndex) {
                        detectTapGestures(
                            onTap   = { if (currentStoryIndex > 0) goToStory(currentStoryIndex - 1) else onPreviousGroup() },
                            onPress = { isPaused = true; tryAwaitRelease(); isPaused = false }
                        )
                    }
            )
            Box(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
                    .pointerInput(currentStoryIndex) {
                        detectTapGestures(
                            onTap   = { if (currentStoryIndex < group.stories.size - 1) goToStory(currentStoryIndex + 1) else onGroupFinished() },
                            onPress = { isPaused = true; tryAwaitRelease(); isPaused = false }
                        )
                    }
            )
        }

        // ── TOP: progress bars + header ───────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 8.dp)
                .padding(top = 10.dp)
        ) {
            // Progress bars
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                progressList.forEachIndexed { index, anim ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.38f))
                    ) {
                        val fill = when {
                            index < currentStoryIndex  -> 1f
                            index == currentStoryIndex -> anim.value
                            else                       -> 0f
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(fill)
                                .background(Color.White)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Header row: avatar · name · time · mute · ⋮ · ✕
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                AsyncImage(
                    model              = group.userAvatar,
                    contentDescription = null,
                    modifier           = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentScale       = ContentScale.Crop
                )
                Spacer(Modifier.width(9.dp))
                // Name + time
                Column {
                    Text(
                        group.userName,
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        letterSpacing = 0.1.sp
                    )
                    Text(
                        group.timeAgo,
                        color    = Color.White.copy(alpha = 0.65f),
                        fontSize = 11.5.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                // Mute icon (paused indicator)
                if (isPaused) {
                    Icon(
                        Icons.Default.PauseCircle, null,
                        tint     = Color.White.copy(0.85f),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                // Three-dot menu
                IconButton(
                    onClick  = { /* TODO: story options */ },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert, null,
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                // Close
                IconButton(
                    onClick  = onDismiss,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Close, null,
                        tint     = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ── BOTTOM: send message + ❤️ 💬 ✈️ ─────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp)
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // "Send message" pill input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.55f), RoundedCornerShape(23.dp))
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value         = replyText,
                        onValueChange = { replyText = it },
                        modifier      = Modifier.fillMaxWidth(),
                        textStyle     = TextStyle(
                            color    = Color.White,
                            fontSize = 15.sp
                        ),
                        cursorBrush   = SolidColor(Color.White),
                        singleLine    = true,
                        decorationBox = { inner ->
                            if (replyText.isEmpty()) {
                                Text(
                                    "Send message",
                                    color    = Color.White.copy(alpha = 0.55f),
                                    fontSize = 15.sp
                                )
                            }
                            inner()
                        }
                    )
                }

                // ❤️ Like story
                val heartScale by animateFloatAsState(
                    targetValue   = if (isLiked) 1.35f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy),
                    label         = "heart"
                )
                Icon(
                    imageVector        = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint               = if (isLiked) Color(0xFFED4956) else Color.White,
                    modifier           = Modifier
                        .size(28.dp)
                        .scale(heartScale)
                        .clickable {
                            isLiked = !isLiked
                            if (isLiked) onReplySend("❤️", currentStory.imageUrl)
                        }
                )

                // 💬 Comment / DM
                Icon(
                    imageVector        = Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comment",
                    tint               = Color.White,
                    modifier           = Modifier
                        .size(26.dp)
                        .clickable {
                            if (replyText.isNotBlank()) {
                                onReplySend(replyText, currentStory.imageUrl)
                                replyText = ""
                            }
                        }
                )

                // ✈️ Forward / Share
                Icon(
                    imageVector        = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Share",
                    tint               = Color.White,
                    modifier           = Modifier
                        .size(26.dp)
                        .clickable {
                            if (replyText.isNotBlank()) {
                                onReplySend(replyText, currentStory.imageUrl)
                                replyText = ""
                            }
                        }
                )
            }
        }
    }
}
