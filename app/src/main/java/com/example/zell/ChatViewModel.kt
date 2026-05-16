package com.example.zell

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * ChatViewModel — Real-time messaging with full conversation metadata sync.
 *
 * Firestore paths:
 *   conversations/{conversationId}/messages/{messageId}  ← individual messages
 *   conversations/{conversationId}                       ← conversation metadata
 *     (lastMessage, lastMessageTimestamp, unreadCounts, etc.)
 *
 * The conversationId is deterministic: smaller UID + "_" + larger UID
 * so both users always read/write the same document.
 */
class ChatViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: "guest"

    private val _messages = mutableStateListOf<ChatMessage>()
    val messages: List<ChatMessage> get() = _messages

    private val _error = mutableStateOf<AppError?>(null)
    val error: State<AppError?> = _error

    private var messagesListener: ListenerRegistration? = null
    private var activeChatId: String? = null

    // ─── Listen ─────────────────────────────────────────────────────────────

    /**
     * Start streaming messages for a conversation.
     * @param otherUserId The other participant's Firebase UID
     */
    fun startListening(otherUserId: String) {
        val conversationId = getConversationId(currentUserId, otherUserId)
        if (activeChatId == conversationId) return

        activeChatId = conversationId
        messagesListener?.remove()
        _messages.clear()

        messagesListener = db.collection("conversations")
            .document(conversationId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _error.value = ErrorHandler.classifyException(error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val newMessages = snapshot.toObjects(ChatMessage::class.java)
                    _messages.clear()
                    _messages.addAll(newMessages)
                    // Zero out our unread count when we open the conversation
                    markConversationRead(conversationId)
                    // Mark individual messages as read
                    markMessagesRead(conversationId, newMessages)
                }
            }
    }

    // ─── Send ────────────────────────────────────────────────────────────────

    /**
     * Sends a message and updates the conversation's last-message metadata.
     * @param otherUserId The other participant's Firebase UID
     */
    fun sendMessage(
        otherUserId: String,
        text: String,
        type: MessageType = MessageType.TEXT,
        imageUrl: String? = null
    ) {
        if (text.isBlank() && imageUrl == null) return

        val conversationId = getConversationId(currentUserId, otherUserId)
        val msgId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val message = ChatMessage(
            id = msgId,
            senderId = currentUserId,
            text = text.trim(),
            time = TimeUtils.formatMessageTime(timestamp),
            timestamp = timestamp,
            type = type,
            imageUrl = imageUrl,
            isRead = false
        )

        // Human-readable preview for the conversation list
        val lastMessagePreview = when (type) {
            MessageType.IMAGE -> "📷 Photo"
            MessageType.VIDEO -> "📹 Video"
            MessageType.AUDIO -> "🎤 Voice message"
            else -> text.trim()
        }

        viewModelScope.launch {
            try {
                // 1. Save the message
                RetryHelper.firebaseRetry {
                    db.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .document(msgId)
                        .set(message)
                        .await()
                }

                // 2. Upsert conversation metadata — set(merge) CREATES the document if
                //    it doesn't exist yet (happens on first message from Create Space).
                //    This also ensures "participants" is written so SpacesViewModel's
                //    whereArrayContains query can find the conversation.
                RetryHelper.firebaseRetry {
                    db.collection("conversations")
                        .document(conversationId)
                        .set(
                            mapOf(
                                // participants is the key field SpacesViewModel queries on
                                "participants"         to listOf(currentUserId, otherUserId).sorted(),
                                "lastMessage"         to lastMessagePreview,
                                "lastMessageTimestamp" to timestamp,
                                "lastMessageSenderId" to currentUserId,
                                // FieldValue.increment works inside set(merge) too
                                "unreadCounts"        to mapOf(otherUserId to FieldValue.increment(1L))
                            ),
                            SetOptions.merge()
                        )
                        .await()
                }

                CrashlyticsLogger.i("ChatViewModel", "Message sent in conversation $conversationId")
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("ChatViewModel", "Failed to send message", e)
            }
        }
    }

    // ─── Story Reply ─────────────────────────────────────────────────────────

    /**
     * Sends a story reply that appears in DM as a special bubble showing
     * the story thumbnail above the reply text (or emoji reaction).
     */
    fun sendStoryReply(otherUserId: String, replyText: String, storyImageUrl: String) {
        val conversationId = getConversationId(currentUserId, otherUserId)
        val msgId    = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        val message = ChatMessage(
            id            = msgId,
            senderId      = currentUserId,
            text          = replyText.trim(),
            time          = TimeUtils.formatMessageTime(timestamp),
            timestamp     = timestamp,
            type          = MessageType.STORY_REPLY,
            storyImageUrl = storyImageUrl,
            isRead        = false
        )

        val preview = if (replyText.trim().length <= 2) "Reacted ${replyText.trim()} to your story"
                      else "Replied to your story: ${replyText.trim()}"

        viewModelScope.launch {
            try {
                RetryHelper.firebaseRetry {
                    db.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .document(msgId)
                        .set(message)
                        .await()
                }
                RetryHelper.firebaseRetry {
                    db.collection("conversations")
                        .document(conversationId)
                        .set(
                            mapOf(
                                "participants"         to listOf(currentUserId, otherUserId).sorted(),
                                "lastMessage"         to preview,
                                "lastMessageTimestamp" to timestamp,
                                "lastMessageSenderId" to currentUserId,
                                "unreadCounts"        to mapOf(otherUserId to FieldValue.increment(1L))
                            ),
                            SetOptions.merge()
                        )
                        .await()
                }
                CrashlyticsLogger.i("ChatViewModel", "Story reply sent in conversation $conversationId")
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("ChatViewModel", "Failed to send story reply", e)
            }
        }
    }

    // ─── Story Views ──────────────────────────────────────────────────────────

    /**
     * Records the current user viewing a story. Idempotent — uses arrayUnion
     * so repeated calls for the same viewer don't inflate the count.
     */
    fun recordStoryView(storyOwnerId: String, storyId: String) {
        if (currentUserId == "guest" || currentUserId == storyOwnerId) return
        viewModelScope.launch {
            try {
                db.collection("storyViews")
                    .document("${storyOwnerId}_$storyId")
                    .set(
                        mapOf(
                            "ownerId"  to storyOwnerId,
                            "storyId"  to storyId,
                            "viewers"  to FieldValue.arrayUnion(currentUserId)
                        ),
                        SetOptions.merge()
                    )
                    .await()
            } catch (e: Exception) {
                CrashlyticsLogger.w("ChatViewModel", "Failed to record story view: ${e.message}")
            }
        }
    }

    // ─── Delete ──────────────────────────────────────────────────────────────

    /**
     * Soft-deletes a message — sets isDeleted = true so the UI shows
     * "This message was deleted" instead of removing it entirely.
     * Only the sender should be able to call this (enforced in UI).
     */
    fun deleteMessage(otherUserId: String, messageId: String) {
        val conversationId = getConversationId(currentUserId, otherUserId)
        viewModelScope.launch {
            try {
                RetryHelper.firebaseRetry {
                    db.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .document(messageId)
                        .update(
                            mapOf(
                                "isDeleted" to true,
                                "text" to "",           // clear the content
                                "imageUrl" to null,     // clear any image
                                "reaction" to null      // clear reactions
                            )
                        )
                        .await()
                }
                CrashlyticsLogger.i("ChatViewModel", "Message $messageId deleted")
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("ChatViewModel", "Failed to delete message $messageId", e)
            }
        }
    }

    // ─── Edit ─────────────────────────────────────────────────────────────────

    /**
     * Edits a message's text and marks it as edited.
     * Only works on TEXT messages (not images/audio).
     */
    fun editMessage(otherUserId: String, messageId: String, newText: String) {
        if (newText.isBlank()) return
        val conversationId = getConversationId(currentUserId, otherUserId)
        viewModelScope.launch {
            try {
                RetryHelper.firebaseRetry {
                    db.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .document(messageId)
                        .update(
                            mapOf(
                                "text" to newText.trim(),
                                "isEdited" to true
                            )
                        )
                        .await()
                }
                CrashlyticsLogger.i("ChatViewModel", "Message $messageId edited")
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("ChatViewModel", "Failed to edit message $messageId", e)
            }
        }
    }

    // ─── Reactions ───────────────────────────────────────────────────────────

    fun toggleReaction(otherUserId: String, messageId: String, reaction: String) {
        val conversationId = getConversationId(currentUserId, otherUserId)
        viewModelScope.launch {
            try {
                RetryHelper.firebaseRetry {
                    db.collection("conversations")
                        .document(conversationId)
                        .collection("messages")
                        .document(messageId)
                        .update("reaction", reaction)
                        .await()
                }
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("ChatViewModel", "Failed to update reaction on $messageId", e)
            }
        }
    }

    // ─── Read Receipts ───────────────────────────────────────────────────────

    /** Zero out current user's unread count for this conversation */
    private fun markConversationRead(conversationId: String) {
        viewModelScope.launch {
            try {
                db.collection("conversations")
                    .document(conversationId)
                    .update("unreadCounts.$currentUserId", 0)
                    .await()
            } catch (e: Exception) {
                // Non-critical — don't surface this error to the user
                CrashlyticsLogger.w("ChatViewModel", "Failed to reset unread count: ${e.message}")
            }
        }
    }

    /** Mark individual messages from the other person as isRead = true */
    private fun markMessagesRead(conversationId: String, messages: List<ChatMessage>) {
        viewModelScope.launch {
            messages
                .filter { it.senderId != currentUserId && !it.isRead }
                .forEach { msg ->
                    try {
                        RetryHelper.firebaseRetry {
                            db.collection("conversations")
                                .document(conversationId)
                                .collection("messages")
                                .document(msg.id)
                                .update("isRead", true)
                                .await()
                        }
                    } catch (e: Exception) {
                        CrashlyticsLogger.w("ChatViewModel", "Failed to mark message ${msg.id} as read")
                    }
                }
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    fun clearError() { _error.value = null }

    /**
     * Deterministic conversation ID — same result no matter who computes it.
     * e.g. "abc_xyz" (smaller UID always first)
     */
    private fun getConversationId(uid1: String, uid2: String): String =
        if (uid1 < uid2) "${uid1}_$uid2" else "${uid2}_$uid1"

    override fun onCleared() {
        super.onCleared()
        messagesListener?.remove()
    }
}
