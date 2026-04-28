package com.example.zell

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// ─── Data ─────────────────────────────────────────────────────────────────────

data class Story(
    val id: String,
    val imageUrl: String,
    val duration: Long = 5000L,
)

data class StoryGroup(
    val userId: String,
    val userName: String,
    val userAvatar: String,
    val timeAgo: String = "now",
    val stories: List<Story>,
)

// ─── Story Viewer ─────────────────────────────────────────────────────────────

@Composable
fun StoryViewer(
    storyGroups: List<StoryGroup>,
    initialGroupIndex: Int = 0,
    onDismiss: () -> Unit,
) {
    if (storyGroups.isEmpty()) {
        onDismiss()
        return
    }
    
    val pagerState = rememberPagerState(initialPage = initialGroupIndex) { storyGroups.size }
    val coroutineScope = rememberCoroutineScope()

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize().background(Color.Black),
        beyondViewportPageCount = 1
    ) { pageIndex ->
        val group = storyGroups[pageIndex]
        StoryGroupContent(
            group = group,
            onGroupFinished = {
                if (pageIndex < storyGroups.size - 1) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pageIndex + 1)
                    }
                } else {
                    onDismiss()
                }
            },
            onPreviousGroup = {
                if (pageIndex > 0) {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pageIndex - 1)
                    }
                }
            },
            onDismiss = onDismiss
        )
    }
}

@Composable
fun StoryGroupContent(
    group: StoryGroup,
    onGroupFinished: () -> Unit,
    onPreviousGroup: () -> Unit,
    onDismiss: () -> Unit
) {
    var currentStoryIndex by remember { mutableIntStateOf(0) }
    var isPaused by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var showQuickReactions by remember { mutableStateOf(false) }
    var activeReactionEmoji by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()

    // Swipe down to dismiss logic
    val offsetY = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }

    val currentStory = group.stories.getOrNull(currentStoryIndex) ?: run {
        onGroupFinished(); return
    }

    val progressList = remember(group.userId) {
        List(group.stories.size) { Animatable(0f) }
    }

    // Auto-advance logic
    LaunchedEffect(currentStoryIndex, isPaused, group.userId) {
        val progress = progressList[currentStoryIndex]
        if (!isPaused) {
            val remaining = (1f - progress.value) * currentStory.duration
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = remaining.roundToInt(),
                    easing = LinearEasing,
                )
            )

            if (currentStoryIndex < group.stories.size - 1) {
                currentStoryIndex++
            } else {
                onGroupFinished()
            }
        }
    }

    // Fill previous stories instantly
    LaunchedEffect(currentStoryIndex) {
        progressList.forEachIndexed { i, anim ->
            when {
                i < currentStoryIndex -> anim.snapTo(1f)
                i > currentStoryIndex -> anim.snapTo(0f)
                else -> { /* current */ }
            }
        }
    }

    fun goNext() {
        coroutineScope.launch {
            progressList[currentStoryIndex].snapTo(1f)
            if (currentStoryIndex < group.stories.size - 1) {
                currentStoryIndex++
            } else {
                onGroupFinished()
            }
        }
    }

    fun goPrev() {
        coroutineScope.launch {
            progressList[currentStoryIndex].snapTo(0f)
            if (currentStoryIndex > 0) {
                progressList[currentStoryIndex - 1].snapTo(0f)
                currentStoryIndex--
            } else {
                onPreviousGroup()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, offsetY.value.roundToInt()) }
            .graphicsLayer { this.alpha = alpha.value }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        if (dragAmount.y > 0 || offsetY.value > 0) {
                            coroutineScope.launch {
                                offsetY.snapTo(offsetY.value + dragAmount.y)
                                alpha.snapTo((1f - (offsetY.value / 1200f)).coerceIn(0.2f, 1f))
                            }
                        }
                    },
                    onDragEnd = {
                        if (offsetY.value > 400f) {
                            onDismiss()
                        } else {
                            coroutineScope.launch {
                                launch { offsetY.animateTo(0f, spring()) }
                                launch { alpha.animateTo(1f, spring()) }
                            }
                        }
                    }
                )
            }
    ) {
        // Story image
        AnimatedContent(
            targetState = currentStory.imageUrl,
            transitionSpec = { fadeIn(tween(500)) togetherWith fadeOut(tween(500)) },
            label = "story_image",
            modifier = Modifier.fillMaxSize()
        ) { imageUrl ->
            AsyncImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }

        // Tap Navigation Zones
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                detectTapGestures(onTap = { goPrev() }, onPress = { isPaused = true; tryAwaitRelease(); isPaused = false })
            })
            Box(modifier = Modifier.weight(2f).fillMaxHeight().pointerInput(Unit) {
                detectTapGestures(onTap = { goNext() }, onPress = { isPaused = true; tryAwaitRelease(); isPaused = false })
            })
        }

        // --- TOP UI (Progress Bars & User Info) ---
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).statusBarsPadding().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                progressList.forEachIndexed { _, anim -> StoryProgressBar(progress = anim.value, modifier = Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = group.userAvatar, contentDescription = null, modifier = Modifier.size(42.dp).clip(CircleShape).border(1.5.dp, Color.White, CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(group.userName, fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color.White)
                    Text(group.timeAgo, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
        }

        // --- BOTTOM UI (Reply & Reactions) ---
        Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 16.dp)) {
            
            // Quick Reactions Overlay
            AnimatedVisibility(
                visible = showQuickReactions,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                QuickReactionTray(onEmojiSelected = { emoji ->
                    activeReactionEmoji = emoji
                    showQuickReactions = false
                    coroutineScope.launch {
                        delay(2000)
                        activeReactionEmoji = null
                    }
                })
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(32.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(32.dp))
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .clickable { showQuickReactions = !showQuickReactions }
                ) {
                    BasicTextField(
                        value = replyText,
                        onValueChange = { replyText = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                        cursorBrush = SolidColor(Color.White),
                        decorationBox = { inner ->
                            if (replyText.isEmpty()) Text("Reply to ${group.userName.split(" ").first()}...", color = Color.White.copy(alpha = 0.6f), fontSize = 16.sp)
                            inner()
                        }
                    )
                }
                IconButton(onClick = { 
                    activeReactionEmoji = "❤️"
                    coroutineScope.launch { delay(2000); activeReactionEmoji = null }
                }) { 
                    Icon(
                        if (activeReactionEmoji == "❤️") Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, 
                        null, 
                        tint = if (activeReactionEmoji == "❤️") Color.Red else Color.White, 
                        modifier = Modifier.size(28.dp) 
                    ) 
                }
                IconButton(onClick = {}) { Icon(Icons.Outlined.Share, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            }
        }

        // --- REACTION BURST ANIMATION ---
        activeReactionEmoji?.let { emoji ->
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                StoryReactionBurst(emoji)
            }
        }
    }
}

@Composable
fun QuickReactionTray(onEmojiSelected: (String) -> Unit) {
    val emojis = listOf("😂", "😮", "❤️", "😢", "👏", "🔥", "🙌", "😡")
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = Color.Black.copy(alpha = 0.8f),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            emojis.forEach { emoji ->
                Text(
                    text = emoji,
                    fontSize = 32.sp,
                    modifier = Modifier.clickable { onEmojiSelected(emoji) }
                )
            }
        }
    }
}

@Composable
fun StoryReactionBurst(emoji: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "burst")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "alpha"
    )

    Text(
        text = emoji,
        fontSize = 60.sp,
        modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
    )
}

@Composable
fun StoryProgressBar(progress: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier.height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(Color.White))
    }
}
