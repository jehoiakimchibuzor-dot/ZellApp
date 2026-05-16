package com.example.zell

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged

// ─────────────────────────────────────────────────────────────────────────────
// HomeTab  — Instagram-style home feed
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTab(
    onPostClick: (String) -> Unit,
    onProfileClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onStoryClick: (Int) -> Unit,
    onAddStoryClick: () -> Unit,
    onComposeClick: () -> Unit,
    currentUser: StoryUser,
    homeViewModel: HomeViewModel = viewModel()
) {
    val feedPosts     = homeViewModel.feedItems
    val isLoading     = homeViewModel.isLoading
    val isLoadingMore = homeViewModel.isLoadingMore
    val isRefreshing  = homeViewModel.isRefreshing
    val error         by homeViewModel.error
    val stories       = mockStories.filter { it.stories.isNotEmpty() }
    val listState     = rememberLazyListState()

    // ── Infinite scroll ───────────────────────────────────────────────────────
    LaunchedEffect(listState) {
        snapshotFlow {
            val info        = listState.layoutInfo
            val total       = info.totalItemsCount
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            total > 0 && lastVisible >= total - 4 && !homeViewModel.isLoadingMore
        }
            .distinctUntilChanged()
            .collect { nearEnd -> if (nearEnd) homeViewModel.loadMorePosts() }
    }

    // ── Error banner ──────────────────────────────────────────────────────────
    if (error != null) {
        ErrorBanner(error = error, onDismiss = { homeViewModel.clearError() })
    }

    // ── Full-screen initial loader ────────────────────────────────────────────
    if (isLoading && feedPosts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // ── Pull-to-refresh wrapping the whole list ───────────────────────────────
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh    = { homeViewModel.refreshFeed() },
        modifier     = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state          = listState,
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {

            // ── Stories row ───────────────────────────────────────────────────
            item(key = "stories") {
                IgStoriesRow(
                    stories         = stories,
                    currentUser     = currentUser,
                    onStoryClick    = onStoryClick,
                    onAddStoryClick = onAddStoryClick
                )
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.10f),
                    thickness = 0.5.dp
                )
            }

            // ── Feed posts ────────────────────────────────────────────────────
            items(feedPosts, key = { it.id }) { post ->
                IgPostCard(
                    post           = post,
                    onPostClick    = { onPostClick(post.id) },
                    onProfileClick = { onProfileClick(post.authorId) },
                    onCommentClick = { onCommentClick(post.id) },
                    onLikeClick    = { homeViewModel.toggleLike(post.id) }
                )
                HorizontalDivider(
                    color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f),
                    thickness = 0.5.dp
                )
            }

            // ── Load-more spinner ─────────────────────────────────────────────
            if (isLoadingMore) {
                item(key = "loading_more") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // ── End-of-feed ───────────────────────────────────────────────────
            if (!isLoadingMore && feedPosts.isNotEmpty() && homeViewModel.isLastPage) {
                item(key = "end") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "You're all caught up 🎉",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Stories Row  — exact Instagram style
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IgStoriesRow(
    stories: List<StoryUser>,
    currentUser: StoryUser,
    onStoryClick: (Int) -> Unit,
    onAddStoryClick: () -> Unit
) {
    LazyRow(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        contentPadding        = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // "Your story" + button
        item {
            IgAddStoryBubble(avatarUrl = currentUser.avatarUrl, onClick = onAddStoryClick)
        }
        // Other users' stories
        items(stories) { user ->
            IgStoryBubble(user = user, onClick = { onStoryClick(stories.indexOf(user)) })
        }
    }
}

@Composable
private fun IgAddStoryBubble(avatarUrl: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.width(68.dp).clickable { onClick() }
    ) {
        Box(modifier = Modifier.size(60.dp)) {
            AsyncImage(
                model              = avatarUrl,
                contentDescription = null,
                modifier           = Modifier.fillMaxSize().clip(CircleShape),
                contentScale       = ContentScale.Crop
            )
            // Blue + badge
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .align(Alignment.BottomEnd),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(12.dp))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Your story",
            fontSize  = 11.sp,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            color     = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
private fun IgStoryBubble(user: StoryUser, onClick: () -> Unit) {
    // Instagram gradient ring
    val gradientBrush = androidx.compose.ui.graphics.Brush.sweepGradient(
        listOf(Color(0xFFFCAF45), Color(0xFFFC6831), Color(0xFFBC1888), Color(0xFFFCAF45))
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.width(68.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(gradientBrush, CircleShape)
                .padding(2.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background, CircleShape)
                    .padding(2.dp)
            ) {
                AsyncImage(
                    model              = user.avatarUrl,
                    contentDescription = user.name,
                    modifier           = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale       = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            user.name.split(" ").first(),
            fontSize  = 11.sp,
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            color     = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Instagram Post Card
// Layout (top → bottom):
//   1. Header      — avatar · username · timestamp · ···
//   2. Image       — full-width, double-tap to like
//   3. Caption     — if text exists (above image on our data model)
//   4. Action bar  — ❤ 💬 ✈ ···· 🔖
//   5. Likes       — "X likes" bold
//   6. Comments    — "View all X comments"
//   7. Reposts     — "X reposts" muted
//   8. Timestamp   — "2H AGO"
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun IgPostCard(
    post: FeedPost,
    onPostClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCommentClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current

    // ── Like state — local, keyed on post.id ─────────────────────────────────
    // Key = post.id (NOT post.isLiked). This means:
    //   • Tapping the heart updates isLiked INSTANTLY (no async wait)
    //   • When Firestore's real-time listener fires and pushes a new post object,
    //     the key (post.id) hasn't changed, so this state is NEVER reset.
    //   • Only resets if this slot shows a completely different post (different id).
    var isLiked by remember(post.id) { mutableStateOf(post.isLiked) }

    val heartScale by animateFloatAsState(
        targetValue   = if (isLiked) 1.3f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessLow
        ),
        label = "heartScale"
    )
    val heartTint by animateColorAsState(
        targetValue   = if (isLiked) Color(0xFFED4956) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        animationSpec = tween(200),
        label         = "heartTint"
    )

    // Double-tap heart overlay
    var showHeartOverlay by remember { mutableStateOf(false) }
    val overlayAlpha by animateFloatAsState(
        targetValue   = if (showHeartOverlay) 1f else 0f,
        animationSpec = tween(if (showHeartOverlay) 150 else 400),
        label         = "overlayAlpha"
    )
    val overlayScale by animateFloatAsState(
        targetValue   = if (showHeartOverlay) 1f else 0.6f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "overlayScale"
    )
    if (showHeartOverlay) {
        LaunchedEffect(showHeartOverlay) {
            delay(700)
            showHeartOverlay = false
        }
    }

    // Menu
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── 1. Header ─────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .clickable { onProfileClick() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            AsyncImage(
                model              = post.avatarUrl,
                contentDescription = post.author,
                modifier           = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onProfileClick() },
                contentScale       = ContentScale.Crop
            )
            Spacer(Modifier.width(10.dp))
            // Name + timestamp
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    post.author,
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = 14.sp,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                val timeAgo = remember(post.timestamp) {
                    post.timestamp?.let { TimeUtils.timeAgo(it) } ?: ""
                }
                if (timeAgo.isNotEmpty()) {
                    Text(
                        timeAgo,
                        fontSize = 12.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
            // Three-dot menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        Icons.Default.MoreHoriz,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text        = { Text("Save post") },
                        leadingIcon = { Icon(Icons.Default.BookmarkBorder, null) },
                        onClick     = { showMenu = false }
                    )
                    DropdownMenuItem(
                        text        = { Text("Hide post") },
                        leadingIcon = { Icon(Icons.Default.VisibilityOff, null) },
                        onClick     = { showMenu = false }
                    )
                    DropdownMenuItem(
                        text        = { Text("Report", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Default.Flag, null, tint = MaterialTheme.colorScheme.error)
                        },
                        onClick     = { showMenu = false }
                    )
                }
            }
        }

        // ── 2. Caption text (above image, matching our data model) ────────────
        if (post.content.isNotBlank()) {
            Text(
                text     = post.content,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPostClick() }
                    .padding(horizontal = 12.dp)
                    .padding(bottom = if (post.imageUrl != null) 10.dp else 4.dp),
                style    = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                color    = MaterialTheme.colorScheme.onSurface
            )
        }

        // ── 3. Image — full-width with double-tap-to-like ─────────────────────
        post.imageUrl?.let { url ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 480.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap         = { onPostClick() },
                            onDoubleTap   = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                if (!isLiked) {
                                    isLiked = true
                                    onLikeClick()
                                }
                                showHeartOverlay = true
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model              = url,
                    contentDescription = null,
                    modifier           = Modifier.fillMaxWidth(),
                    contentScale       = ContentScale.Crop
                )
                // Animated heart overlay on double-tap
                if (overlayAlpha > 0f) {
                    Icon(
                        imageVector        = Icons.Default.Favorite,
                        contentDescription = null,
                        tint               = Color.White.copy(alpha = overlayAlpha),
                        modifier           = Modifier
                            .size(90.dp)
                            .scale(overlayScale)
                    )
                }
            }
        }

        // ── 4. Action bar — icon + count inline, exactly like the screenshot ────
        //   ❤️ 31.1K   💬 46   🔄 3,055   ✈️ 7,913          🔖
        // Local isLiked drives the count too — instant feedback
        val displayLikes = when {
            isLiked && !post.isLiked -> post.likes + 1
            !isLiked && post.isLiked -> maxOf(0, post.likes - 1)
            else                     -> post.likes
        }

        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ❤️ Like + count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .scale(heartScale)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        isLiked = !isLiked   // instant local flip — heart turns red NOW
                        onLikeClick()        // ViewModel syncs to Firestore in background
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Like",
                    tint     = heartTint,
                    modifier = Modifier.size(24.dp)
                )
                if (displayLikes > 0) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        formatSocialCount(displayLikes),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = heartTint
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // 💬 Comment + count
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable { onCommentClick() }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = "Comment",
                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(23.dp)
                )
                if (post.comments > 0) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        formatSocialCount(post.comments),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            // 🔄 Repost + count
            if (post.reposts > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Outlined.Repeat,
                        contentDescription = "Reposts",
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        modifier = Modifier.size(23.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        formatSocialCount(post.reposts),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
                Spacer(Modifier.width(14.dp))
            }

            // ✈️ Share + views
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clickable {
                        val shareText = "${post.author}: ${post.content}\n\nShared via Zell 📱"
                        context.startActivity(
                            Intent.createChooser(
                                Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                    type   = "text/plain"
                                },
                                "Share via"
                            )
                        )
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Send,
                    contentDescription = "Share",
                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    modifier = Modifier.size(23.dp)
                )
                val viewCount = post.views.toIntOrNull() ?: 0
                if (viewCount > 0) {
                    Spacer(Modifier.width(5.dp))
                    Text(
                        formatSocialCount(viewCount),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                    )
                }
            }

            Spacer(Modifier.weight(1f))   // pushes bookmark to far right

            // 🔖 Bookmark (no count, right-aligned)
            Icon(
                if (post.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Save",
                tint     = if (post.isBookmarked)
                               MaterialTheme.colorScheme.onSurface
                           else
                               MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier
                    .size(24.dp)
                    .padding(end = 4.dp)
            )
        }

        // ── 5. Caption — bold author name + text ─────────────────────────────
        if (post.content.isNotBlank()) {
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append(post.author.split(" ").first())
                    }
                    append("  ")
                    append(post.content)
                },
                modifier   = Modifier
                    .clickable { onPostClick() }
                    .padding(horizontal = 12.dp),
                fontSize   = 14.sp,
                maxLines   = 3,
                overflow   = TextOverflow.Ellipsis,
                color      = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(3.dp))
        }

        // ── 6. View all comments link ─────────────────────────────────────────
        if (post.comments > 0) {
            Text(
                text     = "View all ${formatSocialCount(post.comments)} comments",
                modifier = Modifier
                    .clickable { onCommentClick() }
                    .padding(horizontal = 12.dp, vertical = 1.dp),
                fontSize = 14.sp,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
            )
            Spacer(Modifier.height(2.dp))
        }

        // ── 7. Timestamp — uppercase tiny text ───────────────────────────────
        val timeAgo = remember(post.timestamp) {
            post.timestamp?.let { TimeUtils.timeAgo(it) } ?: ""
        }
        if (timeAgo.isNotEmpty()) {
            Text(
                text          = timeAgo.uppercase(),
                modifier      = Modifier
                    .padding(horizontal = 12.dp)
                    .padding(bottom = 10.dp, top = 2.dp),
                fontSize      = 10.sp,
                fontWeight    = FontWeight.Medium,
                letterSpacing = 0.5.sp,
                color         = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
            )
        } else {
            Spacer(Modifier.height(10.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Legacy helpers kept so other files that import from HomeTab still compile
// ─────────────────────────────────────────────────────────────────────────────

// Keep the old StoriesRow / StoryCircle / AddStoryButton names in case
// other files reference them directly — they now delegate to the Ig* versions.

@Composable
fun StoriesRow(
    stories: List<StoryUser>,
    currentUser: StoryUser,
    onStoryClick: (Int) -> Unit,
    onAddStoryClick: () -> Unit
) = IgStoriesRow(stories, currentUser, onStoryClick, onAddStoryClick)

@Composable
fun StoryCircle(user: StoryUser, onClick: () -> Unit) = IgStoryBubble(user, onClick)

@Composable
fun AddStoryButton(avatarUrl: String, onClick: () -> Unit) =
    IgAddStoryBubble(avatarUrl, onClick)

@Composable
fun QuickComposeRow(currentUser: StoryUser, onClick: () -> Unit) {
    // Kept for compile-compat. The FAB in MainDashboard handles composing.
    // Render nothing — avoids a double compose bar.
}

@Composable
fun FeedPostItem(
    post: FeedPost,
    onClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onCommentClick: (String) -> Unit,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit
) = IgPostCard(
    post           = post,
    onPostClick    = onClick,
    onProfileClick = { onProfileClick(post.authorId) },
    onCommentClick = { onCommentClick(post.id) },
    onLikeClick    = onLikeClick
)

@Composable
fun InteractionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    color: Color,
    onClick: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClick) { Icon(icon, null, tint = color) }
        Text(count.toString(), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

// ── Count formatter ───────────────────────────────────────────────────────────
private fun formatSocialCount(count: Int): String = when {
    count >= 1_000_000 -> "%.1fM".format(count / 1_000_000.0).trimEnd('0').trimEnd('.')
    count >= 1_000     -> "%.1fK".format(count / 1_000.0).trimEnd('0').trimEnd('.')
    else               -> count.toString()
}
