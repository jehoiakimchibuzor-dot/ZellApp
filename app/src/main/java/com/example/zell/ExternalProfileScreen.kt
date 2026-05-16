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
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalProfileScreen(
    userId: String,
    onBack: () -> Unit,
    onPostClick: (String) -> Unit,
    onChatClick: (String) -> Unit = {},
    followViewModel: FollowViewModel = viewModel()
) {
    val haptic = LocalHapticFeedback.current
    val db = FirebaseFirestore.getInstance()

    var user by remember { mutableStateOf(StoryUser(id = userId, name = "Loading…")) }
    var userPosts by remember { mutableStateOf<List<FeedPost>>(emptyList()) }
    var selectedTab by remember { mutableIntStateOf(0) }
    
    val isFollowing = followViewModel.followingState[userId] ?: false

    DisposableEffect(userId) {
        val userListener = db.collection("users").document(userId).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                snapshot.toObject(StoryUser::class.java)?.let { user = it }
            }
        }

        val postsListener = db.collection("posts")
            .whereEqualTo("authorId", userId)
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    userPosts = value.toObjects(FeedPost::class.java)
                }
            }

        onDispose {
            userListener.remove()
            postsListener.remove()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item {
                ProfileHeaderSection(user = user, avatarScale = 1f)
                
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user.name, fontWeight = FontWeight.Black, fontSize = 22.sp)
                            Text("@${user.name.lowercase().replace(" ", "")}", color = Color.Gray)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { onChatClick(user.id) }) {
                                Icon(Icons.Outlined.ChatBubbleOutline, null)
                            }

                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    followViewModel.toggleFollow(user.id, user.name)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isFollowing) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                                    contentColor = if (isFollowing) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(if (isFollowing) "Following" else "Follow", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text(user.bio.ifEmpty { "Digital creator on Zell ✨" })
                    Spacer(Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        ProfileStat("1.2K", "Followers")
                        ProfileStat("450", "Following")
                        ProfileStat(userPosts.size.toString(), "Posts")
                    }
                    Spacer(Modifier.height(16.dp))
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Posts") })
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Saved") })
                }
            }

            if (selectedTab == 0) {
                // 🔧 REFACTORED: Using a proper Grid for posts (doneby Gemini)
                item {
                    PostsGrid(posts = userPosts, onPostClick = onPostClick)
                }
            }
        }

        IconButton(
            onClick = onBack,
            modifier = Modifier.statusBarsPadding().padding(8.dp).background(Color.Black.copy(0.3f), CircleShape)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
        }
    }
}

@Composable
fun ProfileHeaderSection(user: StoryUser, avatarScale: Float) {
    Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer))
        Box(modifier = Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 10.dp)) {
            AsyncImage(
                model = user.avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(CircleShape).border(3.dp, Color.White, CircleShape).graphicsLayer(scaleX = avatarScale, scaleY = avatarScale),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun ProfileStat(value: String, label: String) {
    Column {
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text(label, fontSize = 13.sp, color = Color.Gray)
    }
}

@Composable
fun PostsGrid(posts: List<FeedPost>, onPostClick: (String) -> Unit) {
    // 🔧 REFACTORED: Implementation of the previously missing PostsGrid (doneby Gemini)
    Box(modifier = Modifier.heightIn(max = 1000.dp)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            items(posts) { post ->
                AsyncImage(
                    model = post.imageUrl ?: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400",
                    contentDescription = null,
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onPostClick(post.id) },
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}
