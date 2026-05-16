package com.example.zell

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/**
 * SearchScreen - Find friends and creators
 * 🔧 REFACTORED: Added error handling for search failures (doneby Gemini)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onPostClick: (String) -> Unit = {},
    searchViewModel: SearchViewModel = viewModel()
) {
    val searchQuery = searchViewModel.searchQuery
    val searchResults = searchViewModel.searchResults
    val isSearching = searchViewModel.isSearching
    // 🔧 REFACTORED: Hooked into the now-exposed error state (doneby Gemini)
    val searchError by searchViewModel.error

    Scaffold(
        topBar = {
            Surface(tonalElevation = 4.dp) {
                Row(
                    modifier = Modifier.statusBarsPadding().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchViewModel.onSearchQueryChange(it) },
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) Text("Search users...", color = Color.Gray)
                                    inner()
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 🔧 REFACTORED: Display search errors if they occur using global banner (doneby Gemini)
            if (searchError != null) {
                ErrorBanner(error = searchError, onDismiss = { searchViewModel.clearError() })
            }

            Box(modifier = Modifier.weight(1f)) {
                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                } else if (searchQuery.isEmpty()) {
                    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Search Zell", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Find friends and creators by their name.", color = Color.Gray, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                } else if (searchResults.isEmpty()) {
                    Text("No users found for \"$searchQuery\"", modifier = Modifier.align(Alignment.Center), color = Color.Gray)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(searchResults) { user ->
                            SuggestedPersonItem(user = user, onClick = onUserClick)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestedPersonItem(user: StoryUser, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(user.id) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(CircleShape).border(1.dp, Color.LightGray, CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text("@${user.name.lowercase().replace(" ", "")}", color = Color.Gray, fontSize = 14.sp)
        }
        Button(
            onClick = { /* Follow Logic */ },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text("Follow", fontSize = 13.sp)
        }
    }
}
