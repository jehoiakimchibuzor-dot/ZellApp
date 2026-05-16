package com.example.zell

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter

enum class TabType { HOME, DISCOVER, SPACES, PROFILE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(
    onChatClick: (SpaceChat) -> Unit,
    onStoryClick: (Int) -> Unit,
    onNotificationClick: () -> Unit,
    onComposeClick: () -> Unit,
    onPostClick: (String) -> Unit,
    onPinClick: (DiscoverItem) -> Unit,
    onCreatePinClick: () -> Unit,
    onSearchClick: () -> Unit,
    onCreateSpaceClick: () -> Unit,
    onExternalProfileClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onAddStoryClick: () -> Unit,
    onCommentClick: (String) -> Unit,
    currentUser: StoryUser,
    spacesViewModel: SpacesViewModel
) {
    var selectedTab by remember { mutableStateOf(TabType.HOME) }
    val homeViewModel: HomeViewModel = viewModel()
    val totalUnread = spacesViewModel.conversations.sumOf { it.unreadCount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Zell",
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    // Tap own avatar → go to Profile tab (NOT edit profile)
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .clickable { selectedTab = TabType.PROFILE },
                        contentAlignment = Alignment.Center
                    ) {
                        val hasStory = currentUser.stories.isNotEmpty()
                        if (hasStory) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        2.dp,
                                        Brush.sweepGradient(
                                            listOf(Color(0xFFF9CE34), Color(0xFFEE2A7B), Color(0xFF6228D7))
                                        ),
                                        CircleShape
                                    )
                                    .padding(3.dp)
                            ) {
                                Image(
                                    painter = rememberAsyncImagePainter(currentUser.avatarUrl),
                                    contentDescription = "My profile",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else {
                            Image(
                                painter = rememberAsyncImagePainter(currentUser.avatarUrl),
                                contentDescription = "My profile",
                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search")
                    }
                    // Notifications with badge
                    IconButton(onClick = onNotificationClick) {
                        BadgedBox(badge = {
                            // Could wire to a notification count — placeholder dot for now
                        }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                        }
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, "Home") },
                    label = { Text("Home") },
                    selected = selectedTab == TabType.HOME,
                    onClick = {
                        if (selectedTab == TabType.HOME) homeViewModel.refreshFeed()
                        selectedTab = TabType.HOME
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Explore, "Discover") },
                    label = { Text("Discover") },
                    selected = selectedTab == TabType.DISCOVER,
                    onClick = { selectedTab = TabType.DISCOVER }
                )
                NavigationBarItem(
                    icon = {
                        BadgedBox(badge = {
                            if (totalUnread > 0)
                                Badge { Text(if (totalUnread > 99) "99+" else "$totalUnread") }
                        }) {
                            Icon(Icons.Default.Group, "Spaces")
                        }
                    },
                    label = { Text("Spaces") },
                    selected = selectedTab == TabType.SPACES,
                    onClick = { selectedTab = TabType.SPACES }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == TabType.PROFILE,
                    onClick = { selectedTab = TabType.PROFILE }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == TabType.HOME) {
                FloatingActionButton(
                    onClick = onComposeClick,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, "Create Post")
                }
            }
        }
    ) { padding ->
        val homeError by homeViewModel.error
        if (homeError != null && selectedTab == TabType.HOME) {
            ErrorBanner(error = homeError, onDismiss = { homeViewModel.clearError() })
        }

        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                TabType.HOME -> HomeTab(
                    onPostClick       = onPostClick,
                    onProfileClick    = onExternalProfileClick,
                    onCommentClick    = onCommentClick,
                    onStoryClick      = onStoryClick,
                    onAddStoryClick   = onAddStoryClick,
                    onComposeClick    = onComposeClick,
                    currentUser       = currentUser,
                    homeViewModel     = homeViewModel
                )
                TabType.DISCOVER -> DiscoverTab(
                    onPinClick       = onPinClick,
                    onCreatePinClick = onCreatePinClick
                )
                TabType.SPACES -> SpacesTab(
                    onChatClick       = onChatClick,
                    onCreateSpaceClick = onCreateSpaceClick,
                    spacesViewModel   = spacesViewModel
                )
                TabType.PROFILE -> ProfileTab(
                    currentUser       = currentUser,
                    onEditProfileClick = onEditProfileClick
                )
            }
        }
    }
}
