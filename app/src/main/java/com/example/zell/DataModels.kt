package com.example.zell

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import android.os.Parcelable
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date
import kotlinx.parcelize.Parcelize

@Parcelize
data class SpaceChat(
    val id: String = "",            // otherUserId — used to compute conversationId in ChatViewModel
    val name: String = "",
    val avatarUrl: String = "",
    val isOnline: Boolean = false,
    val lastMessage: String = "",           // preview text shown in conversation list
    val lastMessageTimestamp: Long = 0L,    // for sorting and display
    val lastMessageSenderId: String = "",   // to show "You: " prefix
    val unreadCount: Int = 0               // badge count for this user
) : Parcelable

enum class MessageType {
    TEXT, IMAGE, VIDEO, AUDIO, STORY_REPLY
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val time: String = "",
    val timestamp: Long = 0,
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val storyImageUrl: String? = null,   // set for STORY_REPLY — the story thumbnail
    val isRead: Boolean = false,
    val reaction: String? = null,
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false
)

data class FeedPost(
    val id: String = "",
    val author: String = "",
    val authorId: String = "",
    val handle: String = "",
    val avatarUrl: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val likes: Int = 0,
    val comments: Int = 0,
    val reposts: Int = 0,
    val views: String = "0",
    val isLiked: Boolean = false,
    val isReposted: Boolean = false,
    val isBookmarked: Boolean = false,
    val userReaction: String? = null,
    val reactionsCount: Int = 0,
    @ServerTimestamp val timestamp: Date? = null
)

// 🔧 NEW: Standardized Comment Model for Firestore
data class PostComment(
    val id: String = "",
    val author: String = "",
    val authorId: String = "",
    val handle: String = "",
    val avatarUrl: String = "",
    val content: String = "",
    val imageUrl: String? = null,
    val likes: Int = 0,
    val isLiked: Boolean = false,
    @ServerTimestamp val timestamp: Date? = null,
    val reaction: String? = null
)

// Updated to support Pinterest-style layout
data class DiscoverItem(
    val id: String = "",
    val title: String = "",
    val imageUrl: String = "",
    val category: String = "",
    val creatorName: String = "",
    val creatorAvatar: String = "",
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val aspectRatio: Float = 1f // Height / Width
)

// NEW: Pinterest Board Model
data class PinBoard(
    val id: String = "",
    val name: String = "",
    val ownerId: String = "",
    val pinCount: Int = 0,
    val thumbnails: List<String> = emptyList(), // Top 3 pin images for the collage
    val isSecret: Boolean = false,
    val category: String = ""
)

data class StoryUser(
    val id: String = "",
    val name: String = "",
    val avatarUrl: String = "",
    val bio: String = "",
    val about: String? = null,
    val skills: String? = null,
    val institution: String? = null,
    val location: String = "",
    val website: String = "",
    val themeColor: String? = null,
    val stories: List<StoryItem> = emptyList(),
    val hasUnread: Boolean = false,
    val isYou: Boolean = false
)

data class StoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String = "",
    val isVideo: Boolean = false,
    @ServerTimestamp val timestamp: Date? = null
)

enum class NotificationType(val icon: ImageVector, val color: Color) {
    LIKE(Icons.Default.Favorite, Color(0xFFEF4444)),
    REPIN(Icons.Default.PushPin, Color(0xFFE60023)),
    COMMENT(Icons.Default.ChatBubble, Color(0xFF3B82F6)),
    FOLLOW(Icons.Default.PersonAdd, Color(0xFF10B981)),
    MENTION(Icons.Default.AlternateEmail, Color(0xFFFF9800))
}

data class ZellNotification(
    val id: String = "",
    val userName: String = "",
    val actionText: String = "",
    val timestamp: String = "",
    val type: NotificationType = NotificationType.LIKE,
    val isRead: Boolean = false,
    val hasPreview: Boolean = false,
    val avatarUrl: String? = null,
    val otherAvatars: List<String> = emptyList()
)
