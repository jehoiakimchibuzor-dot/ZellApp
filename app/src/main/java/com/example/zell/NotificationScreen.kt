package com.example.zell

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    viewModel: NotificationViewModel = viewModel()
) {
    val notifications = viewModel.notifications
    val error by viewModel.error
    val isLoadingMore by viewModel.isLoadingMore
    val hasMore by viewModel.hasMore

    // Tracks where the user is in the list
    val listState = rememberLazyListState()

    // Watch the scroll position — when the user is 4 items from the bottom, load more
    LaunchedEffect(listState.firstVisibleItemIndex) {
        val totalItems = listState.layoutInfo.totalItemsCount
        val lastVisible = listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size
        if (totalItems > 0 && lastVisible >= totalItems - 4 && hasMore && !isLoadingMore) {
            viewModel.loadMore()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.markAllAsRead() }) {
                        Text("Mark all read", color = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            if (error != null) {
                ErrorBanner(error = error, onDismiss = { viewModel.clearError() })
            }

            if (notifications.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No new notifications",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notifications) { notification ->
                        NotificationItemRow(notification)
                    }

                    // Show a spinner at the very bottom while loading the next page
                    if (isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }

                    // When there's nothing left to load, show a subtle end message
                    if (!hasMore && notifications.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "You're all caught up!",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItemRow(notification: ZellNotification) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (notification.isRead) Color.Transparent
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        AsyncImage(
            model = notification.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${notification.userName} ${notification.actionText}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = notification.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(Modifier.width(8.dp))

        // Type icon on the right (like, comment, follow, mention)
        Icon(
            imageVector = notificationIcon(notification.type),
            contentDescription = null,
            tint = notificationIconColor(notification.type),
            modifier = Modifier.size(20.dp)
        )

        // Unread dot
        if (!notification.isRead) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

/** Returns the right icon for each notification type */
fun notificationIcon(type: NotificationType): ImageVector {
    return when (type) {
        NotificationType.LIKE -> Icons.Default.Favorite
        NotificationType.COMMENT -> Icons.Default.ChatBubble
        NotificationType.FOLLOW -> Icons.Default.PersonAdd
        NotificationType.MENTION -> Icons.Default.AlternateEmail
        else -> Icons.Default.Notifications
    }
}

/** Returns a color that matches each notification type */
@Composable
fun notificationIconColor(type: NotificationType): Color {
    return when (type) {
        NotificationType.LIKE -> Color(0xFFE91E63)      // pink/red for likes
        NotificationType.COMMENT -> Color(0xFF2196F3)   // blue for comments
        NotificationType.FOLLOW -> Color(0xFF4CAF50)    // green for follows
        NotificationType.MENTION -> Color(0xFFFF9800)   // orange for mentions
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    }
}
