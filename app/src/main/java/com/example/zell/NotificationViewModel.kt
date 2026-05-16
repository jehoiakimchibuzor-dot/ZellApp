package com.example.zell

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * NotificationViewModel - Manages user notification stream with pagination.
 * - First 20 notifications load instantly via real-time listener
 * - User scrolls to bottom → loadMore() fetches the next 20
 * - Keeps going until there's nothing left to load
 */
class NotificationViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    // How many notifications to load per page
    private val PAGE_SIZE = 20L

    private val _notifications = mutableStateListOf<ZellNotification>()
    val notifications: List<ZellNotification> get() = _notifications

    private val _error = mutableStateOf<AppError?>(null)
    val error: State<AppError?> = _error

    // True while we're fetching the next page (shows a spinner at the bottom)
    private val _isLoadingMore = mutableStateOf(false)
    val isLoadingMore: State<Boolean> = _isLoadingMore

    // False when we've loaded every notification (hides the "load more" trigger)
    private val _hasMore = mutableStateOf(true)
    val hasMore: State<Boolean> = _hasMore

    // Tracks the last document we loaded so we know where to start the next page from
    private var lastDocument: DocumentSnapshot? = null

    private var listener: ListenerRegistration? = null

    init {
        if (currentUserId.isNotEmpty()) {
            listenToFirstPage()
        }
    }

    /**
     * Real-time listener for the FIRST page only (most recent 20).
     * This means new notifications pop in automatically without refreshing.
     */
    private fun listenToFirstPage() {
        listener?.remove()

        listener = db.collection("users").document(currentUserId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    _error.value = ErrorHandler.classifyException(error)
                    return@addSnapshotListener
                }
                if (value != null) {
                    val list = value.documents.mapNotNull { doc ->
                        doc.toObject(ZellNotification::class.java)
                    }

                    // Replace only the first page worth of items in the list
                    // (keep any extra pages the user already loaded below)
                    val extraPages = if (_notifications.size > PAGE_SIZE) {
                        _notifications.drop(PAGE_SIZE.toInt())
                    } else emptyList()

                    _notifications.clear()
                    _notifications.addAll(list)
                    _notifications.addAll(extraPages)

                    // Save the last doc of this page so loadMore() knows where to start
                    lastDocument = value.documents.lastOrNull()

                    // If we got fewer than PAGE_SIZE, there's nothing more to load
                    if (list.size < PAGE_SIZE) _hasMore.value = false
                }
            }
    }

    /**
     * Called when the user scrolls to the bottom.
     * Fetches the next 20 notifications and appends them to the list.
     */
    fun loadMore() {
        // Don't load if already loading, nothing left, or no anchor point
        if (_isLoadingMore.value || !_hasMore.value || lastDocument == null) return

        viewModelScope.launch {
            try {
                _isLoadingMore.value = true

                val result = RetryHelper.firebaseRetry {
                    db.collection("users").document(currentUserId)
                        .collection("notifications")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .startAfter(lastDocument!!) // start right after the last one we loaded
                        .limit(PAGE_SIZE)
                        .get()
                        .await()
                }

                val newItems = result.documents.mapNotNull { doc ->
                    doc.toObject(ZellNotification::class.java)
                }

                _notifications.addAll(newItems)

                // Update our bookmark to the last doc of this new page
                lastDocument = result.documents.lastOrNull()

                // If we got fewer than PAGE_SIZE, we've hit the end
                if (newItems.size < PAGE_SIZE) _hasMore.value = false

                CrashlyticsLogger.i("NotificationViewModel", "Loaded ${newItems.size} more notifications")
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("NotificationViewModel", "Failed to load more notifications", e)
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    /**
     * Marks all currently loaded notifications as read.
     */
    fun markAllAsRead() {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            try {
                _error.value = null
                RetryHelper.firebaseRetry {
                    val batch = db.batch()
                    _notifications.forEach { notification ->
                        if (!notification.isRead) {
                            val ref = db.collection("users").document(currentUserId)
                                .collection("notifications").document(notification.id.toString())
                            batch.update(ref, "isRead", true)
                        }
                    }
                    batch.commit().await()
                }
                CrashlyticsLogger.i("NotificationViewModel", "All notifications marked as read")
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("NotificationViewModel", "Failed to mark notifications as read", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
