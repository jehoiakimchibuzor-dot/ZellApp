package com.example.zell

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExternalProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onChatClick: (String) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val db = FirebaseFirestore.getInstance()

    // ── Real-time user data ───────────────────────────────────
    var user by remember {
        mutableStateOf(
            StoryUser(
                id = userId,
                name = "Loading…",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200"
            )
        )
    }
    var userPosts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var isUserLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        // Try Firestore first
        db.collection("users").document(userId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && snapshot.exists()) {
                    snapshot.toObject(StoryUser::class.java)?.let { user = it }
                } else {
                    // Fallback to mock data
                    val mockMatch = mockFeed.find { it.id == userId || it.handle.contains(userId) }
                    if (mockMatch != null) {
                        user = StoryUser(mockMatch.id, mockMatch.author, mockMatch.avatarUrl,
                            bio = "Digital creator on Zell ✨", website = "zell.app/@${mockMatch.handle.removePrefix("@")}")
                    }
                }
                isUserLoading = false
            }

        // Real-time posts for this user
        db.collection("posts")
            .whereEqualTo("authorId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { value, _ ->
                if (value != null && !value.isEmpty) {
                    userPosts = value.toObjects(FeedPost::class.java)
                } else {
                    // Fallback to mock
                    userPosts = mockFeed.filter { it.id == userId || it.author == user.name }
                        .ifEmpty { mockFeed.take(3) }
                }
            }
    }

    var isFollowing by remember { mutableStateOf(false) }
    var isNotifying by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showFollowSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Follow count state  
    var followerCount by remember { mutableIntStateOf(1247) }

    // Entrance animations
    var entranceTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(50); entranceTriggered = true }
    val avatarScale by animateFloatAsState(
        targetValue = if (entranceTriggered) 1f else 0.7f,
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "avatar_scale"
    )

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Cover + Avatar ────────────────────────────────────
            item {
                Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    // Cover photo
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=1200",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Bottom gradient into background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Transparent, MaterialTheme.colorScheme.background)
                                )
                            )
                    )

                    // Avatar overlapping the cover
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 20.dp, bottom = (-44).dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background)
                                .padding(3.dp)
                        ) {
                            AsyncImage(
                                model = user.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .graphicsLayer(scaleX = avatarScale, scaleY = avatarScale),
                                contentScale = ContentScale.Crop
                            )
                        }
                        // Online indicator
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .background(MaterialTheme.colorScheme.background, CircleShape)
                                .padding(3.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                    }
                }

                // Space for avatar overlap
                Spacer(Modifier.height(54.dp))

                // ── Profile Info ──────────────────────────────────
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AnimatedContent(targetState = user.name, label = "name") {
                                Text(it, fontWeight = FontWeight.Black, fontSize = 22.sp, letterSpacing = (-0.5).sp)
                            }
                            Text(
                                "@${user.name.lowercase().replace(" ", "")}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }

                        // Action Buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            // Notify bell
                            if (isFollowing) {
                                IconButton(
                                    onClick = { isNotifying = !isNotifying },
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(0.6f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        if (isNotifying) Icons.Default.Notifications else Icons.Outlined.NotificationsNone,
                                        null,
                                        modifier = Modifier.size(18.dp),
                                        tint = if (isNotifying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            // Message button
                            IconButton(
                                onClick = { onChatClick(user.id) },
                                modifier = Modifier
                                    .size(38.dp)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.6f), CircleShape)
                            ) {
                                Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(18.dp))
                            }

                            // Follow button
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isFollowing = !isFollowing
                                    followerCount += if (isFollowing) 1 else -1
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                                modifier = Modifier.height(38.dp).animateContentSize()
                            ) {
                                AnimatedContent(targetState = isFollowing, label = "follow_btn") { following ->
                                    if (following) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Following", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                    } else {
                                        Text("Follow", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Bio
                    if (user.bio.isNotBlank()) {
                        Text(user.bio, fontSize = 14.sp, lineHeight = 21.sp, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(6.dp))
                    } else {
                        Text(
                            "Digital creator & explorer. Building on Zell. 🌍✨",
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(6.dp))
                    }

                    // Website link
                    if (user.website.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Link, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(4.dp))
                            Text(user.website, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // Joined date
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text("Joined April 2024", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Stats row
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ExternalProfileStat(followerCount.formatCompact(), "Followers") { showFollowSheet = true }
                        ExternalProfileStat("450", "Following") { showFollowSheet = true }
                        ExternalProfileStat(userPosts.size.toString().ifEmpty { "24" }, "Posts")
                    }

                    Spacer(Modifier.height(16.dp))
                }

                // ── Tab Bar ───────────────────────────────────────
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary,
                            height = 2.dp
                        )
                    },
                    divider = { HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)) }
                ) {
                    listOf(
                        Icons.Default.GridView to "Posts",
                        Icons.Outlined.BookmarkBorder to "Saved",
                        Icons.Default.Tag to "Tagged"
                    ).forEachIndexed { i, (icon, label) ->
                        Tab(
                            selected = selectedTab == i,
                            onClick = { selectedTab = i },
                            icon = {
                                Icon(
                                    icon, null,
                                    modifier = Modifier.size(22.dp),
                                    tint = if (selectedTab == i) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            }
                        )
                    }
                }
            }

            // ── Tab Content ───────────────────────────────────────
            when (selectedTab) {
                0 -> {
                    // Posts grid (Pinterest / Instagram style)
                    if (isUserLoading) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    } else if (userPosts.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Outlined.GridView, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                                Spacer(Modifier.height(12.dp))
                                Text("No posts yet", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                            }
                        }
                    } else {
                        // 3-column image grid like Instagram
                        item {
                            PostsGrid(posts = userPosts, onPostClick = onPostClick)
                        }
                    }
                }
                1 -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Outlined.BookmarkBorder, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                        Text("Saved posts are private", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                    }
                }
                2 -> item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Tag, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Spacer(Modifier.height(12.dp))
                        Text("No tagged posts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                    }
                }
            }
        }

        // ── Floating Top bar (always on top, transparent) ─────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            IconButton(
                onClick = {},
                modifier = Modifier
                    .size(40.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.MoreVert, null, tint = Color.White)
            }
        }
    }

    // ── Followers bottom sheet ────────────────────────────────
    if (showFollowSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFollowSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                Text("Followers", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.height(380.dp)) {
                    items(mockStories.take(5)) { follower ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = follower.avatarUrl,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(follower.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("@${follower.name.lowercase()}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            }
                            Button(
                                onClick = {},
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Text("Follow", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.07f))
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── 3-Column Posts Grid (Instagram style) ────────────────────

@Composable
fun PostsGrid(posts: List<FeedPost>, onPostClick: (String) -> Unit) {
    val rows = posts.chunked(3)
    Column {
        rows.forEach { rowPosts ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowPosts.forEach { post ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(0.5.dp)
                            .clickable { onPostClick(post.id) }
                    ) {
                        if (post.imageUrl != null) {
                            AsyncImage(
                                model = post.imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // Text post preview
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    post.content.take(60),
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
                // Fill empty cells in last row
                repeat(3 - rowPosts.size) {
                    Spacer(modifier = Modifier.weight(1f).aspectRatio(1f).padding(0.5.dp))
                }
            }
        }
    }
}

// ── External profile stat ─────────────────────────────────────

@Composable
fun ExternalProfileStat(value: String, label: String, onClick: (() -> Unit)? = null) {
    Column(
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        horizontalAlignment = Alignment.Start
    ) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Helper ────────────────────────────────────────────────────

fun Int.formatCompact(): String = when {
    this >= 1_000_000 -> "${this / 1_000_000}.${(this % 1_000_000) / 100_000}M"
    this >= 1_000 -> "${this / 1_000}.${(this % 1_000) / 100}K"
    else -> this.toString()
}

// Kept for backward compatibility
@Composable
fun FollowListContent() {
    Column(modifier = Modifier.fillMaxWidth().height(400.dp).padding(16.dp)) {
        Text("Followers", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        LazyColumn {
            items(5) { i ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    AsyncImage(model = "https://i.pravatar.cc/150?u=$i", contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text("User $i", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
