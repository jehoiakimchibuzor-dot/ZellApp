package com.example.zell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * CreateSpaceScreen - UI for starting new conversations or groups
 * 🔧 REFACTORED: Improved responsiveness and theme consistency (doneby Gemini)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSpaceScreen(
    onBack: () -> Unit,
    onContactClick: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Mock contacts for UI development
    val contacts = remember {
        listOf(
            StoryUser("1", "Sarah Okonkwo", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200"),
            StoryUser("2", "Marcus Obi", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200"),
            StoryUser("3", "Lena Williams", "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200"),
            StoryUser("4", "James Obi", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200"),
            StoryUser("10", "Tunde Sanusi", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200"),
            StoryUser("11", "Chinelo A.", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200"),
            StoryUser("12", "Ibrahim K.", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200")
        )
    }

    val filteredContacts = contacts.filter { 
        it.name.contains(searchQuery, ignoreCase = true) 
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Message", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search people...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = MaterialTheme.colorScheme.primary
                )
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                // Action Items
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* Create Group Logic */ }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.GroupAdd, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Create a Group", 
                            fontWeight = FontWeight.Bold, 
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                item {
                    Text(
                        "Suggested",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Contacts List
                items(filteredContacts) { user ->
                    ContactItem(user) { onContactClick(user.id) }
                }
            }
        }
    }
}

@Composable
fun ContactItem(user: StoryUser, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                user.name, 
                fontWeight = FontWeight.SemiBold, 
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "@${user.name.lowercase().replace(" ", "")}", 
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                fontSize = 13.sp
            )
        }
    }
}
