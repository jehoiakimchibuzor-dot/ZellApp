package com.example.zell

import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * SearchViewModel - Handles user search logic
 * 🔧 REFACTORED: Integrated RetryHelper for robust searching
 * 🔗 CONNECTED: RetryHelper.kt
 */
class SearchViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    var searchQuery by mutableStateOf("")
        private set

    private val _searchResults = mutableStateListOf<StoryUser>()
    val searchResults: List<StoryUser> get() = _searchResults

    var isSearching by mutableStateOf(false)
        private set

    // 🔧 REFACTORED: Added error state for UI feedback (doneby Gemini)
    private val _error = mutableStateOf<AppError?>(null)
    val error: State<AppError?> = _error

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
        if (newQuery.length >= 2) {
            performSearch(newQuery)
        } else {
            _searchResults.clear()
            _error.value = null
        }
    }

    /**
     * Performs user search with automatic retry logic
     * 🔧 REFACTORED: Converted to coroutines and wrapped with RetryHelper.firebaseRetry
     */
    private fun performSearch(query: String) {
        isSearching = true
        _error.value = null
        viewModelScope.launch {
            try {
                // 🔧 REFACTORED: Using RetryHelper to handle network flakes during search
                val snapshot = RetryHelper.firebaseRetry {
                    db.collection("users")
                        .whereGreaterThanOrEqualTo("name", query)
                        .whereLessThanOrEqualTo("name", query + "\uf8ff")
                        .limit(10)
                        .get()
                        .await()
                }
                
                val users = snapshot.documents.mapNotNull { it.toObject(StoryUser::class.java) }
                _searchResults.clear()
                _searchResults.addAll(users)
                CrashlyticsLogger.i("SearchViewModel", "Search successful for: $query")
            } catch (e: Exception) {
                // 🔧 REFACTORED: Propagate search error to UI (doneby Gemini)
                _error.value = ErrorHandler.classifyException(e)
                CrashlyticsLogger.e("SearchViewModel", "Search failed after retries", e)
            } finally {
                isSearching = false
            }
        }
    }

    // 🔧 REFACTORED: Added clearError helper (doneby Gemini)
    fun clearError() {
        _error.value = null
    }
}
