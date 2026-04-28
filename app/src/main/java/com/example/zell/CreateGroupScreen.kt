package com.example.zell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateGroupScreen(
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    var groupName by remember { mutableStateOf("") }
    var groupDescription by remember { mutableStateOf("") }
    var groupIconUri by remember { mutableStateOf<Uri?>(null) }
    val selectedParticipants = remember { mutableStateListOf<StoryUser>() }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { groupIconUri = it }

    val contacts = remember {
        listOf(
            StoryUser("1", "Sarah Okonkwo", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200"),
            StoryUser("2", "Marcus Obi", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200"),
            StoryUser("3", "Lena Williams", "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=200"),
            StoryUser("4", "James Obi", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200")
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("New Group", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { if (groupName.isNotBlank()) onGroupCreated(groupName) },
                        enabled = groupName.isNotBlank() && selectedParticipants.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Create", tint = if (groupName.isNotBlank() && selectedParticipants.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Group Icon Picker
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { photoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (groupIconUri != null) {
                            AsyncImage(
                                model = groupIconUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(32.dp), tint = Color.Gray)
                        }
                    }
                    
                    Spacer(Modifier.height(24.dp))

                    TextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        placeholder = { Text("Group Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )
                    
                    Spacer(Modifier.height(12.dp))

                    TextField(
                        value = groupDescription,
                        onValueChange = { groupDescription = it },
                        placeholder = { Text("Group Description (Optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        )
                    )
                }
            }

            item {
                Text(
                    "Add Participants (${selectedParticipants.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            items(contacts) { user ->
                val isSelected = selectedParticipants.contains(user)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) selectedParticipants.remove(user)
                            else selectedParticipants.add(user)
                        }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = user.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(16.dp))
                    Text(user.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = {
                            if (it) selectedParticipants.add(user)
                            else selectedParticipants.remove(user)
                        }
                    )
                }
            }
        }
    }
}
