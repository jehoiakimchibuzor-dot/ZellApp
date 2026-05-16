package com.example.zell

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Facebook-style Profile tab.
 *
 * Layout (LazyColumn):
 *  1. Cover photo + overlapping avatar + name/bio/buttons
 *  2. Intro card  (about, institution, location, website, skills)
 *  3. Photos card (horizontal strip of post images)
 *  4. Posts feed  (user's actual Firestore posts)
 */
@Composable
fun ProfileTab(
    currentUser: StoryUser,
    onEditProfileClick: () -> Unit
) {
    val themeColor = remember(currentUser.themeColor) {
        runCatching {
            Color(android.graphics.Color.parseColor(currentUser.themeColor ?: "#1877F2"))
        }.getOrDefault(Color(0xFF1877F2))
    }

    // ── Load user's posts from Firestore ──────────────────────────────────────
    val userPosts      = remember { mutableStateListOf<FeedPost>() }
    var loadingPosts   by remember { mutableStateOf(true) }

    LaunchedEffect(currentUser.id) {
        if (currentUser.id.isBlank()) { loadingPosts = false; return@LaunchedEffect }
        try {
            val snap = FirebaseFirestore.getInstance()
                .collection("posts")
                .whereEqualTo("authorId", currentUser.id)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(15)
                .get()
                .await()
            userPosts.clear()
            userPosts.addAll(snap.documents.mapNotNull {
                it.toObject(FeedPost::class.java)?.copy(id = it.id)
            })
        } catch (_: Exception) { /* show empty state */ }
        finally { loadingPosts = false }
    }

    val photoPosts = userPosts.filter { !it.imageUrl.isNullOrBlank() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── 1. HEADER — cover + avatar + name ─────────────────────────────────
        item(key = "header") {
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(0.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                // Cover photo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(themeColor, themeColor.copy(alpha = 0.35f))
                            )
                        )
                )

                // Avatar — overlaps the cover by half (60dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-60).dp)
                        .padding(start = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        AsyncImage(
                            model              = currentUser.avatarUrl,
                            contentDescription = "Profile photo",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                    }
                }

                // Pull content up to close the gap left by the negative offset
                Column(
                    modifier = Modifier
                        .offset(y = (-52).dp)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text       = currentUser.name,
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = MaterialTheme.colorScheme.onSurface
                    )

                    if (currentUser.bio.isNotBlank()) {
                        Text(
                            text     = currentUser.bio,
                            fontSize = 15.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    // Quick-glance info
                    if (!currentUser.institution.isNullOrBlank()) {
                        ProfileInfoRow(Icons.Default.Work, currentUser.institution!!)
                    }
                    if (currentUser.location.isNotBlank()) {
                        ProfileInfoRow(Icons.Default.LocationOn, "Lives in ${currentUser.location}")
                    }
                    if (currentUser.website.isNotBlank()) {
                        ProfileInfoRow(Icons.Default.Link, currentUser.website, tint = themeColor)
                    }

                    Spacer(Modifier.height(14.dp))

                    // Action buttons
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick  = onEditProfileClick,
                            modifier = Modifier.weight(1f),
                            shape    = RoundedCornerShape(8.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = themeColor)
                        ) {
                            Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit Profile", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {},
                            shape   = RoundedCornerShape(8.dp),
                            border  = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.MoreHoriz, null)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }

        // ── 2. INTRO CARD ─────────────────────────────────────────────────────
        val hasIntro = !currentUser.about.isNullOrBlank()
            || !currentUser.institution.isNullOrBlank()
            || currentUser.location.isNotBlank()
            || currentUser.website.isNotBlank()
            || !currentUser.skills.isNullOrBlank()

        if (hasIntro) {
            item(key = "intro") {
                FbCard(title = "Intro") {
                    if (!currentUser.about.isNullOrBlank()) {
                        Text(
                            text     = currentUser.about!!,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
                        Spacer(Modifier.height(10.dp))
                    }
                    if (!currentUser.institution.isNullOrBlank())
                        IntroDetailRow(icon = Icons.Default.School, text = currentUser.institution!!)
                    if (currentUser.location.isNotBlank())
                        IntroDetailRow(icon = Icons.Default.LocationOn, text = "Lives in ${currentUser.location}")
                    if (currentUser.website.isNotBlank())
                        IntroDetailRow(icon = Icons.Default.Link, text = currentUser.website, color = themeColor)

                    // Skills chips
                    val chips = currentUser.skills
                        ?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() }
                        ?: emptyList()
                    if (chips.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp)
                        ) {
                            chips.forEach { chip ->
                                Surface(
                                    shape  = RoundedCornerShape(20.dp),
                                    color  = themeColor.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        chip,
                                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                        fontSize   = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color      = themeColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── 3. PHOTOS CARD ────────────────────────────────────────────────────
        item(key = "photos") {
            FbCard(
                title         = "Photos",
                actionLabel   = if (photoPosts.isNotEmpty()) "See All" else null,
                onActionClick = {}
            ) {
                when {
                    loadingPosts -> Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp) }

                    photoPosts.isEmpty() -> Box(
                        modifier         = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No photos yet", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }

                    else -> LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(photoPosts.take(9)) { post ->
                            AsyncImage(
                                model              = post.imageUrl,
                                contentDescription = null,
                                modifier           = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale       = ContentScale.Crop
                            )
                        }
                    }
                }
            }
        }

        // ── 4. POSTS label ────────────────────────────────────────────────────
        item(key = "posts_label") {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text("Posts", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier              = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.FilterList, null, modifier = Modifier.size(15.dp))
                        Text("Filters", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ── 5. POSTS feed ─────────────────────────────────────────────────────
        when {
            loadingPosts -> item(key = "posts_loading") {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(40.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(modifier = Modifier.size(28.dp)) }
            }

            userPosts.isEmpty() -> item(key = "posts_empty") {
                Box(
                    modifier         = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Article, null,
                            modifier = Modifier.size(48.dp),
                            tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No posts yet", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }

            else -> items(userPosts, key = { it.id }) { post ->
                ProfilePostCard(post = post, accentColor = themeColor)
                Spacer(Modifier.height(8.dp))
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

// ── Facebook-style post card ──────────────────────────────────────────────────

@Composable
private fun ProfilePostCard(post: FeedPost, accentColor: Color) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(0.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Header
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AsyncImage(
                    model              = post.avatarUrl,
                    contentDescription = null,
                    modifier           = Modifier.size(42.dp).clip(CircleShape),
                    contentScale       = ContentScale.Crop
                )
                Column {
                    Text(post.author, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Public, null, modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                        Text(
                            text  = post.handle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }

            // Content
            if (post.content.isNotBlank()) {
                Text(
                    text     = post.content,
                    modifier = Modifier.padding(horizontal = 12.dp).padding(bottom = if (post.imageUrl != null) 10.dp else 0.dp),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color    = MaterialTheme.colorScheme.onSurface
                )
            }

            // Image
            if (!post.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model              = post.imageUrl,
                    contentDescription = null,
                    modifier           = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 380.dp),
                    contentScale       = ContentScale.Crop
                )
            }

            // Stats row
            if (post.likes > 0 || post.comments > 0 || post.reposts > 0) {
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (post.likes > 0) {
                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier         = Modifier
                                    .size(18.dp)
                                    .background(Color(0xFFE53935), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(11.dp))
                            }
                            Text(formatProfileCount(post.likes), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                        }
                    } else Spacer(Modifier.size(0.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (post.comments > 0) Text("${formatProfileCount(post.comments)} comments", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                        if (post.reposts > 0) Text("${formatProfileCount(post.reposts)} shares", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
            }

            // Action buttons — Like · Comment · Share with counts
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileActionBtn(
                    icon  = if (post.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    label = if (post.likes > 0) "Like · ${formatProfileCount(post.likes)}" else "Like",
                    tint  = if (post.isLiked) Color(0xFFE53935) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                ProfileActionBtn(
                    icon  = Icons.Outlined.ChatBubbleOutline,
                    label = if (post.comments > 0) "Comment · ${formatProfileCount(post.comments)}" else "Comment",
                    tint  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                ProfileActionBtn(
                    icon  = Icons.Default.Share,
                    label = if (post.reposts > 0) "Share · ${formatProfileCount(post.reposts)}" else "Share",
                    tint  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
        }
    }
}

// ── Reusable card wrapper ─────────────────────────────────────────────────────

@Composable
private fun FbCard(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape     = RoundedCornerShape(0.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (actionLabel != null && onActionClick != null) {
                    TextButton(onClick = onActionClick, contentPadding = PaddingValues(0.dp)) {
                        Text(actionLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            content()
        }
    }
}

// ── Small helpers ─────────────────────────────────────────────────────────────

@Composable
private fun ProfileInfoRow(icon: ImageVector, text: String, tint: Color = Color.Unspecified) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier              = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            icon, null,
            modifier = Modifier.size(14.dp),
            tint     = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else tint
        )
        Text(
            text     = text,
            fontSize = 13.sp,
            color    = if (tint == Color.Unspecified) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f) else tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun IntroDetailRow(icon: ImageVector, text: String, color: Color = Color.Unspecified) {
    val effectiveColor = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier              = Modifier.padding(vertical = 5.dp)
    ) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = effectiveColor.copy(alpha = 0.7f))
        Text(text, fontSize = 15.sp, color = effectiveColor, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ProfileActionBtn(icon: ImageVector, label: String, tint: Color) {
    TextButton(onClick = {}) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

private fun formatProfileCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0).trimEnd('0').trimEnd('.')
    n >= 1_000     -> "%.1fK".format(n / 1_000.0).trimEnd('0').trimEnd('.')
    else           -> "$n"
}
