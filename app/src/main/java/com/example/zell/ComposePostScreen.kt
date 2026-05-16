package com.example.zell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ── Audience options ──────────────────────────────────────────────────────────

private enum class Audience(
    val label: String,
    val icon: ImageVector,
    val color: Color
) {
    PUBLIC("Public",      Icons.Default.Public,      Color(0xFF1877F2)),
    FRIENDS("Friends",    Icons.Default.Group,        Color(0xFF45BD62)),
    ONLY_ME("Only me",   Icons.Default.Lock,         Color(0xFF808080))
}

// ── Screen ────────────────────────────────────────────────────────────────────

/**
 * ComposePostScreen — Facebook-style post composer.
 *
 * Features:
 *  - Audience picker (Public / Friends / Only me)
 *  - Feeling / Activity tag
 *  - Multiple photo picker with removable grid
 *  - GIF via URL paste dialog
 *  - Location, Tag people (UI ready — extend with APIs)
 *  - Live character counter
 *  - Proper loading state while uploading
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposePostScreen(
    onDismiss: () -> Unit,
    composePostViewModel: ComposePostViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val currentUser     by userViewModel.currentUserProfile
    val context         = LocalContext.current
    val scope           = rememberCoroutineScope()
    val focusRequester  = remember { FocusRequester() }

    // ── Post state ────────────────────────────────────────────────────────────
    var text             by remember { mutableStateOf("") }
    var selectedAudience by remember { mutableStateOf(Audience.PUBLIC) }
    var feeling          by remember { mutableStateOf<String?>(null) }
    var location         by remember { mutableStateOf<String?>(null) }
    var taggedPeople     by remember { mutableStateOf<String?>(null) }
    val selectedMedia    = remember { mutableStateListOf<Uri>() }
    var submissionError  by remember { mutableStateOf<String?>(null) }
    // index of the slot currently being replaced (-1 = adding new)
    var replaceIndex     by remember { mutableStateOf(-1) }

    // ── Dialog state ──────────────────────────────────────────────────────────
    var showAudiencePicker  by remember { mutableStateOf(false) }
    var showFeelingPicker   by remember { mutableStateOf(false) }
    var showGifDialog       by remember { mutableStateOf(false) }
    var showLocationDialog  by remember { mutableStateOf(false) }
    var showTagDialog       by remember { mutableStateOf(false) }

    // ── Gallery pickers ───────────────────────────────────────────────────────
    // Multi-pick for adding new photos
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (replaceIndex >= 0 && uris.isNotEmpty()) {
            // Replace a specific slot
            if (replaceIndex < selectedMedia.size) selectedMedia[replaceIndex] = uris.first()
            replaceIndex = -1
        } else {
            selectedMedia.addAll(uris.take(10 - selectedMedia.size))
        }
    }

    // Auto-focus text field
    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    val canPost = (text.isNotBlank() || selectedMedia.isNotEmpty()) &&
                  !composePostViewModel.isPosting

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            // ── Top bar ───────────────────────────────────────────────────────
            Column {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                    Text(
                        "Create post",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        modifier   = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    // POST button
                    Button(
                        onClick = {
                            submissionError = null
                            composePostViewModel.uploadPost(
                                content      = text,
                                mediaUris    = selectedMedia.toList(),
                                currentUser  = currentUser,
                                context      = context,
                                audience     = selectedAudience.name,
                                feeling      = feeling,
                                location     = location,
                                taggedPeople = taggedPeople,
                                onSuccess    = { onDismiss() },
                                onError      = { submissionError = it }
                            )
                        },
                        enabled = canPost,
                        shape   = RoundedCornerShape(20.dp),
                        colors  = ButtonDefaults.buttonColors(
                            containerColor        = MaterialTheme.colorScheme.primary,
                            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        if (composePostViewModel.isPosting) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(16.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Posting…", color = Color.White)
                        } else {
                            Text("Post", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
            }
        },
        bottomBar = {
            // ── Toolbar ───────────────────────────────────────────────────────
            Column {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Surface(color = MaterialTheme.colorScheme.surface) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Photo / Video
                        ComposeToolItem(
                            icon  = Icons.Default.Image,
                            label = "Photo",
                            color = Color(0xFF45BD62),
                            onClick = { galleryLauncher.launch("image/*") }
                        )
                        // GIF
                        ComposeToolItem(
                            icon  = Icons.Default.Gif,
                            label = "GIF",
                            color = Color(0xFF1877F2),
                            onClick = { showGifDialog = true }
                        )
                        // Feeling / Activity
                        ComposeToolItem(
                            icon  = Icons.Default.EmojiEmotions,
                            label = "Feeling",
                            color = Color(0xFFF7B928),
                            onClick = { showFeelingPicker = true }
                        )
                        // Tag people
                        ComposeToolItem(
                            icon      = Icons.Default.PersonAdd,
                            label     = if (taggedPeople != null) "Tagged" else "Tag",
                            color     = if (taggedPeople != null) Color(0xFF45BD62) else Color(0xFF1877F2),
                            onClick   = { showTagDialog = true }
                        )
                        // Check in / Location
                        ComposeToolItem(
                            icon      = Icons.Default.LocationOn,
                            label     = if (location != null) "Located" else "Location",
                            color     = if (location != null) Color(0xFF45BD62) else Color(0xFFE53935),
                            onClick   = { showLocationDialog = true }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Error ─────────────────────────────────────────────────────────
            if (submissionError != null) {
                ErrorBanner(
                    error     = AppError(type = ErrorType.UNKNOWN, message = submissionError!!),
                    onDismiss = { submissionError = null }
                )
            }

            // ── Author row ────────────────────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncImage(
                    model              = currentUser.avatarUrl,
                    contentDescription = null,
                    modifier           = Modifier.size(48.dp).clip(CircleShape),
                    contentScale       = ContentScale.Crop
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Author name + feeling tag
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            currentUser.name.ifBlank { "You" },
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp
                        )
                        if (feeling != null) {
                            Text(
                                " — feeling $feeling",
                                fontSize = 14.sp,
                                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        if (location != null) {
                            Text(
                                " 📍 $location",
                                fontSize = 14.sp,
                                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                        if (taggedPeople != null) {
                            Text(
                                " — with $taggedPeople",
                                fontSize = 14.sp,
                                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                    // Audience pill
                    Surface(
                        shape   = RoundedCornerShape(6.dp),
                        color   = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { showAudiencePicker = true }
                    ) {
                        Row(
                            modifier          = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                selectedAudience.icon,
                                contentDescription = null,
                                tint     = selectedAudience.color,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                selectedAudience.label,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }

            // ── Text input ────────────────────────────────────────────────────
            val textSize = when {
                text.length < 50 && selectedMedia.isEmpty() -> 24.sp
                text.length < 120 && selectedMedia.isEmpty() -> 18.sp
                else -> 15.sp
            }

            BasicTextField(
                value     = text,
                onValueChange = { if (it.length <= 1000) text = it },
                modifier  = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 120.dp)
                    .focusRequester(focusRequester)
                    .padding(horizontal = 16.dp),
                textStyle = TextStyle(
                    fontSize = textSize,
                    color    = MaterialTheme.colorScheme.onSurface,
                    lineHeight = (textSize.value * 1.4f).sp
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    if (text.isEmpty()) {
                        Text(
                            "What's on your mind, ${currentUser.name.split(" ").first().ifBlank { "friend" }}?",
                            fontSize = textSize,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            lineHeight = (textSize.value * 1.4f).sp
                        )
                    }
                    inner()
                }
            )

            // Character counter (appears at 800+)
            if (text.length >= 800) {
                Text(
                    "${1000 - text.length} remaining",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    fontSize = 12.sp,
                    color    = if (text.length > 950) MaterialTheme.colorScheme.error
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    fontWeight = FontWeight.Medium
                )
            }

            // ── Media grid ───────────────────────────────────────────────────
            if (selectedMedia.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                MediaGrid(
                    uris      = selectedMedia,
                    onRemove  = { uri -> selectedMedia.remove(uri) },
                    onReplace = { idx ->
                        replaceIndex = idx
                        galleryLauncher.launch("image/*")
                    },
                    onAdd     = { galleryLauncher.launch("image/*") }
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // ── Audience picker dialog ────────────────────────────────────────────────
    if (showAudiencePicker) {
        AlertDialog(
            onDismissRequest = { showAudiencePicker = false },
            title            = { Text("Who can see your post?", fontWeight = FontWeight.Bold) },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Audience.entries.forEach { option ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedAudience = option
                                    showAudiencePicker = false
                                },
                            color = if (selectedAudience == option)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else Color.Transparent
                        ) {
                            Row(
                                modifier          = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(option.color, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(option.icon, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                                Column {
                                    Text(option.label, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        when (option) {
                                            Audience.PUBLIC  -> "Anyone on Zell"
                                            Audience.FRIENDS -> "Your connections"
                                            Audience.ONLY_ME -> "Only visible to you"
                                        },
                                        fontSize = 12.sp,
                                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                if (selectedAudience == option) {
                                    Spacer(Modifier.weight(1f))
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }

    // ── Feeling picker dialog ─────────────────────────────────────────────────
    if (showFeelingPicker) {
        val feelings = listOf(
            "😊 happy", "😍 loved", "😎 cool", "😢 sad", "😠 angry",
            "😴 tired", "🤩 excited", "😌 blessed", "💪 motivated", "🙏 grateful",
            "🥳 celebratory", "😤 frustrated", "🤔 thoughtful", "😂 amused", "🥰 thankful"
        )
        AlertDialog(
            onDismissRequest = { showFeelingPicker = false },
            title            = { Text("How are you feeling?", fontWeight = FontWeight.Bold) },
            text             = {
                Column {
                    feelings.chunked(3).forEach { row ->
                        Row(
                            modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { f ->
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            feeling = f
                                            showFeelingPicker = false
                                        },
                                    color = if (feeling == f) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        f,
                                        modifier  = Modifier.padding(8.dp),
                                        fontSize  = 12.sp,
                                        maxLines  = 1,
                                        overflow  = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // fill empty slots so the grid stays even
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    if (feeling != null) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = { feeling = null; showFeelingPicker = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove feeling", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showFeelingPicker = false }) { Text("Done") }
            }
        )
    }

    // ── Location dialog ───────────────────────────────────────────────────────
    if (showLocationDialog) {
        var locationInput by remember { mutableStateOf(location ?: "") }
        AlertDialog(
            onDismissRequest = { showLocationDialog = false },
            title = { Text("Where are you?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value         = locationInput,
                        onValueChange = { locationInput = it },
                        placeholder   = { Text("City, place or address…") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        leadingIcon   = {
                            Icon(Icons.Default.LocationOn, null, tint = Color(0xFFE53935))
                        }
                    )
                    if (location != null) {
                        TextButton(
                            onClick  = { location = null; showLocationDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove location", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick  = {
                        location = locationInput.trim().ifBlank { null }
                        showLocationDialog = false
                    },
                    shape    = RoundedCornerShape(20.dp)
                ) { Text("Add") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── Tag people dialog ─────────────────────────────────────────────────────
    if (showTagDialog) {
        var tagInput by remember { mutableStateOf(taggedPeople ?: "") }
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Tag someone", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Type a name or @username",
                        fontSize = 13.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    OutlinedTextField(
                        value         = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder   = { Text("e.g. John Doe or @johndoe") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp),
                        leadingIcon   = {
                            Icon(Icons.Default.PersonAdd, null, tint = Color(0xFF1877F2))
                        }
                    )
                    if (taggedPeople != null) {
                        TextButton(
                            onClick  = { taggedPeople = null; showTagDialog = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Remove tag", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick  = {
                        taggedPeople = tagInput.trim().ifBlank { null }
                        showTagDialog = false
                    },
                    shape    = RoundedCornerShape(20.dp)
                ) { Text("Tag") }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ── GIF URL dialog ────────────────────────────────────────────────────────
    if (showGifDialog) {
        var gifInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showGifDialog = false; gifInput = "" },
            title            = { Text("Add a GIF", fontWeight = FontWeight.Bold) },
            text             = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Paste a GIF URL from Giphy, Tenor, or any direct .gif link:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    OutlinedTextField(
                        value         = gifInput,
                        onValueChange = { gifInput = it },
                        placeholder   = { Text("https://media.giphy.com/...") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        shape         = RoundedCornerShape(12.dp)
                    )
                    // Preview if it looks like a URL
                    if (gifInput.startsWith("http") && (gifInput.endsWith(".gif") || gifInput.contains("giphy") || gifInput.contains("tenor"))) {
                        AsyncImage(
                            model              = gifInput,
                            contentDescription = "GIF preview",
                            modifier           = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale       = ContentScale.Crop
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (gifInput.isNotBlank()) {
                            selectedMedia.add(Uri.parse(gifInput))
                        }
                        showGifDialog = false
                        gifInput = ""
                    },
                    enabled = gifInput.isNotBlank()
                ) { Text("Add GIF") }
            },
            dismissButton = {
                TextButton(onClick = { showGifDialog = false; gifInput = "" }) { Text("Cancel") }
            }
        )
    }
}

// ── Media grid composable ─────────────────────────────────────────────────────

@Composable
private fun MediaGrid(
    uris: List<Uri>,
    onRemove: (Uri) -> Unit,
    onReplace: (Int) -> Unit,
    onAdd: () -> Unit
) {
    val gridHeight = when (uris.size) {
        1    -> 300.dp
        2    -> 200.dp
        else -> 280.dp
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight)
    ) {
        when (uris.size) {
            1 -> MediaTile(
                    uri       = uris[0],
                    onRemove  = { onRemove(uris[0]) },
                    onReplace = { onReplace(0) },
                    modifier  = Modifier.fillMaxSize()
                 )

            2 -> Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    uris.forEachIndexed { i, uri ->
                        MediaTile(
                            uri       = uri,
                            onRemove  = { onRemove(uri) },
                            onReplace = { onReplace(i) },
                            modifier  = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }

            else -> {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(Modifier.weight(1.5f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        MediaTile(uri = uris[0], onRemove = { onRemove(uris[0]) }, onReplace = { onReplace(0) }, modifier = Modifier.weight(2f).fillMaxHeight())
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (uris.size > 1)
                                MediaTile(uri = uris[1], onRemove = { onRemove(uris[1]) }, onReplace = { onReplace(1) }, modifier = Modifier.weight(1f).fillMaxWidth())
                            if (uris.size > 2)
                                MediaTile(uri = uris[2], onRemove = { onRemove(uris[2]) }, onReplace = { onReplace(2) }, modifier = Modifier.weight(1f).fillMaxWidth())
                        }
                    }
                    if (uris.size > 3) {
                        Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            uris.drop(3).take(3).forEachIndexed { i, uri ->
                                MediaTile(uri = uri, onRemove = { onRemove(uri) }, onReplace = { onReplace(i + 3) }, modifier = Modifier.weight(1f).fillMaxHeight())
                            }
                            if (uris.size < 10) {
                                AddMoreTile(modifier = Modifier.weight(1f).fillMaxHeight(), onClick = onAdd)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MediaTile(uri: Uri, onRemove: () -> Unit, onReplace: () -> Unit, modifier: Modifier) {
    Box(modifier = modifier) {
        AsyncImage(
            model              = uri,
            contentDescription = null,
            modifier           = Modifier.fillMaxSize(),
            contentScale       = ContentScale.Crop
        )
        // ❌ Remove button — top end
        IconButton(
            onClick  = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
        ) {
            Icon(Icons.Default.Close, "Remove", tint = Color.White, modifier = Modifier.size(15.dp))
        }
        // ✏️ Edit / Replace button — top start
        IconButton(
            onClick  = onReplace,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(4.dp)
                .size(28.dp)
                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
        ) {
            Icon(Icons.Default.Edit, "Replace image", tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun AddMoreTile(modifier: Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color    = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.AddPhotoAlternate, null,
                    modifier = Modifier.size(28.dp),
                    tint     = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                Text("Add more", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ── Toolbar item ──────────────────────────────────────────────────────────────

@Composable
private fun ComposeToolItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier              = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
    }
}
