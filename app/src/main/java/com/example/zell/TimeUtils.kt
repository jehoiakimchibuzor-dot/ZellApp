package com.example.zell

import java.text.SimpleDateFormat
import java.util.*

/**
 * TimeUtils — Human-friendly timestamp formatting.
 *
 * Instead of raw "14:32:07", users see things like:
 *   "Just now", "2m ago", "3:45 PM", "Yesterday", "Mon", "Jan 12"
 */
object TimeUtils {

    /**
     * For individual message timestamps inside a chat.
     * e.g. "Just now" | "5m ago" | "3:45 PM" | "Yesterday" | "Mon" | "Jan 12"
     */
    fun formatMessageTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000}m ago"
            diff < 86_400_000L -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
            diff < 172_800_000L -> "Yesterday"
            diff < 604_800_000L -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }

    /**
     * For feed posts — accepts a java.util.Date (Firestore @ServerTimestamp).
     * e.g. "Just now" | "5 minutes ago" | "2 hours ago" | "3 days ago" | "Jan 12"
     */
    fun timeAgo(date: Date): String {
        val now  = System.currentTimeMillis()
        val diff = now - date.time
        return when {
            diff < 60_000L          -> "Just now"
            diff < 3_600_000L       -> "${diff / 60_000}m ago"
            diff < 86_400_000L      -> "${diff / 3_600_000}h ago"
            diff < 604_800_000L     -> "${diff / 86_400_000}d ago"
            else                    -> SimpleDateFormat("MMM d", Locale.getDefault()).format(date)
        }
    }

    /**
     * For conversation list previews (shorter format).
     * e.g. "now" | "5m" | "3:45 PM" | "Yesterday" | "Mon" | "Jan 12"
     */
    fun formatConversationTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60_000L -> "now"
            diff < 3_600_000L -> "${diff / 60_000}m"
            diff < 86_400_000L -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
            diff < 172_800_000L -> "Yesterday"
            diff < 604_800_000L -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
