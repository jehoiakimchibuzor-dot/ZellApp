package com.example.zell

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * ComposePostViewModel - Manages post creation and media uploads
 */
class ComposePostViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    var isPosting by mutableStateOf(false)
        private set

    /**
     * Uploads a new post with automatic retry logic
     */
    fun uploadPost(
        content: String,
        mediaUris: List<Uri>,
        currentUser: StoryUser,
        context: Context,
        audience: String = "PUBLIC",
        feeling: String? = null,
        location: String? = null,
        taggedPeople: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = auth.currentUser?.uid ?: return onError("User not logged in")
        isPosting = true

        viewModelScope.launch {
            try {
                // Upload to posts/ — matches storage rule: match /posts/{fileName}
                var imageUrl: String? = null
                if (mediaUris.isNotEmpty()) {
                    imageUrl = RetryHelper.firebaseRetry {
                        FirebaseUtils.uploadImage(context, mediaUris.first(), "posts")
                    }
                }

                val postId = UUID.randomUUID().toString()
                val newPost = FeedPost(
                    id        = postId,
                    author    = currentUser.name,
                    authorId  = uid,
                    handle    = "@${currentUser.name.lowercase().replace(" ", "")}",
                    avatarUrl = currentUser.avatarUrl,
                    content   = buildString {
                        append(content)
                        if (!feeling.isNullOrBlank())      append("\n— feeling $feeling")
                        if (!location.isNullOrBlank())     append("\n📍 $location")
                        if (!taggedPeople.isNullOrBlank()) append("\n👥 with $taggedPeople")
                    },
                    imageUrl  = imageUrl,
                    likes     = 0,
                    comments  = 0,
                    reposts   = 0
                )

                RetryHelper.firebaseRetry {
                    db.collection("posts").document(postId)
                        .set(newPost)
                        .await()
                }

                isPosting = false
                onSuccess()
            } catch (e: Exception) {
                isPosting = false
                val error = ErrorHandler.classifyException(e)
                onError(error.message) // This will now show the REAL cause
                CrashlyticsLogger.e("ComposePostViewModel", "Failed to upload post", e)
            }
        }
    }
}
