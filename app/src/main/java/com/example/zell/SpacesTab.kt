package com.example.zell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

// Messenger signature blue — used for unread badges, timestamps, "You" prefix
private val MessengerBlue = Color(0xFF0084FF)

/**
 * SpacesTab — pixel-accurate Facebook Messenger conversation list.
 *
 * Every row shows:
 *  • Avatar + green online dot (if active)
 *  • Bold name (unread) or SemiBold name (read)
 *  • Last message preview  — "You: …" prefix when sent by current user
 *    ┌ unread → dark text + bold
 *    └ read   → grey text + normal weight
 *  • Right column: timestamp (blue if unread) + blue unread badge OR seen/delivered tick
 *
 * The blue unread badge is the number of unread messages (Messenger style):
 *  1  →  "1"  in a filled blue circle
 *  99+ → "99+" pill
 */
@Composable
fun SpacesTab(
    onChatClick: (SpaceChat) -> Unit,
    onCreateSpaceClick: () -> Unit,
    spacesViewModel: SpacesViewModel
) {
    val conversations = spacesViewModel.conversations
    val isLoading by spacesViewModel.isLoading
    val error by spacesViewModel.error
    var searchQuery by remember { mutableStateOf("") }

    val onlineContacts = conversations.filter { it.isOnline }
    val filtered = if (searchQuery.isBlank()) conversations
                   else conversations.filter { it.name.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        // ── Header ───────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text       = "Chats",
                fontSize   = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier   = Modifier.weight(1f),
                color      = MaterialTheme.colorScheme.onBackground
            )
            // New message button — Messenger pencil icon
            Surface(
                modifier  = Modifier.size(36.dp),
                shape     = CircleShape,
                color     = MaterialTheme.colorScheme.surfaceVariant
            ) {
                IconButton(onClick = onCreateSpaceClick) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "New message",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // ── Search bar ───────────────────────────────────────────────────────
        Surface(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape     = RoundedCornerShape(24.dp),
            color     = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                BasicTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine    = true,
                    textStyle     = TextStyle(
                        fontSize = 15.sp,
                        color    = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MessengerBlue),
                    modifier    = Modifier.weight(1f),
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search",
                                fontSize = 15.sp,
                                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                            )
                        }
                        inner()
                    }
                )
                if (searchQuery.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { searchQuery = "" }
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Error Banner ─────────────────────────────────────────────────────
        if (error != null) {
            ErrorBanner(error = error, onDismiss = { spacesViewModel.clearError() })
        }

        // ── Loading skeleton ─────────────────────────────────────────────────
        if (isLoading) {
            repeat(6) { ConversationRowSkeleton() }
            return@Column
        }

        // ── Empty state ──────────────────────────────────────────────────────
        if (conversations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No conversations yet",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Tap the pencil to start a chat",
                        fontSize = 14.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onCreateSpaceClick,
                        colors  = ButtonDefaults.buttonColors(containerColor = MessengerBlue)
                    ) {
                        Text("Start a Chat", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Column
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {

            // ── Active Now ────────────────────────────────────────────────────
            if (onlineContacts.isNotEmpty() && searchQuery.isBlank()) {
                item {
                    Text(
                        "Active Now",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        modifier   = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 6.dp)
                    )
                    LazyRow(
                        contentPadding        = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        items(onlineContacts) { contact ->
                            ActiveContactBubble(
                                contact = contact,
                                onClick = { onChatClick(contact) }
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(
                        color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f),
                        thickness = 0.5.dp
                    )
                }
            }

            // ── Conversation rows ─────────────────────────────────────────────
            if (filtered.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No results for \"$searchQuery\"",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }
            } else {
                items(filtered, key = { it.id }) { chat ->
                    ConversationRow(
                        chat          = chat,
                        currentUserId = spacesViewModel.currentUserId,
                        onClick       = { onChatClick(chat) }
                    )
                }
            }

            item { Spacer(Modifier.height(88.dp)) }
        }
    }
}

// ── Active contact bubble ─────────────────────────────────────────────────────

@Composable
fun ActiveContactBubble(contact: SpaceChat, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier
            .width(64.dp)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.size(56.dp)) {
            AsyncImage(
                model              = contact.avatarUrl,
                contentDescription = contact.name,
                modifier           = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale       = ContentScale.Crop
            )
            // Green online dot
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .background(Color(0xFF31A24C), CircleShape)
                    .border(2.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text     = contact.name.split(" ").first(),
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color    = MaterialTheme.colorScheme.onBackground
        )
    }
}

// ── Conversation row ──────────────────────────────────────────────────────────

@Composable
fun ConversationRow(
    chat: SpaceChat,
    currentUserId: String,
    onClick: () -> Unit
) {
    val isUnread    = chat.unreadCount > 0
    val isSentByMe  = chat.lastMessageSenderId == currentUserId

    // Last message preview — exactly what Messenger shows
    val preview = when {
        chat.lastMessage.isEmpty()              -> "Say hello 👋"
        chat.lastMessage == "📷 Photo"          -> if (isSentByMe) "You sent a photo" else "Sent a photo 📷"
        chat.lastMessage == "📹 Video"          -> if (isSentByMe) "You sent a video" else "Sent a video 📹"
        chat.lastMessage == "🎤 Voice message"  -> if (isSentByMe) "You sent a voice message" else "🎤 Voice message"
        isSentByMe                             -> "You: ${chat.lastMessage}"
        else                                   -> chat.lastMessage
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        // ── Avatar ───────────────────────────────────────────────────────────
        Box(modifier = Modifier.size(58.dp)) {
            AsyncImage(
                model              = chat.avatarUrl,
                contentDescription = chat.name,
                modifier           = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale       = ContentScale.Crop
            )
            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .size(15.dp)
                        .background(Color(0xFF31A24C), CircleShape)
                        .border(2.5.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // ── Name + preview ───────────────────────────────────────────────────
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = chat.name,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                fontSize   = 15.sp,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
                color      = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = preview,
                    fontSize   = 13.sp,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    color      = when {
                        isUnread   -> MaterialTheme.colorScheme.onBackground
                        else       -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    },
                    modifier   = Modifier.weight(1f, fill = false)
                )
                // Delivered / Seen dot for sent messages (read = hollow, unread = filled blue)
                if (isSentByMe && chat.lastMessage.isNotEmpty()) {
                    Spacer(Modifier.width(4.dp))
                    if (isUnread) {
                        // Hollow circle = delivered but not read
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .border(1.5.dp, MessengerBlue, CircleShape)
                        )
                    } else {
                        // Filled blue circle = seen
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(MessengerBlue, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "Seen",
                                tint     = Color.White,
                                modifier = Modifier.size(7.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        // ── Right column: timestamp + badge ──────────────────────────────────
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            // Timestamp
            if (chat.lastMessageTimestamp > 0L) {
                Text(
                    text      = TimeUtils.formatConversationTime(chat.lastMessageTimestamp),
                    fontSize  = 11.sp,
                    color     = if (isUnread) MessengerBlue
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }
            Spacer(Modifier.height(4.dp))

            // Unread badge — exactly like Messenger (blue filled circle/pill)
            if (isUnread) {
                val badgeText = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString()
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .background(MessengerBlue, CircleShape)
                        .padding(horizontal = if (chat.unreadCount > 9) 5.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text       = badgeText,
                        color      = Color.White,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Placeholder so the column height stays consistent
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ── Loading skeleton ──────────────────────────────────────────────────────────

@Composable
private fun ConversationRowSkeleton() {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            )
        }
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(11.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
    }
}
