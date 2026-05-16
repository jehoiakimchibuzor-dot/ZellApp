package com.example.zell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/**
 * PostDetailScreen - Detailed view of a single post and its discussion
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    onBack: () -> Unit,
    postViewModel: PostDetailViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val postFromHome = homeViewModel.feedItems.find { it.id == postId }

    LaunchedEffect(postId) {
        if (postFromHome == null) {
            // Only load fresh if not in home feed
            postViewModel.loadPostDetails(postId)
        }
    }

    val post by postViewModel.post
    val displayPost = postFromHome ?: post
    val comments = postViewModel.comments
    val isLoading by postViewModel.isLoading
    val isSubmitting by postViewModel.isSubmitting
    val currentUser by userViewModel.currentUserProfile
    val error by postViewModel.error
    
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var replyText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Post", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp, 
                modifier = Modifier.imePadding(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { /* Media Picker */ }) {
                        Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                if (replyText.isEmpty()) Text("Post your reply", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                inner()
                            }
                        )
                    }
                    IconButton(onClick = {
                        if (replyText.isNotBlank()) {
                            postViewModel.addComment(postId, replyText, currentUser)
                            replyText = ""
                            focusManager.clearFocus()
                        }
                    }) {
                        if (isSubmitting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.background)) {
            
            if (error != null) {
                ErrorBanner(error = error, onDismiss = { postViewModel.clearError() })
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isLoading && displayPost == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (displayPost != null) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            PostMainContentDetail(
                                post = displayPost,
                                onRepostClick = { /* logic */ },
                                onBookmarkClick = { /* logic */ },
                                onReactionClick = { /* logic */ }
                            )
                        }
                        
                        item {
                            Text(
                                "Comments", 
                                modifier = Modifier.padding(16.dp), 
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }

                        itemsIndexed(comments) { _, comment ->
                            CommentItemDetail(comment)
                        }
                    }
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
    var showReactions by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = post.avatarUrl, 
                contentDescription = null, 
                modifier = Modifier.size(52.dp).clip(CircleShape), 
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(post.author, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                Text(post.handle, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(post.content, fontSize = 18.sp, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onBackground)
        
        post.imageUrl?.let { url ->
            Spacer(modifier = Modifier.height(12.dp))
            AsyncImage(
                model = url,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

        // ── Action buttons WITH counts ────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Like
            DetailActionBtn(
                icon    = if (post.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label   = if (post.likes > 0) formatDetailCount(post.likes) else "Like",
                tint    = if (post.isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                onClick = {}
            )
            // Comment
            DetailActionBtn(
                icon    = Icons.Outlined.ChatBubbleOutline,
                label   = if (post.comments > 0) formatDetailCount(post.comments) else "Comment",
                tint    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                onClick = {}
            )
            // Share/Repost
            DetailActionBtn(
                icon    = Icons.Default.Repeat,
                label   = if (post.reposts > 0) formatDetailCount(post.reposts) else "Share",
                tint    = if (post.isReposted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                onClick = onRepostClick
            )
            // Bookmark
            DetailActionBtn(
                icon    = if (post.isBookmarked) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                label   = "Save",
                tint    = if (post.isBookmarked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                onClick = onBookmarkClick
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    }

    if (showReactions) {
        Popup(onDismissRequest = { showReactions = false }) {
            Surface(
                tonalElevation = 8.dp, 
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            ) {
                Row(modifier = Modifier.padding(8.dp)) {
                    listOf("❤️", "🔥", "😂", "😮", "😢").forEach { emoji ->
                        Text(
                            text = emoji, 
                            fontSize = 24.sp, 
                            modifier = Modifier
                                .clickable { onReactionClick(emoji); showReactions = false }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatDetailCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0).trimEnd('0').trimEnd('.')
    n >= 1_000     -> "%.1fK".format(n / 1_000.0).trimEnd('0').trimEnd('.')
    else           -> "$n"
}

@Composable
private fun DetailActionBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    onClick: () -> Unit
) {
    TextButton(onClick = onClick) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = tint, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun CommentItemDetail(comment: PostComment) {
    Row(modifier = Modifier.padding(16.dp)) {
        AsyncImage(
            model = comment.avatarUrl, 
            contentDescription = null, 
            modifier = Modifier.size(36.dp).clip(CircleShape), 
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(comment.author, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(comment.content, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = if (comment.timestamp == null) "Just now" else "Active", 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), 
                fontSize = 12.sp
            )
        }
    }
}
