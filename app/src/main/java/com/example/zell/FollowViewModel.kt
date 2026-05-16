package com.example.zell

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID


class FollowViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    // Map to track following status per user ID for instant UI updates
    val followingState = mutableStateMapOf<String, Boolean>()

    // Error state so the UI can show something went wrong
    private val _error = mutableStateOf<AppError?>(null)
    val error = _error

    /**
     * Follows/Unfollows a user and creates a real-time notification.
     * - Updates the UI instantly (optimistic update)
     * - If Firestore fails, rolls back the UI and shows an error
     */
    fun toggleFollow(targetUserId: String, targetUserName: String) {
        if (currentUserId.isEmpty() || targetUserId == currentUserId) return

        val isFollowing = followingState[targetUserId] ?: false
        val newStatus = !isFollowing

        // Step 1: Update the UI immediately so it feels instant
        followingState[targetUserId] = newStatus

        viewModelScope.launch {
            try {
                val followingRef = db.collection("users")
                    .document(currentUserId)
                    .collection("following")
                    .document(targetUserId)

                val followerRef = db.collection("users")
                    .document(targetUserId)
                    .collection("followers")
                    .document(currentUserId)

                if (newStatus) {
                    // Step 2a: Save follow to Firestore — wait for confirmation
                    RetryHelper.firebaseRetry {
                        followingRef.set(mapOf("timestamp" to System.currentTimeMillis())).await()
                    }
                    RetryHelper.firebaseRetry {
                        followerRef.set(mapOf("timestamp" to System.currentTimeMillis())).await()
                    }

                    // Step 2b: Send a notification to the user being followed
                    val notificationId = UUID.randomUUID().toString()
                    val notification = ZellNotification(
                        id = notificationId,
                        userName = auth.currentUser?.displayName ?: "A user",
                        actionText = "started following you",
                        timestamp = "Just now",
                        type = NotificationType.FOLLOW,
                        isRead = false,
                        avatarUrl = auth.currentUser?.photoUrl?.toString()
                    )
                    RetryHelper.firebaseRetry {
                        db.collection("users")
                            .document(targetUserId)
                            .collection("notifications")
                            .document(notificationId)
                            .set(notification)
                            .await()
                    }
                } else {
                    // Step 2c: Remove follow from Firestore — wait for confirmation
                    RetryHelper.firebaseRetry {
                        followingRef.delete().await()
                    }
                    RetryHelper.firebaseRetry {
                        followerRef.delete().await()
                    }
                }

            } catch (e: Exception) {
                // Step 3: Something went wrong — roll back the UI to what it was before
                followingState[targetUserId] = isFollowing
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("FollowViewModel", "toggleFollow failed for $targetUserId", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
