package com.example.zell

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * SpacesViewModel — Loads and manages real conversations from Firestore.
 *
 * Firestore structure:
 *   conversations/{conversationId}
 *     - participants: [uid1, uid2]
 *     - names: {uid: displayName}
 *     - avatars: {uid: avatarUrl}
 *     - lastMessage: String
 *     - lastMessageTimestamp: Long
 *     - lastMessageSenderId: String
 *     - unreadCounts: {uid: Int}
 *     - onlineStatus: {uid: Boolean}
 *     - createdAt: Long
 *
 *   conversations/{conversationId}/messages/{messageId}
 *     - (ChatMessage fields)
 *
 * NOTE: The query below uses whereArrayContains + orderBy which requires
 * a Firestore composite index. Firebase will show a link in Logcat
 * the first time this query runs — click it to auto-create the index.
 */
class SpacesViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: ""

    private val _conversations = mutableStateListOf<SpaceChat>()
    val conversations: List<SpaceChat> get() = _conversations

    private val _isLoading = mutableStateOf(true)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<AppError?>(null)
    val error: State<AppError?> = _error

    private var listener: ListenerRegistration? = null

    init {
        if (currentUserId.isNotEmpty()) listenToConversations()
    }

    private fun listenToConversations() {
        listener?.remove()

        listener = db.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false

                if (error != null) {
                    _error.value = ErrorHandler.classifyException(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val convos = snapshot.documents.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null

                        @Suppress("UNCHECKED_CAST")
                        val participants = data["participants"] as? List<String> ?: emptyList()

                        // The other person is whoever isn't the current user
                        val otherUserId = participants.firstOrNull { it != currentUserId }
                            ?: return@mapNotNull null

                        @Suppress("UNCHECKED_CAST")
                        val names = data["names"] as? Map<String, String> ?: emptyMap()
                        @Suppress("UNCHECKED_CAST")
                        val avatars = data["avatars"] as? Map<String, String> ?: emptyMap()
                        @Suppress("UNCHECKED_CAST")
                        val onlineStatus = data["onlineStatus"] as? Map<String, Boolean> ?: emptyMap()
                        @Suppress("UNCHECKED_CAST")
                        val unreadCounts = data["unreadCounts"] as? Map<String, Any> ?: emptyMap()

                        SpaceChat(
                            id = otherUserId,  // ChatViewModel uses this as otherUserId to compute conversationId
                            name = names[otherUserId] ?: "Unknown",
                            avatarUrl = avatars[otherUserId] ?: "",
                            isOnline = onlineStatus[otherUserId] ?: false,
                            lastMessage = data["lastMessage"] as? String ?: "",
                            lastMessageTimestamp = (data["lastMessageTimestamp"] as? Long) ?: 0L,
                            lastMessageSenderId = data["lastMessageSenderId"] as? String ?: "",
                            unreadCount = (unreadCounts[currentUserId] as? Long)?.toInt() ?: 0
                        )
                    }

                    _conversations.clear()
                    _conversations.addAll(convos)
                }
            }
    }

    /**
     * Creates a conversation with another user if it doesn't exist, then returns
     * the SpaceChat object so the UI can navigate to it.
     *
     * The conversation ID is deterministic: sorted UIDs joined with "_"
     * so both users always land on the same document.
     */
    fun getOrCreateConversation(
        otherUserId: String,
        otherUserName: String,
        otherUserAvatar: String,
        onResult: (SpaceChat) -> Unit
    ) {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                val currentUserName = currentUser?.displayName ?: "User"
                val currentUserAvatar = currentUser?.photoUrl?.toString() ?: ""

                // Deterministic ID — same result regardless of who initiates
                val conversationId = listOf(currentUserId, otherUserId).sorted().joinToString("_")
                val docRef = db.collection("conversations").document(conversationId)
                val doc = docRef.get().await()

                if (!doc.exists()) {
                    docRef.set(
                        mapOf(
                            "participants" to listOf(currentUserId, otherUserId),
                            "names" to mapOf(
                                currentUserId to currentUserName,
                                otherUserId to otherUserName
                            ),
                            "avatars" to mapOf(
                                currentUserId to currentUserAvatar,
                                otherUserId to otherUserAvatar
                            ),
                            "lastMessage" to "",
                            "lastMessageTimestamp" to 0L,
                            "lastMessageSenderId" to "",
                            "unreadCounts" to mapOf(currentUserId to 0, otherUserId to 0),
                            "onlineStatus" to mapOf(currentUserId to true, otherUserId to false),
                            "createdAt" to System.currentTimeMillis()
                        )
                    ).await()
                }

                val chat = SpaceChat(
                    id = otherUserId,
                    name = otherUserName,
                    avatarUrl = otherUserAvatar,
                    isOnline = false
                )

                onResult(chat)
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("SpacesViewModel", "Failed to get/create conversation", e)
            }
        }
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
