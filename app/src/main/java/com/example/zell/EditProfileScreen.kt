package com.example.zell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    initialUser: StoryUser,
    onBack: () -> Unit,
    onSave: (String, String, Uri?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf(initialUser.name) }
    var bio by remember { mutableStateOf(initialUser.bio) }
    var location by remember { mutableStateOf(initialUser.location) }
    var website by remember { mutableStateOf(initialUser.website) }
    
    var headerUri by remember { mutableStateOf<Uri?>(null) }
    var avatarUri by remember { mutableStateOf<Uri?>(null) }

    val hasChanges = name != initialUser.name || bio != initialUser.bio || 
                     location != initialUser.location || website != initialUser.website || 
                     headerUri != null || avatarUri != null

    val headerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { headerUri = it }
    val avatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { avatarUri = it }

    var isSaving by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    val pulseScale by animateFloatAsState(
                        targetValue = if (hasChanges && !isSaving) 1.1f else 1f,
                        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
                        label = "save_pulse"
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                isSaving = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                delay(1500)
                                isSaving = false
                                onSave(name, bio, avatarUri)
                            }
                        },
                        enabled = !isSaving,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (hasChanges) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (hasChanges) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(36.dp)
                            .scale(if (hasChanges && !isSaving) pulseScale else 1f)
                    ) {
                        AnimatedContent(targetState = isSaving, label = "save_btn") { saving ->
                            if (saving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            // Header & Avatar Edit
            Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                AsyncImage(
                    model = headerUri ?: "https://images.unsplash.com/photo-1558591710-4b4a1ae0f04d?w=800",
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clickable { headerLauncher.launch("image/*") },
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                        .clickable { headerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Surface(color = Color.Black.copy(alpha = 0.4f), shape = CircleShape) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.padding(12.dp).size(24.dp))
                    }
                }

                // Avatar
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 20.dp, bottom = (-44).dp)
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.background)
                        .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                        .clickable { avatarLauncher.launch("image/*") }
                ) {
                    AsyncImage(
                        model = avatarUri ?: initialUser.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }
            }

            Spacer(Modifier.height(64.dp))

            // Fields
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                EditField(label = "Name", value = name, onValueChange = { name = it })
                EditField(label = "Bio", value = bio, onValueChange = { bio = it }, singleLine = false)
                EditField(label = "Location", value = location, onValueChange = { location = it })
                EditField(label = "Website", value = website, onValueChange = { website = it })
            }
            
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
fun EditField(label: String, value: String, onValueChange: (String) -> Unit, singleLine: Boolean = true) {
    var isFocused by remember { mutableStateOf(false) }
    val indicatorColor by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        label = "indicator"
    )
    val labelColor by animateColorAsState(
        if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "label"
    )

    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Text(label, fontSize = 13.sp, color = labelColor, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused },
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            singleLine = singleLine,
            textStyle = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Medium)
        )
    }
}
