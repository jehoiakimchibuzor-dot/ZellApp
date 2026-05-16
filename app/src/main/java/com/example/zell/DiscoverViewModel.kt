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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * DiscoverViewModel - Manages the discovery feed (Pinterest style)
 * 🔧 REFACTORED: Integrated RetryHelper and Error handling (doneby Gemini)
 * 🔗 CONNECTED: RetryHelper.kt, DiscoverTab.kt
 */
class DiscoverViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val _discoverItems = mutableStateListOf<DiscoverItem>()
    val discoverItems: List<DiscoverItem> get() = _discoverItems

    var isLoading by mutableStateOf(true)
        private set

    var isLoadingMore by mutableStateOf(false)
        private set

    // 🔧 REFACTORED: Added error state for UI propagation (doneby Gemini)
    private val _error = mutableStateOf<AppError?>(null)
    val error: State<AppError?> = _error

    private var lastDocument: DocumentSnapshot? = null
    private var isLastPage = false

    init {
        fetchDiscoverItems()
    }

    /**
     * Initial fetch or refresh of discovery items
     * 🔧 REFACTORED: Converted to coroutines and wrapped with RetryHelper (doneby Gemini)
     */
    fun fetchDiscoverItems(isRefresh: Boolean = false) {
        if (isRefresh) {
            lastDocument = null
            isLastPage = false
            _discoverItems.clear()
        }
        
        isLoading = true
        _error.value = null
        
        viewModelScope.launch {
            try {
                // 🔧 REFACTORED: Using RetryHelper to handle transient Firestore issues (doneby Gemini)
                val snapshot = RetryHelper.firebaseRetry {
                    db.collection("discover")
                        .orderBy("id", Query.Direction.DESCENDING)
                        .limit(15)
                        .get()
                        .await()
                }

                val items = snapshot.documents.mapNotNull { it.toObject(DiscoverItem::class.java) }
                _discoverItems.addAll(items)
                if (snapshot.documents.isNotEmpty()) {
                    lastDocument = snapshot.documents.last()
                }
                CrashlyticsLogger.i("DiscoverViewModel", "Successfully fetched ${items.size} items")
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
                // Fallback to mock if empty
                if (_discoverItems.isEmpty()) {
                    _discoverItems.addAll(mockDiscover)
                }
                CrashlyticsLogger.e("DiscoverViewModel", "Fetch failed after retries", e)
            } finally {
                isLoading = false
            }
        }
    }

    /**
     * Paginates discovery items
     * 🔧 REFACTORED: Integrated RetryHelper for pagination (doneby Gemini)
     */
    fun loadMore() {
        if (isLoadingMore || isLastPage || lastDocument == null) return

        isLoadingMore = true
        viewModelScope.launch {
            try {
                // 🔧 REFACTORED: Wrapped pagination query with RetryHelper (doneby Gemini)
                val snapshot = RetryHelper.firebaseRetry {
                    db.collection("discover")
                        .orderBy("id", Query.Direction.DESCENDING)
                        .startAfter(lastDocument!!)
                        .limit(10)
                        .get()
                        .await()
                }

                if (snapshot.isEmpty) {
                    isLastPage = true
                } else {
                    val newItems = snapshot.documents.mapNotNull { it.toObject(DiscoverItem::class.java) }
                    _discoverItems.addAll(newItems)
                    lastDocument = snapshot.documents.last()
                }
            } catch (e: Exception) {
                _error.value = ErrorHandler.classifyException(e)
            } finally {
                isLoadingMore = false
            }
        }
    }

    // 🔧 REFACTORED: Added clearError helper (doneby Gemini)
    fun clearError() {
        _error.value = null
    }
}
