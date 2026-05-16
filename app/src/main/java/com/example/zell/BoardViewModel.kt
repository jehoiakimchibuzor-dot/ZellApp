package com.example.zell

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class BoardViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    private val _boards = mutableStateListOf<PinBoard>()
    val boards: List<PinBoard> get() = _boards

    // Error state so the UI can show something went wrong
    private val _error = mutableStateOf<AppError?>(null)
    val error = _error

    private var boardsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        if (currentUserId.isNotEmpty()) {
            fetchUserBoards()
        }
    }

    private fun fetchUserBoards() {
        boardsListener?.remove()

        boardsListener = db.collection("users").document(currentUserId).collection("boards")
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error == null && value != null) {
                    val boardList = value.toObjects(PinBoard::class.java)
                    _boards.clear()
                    _boards.addAll(boardList)
                }
            }
    }

    fun createBoard(name: String, isSecret: Boolean = false, category: String = "") {
        if (currentUserId.isEmpty()) return

        val boardId = UUID.randomUUID().toString()
        val newBoard = PinBoard(
            id = boardId,
            name = name,
            ownerId = currentUserId,
            isSecret = isSecret,
            category = category
        )

        viewModelScope.launch {
            try {
                RetryHelper.firebaseRetry {
                    db.collection("users")
                        .document(currentUserId)
                        .collection("boards")
                        .document(boardId)
                        .set(newBoard)
                        .await()
                }
                CrashlyticsLogger.i("BoardViewModel", "Board '$name' created successfully")
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("BoardViewModel", "Failed to create board '$name'", e)
            }
        }
    }

    fun savePinToBoard(boardId: String, pinId: String, pinImageUrl: String) {
        if (currentUserId.isEmpty()) return

        viewModelScope.launch {
            try {
                val boardRef = db.collection("users")
                    .document(currentUserId)
                    .collection("boards")
                    .document(boardId)

                // Step 1: Save the pin — wait for it to fully complete before moving on
                RetryHelper.firebaseRetry {
                    boardRef.collection("pins")
                        .document(pinId)
                        .set(mapOf("timestamp" to System.currentTimeMillis()))
                        .await()
                }

                // Step 2: Now that Step 1 is confirmed done, fetch the board and update thumbnails
                val snapshot = RetryHelper.firebaseRetry {
                    boardRef.get().await()
                }

                val board = snapshot.toObject(PinBoard::class.java) ?: return@launch
                val newThumbnails = (listOf(pinImageUrl) + board.thumbnails).take(3)

                RetryHelper.firebaseRetry {
                    boardRef.update(
                        "pinCount", board.pinCount + 1,
                        "thumbnails", newThumbnails
                    ).await()
                }

                CrashlyticsLogger.i("BoardViewModel", "Pin $pinId saved to board $boardId")
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("BoardViewModel", "Failed to save pin $pinId to board $boardId", e)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        boardsListener?.remove()
        boardsListener = null
    }
}
