package com.example.zell

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * UserViewModel - Manages current user profile and authentication state
 */
class UserViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val _currentUserProfile = mutableStateOf(createGuestProfile())
    val currentUserProfile: State<StoryUser> = _currentUserProfile

    private val _error = mutableStateOf<AppError?>(null)
    val error: State<AppError?> = _error

    private var profileListener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        monitorAuthState()
    }

    private fun monitorAuthState() {
        authListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                startProfileListener(user.uid)
            } else {
                stopProfileListener()
                _currentUserProfile.value = createGuestProfile()
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    private fun startProfileListener(uid: String) {
        profileListener?.remove()
        profileListener = db.collection("users").document(uid).addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null) {
                snapshot.toObject(StoryUser::class.java)?.let {
                    _currentUserProfile.value = it.copy(id = uid, isYou = true)
                }
            }
        }
    }

    private fun stopProfileListener() {
        profileListener?.remove()
        profileListener = null
    }

    private fun createGuestProfile() = StoryUser(
        id = UUID.randomUUID().toString(),
        name = "Guest",
        avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
        stories = emptyList(),
        bio = "",
        hasUnread = false,
        isYou = true
    )

    /**
     * Updates the user profile with automatic retry logic.
     */
    fun updateProfile(
        name: String,
        bio: String,
        location: String,
        website: String,
        about: String? = null,
        skills: String? = null,
        institution: String? = null,
        themeColor: String? = null,
        avatarUri: Uri? = null,
        context: Context? = null,
        // onComplete is called on the MAIN thread after save (true = success, false = failed)
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val uid = auth.currentUser?.uid ?: run { onComplete?.invoke(false); return }

        viewModelScope.launch {
            try {
                var finalAvatarUrl = _currentUserProfile.value.avatarUrl

                // 1. Upload avatar if a new one was chosen
                if (avatarUri != null && context != null) {
                    finalAvatarUrl = RetryHelper.firebaseRetry {
                        FirebaseUtils.uploadImage(context, avatarUri, "avatars/$uid")
                    }
                }

                // 2. Build the full update map — always write every field so
                //    clearing a value (e.g. removing the institution) works too
                val updates: Map<String, Any?> = mapOf(
                    "name"        to name,
                    "bio"         to bio,
                    "location"    to location,
                    "website"     to website,
                    "avatarUrl"   to finalAvatarUrl,
                    "about"       to (about?.ifBlank { null }),
                    "skills"      to (skills?.ifBlank { null }),
                    "institution" to (institution?.ifBlank { null }),
                    "themeColor"  to (themeColor?.ifBlank { null })
                ).filterValues { it != null }
                    .mapValues { it.value as Any }

                RetryHelper.firebaseRetry {
                    db.collection("users").document(uid)
                        .set(updates, SetOptions.merge())
                        .await()
                }

                CrashlyticsLogger.i("UserViewModel", "Profile updated for $uid")
                onComplete?.invoke(true)
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("UserViewModel", "Profile update failed", e)
                onComplete?.invoke(false)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun updateProfileLocally(updated: StoryUser) {
        _currentUserProfile.value = updated
    }

    /**
     * Deletes account with automatic retry for DB cleanup.
     */
    fun deleteUserAccount(onComplete: () -> Unit) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        
        viewModelScope.launch {
            try {
                RetryHelper.firebaseRetry {
                    db.collection("users").document(uid).delete().await()
                }
                user.delete().await()
                onComplete()
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopProfileListener()
        authListener?.let { auth.removeAuthStateListener(it) }
        authListener = null
    }
}
