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
import java.util.UUID

/**
 * ViewModel for PostDetailScreen.
 * Handles fetching a single post, its comments, and posting new comments.
 */
class PostDetailViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _post = mutableStateOf<FeedPost?>(null)
    val post: State<FeedPost?> = _post

    private val _comments = mutableStateListOf<PostComment>()
    val comments: List<PostComment> get() = _comments

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _isSubmitting = mutableStateOf(false)
    val isSubmitting: State<Boolean> = _isSubmitting

    private val _error = mutableStateOf<AppError?>(null)
    val error: State<AppError?> = _error

    private var commentsListener: ListenerRegistration? = null

    fun loadPostDetails(postId: String) {
        if (postId.isBlank()) return
        
        _isLoading.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val postDoc = RetryHelper.firebaseRetry {
                    db.collection("posts").document(postId).get().await()
                }
                
                _post.value = postDoc.toObject(FeedPost::class.java)?.copy(id = postDoc.id)
                
                startCommentsListener(postId)
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("PostDetailVM", "Failed to load post details", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun startCommentsListener(postId: String) {
        commentsListener?.remove()
        // Real-time listener handles its own retries via Firestore SDK
        commentsListener = db.collection("posts").document(postId).collection("comments")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _error.value = ErrorHandler.classifyException(error)
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val newComments = snapshot.documents.mapNotNull { it.toObject(PostComment::class.java)?.copy(id = it.id) }
                    _comments.clear()
                    _comments.addAll(newComments)
                }
            }
    }

    fun addComment(postId: String, content: String, authorProfile: StoryUser) {
        if (content.isBlank()) return
        
        val userId = auth.currentUser?.uid ?: return
        _isSubmitting.value = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                val commentId = UUID.randomUUID().toString()
                val newComment = PostComment(
                    id = commentId,
                    author = authorProfile.name,
                    authorId = userId,
                    handle = "@${authorProfile.name.lowercase().replace(" ", "_")}",
                    avatarUrl = authorProfile.avatarUrl,
                    content = content,
                    timestamp = null // Firestore will set this
                )

                RetryHelper.firebaseRetry {
                    db.collection("posts").document(postId).collection("comments")
                        .document(commentId).set(newComment).await()
                    db.collection("posts").document(postId)
                        .update("comments", com.google.firebase.firestore.FieldValue.increment(1)).await()
                }
                
                CrashlyticsLogger.i("PostDetailVM", "Comment added successfully")
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("PostDetailVM", "Failed to add comment", e)
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        commentsListener?.remove()
    }
}
