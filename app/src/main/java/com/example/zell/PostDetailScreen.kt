package com.example.zell

// =====================================================================================
// 🔥 UPDATE ALERT: 'FACEBOOK REACTION VIBES' HAVE REPLACED 'LIKES' IN THIS FILE!
// Scroll down to see the floating emoji trays and Reaction Burst animations!
// =====================================================================================

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

data class PostComment(
    val id: String,
    val author: String,
    val handle: String,
    val avatarUrl: String,
    val content: String,
    val timeAgo: String,
    var likes: Int,
    val imageUrl: String? = null,
    var isLiked: Boolean = false,
    val replies: List<PostComment> = emptyList(),
    var reaction: String? = null
)

// Global store for comments to persist across screens
val globalPostComments = mutableStateMapOf<String, MutableList<PostComment>>()

@Composable
fun ZellOdometerTextDetail(
    value: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = LocalTextStyle.current
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            if (targetState > initialState) {
                (slideInVertically { height -> height } + fadeIn()).togetherWith(slideOutVertically { height -> -height } + fadeOut())
            } else {
                (slideInVertically { height -> -height } + fadeIn()).togetherWith(slideOutVertically { height -> height } + fadeOut())
            }.using(SizeTransform(clip = false))
        },
        label = "odometer"
    ) { targetValue ->
        Text(text = targetValue.toString(), modifier = modifier, color = color, style = style)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    onLikeClick: () -> Unit = {},
    onRepostClick: () -> Unit = {},
    onBookmarkClick: () -> Unit = {},
) {
    // Shared mockFeed is the source of truth
    val postIndex = mockFeed.indexOfFirst { it.id == postId }
    if (postIndex == -1) {
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val post = mockFeed[postIndex]
    
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    var replyText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showCheckmark by remember { mutableStateOf(false) }

    val postComments = remember(postId) {
        globalPostComments.getOrPut(postId) {
            mutableStateListOf(
                PostComment("c1", "Victor I.", "@victor_builds", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200", "This is absolutely fire! Zell is looking good. 🚀", "2m", 12),
                PostComment("c2", "Sarah O.", "@sarah_design", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200", "Obsessed with this layout! ✨", "5m", 8)
            )
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.navigationBarsPadding().imePadding()
            ) {
                Column(modifier = Modifier.animateContentSize()) {
                    if (selectedImageUri != null) {
                        Box(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp).size(100.dp)) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { selectedImageUri = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 8.dp, y = (-8).dp)
                                    .size(24.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Outlined.SentimentSatisfiedAlt, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = replyText,
                                onValueChange = { replyText = it },
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                decorationBox = { inner ->
                                    if (replyText.isEmpty()) {
                                        Text("Post your reply", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), fontSize = 16.sp)
                                    }
                                    inner()
                                }
                            )
                        }

                        val isEnabled = replyText.isNotBlank() || selectedImageUri != null
                        val pulseScale by animateFloatAsState(
                            targetValue = if (isEnabled && !isSubmitting) 1.1f else 1f,
                            animationSpec = if (isEnabled && !isSubmitting) infiniteRepeatable(tween(800, easing = FastOutSlowInEasing), RepeatMode.Reverse) else snap(),
                            label = "pulse"
                        )

                        IconButton(
                            onClick = {
                                if (!isSubmitting) {
                                    scope.launch {
                                        isSubmitting = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        delay(1200)
                                        isSubmitting = false
                                        showCheckmark = true
                                        delay(800)
                                        
                                        // Update feed count
                                        val currentPost = mockFeed[postIndex]
                                        mockFeed[postIndex] = currentPost.copy(comments = currentPost.comments + 1)

                                        postComments.add(0, PostComment(
                                            id = UUID.randomUUID().toString(),
                                            author = "You",
                                            handle = "@you",
                                            avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                                            content = replyText,
                                            timeAgo = "now",
                                            likes = 0,
                                            imageUrl = selectedImageUri?.toString()
                                        ))
                                        replyText = ""
                                        selectedImageUri = null
                                        showCheckmark = false
                                        focusManager.clearFocus()
                                    }
                                }
                            },
                            enabled = isEnabled,
                            modifier = Modifier.padding(start = 4.dp).scale(if (isEnabled && !isSubmitting) pulseScale else 1f)
                        ) {
                            Crossfade(targetState = if (isSubmitting) 1 else if (showCheckmark) 2 else 0, label = "submit") { state ->
                                when(state) {
                                    1 -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = MaterialTheme.colorScheme.primary)
                                    2 -> Icon(Icons.Default.Check, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(28.dp))
                                    else -> Icon(Icons.AutoMirrored.Filled.Send, null, tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(28.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                PostMainContentDetail(
                    post = post, 
                    onRepostClick = onRepostClick, 
                    onBookmarkClick = onBookmarkClick,
                    onReactionClick = { emoji ->
                        val currentPost = mockFeed[postIndex]
                        mockFeed[postIndex] = currentPost.copy(userReaction = emoji, reactionsCount = currentPost.reactionsCount + 1)
                    }
                )
            }

            item {
                Text(
                    "Comments",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            itemsIndexed(postComments, key = { _, c -> c.id }) { index, comment ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(index * 100L); visible = true }
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { it / 2 }
                ) {
                    CommentItemDetail(
                        comment = comment,
                        onReply = { 
                            replyText = "@${comment.handle.removePrefix("@")} "
                            focusRequester.requestFocus()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostMainContentDetail(
    post: FeedPost, 
    onRepostClick: () -> Unit, 
    onBookmarkClick: () -> Unit,
    onReactionClick: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    var showReactions by remember { mutableStateOf(false) }
    var activeReaction by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {},
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        showReactions = true
                    }
                )
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = post.avatarUrl, contentDescription = null, modifier = Modifier.size(52.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(post.author, fontWeight = FontWeight.Black, fontSize = 17.sp)
                    Text(post.handle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) }
            }
            Spacer(Modifier.height(16.dp))
            Text(text = post.content, fontSize = 19.sp, lineHeight = 28.sp, color = MaterialTheme.colorScheme.onBackground)
            if (post.imageUrl != null) {
                Spacer(Modifier.height(16.dp))
                AsyncImage(model = post.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp).clip(RoundedCornerShape(20.dp)), contentScale = ContentScale.Crop)
            }
            
            if (post.userReaction != null) {
                Spacer(Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    shape = CircleShape
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(post.userReaction!!, fontSize = 16.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("Vibed", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "8:42 PM · Oct 24, 2024 · ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Outlined.Visibility, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("${post.views} Views", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                InteractionButtonDetail(Icons.Outlined.ChatBubbleOutline, post.comments.toString())
                
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(CircleShape).clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showReactions = true }.padding(8.dp)) {
                    if (post.userReaction != null) {
                        Text(post.userReaction!!, fontSize = 24.sp)
                    } else {
                        Icon(Icons.Outlined.AddReaction, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (post.reactionsCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        ZellOdometerTextDetail(post.reactionsCount, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
                    }
                }
                
                val repostRot by animateFloatAsState(targetValue = if (post.isReposted) 180f else 0f, animationSpec = spring())
                InteractionButtonDetail(icon = Icons.Default.Repeat, label = post.reposts.toString(), tint = if (post.isReposted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onRepostClick() }, rotation = repostRot)
                val bRot by animateFloatAsState(if(post.isBookmarked) 360f else 0f)
                InteractionButtonDetail(icon = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder, label = "", tint = if (post.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onBookmarkClick() }, rotation = bRot)
                InteractionButtonDetail(Icons.Outlined.Share, "")
            }
            HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        }

        activeReaction?.let { emoji ->
            ReactionBurstDetail(emoji = emoji, onAnimationFinished = { activeReaction = null })
        }
    }

    if (showReactions) {
        Popup(
            alignment = Alignment.TopCenter,
            onDismissRequest = { showReactions = false }
        ) {
            Surface(
                modifier = Modifier.padding(top = 100.dp).shadow(12.dp, RoundedCornerShape(40.dp)),
                shape = RoundedCornerShape(40.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    val emojis = listOf("❤️", "🔥", "😂", "😮", "😢", "🙌", "😡")
                    emojis.forEach { emoji ->
                        Text(
                            text = emoji,
                            modifier = Modifier
                                .padding(horizontal = 10.dp)
                                .clickable {
                                    activeReaction = emoji
                                    onReactionClick(emoji)
                                    showReactions = false
                                },
                            fontSize = 28.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractionButtonDetail(icon: ImageVector, label: String, tint: Color = MaterialTheme.colorScheme.onSurfaceVariant, onClick: () -> Unit = {}, scale: Float = 1f, rotation: Float = 0f) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(CircleShape).clickable { onClick() }.padding(8.dp)) {
        Icon(icon, null, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = scale; scaleY = scale; rotationZ = rotation }, tint = tint)
        if (label.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            ZellOdometerTextDetail(label.toIntOrNull() ?: 0, color = tint, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun CommentItemDetail(comment: PostComment, onReply: () -> Unit = {}) {
    val haptic = LocalHapticFeedback.current
    var isLiked by remember { mutableStateOf(comment.isLiked) }
    var likes by remember { mutableIntStateOf(comment.likes) }
    var reaction by remember { mutableStateOf(comment.reaction) }
    var showReplies by remember { mutableStateOf(false) }
    var showReactionPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).animateContentSize()) {
        Row {
            AsyncImage(model = comment.avatarUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("· ${comment.timeAgo}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Text(comment.handle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                
                val annotatedContent = buildAnnotatedString {
                    val parts = comment.content.split(" ")
                    parts.forEach { part ->
                        if (part.startsWith("@")) {
                            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                                append(part)
                            }
                        } else {
                            append(part)
                        }
                        append(" ")
                    }
                }
                Text(
                    text = annotatedContent, 
                    fontSize = 15.sp, 
                    lineHeight = 22.sp, 
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showReactionPicker = true
                        })
                    }
                )

                if (comment.imageUrl != null) {
                    Spacer(Modifier.height(12.dp))
                    AsyncImage(model = comment.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                }
                
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val scale by animateFloatAsState(targetValue = if (isLiked) 1.4f else 1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, 
                        contentDescription = null, 
                        modifier = Modifier.size(18.dp).scale(scale).clickable { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isLiked = !isLiked
                            if(isLiked) likes++ else likes--
                        }, 
                        tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    ZellOdometerTextDetail(likes, style = MaterialTheme.typography.labelSmall)
                    
                    if (reaction != null) {
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.clickable { reaction = null }
                        ) {
                            Text(reaction!!, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontSize = 12.sp)
                        }
                    }

                    Spacer(Modifier.width(24.dp))
                    Text(
                        "Reply", 
                        fontSize = 13.sp, 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold, 
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onReply() }.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                if (comment.replies.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (showReplies) "Hide replies" else "View ${comment.replies.size} replies",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showReplies = !showReplies }
                    )
                    
                    if (showReplies) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            comment.replies.forEach { reply ->
                                Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                    AsyncImage(model = reply.avatarUrl, contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(reply.author, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Spacer(Modifier.width(6.dp))
                                            Text("· ${reply.timeAgo}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                                        }
                                        Text(reply.content, fontSize = 14.sp, lineHeight = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showReactionPicker) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(60, (-40).dp.value.toInt()),
                onDismissRequest = { showReactionPicker = false }
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("👍", "❤️", "😂", "😮", "😢", "😡").forEach { emoji ->
                            Text(
                                text = emoji, 
                                fontSize = 22.sp, 
                                modifier = Modifier
                                    .clickable { reaction = emoji; showReactionPicker = false }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    }
}

@Composable
fun ReactionBurstDetail(emoji: String, onAnimationFinished: () -> Unit) {
    val transitionState = remember { MutableTransitionState(false) }.apply { targetState = true }
    val transition = rememberTransition(transitionState, label = "burst")
    
    val alpha by transition.animateFloat(
        transitionSpec = { tween(800) }, label = "alpha"
    ) { if (it) 0f else 1f }
    
    val scale by transition.animateFloat(
        transitionSpec = { spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow) },
        label = "scale"
    ) { if (it) 3f else 1f }

    val offsetY by transition.animateFloat(
        transitionSpec = { tween(800, easing = FastOutSlowInEasing) },
        label = "offset"
    ) { if (it) -200f else 0f }

    if (transitionState.currentState && transitionState.isIdle) {
        LaunchedEffect(Unit) { onAnimationFinished() }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = emoji,
            fontSize = 40.sp,
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    alpha = alpha,
                    translationY = offsetY
                )
        )
    }
}
