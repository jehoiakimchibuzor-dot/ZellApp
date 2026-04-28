package com.example.zell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
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
fun ComposePostScreen(
    onDismiss: () -> Unit,
    onPostSuccess: (String, List<Uri>, String?) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    val selectedMediaUris = remember { mutableStateListOf<Uri>() }
    var selectedMusicName by remember { mutableStateOf<String?>(null) }
    var isPollActive by remember { mutableStateOf(false) }
    var scheduledDate by remember { mutableStateOf<String?>(null) }
    
    val maxChars = 280
    val progress = text.length.toFloat() / maxChars
    
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        selectedMediaUris.addAll(uris)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Create Post", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                },
                actions = {
                    val isEnabled = text.isNotBlank() || selectedMediaUris.isNotEmpty()
                    val pulseScale by animateFloatAsState(
                        targetValue = if (isEnabled) 1.05f else 1f,
                        animationSpec = if (isEnabled) infiniteRepeatable(tween(1200), RepeatMode.Reverse) else snap(),
                        label = "post_pulse"
                    )

                    Button(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onPostSuccess(text, selectedMediaUris.toList(), selectedMusicName) 
                        },
                        enabled = isEnabled && text.length <= maxChars,
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(36.dp)
                            .scale(if (isEnabled) pulseScale else 1f)
                    ) {
                        if (scheduledDate != null) {
                            Icon(Icons.Default.Schedule, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Scheduled", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Text("Post", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                AsyncImage(
                    model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                    contentDescription = null,
                    modifier = Modifier.size(48.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f).animateContentSize()) {
                    BasicTextField(
                        value = text,
                        onValueChange = { if (it.length <= maxChars + 20) text = it },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        textStyle = TextStyle(
                            fontSize = 19.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            lineHeight = 28.sp
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            if (text.isEmpty()) {
                                Text("What's happening?", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), fontSize = 19.sp)
                            }
                            innerTextField()
                        }
                    )

                    // Autocomplete Mentions Mock
                    AnimatedVisibility(
                        visible = text.endsWith("@"),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        AutocompleteDropdown(
                            items = listOf(
                                "amara" to "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100",
                                "kofi" to "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100",
                                "zara" to "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=100"
                            ),
                            onSelect = { name -> text = text.dropLast(1) + "@$name " }
                        )
                    }

                    // Autocomplete Hashtags Mock
                    AnimatedVisibility(
                        visible = text.endsWith("#"),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        AutocompleteDropdown(
                            items = listOf(
                                "ZellApp" to "Trending · 12.5K posts",
                                "DesignThinking" to "Trending · 8.2K posts",
                                "Kotlin" to "5.1K posts"
                            ),
                            onSelect = { tag -> text = text.dropLast(1) + "#$tag " },
                            isHashtag = true
                        )
                    }

                    if (scheduledDate != null) {
                        SuggestionChip(
                            onClick = { scheduledDate = null },
                            label = { Text("Will post on $scheduledDate") },
                            icon = { Icon(Icons.Default.Schedule, null, modifier = Modifier.size(14.dp)) },
                            modifier = Modifier.padding(top = 12.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    if (selectedMusicName != null) {
                        Surface(
                            modifier = Modifier.padding(top = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.MusicNote, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text(selectedMusicName!!, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { selectedMusicName = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = isPollActive) {
                        Column(modifier = Modifier.padding(top = 20.dp).fillMaxWidth()) {
                            PollOptionField("Option 1")
                            Spacer(Modifier.height(10.dp))
                            PollOptionField("Option 2")
                        }
                    }

                    if (selectedMediaUris.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(selectedMediaUris) { uri ->
                                Box(modifier = Modifier.size(160.dp).clip(RoundedCornerShape(18.dp))) {
                                    AsyncImage(model = uri, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    IconButton(
                                        onClick = { selectedMediaUris.remove(uri) },
                                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.6f), CircleShape).size(28.dp)
                                    ) { Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp)) }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Character Counter & Toolbar
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        val color = when {
                            progress >= 1f -> Color.Red
                            progress >= 0.95f -> Color.Red
                            progress >= 0.8f -> Color(0xFFFFA500)
                            else -> MaterialTheme.colorScheme.primary
                        }
                        val shakeOffset by animateDpAsState(
                            targetValue = if (progress >= 1f) 2.dp else 0.dp,
                            animationSpec = if (progress >= 1f) infiniteRepeatable(tween(50), RepeatMode.Reverse) else snap(),
                            label = "shake"
                        )
                        CircularProgressIndicator(
                            progress = { progress.coerceIn(0f, 1f) },
                            modifier = Modifier.size(24.dp).offset(x = shakeOffset),
                            color = color,
                            strokeWidth = 2.5.dp,
                            trackColor = color.copy(alpha = 0.1f)
                        )
                        if (progress >= 0.85f) {
                            Text("${maxChars - text.length}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = color, fontWeight = FontWeight.Black)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ComposerToolButton(Icons.Outlined.Image, selectedMediaUris.isNotEmpty()) { mediaPickerLauncher.launch("image/*") }
                        ComposerToolButton(Icons.Outlined.Videocam, false) { }
                        ComposerToolButton(Icons.Outlined.Poll, isPollActive) { isPollActive = !isPollActive }
                        ComposerToolButton(Icons.Default.MusicNote, selectedMusicName != null) { selectedMusicName = "Midnight City - M83" }
                        ComposerToolButton(Icons.Outlined.Schedule, scheduledDate != null) { scheduledDate = "Tomorrow, 10:00 AM" }
                    }
                    
                    TextButton(onClick = { }, shape = RoundedCornerShape(12.dp)) {
                        Icon(Icons.Default.Public, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text("Public", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AutocompleteDropdown(items: List<Pair<String, String>>, onSelect: (String) -> Unit, isHashtag: Boolean = false) {
    Surface(
        modifier = Modifier.padding(top = 12.dp).fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            items.forEach { (title, subtitle) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(title) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isHashtag) {
                        Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                            Box(contentAlignment = Alignment.Center) { Text("#", fontWeight = FontWeight.Black) }
                        }
                    } else {
                        AsyncImage(model = subtitle, contentDescription = null, modifier = Modifier.size(32.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(if(isHashtag) "#$title" else "@$title", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        if (isHashtag) Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun PollOptionField(placeholder: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    ) {
        BasicTextField(
            value = "",
            onValueChange = {},
            modifier = Modifier.padding(14.dp),
            textStyle = TextStyle(fontSize = 15.sp),
            decorationBox = { inner ->
                if (true) Text(placeholder, color = Color.Gray, fontSize = 15.sp)
                inner()
            }
        )
    }
}

@Composable
fun ComposerToolButton(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.85f else 1f, label = "tool_scale")
    
    IconButton(
        onClick = { 
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick() 
        },
        interactionSource = interactionSource,
        modifier = Modifier.scale(scale)
    ) {
        Icon(
            icon, 
            null, 
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}
