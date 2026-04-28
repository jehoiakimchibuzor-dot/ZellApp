package com.example.zell

// =====================================================================================
// 🔥 UPDATE ALERT: 'PULSE', 'SPACES RING', 'DISCOVER CINEMATIC SCROLL' & 'FACEBOOK REACTIONS' 
// HAVE BEEN ADDED TO THIS FILE! Scroll down to see the updated PulseScreen, 
// DiscoverScreen, SpaceChatItem, and FeedPost models.
// =====================================================================================

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.compose.foundation.layout.statusBarsPadding

// ─── Data Models ──────────────────────────────────────────────────────────────

data class StoryItem(
    val id: String = UUID.randomUUID().toString(),
    val url: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class StoryUser(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val headerUrl: String = "",
    val stories: List<StoryItem> = emptyList(),
    val hasUnread: Boolean = true,
    val isYou: Boolean = false,
    val bio: String = "Digital Architect. Making Zell — a lifestyle super-app.",
    val location: String = "Lagos, Nigeria",
    val website: String = "zell.app",
    val accentColor: String = "#C5A059",
    val profileLayout: String = "Staggered"
)

data class FeedPost(
    val id: String = "",
    val author: String = "",
    val handle: String = "",
    val avatarUrl: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val timeAgo: String = "",
    var reactionsCount: Int = 0,
    var comments: Int = 0,
    var reposts: Int = 0,
    val tag: String = "",
    var isBookmarked: Boolean = false,
    var isReposted: Boolean = false,
    var skill: String? = null,
    var userReaction: String? = null,
    var views: String = "12.4K",
    var showInlineComments: Boolean = false,
    var isLiked: Boolean = false,
    var likes: Int = 0
)

data class DiscoverItem(
    val id: String = "",
    val imageUrl: String = "",
    val height: Dp = 200.dp,
    val category: String = "",
    val title: String = "",
)

data class Board(
    val id: String = "",
    val name: String = "",
    val coverImageUrl: String = "",
    val itemCount: Int = 0
)

data class SpaceChat(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val lastMessage: String = "",
    val time: String = "",
    val unread: Int = 0,
    val isOnline: Boolean = false,
    val isTyping: Boolean = false,
    val isGroup: Boolean = false,
    val hasStory: Boolean = false,
    val sharedBoardPreview: List<String> = emptyList()
)

// ─── Global State ─────────────────────────────────────────────────────────────

val mockStories = mutableStateListOf(
    StoryUser("u1", "Amara", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200", stories = listOf(StoryItem(url="https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800")), hasUnread = true),
    StoryUser("u2", "Kofi", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200", stories = listOf(StoryItem(url="https://images.unsplash.com/photo-1558591710-4b4a1ae0f04d?w=800")), hasUnread = true),
    StoryUser("u3", "Zara", "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?w=200", stories = listOf(StoryItem(url="https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=800")), hasUnread = true),
    StoryUser("u4", "Marcus", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200", stories = listOf(StoryItem(url="https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800")), hasUnread = false),
    StoryUser("u5", "Lena", "https://images.unsplash.com/photo-1531746020798-e6953c6e8e04?w=200", stories = listOf(StoryItem(url="https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800")), hasUnread = true),
    StoryUser("u6", "David", "https://images.unsplash.com/photo-1519345182560-3f2917c472ef?w=200", stories = listOf(StoryItem(url="https://images.unsplash.com/photo-1558591710-4b4a1ae0f04d?w=800")), hasUnread = false)
)

val mockFeed = mutableStateListOf(
    FeedPost("1", "Amara Osei", "@amara", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
        "Just finished redesigning my entire workspace. Minimalism hits different when you actually commit to it. 🖤",
        "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=800",
        "2m", 1200, 48, 12, "Design", skill = "Digital Architect"),
    FeedPost("2", "Kofi Mensah", "@kofi.dev", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
        "Built an entire API in 3 hours using Kotlin + Ktor. The speed of modern tooling is genuinely wild.",
        null, "18m", 834, 21, 5, "Tech", skill = "Backend Engineer"),
)

val mockDiscover = mutableStateListOf(
    DiscoverItem("d1", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=800", 220.dp, "Design", "Color Theory"),
    DiscoverItem("d2", "https://images.unsplash.com/photo-1558591710-4b4a1ae0f04d?w=800", 180.dp, "Design", "Minimalism"),
)

val mockBoards = listOf(
    Board("1", "Design Inspiration", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200", 12),
)

val mockSpaces = mutableStateListOf<SpaceChat>().apply {
    add(SpaceChat("1", "Sarah Okonkwo", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200", "Haha exactly!", "10:45", unread = 3, isOnline = true, hasStory = true, sharedBoardPreview = listOf("https://images.unsplash.com/photo-1558591710-4b4a1ae0f04d?w=200")))
    add(SpaceChat("2", "Design Core", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200", "Check this vibe check out", "Yesterday", unread = 0, isOnline = false, isGroup = true, hasStory = true, sharedBoardPreview = listOf("https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200", "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=200")))
}

val discoverCategories = listOf("For You", "Design", "Tech", "Travel", "Food", "Art", "Fashion", "Sports")

// ─── Main Dashboard ───────────────────────────────────────────────────────────

@Composable
fun MainDashboard(
    onChatClick: (String) -> Unit,
    onStoryClick: (Int) -> Unit,
    onNotificationClick: () -> Unit,
    onComposeClick: () -> Unit,
    onPostClick: (String) -> Unit,
    onPinClick: (DiscoverItem) -> Unit,
    onSearchClick: () -> Unit,
    onSearchTagClick: (String) -> Unit = {},
    onCreateSpaceClick: () -> Unit,
    onExternalProfileClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    currentUser: StoryUser
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val db = FirebaseFirestore.getInstance()

    LaunchedEffect(Unit) {
        db.collection("users").addSnapshotListener { snapshot, _ ->
            if (snapshot != null && !snapshot.isEmpty) {
                val users = snapshot.toObjects(StoryUser::class.java)
                val processed = users.map { it.copy(isYou = it.id == currentUser.id) }.sortedByDescending { it.isYou }
                // Merge: add any Firebase users not already in the mock list
                processed.forEach { fbUser ->
                    if (mockStories.none { it.id == fbUser.id }) {
                        mockStories.add(fbUser)
                    } else {
                        val idx = mockStories.indexOfFirst { it.id == fbUser.id }
                        if (idx >= 0) mockStories[idx] = fbUser
                    }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.background,
                tonalElevation = 0.dp,
                modifier = Modifier.shadow(16.dp)
            ) {
                data class NavDef(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector)
                val items = listOf(
                    NavDef("Pulse", Icons.Outlined.AutoAwesome, Icons.Default.AutoAwesome),
                    NavDef("Reels", Icons.Outlined.PlayCircle, Icons.Default.PlayCircle),
                    NavDef("Discover", Icons.Outlined.GridView, Icons.Default.GridView),
                    NavDef("Spaces", Icons.Outlined.ChatBubble, Icons.Default.ChatBubble),
                    NavDef("Profile", Icons.Outlined.Person, Icons.Default.Person)
                )
                items.forEachIndexed { index, item ->
                    val isSelected = selectedTab == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTab = index },
                        icon = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(if (isSelected) item.selectedIcon else item.icon, null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp))
                                if (isSelected) {
                                    Spacer(Modifier.height(3.dp))
                                    Box(modifier = Modifier.size(4.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                }
                            }
                        },
                        label = null,
                        alwaysShowLabel = false,
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = Color.Transparent
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) FloatingActionButton(onClick = onComposeClick, containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black, shape = RoundedCornerShape(16.dp)) { Icon(Icons.Default.Add, null) }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> PulseScreen(onPostClick, onStoryClick, onNotificationClick, onExternalProfileClick, onAddStoryClick, onPinClick)
                1 -> FlowScreen()
                2 -> DiscoverScreen(onPinClick, onSearchClick)
                3 -> SpacesScreen(onChatClick, onSearchClick, onCreateSpaceClick)
                4 -> ProfileScreen(onSettingsClick, onEditProfileClick, onPostClick, currentUser)
            }
        }
    }
}

// ─── Screens ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PulseScreen(onPostClick: (String) -> Unit, onStoryClick: (Int) -> Unit, onNotificationClick: () -> Unit, onExternalProfileClick: (String) -> Unit, onAddStoryClick: () -> Unit, onPinClick: (DiscoverItem) -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(isRefreshing = isRefreshing, onRefresh = { scope.launch { isRefreshing = true; delay(1000); isRefreshing = false } }) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            item { PulseTopBar(onNotificationClick) }
            item { StoriesRow(onStoryClick, onAddStoryClick) }
            item { ActiveChatsGlimpse() }
            item { TrendingVisualsRow(onPinClick) }
            items(mockFeed, key = { it.id }) { post ->
                PostItem(post = post, onClick = { onPostClick(post.id) }, onReactionClick = { emoji -> post.userReaction = emoji }, onRepostClick = {}, onBookmarkClick = {}, onProfileClick = { onExternalProfileClick(post.handle.removePrefix("@")) }, onInlineCommentToggle = {})
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
fun PulseTopBar(onNotificationClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text("Pulse", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, letterSpacing = (-1.5).sp)
        IconButton(onClick = onNotificationClick, modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)) { Icon(Icons.Outlined.Notifications, null) }
    }
}

// Instagram-style gradient colors
private val storyGradientColors = listOf(
    Color(0xFFD8257D), // deep pink
    Color(0xFFF5501E), // orange-red
    Color(0xFFFDA50F), // warm yellow
)
private val storyGradientViewed = listOf(
    Color(0xFFBBBBBB),
    Color(0xFF888888),
)

@Composable
fun StoriesRow(onStoryClick: (Int) -> Unit, onAddStoryClick: () -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // "Your Story" always first
        item {
            YourStoryItem(onAddStoryClick)
        }
        // Other users
        itemsIndexed(mockStories.filter { !it.isYou }) { index, user ->
            StoryItem(
                user = user,
                onClick = { onStoryClick(mockStories.indexOf(user)) }
            )
        }
    }
}

@Composable
fun YourStoryItem(onAddStoryClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable(onClick = onAddStoryClick)
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            // Grey background circle
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
            // Blue "+" badge at bottom-right
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF))
                    .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            "Your Story",
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun StoryItem(user: StoryUser, onClick: () -> Unit) {
    val gradientColors = if (user.hasUnread) storyGradientColors else storyGradientViewed
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp).clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = gradientColors,
                            start = androidx.compose.ui.geometry.Offset(0f, size.height),
                            end = androidx.compose.ui.geometry.Offset(size.width, 0f)
                        ),
                        radius = size.minDimension / 2f,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx()
                        )
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = user.name,
                modifier = Modifier
                    .size(63.dp)
                    .clip(CircleShape)
                    .border(2.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = user.name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun TrendingVisualsRow(onPinClick: (DiscoverItem) -> Unit) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text("Trending Creations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(mockDiscover) { item ->
                Box(modifier = Modifier.size(140.dp, 180.dp).clip(RoundedCornerShape(16.dp)).clickable { onPinClick(item) }) {
                    AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f)))))
                    Text(item.title, color = Color.White, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PostItem(post: FeedPost, onClick: () -> Unit, onRepostClick: () -> Unit, onBookmarkClick: () -> Unit, onReactionClick: (String) -> Unit, onProfileClick: () -> Unit, onInlineCommentToggle: () -> Unit) {
    var showReactions by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick, onLongClick = { showReactions = true })) {
        // Author Row
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = post.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(42.dp).clip(CircleShape).clickable(onClick = onProfileClick),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(post.author, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (post.skill != null) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(post.skill!!, fontSize = 10.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("${post.handle} · ${post.timeAgo}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
        }

        // Content
        Text(
            post.content,
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 15.sp,
            lineHeight = 22.sp
        )

        // Image
        if (post.imageUrl != null) {
            Spacer(Modifier.height(10.dp))
            AsyncImage(
                model = post.imageUrl,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .height(260.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Reaction popup
        if (showReactions) {
            Popup(alignment = Alignment.TopStart, onDismissRequest = { showReactions = false }) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)),
                    modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("👍", "❤️", "😂", "😮", "😢", "😡").forEach { emoji ->
                            var pressed by remember { mutableStateOf(false) }
                            val scale by animateFloatAsState(if (pressed) 1.5f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                            Text(
                                text = emoji,
                                fontSize = 24.sp,
                                modifier = Modifier
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .clickable { onReactionClick(emoji); showReactions = false }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Actions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reaction
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(CircleShape).clickable { showReactions = !showReactions }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                if (post.userReaction != null) Text(post.userReaction!!, fontSize = 18.sp)
                else Icon(Icons.Outlined.AddReaction, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(4.dp))
                Text(post.reactionsCount.toString(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
            }
            Spacer(Modifier.width(4.dp))
            // Comment
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(CircleShape).clickable { onInlineCommentToggle() }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Icon(Icons.Outlined.ChatBubbleOutline, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(4.dp))
                Text(post.comments.toString(), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f))
            }
            Spacer(Modifier.width(4.dp))
            // Repost
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(CircleShape).clickable { onRepostClick() }.padding(horizontal = 8.dp, vertical = 6.dp)) {
                Icon(Icons.Default.Repeat, null, tint = if (post.isReposted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.6f), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.weight(1f))
            // Bookmark
            IconButton(onClick = onBookmarkClick) {
                Icon(
                    if (post.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                    null,
                    tint = if (post.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
    }
}

// ─── Profile Screen ───────────────────────────────────────────────────────────

@Composable
fun ProfileScreen(onSettingsClick: () -> Unit, onEditProfileClick: () -> Unit, onPostClick: (String) -> Unit, currentUser: StoryUser) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        ProfileHeader(onSettingsClick, onEditProfileClick, currentUser)
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.primary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = MaterialTheme.colorScheme.primary,
                    height = 2.dp
                )
            },
            divider = { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }
        ) {
            listOf("Posts", "Boards", "Tagged").forEachIndexed { i, title ->
                Tab(
                    selected = selectedTab == i,
                    onClick = { selectedTab = i },
                    text = {
                        Text(
                            title,
                            fontWeight = if (selectedTab == i) FontWeight.Black else FontWeight.Normal,
                            fontSize = 14.sp
                        )
                    }
                )
            }
        }
        when (selectedTab) {
            0 -> ProfilePostsList(onPostClick, currentUser)
            1 -> ProfileCreationsGrid(currentUser.profileLayout)
            2 -> ProfileBoardsList()
        }
    }
}

@Composable
fun ProfileHeader(onSettingsClick: () -> Unit, onEditProfileClick: () -> Unit, currentUser: StoryUser) {
    Column {
        // Actions row at top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "@${currentUser.name.lowercase().replace(" ", "")}",
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onSettingsClick) { Icon(Icons.Outlined.Settings, null) }
            }
        }
        // Avatar + Stats row (Instagram-style)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = currentUser.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(0.3f), CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(24.dp))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStat("24", "Posts")
                ProfileStat("12.4K", "Followers")
                ProfileStat("842", "Following")
            }
        }
        // Name + Bio
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(currentUser.name, fontWeight = FontWeight.Black, fontSize = 16.sp)
            if (currentUser.bio.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(currentUser.bio, fontSize = 14.sp, lineHeight = 20.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            if (currentUser.website.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(currentUser.website, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onEditProfileClick,
                modifier = Modifier.weight(1f).height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Edit profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Button(
                onClick = {},
                modifier = Modifier.weight(1f).height(34.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.6f),
                    contentColor = MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text("Share profile", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun ProfileStat(value: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
fun ProfilePostsList(onPostClick: (String) -> Unit, currentUser: StoryUser) {
    LazyColumn(modifier = Modifier.fillMaxSize()) { items(mockFeed.filter { it.author == currentUser.name }) { post -> PostItem(post, { onPostClick(post.id) }, {}, {}, { emoji -> post.userReaction = emoji }, {}, {}) } }
}

@Composable
fun ProfileCreationsGrid(layout: String = "Staggered") { 
    if (layout == "Linear") {
        LazyColumn(modifier = Modifier.fillMaxSize()) { items(mockDiscover) { DiscoverCard(it, {}) } }
    } else {
        LazyVerticalStaggeredGrid(columns = StaggeredGridCells.Fixed(2), modifier = Modifier.fillMaxSize()) { items(mockDiscover) { DiscoverCard(it, {}) } } 
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoverScreen(onPinClick: (DiscoverItem) -> Unit, onSearchClick: () -> Unit) {
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { mockDiscover.size })
    var selectedCategory by remember { mutableStateOf("For You") }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        androidx.compose.foundation.pager.VerticalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val item = mockDiscover[page]
            Box(modifier = Modifier.fillMaxSize().clickable { onPinClick(item) }) {
                AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                // Cinematic gradient vignettes
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent, Color.Transparent), endY = 400f)
                ))
                Box(modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.85f)))
                ))
                // Bottom content
                Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp).padding(bottom = 80.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Text(item.category.uppercase(), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Text(item.title, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Visibility, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("12.4K views", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
                    }
                }
                // Right side actions
                Column(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    DiscoverAction(Icons.Default.Favorite, "4.2K")
                    DiscoverAction(Icons.Outlined.ChatBubbleOutline, "312")
                    DiscoverAction(Icons.Outlined.Share, "Share")
                    DiscoverAction(Icons.Default.BookmarkBorder, "Save")
                }
            }
        }

        // Top overlay bar
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            // Search Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(onClick = onSearchClick)
                    .height(42.dp),
                shape = RoundedCornerShape(21.dp),
                color = Color.Black.copy(alpha = 0.4f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Icon(Icons.Default.Search, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Search Discover", color = Color.White.copy(alpha = 0.5f), fontSize = 15.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            // Category pills
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(discoverCategories) { cat ->
                    val isSelected = cat == selectedCategory
                    Surface(
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .clickable { selectedCategory = cat },
                        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)) else null
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Text(
                                cat,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoverAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp))
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DiscoverCard(item: DiscoverItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(4.dp), shape = RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(2.dp)) {
        Box {
            AsyncImage(model = item.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(item.height), contentScale = ContentScale.Crop)
            Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f)))))
            Text(item.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.align(Alignment.BottomStart).padding(10.dp))
        }
    }
}

@Composable
fun SpacesScreen(onChatClick: (String) -> Unit, onSearchClick: () -> Unit, onCreateSpaceClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Spaces", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, letterSpacing = (-1).sp)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), CircleShape)
                ) { Icon(Icons.Default.Search, null) }
                IconButton(
                    onClick = onCreateSpaceClick,
                    modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                ) { Icon(Icons.Default.Add, null, tint = Color.Black) }
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(mockSpaces) { SpaceChatItem(it) { onChatClick(it.id) } }
        }
    }
}

@Composable
fun SpaceChatItem(chat: SpaceChat, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with story ring
        Box {
            AsyncImage(
                model = chat.avatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .then(
                        if (chat.hasStory)
                            Modifier.border(
                                2.5.dp,
                                Brush.linearGradient(listOf(Color(0xFFD8257D), Color(0xFFFDA50F))),
                                CircleShape
                            )
                        else Modifier
                    ),
                contentScale = ContentScale.Crop
            )
            // Online indicator
            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .align(Alignment.BottomEnd)
                        .background(MaterialTheme.colorScheme.background, CircleShape)
                        .padding(2.dp)
                        .background(Color(0xFF4CAF50), CircleShape)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    chat.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    chat.time,
                    fontSize = 12.sp,
                    color = if (chat.unread > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    fontWeight = if (chat.unread > 0) FontWeight.Bold else FontWeight.Normal
                )
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (chat.isTyping) "typing..." else chat.lastMessage,
                    color = if (chat.isTyping) MaterialTheme.colorScheme.primary
                            else if (chat.unread > 0) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontStyle = if (chat.isTyping) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                    modifier = Modifier.weight(1f),
                    fontWeight = if (chat.unread > 0) FontWeight.SemiBold else FontWeight.Normal
                )
                if (chat.unread > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .sizeIn(minWidth = 20.dp)
                            .height(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            chat.unread.toString(),
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 5.dp)
                        )
                    }
                }
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 88.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.06f)
    )
}

@Composable
fun InfiniteScrollHandler(lazyListState: LazyListState? = null, lazyStaggeredGridState: LazyStaggeredGridState? = null, buffer: Int = 2, onLoadMore: () -> Unit) {
    val loadMore = remember { derivedStateOf { val total = if (lazyListState != null) lazyListState.layoutInfo.totalItemsCount else lazyStaggeredGridState?.layoutInfo?.totalItemsCount ?: 0
        val last = if (lazyListState != null) (lazyListState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1 else (lazyStaggeredGridState?.layoutInfo?.visibleItemsInfo?.lastOrNull()?.index ?: 0) + 1
        last > (total - buffer) } }
    LaunchedEffect(loadMore) { snapshotFlow { loadMore.value }.distinctUntilChanged().filter { it }.collect { onLoadMore() } }
}

@Composable
fun ProfileBoardsList() { LazyColumn(modifier = Modifier.fillMaxSize()) { items(mockBoards) { Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) { AsyncImage(model = it.coverImageUrl, null, modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop); Spacer(Modifier.width(16.dp)); Column { Text(it.name, fontWeight = FontWeight.Bold); Text("${it.itemCount} items") } } } } }

@Composable
fun ActiveChatsGlimpse() {
    val activeChats = mockSpaces.filter { it.isOnline }
    if (activeChats.isNotEmpty()) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
            Text("Active Now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(activeChats) { chat ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { }) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            AsyncImage(model = chat.avatarUrl, contentDescription = null, modifier = Modifier.size(60.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                            Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(Color.Green).border(2.dp, MaterialTheme.colorScheme.background, CircleShape))
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(chat.name.split(" ").first(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
fun ReactionBurst(emoji: String, onAnimationFinished: () -> Unit) {}
