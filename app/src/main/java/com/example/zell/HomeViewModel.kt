package com.example.zell

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class HomeViewModel : ViewModel() {

    // This is the "Source of Truth" for your feed
    private val _feedItems = mutableStateListOf<FeedPost>()
    val feedItems: List<FeedPost> get() = _feedItems

    private val _storyItems = mutableStateListOf<StoryUser>()
    val storyItems: List<StoryUser> get() = _storyItems

    var isLoadingMore = false
        private set

    init {
        // Initialize with your starting mock data
        loadInitialData()
    }

    private fun loadInitialData() {
        _storyItems.addAll(mockStories.toList())
        _feedItems.addAll(mockFeed.toList())
    }

    fun toggleLike(postId: String) {
        val index = _feedItems.indexOfFirst { it.id == postId }
        if (index != -1) {
            val post = _feedItems[index]
            val updatedPost = post.copy(
                isLiked = !post.isLiked,
                likes = if (post.isLiked) post.likes - 1 else post.likes + 1
            )
            _feedItems[index] = updatedPost
        }
    }

    fun toggleBookmark(postId: String) {
        val index = _feedItems.indexOfFirst { it.id == postId }
        if (index != -1) {
            val post = _feedItems[index]
            _feedItems[index] = post.copy(isBookmarked = !post.isBookmarked)
        }
    }

    fun loadMorePosts() {
        if (isLoadingMore) return
        
        isLoadingMore = true
        viewModelScope.launch {
            delay(1500) // Simulate network delay
            val newPosts = _feedItems.take(4).map { 
                it.copy(id = UUID.randomUUID().toString(), timeAgo = "new") 
            }
            _feedItems.addAll(newPosts)
            isLoadingMore = false
        }
    }
}
