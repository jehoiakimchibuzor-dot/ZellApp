package com.example.zell

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// --- 1. DATA MODELS ---

enum class NotificationType(val icon: ImageVector, val color: Color) {
    LIKE(Icons.Default.Favorite, Color(0xFFEF4444)),
    REPIN(Icons.Default.PushPin, Color(0xFFE60023)),
    COMMENT(Icons.Default.ChatBubble, Color(0xFF3B82F6)),
    FOLLOW(Icons.Default.PersonAdd, Color(0xFF10B981))
}

data class ZellNotification(
    val id: Int,
    val userName: String,
    val actionText: String,
    val timestamp: String,
    val type: NotificationType,
    val isRead: Boolean = false,
    val hasPreview: Boolean = false,
    val avatarUrl: String? = null,
    val otherAvatars: List<String> = emptyList()
)

// --- 2. UI COMPONENTS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen (onBack: () -> Unit) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("All", "Mentions", "Pins")
    
    // State to track read status for animation
    var notifications by remember { mutableStateOf(initialNotifications) }

    val filteredNotifications = remember(selectedTabIndex, notifications) {
        when (selectedTabIndex) {
            1 -> notifications.filter { it.type == NotificationType.COMMENT }
            2 -> notifications.filter { it.type == NotificationType.REPIN }
            else -> notifications
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 0.dp) {
                Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.statusBarsPadding().padding(end = 8.dp, top = 8.dp, bottom = 8.dp)) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            notifications = notifications.map { it.copy(isRead = true) }
                        }) {
                            Text("Mark all as read", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTabIndex == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTabIndex = index },
                                text = { Text(text = title, style = MaterialTheme.typography.titleSmall, fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Medium, color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (filteredNotifications.isEmpty()) {
                item {
                    EmptyNotificationsState()
                }
            } else {
                itemsIndexed(filteredNotifications, key = { _, n -> n.id }) { index, notification ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(index * 20L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = slideInVertically(initialOffsetY = { it / 3 }) + fadeIn(tween(300)),
                        exit = fadeOut()
                    ) {
                        NotificationItem(notification)
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: ZellNotification) {
    val readColor = Color.Transparent
    val unreadColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    val bgColor by animateColorAsState(
        if (notification.isRead) readColor else unreadColor,
        animationSpec = tween(500),
        label = "read_bg"
    )
    val stripeWidth by animateDpAsState(if (notification.isRead) 0.dp else 4.dp, animationSpec = spring(), label = "stripe")
    val stripeColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .drawBehind {
                if (stripeWidth > 0.dp) {
                    drawRect(
                        color = stripeColor,
                        topLeft = Offset.Zero,
                        size = Size(width = stripeWidth.toPx(), height = size.height)
                    )
                }
            }
            .clickable { /* Handle Navigation */ }
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar stack for group or single
        Box(modifier = Modifier.size(56.dp)) {
            if (notification.otherAvatars.isEmpty()) {
                // Single Avatar
                AsyncImage(
                    model = notification.avatarUrl ?: "",
                    contentDescription = null,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Stacked Avatars - 2-3 overlapping circles offset by 12dp
                notification.otherAvatars.take(3).forEachIndexed { i, url ->
                    AsyncImage(
                        model = url,
                        contentDescription = null,
                        modifier = Modifier
                            .offset(x = (i * 12).dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Notification Type Badge
            Surface(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(3.dp)) {
                    Icon(
                        imageVector = notification.type.icon,
                        contentDescription = null,
                        tint = notification.type.color,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        // Content Area
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)) {
                        append(notification.userName)
                    }
                    append(" ${notification.actionText}")
                },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )
            Text(
                text = notification.timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp),
                fontWeight = FontWeight.Bold
            )
        }

        // Action or Preview
        if (notification.hasPreview) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=200",
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                contentScale = ContentScale.Crop
            )
        } else if (notification.type == NotificationType.FOLLOW) {
            Button(
                onClick = { /* Follow Logic */ },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                modifier = Modifier.height(34.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Text("Follow", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun EmptyNotificationsState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(140.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.NotificationsNone, null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            }
        }
        Spacer(Modifier.height(28.dp))
        Text("Stay in the loop", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Text(
            "Notifications about your posts and activity will appear here. Start engaging to see some action!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {}, 
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.Black
            )
        ) {
            Text("Explore Trends", fontWeight = FontWeight.Bold)
        }
    }
}

private val initialNotifications = listOf(
    ZellNotification(1, "Shalom and 3 others", "liked your summer collection", "2m", NotificationType.LIKE, hasPreview = true, otherAvatars = listOf("https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200")),
    ZellNotification(2, "Victor", "started following you", "1h", NotificationType.FOLLOW, avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200"),
    ZellNotification(3, "Design_Daily", "re-pinned your layout", "3h", NotificationType.REPIN, hasPreview = true, avatarUrl = "https://images.unsplash.com/photo-1558591710-4b4a1ae0f04d?w=200"),
    ZellNotification(4, "Emma", "mentioned you: 'Love the color palette!'", "5h", NotificationType.COMMENT, hasPreview = true, avatarUrl = "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200"),
    ZellNotification(5, "TechPulse", "started following you", "1d", NotificationType.FOLLOW, isRead = true, avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200"),
    ZellNotification(6, "Zell_Bot", "re-pinned your Welcome Post", "2d", NotificationType.REPIN, isRead = true),
    ZellNotification(7, "Kofi", "mentioned you in a post", "3d", NotificationType.COMMENT, isRead = true, avatarUrl = "https://images.unsplash.com/photo-1552058544-f2b08422138a?w=200"),
    ZellNotification(8, "Zara", "re-pinned your Lagos Sunset Post", "4d", NotificationType.REPIN, isRead = true, hasPreview = true, avatarUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?w=200")
)
