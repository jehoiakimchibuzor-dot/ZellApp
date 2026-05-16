package com.example.zell

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * HomeViewModel - Manages the main feed state and pagination
 */
class HomeViewModel : ViewModel() {

    companion object {
        const val PAGE_SIZE = 12
    }

    private val db = FirebaseFirestore.getInstance()
    private val _feedItems = mutableStateListOf<FeedPost>()
    val feedItems: List<FeedPost> get() = _feedItems

    private var lastDocument: DocumentSnapshot? = null
    var isLastPage by mutableStateOf(false)
        private set
    private var firestoreListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    
    var isLoadingMore by mutableStateOf(false)
        private set

    var isLoading by mutableStateOf(true)
        private set

    var isRefreshing by mutableStateOf(false)
        private set

    private val _error = mutableStateOf<AppError?>(null)
    val error: State<AppError?> = _error

    init {
        refreshFeed()
    }

    fun refreshFeed() {
        if (isRefreshing) return          // prevent double-trigger from nav-tap + PTR
        viewModelScope.launch {
            isRefreshing = true
            _error.value = null

            // Reset pagination cursor so loadMorePosts() starts fresh after refresh
            lastDocument = null
            isLastPage   = false

            try {
                val snapshot = RetryHelper.firebaseRetry {
                    db.collection("posts")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(PAGE_SIZE.toLong())
                        .get()
                        .await()
                }

                _feedItems.clear()
                val posts = snapshot.documents.mapNotNull {
                    it.toObject(FeedPost::class.java)?.copy(id = it.id)
                }

                if (posts.isNotEmpty()) {
                    _feedItems.addAll(posts)
                    lastDocument = snapshot.documents.lastOrNull()
                    isLastPage   = posts.size < PAGE_SIZE
                    setupRealtimeListener()
                } else {
                    // Firestore is empty — show mock so the screen isn't blank
                    _feedItems.addAll(mockFeed.map { it.copy() })
                    isLastPage = true          // no real pagination on mock
                }
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                if (_feedItems.isEmpty()) _feedItems.addAll(mockFeed.map { it.copy() })
                CrashlyticsLogger.e("HomeViewModel", "Failed to refresh feed after retries", e)
            } finally {
                isLoading    = false
                isRefreshing = false
            }
        }
    }

    private fun setupRealtimeListener() {
        firestoreListenerRegistration?.remove()
        firestoreListenerRegistration = db.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(12)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _error.value = ErrorHandler.classifyException(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val updatedPosts = snapshot.documents.mapNotNull { 
                        it.toObject(FeedPost::class.java)?.copy(id = it.id) 
                    }
                    
                    updatedPosts.forEach { updatedPost ->
                        val index = _feedItems.indexOfFirst { it.id == updatedPost.id }
                        if (index != -1) {
                            // Update existing post with fresh data from Firestore
                            _feedItems[index] = updatedPost
                        } else {
                            // New post - add it
                            _feedItems.add(0, updatedPost)
                        }
                    }
                }
            }
    }

    fun loadMorePosts() {
        if (isLoadingMore || isLastPage || lastDocument == null) return

        isLoadingMore = true
        viewModelScope.launch {
            try {
                val cursor = lastDocument ?: return@launch   // snapshot already cleared

                val snapshot = RetryHelper.firebaseRetry {
                    db.collection("posts")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(PAGE_SIZE.toLong())
                        .startAfter(cursor)
                        .get()
                        .await()
                }

                val newPosts = snapshot.documents.mapNotNull {
                    it.toObject(FeedPost::class.java)?.copy(id = it.id)
                }

                if (newPosts.isEmpty()) {
                    isLastPage = true
                } else {
                    _feedItems.addAll(newPosts)
                    lastDocument = snapshot.documents.lastOrNull()
                    if (newPosts.size < PAGE_SIZE) isLastPage = true
                }
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("HomeViewModel", "Failed to load more posts", e)
            } finally {
                isLoadingMore = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun toggleLike(postId: String) {
        val index = _feedItems.indexOfFirst { it.id == postId }
        if (index != -1) {
            val post = _feedItems[index]
            val newLikeState = !post.isLiked
            val newLikeCount = if (newLikeState) post.likes + 1 else post.likes - 1

            val updated = post.copy(isLiked = newLikeState, likes = newLikeCount)
            _feedItems[index] = updated

            // Sync to Firestore in background
            viewModelScope.launch {
                try {
                    RetryHelper.firebaseRetry {
                        db.collection("posts").document(postId).set(updated, SetOptions.merge()).await()
                    }
                } catch (e: Exception) {
                    // If save fails, revert the local change
                    if (_feedItems.getOrNull(index)?.id == postId) {
                        _feedItems[index] = post
                    }
                    _error.value = ErrorHandler.classifyException(e)
                    CrashlyticsLogger.e("HomeViewModel", "Failed to save like status", e)
                }
            }
        }
    }

    fun toggleBookmark(postId: String) {
        val index = _feedItems.indexOfFirst { it.id == postId }
        if (index != -1) {
            val post = _feedItems[index]
            val updated = post.copy(isBookmarked = !post.isBookmarked)
            _feedItems[index] = updated
            db.collection("posts").document(postId).set(updated, SetOptions.merge())
        }
    }

    fun updateReaction(postId: String, emoji: String) {
        val index = _feedItems.indexOfFirst { it.id == postId }
        if (index != -1) {
            val post = _feedItems[index]
            val updated = post.copy(userReaction = emoji)
            _feedItems[index] = updated
            db.collection("posts").document(postId).set(updated, SetOptions.merge())
        }
    }

    override fun onCleared() {
        super.onCleared()
        firestoreListenerRegistration?.remove()
    }
}
