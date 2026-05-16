package com.example.zell

import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ChatScreen(
    chat: SpaceChat,
    onBack: () -> Unit,
    onViewProfile: (String) -> Unit = {},
    chatViewModel: ChatViewModel = viewModel()
) {
    val auth          = FirebaseAuth.getInstance()
    val currentUserId = auth.currentUser?.uid ?: "guest"
    val haptic        = LocalHapticFeedback.current
    val context       = LocalContext.current
    val scope         = rememberCoroutineScope()

    val messages  = chatViewModel.messages
    val chatError by chatViewModel.error
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // ── UI state ──────────────────────────────────────────────────────────────
    var zoomImageUrl   by remember { mutableStateOf<String?>(null) }
    var isUploading    by remember { mutableStateOf(false) }
    var sendButtonScale by remember { mutableStateOf(1f) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    // Message action / edit
    var actionMessage  by remember { mutableStateOf<ChatMessage?>(null) }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }

    // ── Voice recording state ─────────────────────────────────────────────────
    val voiceRecorder = remember { VoiceRecorder(context) }
    var isRecording       by remember { mutableStateOf(false) }
    var recordingSeconds  by remember { mutableStateOf(0) }
    var cancelledBySwipe  by remember { mutableStateOf(false) }

    // Timer — counts up while recording is active
    LaunchedEffect(isRecording) {
        recordingSeconds = 0
        if (isRecording) {
            while (isRecording) {
                delay(1000)
                recordingSeconds++
                // Auto-stop at 2 minutes (120 s) — match WhatsApp limit
                if (recordingSeconds >= 120) {
                    isRecording = false
                    cancelledBySwipe = false
                }
            }
        }
    }

    // ── Permission ────────────────────────────────────────────────────────────
    val micPermission = rememberPermissionState(android.Manifest.permission.RECORD_AUDIO)

    // ── Gallery picker ────────────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    isUploading = true
                    val downloadUrl = RetryHelper.firebaseRetry {
                        FirebaseUtils.uploadImage(context, it, "chat_media/${chat.id}")
                    }
                    chatViewModel.sendMessage(chat.id, "", type = MessageType.IMAGE, imageUrl = downloadUrl)
                } catch (e: Exception) {
                    CrashlyticsLogger.e("ChatScreen", "Media upload failed", e)
                } finally {
                    isUploading = false
                }
            }
        }
    }

    LaunchedEffect(chat.id) { chatViewModel.startListening(chat.id) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    LaunchedEffect(sendButtonScale) {
        if (sendButtonScale > 1f) { delay(100); sendButtonScale = 1f }
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
        ) {
            ChatTopBar(chat = chat, onBack = onBack)
            ErrorBanner(error = chatError, onDismiss = { chatViewModel.clearError() })

            if (messages.isEmpty() && !isUploading) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("No messages yet", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        Text("Say hi to ${chat.name}! 👋", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(
                            message       = message,
                            chat          = chat,
                            currentUserId = currentUserId,
                            onImageClick  = { url -> zoomImageUrl = url },
                            onReact       = { reaction ->
                                chatViewModel.toggleReaction(chat.id, message.id, reaction)
                            },
                            onLongPress   = { actionMessage = message }
                        )
                    }
                    if (isUploading) {
                        item {
                            Surface(
                                modifier = Modifier.padding(8.dp).size(150.dp),
                                shape    = RoundedCornerShape(12.dp),
                                color    = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Edit mode banner
            if (editingMessage != null) {
                Surface(
                    color    = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Edit, null,
                            tint     = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Editing message", fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary)
                            Text(editingMessage!!.text, fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1)
                        }
                        IconButton(onClick = { editingMessage = null; inputText = "" },
                            modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Recording overlay OR normal input bar
            if (isRecording) {
                RecordingBar(
                    seconds        = recordingSeconds,
                    onSendRelease  = {
                        // User released — stop recording and upload
                        isRecording = false
                        val file = voiceRecorder.stop()
                        if (file != null && file.length() > 0) {
                            scope.launch {
                                try {
                                    isUploading = true
                                    val url = RetryHelper.firebaseRetry {
                                        FirebaseUtils.uploadFile(file, "chat_media/${chat.id}", "audio/mp4")
                                    }
                                    chatViewModel.sendMessage(
                                        chat.id, "🎤 Voice message",
                                        type     = MessageType.AUDIO,
                                        imageUrl = url   // imageUrl field stores the media URL for all types
                                    )
                                    file.delete() // clean up local temp file
                                } catch (e: Exception) {
                                    CrashlyticsLogger.e("ChatScreen", "Voice upload failed", e)
                                } finally {
                                    isUploading = false
                                }
                            }
                        }
                    },
                    onCancelSwipe = {
                        // User swiped left — cancel
                        isRecording = false
                        voiceRecorder.cancel()
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                )
            } else {
                ChatInputBar(
                    text           = inputText,
                    onTextChange   = { inputText = it },
                    isEditing      = editingMessage != null,
                    onSend         = {
                        if (inputText.isNotBlank()) {
                            if (editingMessage != null) {
                                chatViewModel.editMessage(chat.id, editingMessage!!.id, inputText)
                                editingMessage = null
                            } else {
                                chatViewModel.sendMessage(chat.id, inputText)
                                sendButtonScale = 1.2f
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            inputText = ""
                        }
                    },
                    onAttachClick  = { galleryLauncher.launch("image/*") },
                    onEmojiClick   = { showEmojiPicker = true },
                    onMicHold      = {
                        if (micPermission.status.isGranted) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            voiceRecorder.start()
                            isRecording = true
                        } else {
                            micPermission.launchPermissionRequest()
                        }
                    },
                    sendButtonScale = sendButtonScale
                )
            }
        }

        // Full-screen image viewer
        if (zoomImageUrl != null) {
            ZoomableImageViewer(imageUrl = zoomImageUrl!!, onDismiss = { zoomImageUrl = null })
        }

        // Emoji picker
        if (showEmojiPicker) {
            ZellEmojiPicker(
                onEmojiSelected = { emoji -> inputText += emoji },
                onDismiss       = { showEmojiPicker = false }
            )
        }

        // Message action sheet (long press)
        if (actionMessage != null) {
            MessageActionSheet(
                message     = actionMessage!!,
                isMyMessage = actionMessage!!.senderId == currentUserId,
                onReact     = { reaction ->
                    chatViewModel.toggleReaction(chat.id, actionMessage!!.id, reaction)
                    actionMessage = null
                },
                onEdit      = {
                    editingMessage = actionMessage
                    inputText      = actionMessage!!.text
                    actionMessage  = null
                },
                onDelete    = {
                    chatViewModel.deleteMessage(chat.id, actionMessage!!.id)
                    actionMessage = null
                },
                onDismiss   = { actionMessage = null }
            )
        }
    }
}

// ─── Recording bar (replaces input bar while recording) ──────────────────────

/**
 * WhatsApp-style recording bar:
 *   [🔴 •  0:07   < Slide to cancel   🎙]
 *
 * - Pulsing red dot shows recording is active
 * - Timer counts up
 * - Swipe left on the bar → cancel
 * - The send button (🎙) releases → stops and sends
 */
@Composable
fun RecordingBar(
    seconds: Int,
    onSendRelease: () -> Unit,
    onCancelSwipe: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // Pulsing dot animation
    val pulse = rememberInfiniteTransition(label = "recordPulse")
    val dotAlpha by pulse.animateFloat(
        initialValue  = 1f,
        targetValue   = 0.2f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label         = "dotAlpha"
    )

    val minutes = seconds / 60
    val secs    = seconds % 60
    val timeStr = "%d:%02d".format(minutes, secs)

    // Track how far the user has dragged left for cancel
    var totalDragX by remember { mutableStateOf(0f) }
    val cancelThreshold = -200f   // px

    Surface(
        color          = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 8.dp,
        modifier       = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart  = { totalDragX = 0f },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDragX += dragAmount
                            if (totalDragX < cancelThreshold) {
                                onCancelSwipe()
                            }
                        },
                        onDragEnd    = { if (totalDragX >= cancelThreshold) onSendRelease() },
                        onDragCancel = { onCancelSwipe() }
                    )
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pulsing red dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .graphicsLayer { alpha = dotAlpha }
                    .background(Color.Red, CircleShape)
            )
            Spacer(Modifier.width(10.dp))

            // Timer
            Text(
                text       = timeStr,
                color      = MaterialTheme.colorScheme.onSurface,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.width(16.dp))

            // "Slide to cancel" hint — fades out when dragging left
            val hintAlpha = ((totalDragX + 0) / cancelThreshold).coerceIn(0f, 1f)
            Text(
                text     = "← Slide to cancel",
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f * (1f - hintAlpha)),
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )

            // Release-to-send mic button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .pointerInput(Unit) {
                        // Detect finger-up on the mic button → send
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                if (event.type == PointerEventType.Release) {
                                    onSendRelease()
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
}

// ─── Message Action Sheet ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageActionSheet(
    message: ChatMessage,
    isMyMessage: Boolean,
    onReact: (String) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val clipboardManager  = LocalClipboardManager.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {

            // Quick reaction row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("❤️", "😂", "😮", "😢", "😡", "🔥", "👍").forEach { emoji ->
                    Text(
                        text     = emoji,
                        fontSize = 28.sp,
                        modifier = Modifier.clickable { onReact(emoji) }.padding(8.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Copy (text only)
            if (message.type == MessageType.TEXT && message.text.isNotBlank() && !message.isDeleted) {
                ListItem(
                    headlineContent  = { Text("Copy") },
                    leadingContent   = { Icon(Icons.Default.ContentCopy, null) },
                    modifier         = Modifier.clickable {
                        clipboardManager.setText(AnnotatedString(message.text))
                        onDismiss()
                    }
                )
            }

            // Edit (my text messages only)
            if (isMyMessage && message.type == MessageType.TEXT && !message.isDeleted) {
                ListItem(
                    headlineContent = { Text("Edit") },
                    leadingContent  = {
                        Icon(Icons.Default.Edit, null, tint = MaterialTheme.colorScheme.primary)
                    },
                    modifier        = Modifier.clickable { onEdit() }
                )
            }

            // Delete (my messages, not already deleted)
            if (isMyMessage && !message.isDeleted) {
                ListItem(
                    headlineContent = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                    leadingContent  = {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier        = Modifier.clickable { showDeleteConfirm = true }
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("Delete message?") },
            text             = { Text("This message will be deleted for everyone. This can't be undone.") },
            confirmButton    = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

// ─── Top Bar ────────────────────────────────────────────────────────────────

@Composable
fun ChatTopBar(chat: SpaceChat, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shadowElevation = 1.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }
            Box(modifier = Modifier.size(40.dp)) {
                AsyncImage(
                    model = chat.avatarUrl, contentDescription = null,
                    modifier     = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                if (chat.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50), CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .align(Alignment.BottomEnd)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(chat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                Text(
                    if (chat.isOnline) "Active now" else "Offline",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (chat.isOnline) Color(0xFF4CAF50)
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Videocam, null, tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// ─── Message Bubble ──────────────────────────────────────────────────────────

@Composable
fun MessageBubble(
    message: ChatMessage,
    chat: SpaceChat,
    currentUserId: String,
    onImageClick: (String) -> Unit,
    onReact: (String) -> Unit,
    onLongPress: () -> Unit
) {
    val isMe      = message.senderId == currentUserId
    val haptic    = LocalHapticFeedback.current
    val bubbleShape = if (isMe) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
                      else      RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)

    // ── Deleted message ───────────────────────────────────────────────────────
    if (message.isDeleted) {
        Row(
            modifier             = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
        ) {
            if (!isMe) Spacer(Modifier.width(40.dp))
            Surface(
                shape  = bubbleShape,
                color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Block, null,
                        modifier = Modifier.size(14.dp),
                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    Spacer(Modifier.width(6.dp))
                    Text("This message was deleted",
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        fontStyle = FontStyle.Italic)
                }
            }
        }
        return
    }

    // ── Normal message ────────────────────────────────────────────────────────
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        if (!isMe) {
            AsyncImage(
                model = chat.avatarUrl, contentDescription = null,
                modifier = Modifier
                    .size(32.dp).clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            Surface(
                color  = if (isMe) MaterialTheme.colorScheme.primary
                         else MaterialTheme.colorScheme.surfaceVariant,
                shape  = bubbleShape,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        },
                        onTap = {
                            if (message.type == MessageType.IMAGE && message.imageUrl != null)
                                onImageClick(message.imageUrl)
                        }
                    )
                }
            ) {
                when (message.type) {
                    MessageType.IMAGE -> AsyncImage(
                        model = message.imageUrl, contentDescription = null,
                        modifier = Modifier
                            .widthIn(min = 120.dp, max = 240.dp)
                            .heightIn(max = 300.dp)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )

                    MessageType.AUDIO -> AudioMessageBubble(
                        audioUrl = message.imageUrl ?: "",
                        isMe     = isMe
                    )

                    MessageType.STORY_REPLY -> StoryReplyBubble(
                        message = message,
                        isMe    = isMe
                    )

                    else -> Text(
                        message.text,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        style    = MaterialTheme.typography.bodyLarge,
                        color    = if (isMe) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Timestamp + edited label + read tick
            Row(
                modifier              = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (message.isEdited) {
                    Text("(edited)", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                Text(TimeUtils.formatMessageTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                if (isMe) {
                    Icon(
                        if (message.isRead) Icons.Default.DoneAll else Icons.Default.Done,
                        contentDescription = if (message.isRead) "Read" else "Sent",
                        modifier = Modifier.size(14.dp),
                        tint     = if (message.isRead) Color(0xFF4CAF50)
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // Reaction bubble
            if (message.reaction != null) {
                Surface(
                    modifier        = Modifier.offset(y = (-6).dp, x = if (isMe) (-8).dp else 8.dp),
                    shape           = CircleShape,
                    color           = MaterialTheme.colorScheme.surface,
                    shadowElevation = 6.dp,
                    border          = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Text(message.reaction, fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
    }
}

// ─── Audio Message Bubble ────────────────────────────────────────────────────

/**
 * Plays a voice note inside a chat bubble.
 *
 * Shows:
 *  - Play / Pause button
 *  - LinearProgressIndicator showing playback position
 *  - Duration label (remaining or total)
 *
 * Uses [MediaPlayer] directly — no ExoPlayer needed for audio-only playback.
 * Properly released in [DisposableEffect] when the composable leaves composition.
 */
@Composable
fun AudioMessageBubble(audioUrl: String, isMe: Boolean) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    var isPlaying    by remember { mutableStateOf(false) }
    var progress     by remember { mutableStateOf(0f) }
    var durationMs   by remember { mutableStateOf(0) }
    var isPrepared   by remember { mutableStateOf(false) }
    var isBuffering  by remember { mutableStateOf(false) }

    val mediaPlayer = remember {
        MediaPlayer().apply {
            setOnPreparedListener  { isPrepared = true; isBuffering = false; durationMs = it.duration }
            setOnCompletionListener { isPlaying = false; progress = 0f }
            setOnErrorListener      { _, _, _ -> isPlaying = false; false }
        }
    }

    // Prepare on first composition
    LaunchedEffect(audioUrl) {
        if (audioUrl.isNotBlank()) {
            try {
                isBuffering = true
                mediaPlayer.setDataSource(audioUrl)
                mediaPlayer.prepareAsync()
            } catch (e: Exception) {
                isBuffering = false
                CrashlyticsLogger.e("AudioBubble", "Failed to prepare MediaPlayer", e)
            }
        }
    }

    // Progress polling — runs while playing
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying && durationMs > 0) {
                progress = mediaPlayer.currentPosition.toFloat() / durationMs
                delay(200)
            }
        }
    }

    DisposableEffect(audioUrl) {
        onDispose {
            mediaPlayer.release()
        }
    }

    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurface

    Row(
        modifier          = Modifier
            .widthIn(min = 180.dp, max = 260.dp)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Play / Pause button
        IconButton(
            onClick = {
                if (!isPrepared) return@IconButton
                if (isPlaying) {
                    mediaPlayer.pause()
                    isPlaying = false
                } else {
                    mediaPlayer.start()
                    isPlaying = true
                }
            },
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = contentColor.copy(alpha = 0.15f),
                    shape = CircleShape
                )
        ) {
            if (isBuffering) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(20.dp),
                    color     = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector        = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint               = contentColor,
                    modifier           = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            // Waveform-style progress bar
            LinearProgressIndicator(
                progress          = { progress },
                modifier          = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                color             = contentColor,
                trackColor        = contentColor.copy(alpha = 0.25f)
            )
            // Duration display
            Text(
                text  = formatAudioDuration(
                    if (isPlaying && durationMs > 0)
                        durationMs - mediaPlayer.currentPosition
                    else
                        durationMs
                ),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.75f)
            )
        }

        // Mic icon to indicate this is a voice note
        Icon(
            Icons.Default.Mic,
            contentDescription = null,
            tint     = contentColor.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
}

/** Formats milliseconds to "m:ss" — e.g. 65000 → "1:05" */
private fun formatAudioDuration(ms: Int): String {
    if (ms <= 0) return "0:00"
    val totalSec = ms / 1000
    return "%d:%02d".format(totalSec / 60, totalSec % 60)
}

// ─── Input Bar ───────────────────────────────────────────────────────────────

@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onAttachClick: () -> Unit,
    onEmojiClick: () -> Unit,
    onMicHold: () -> Unit,
    sendButtonScale: Float,
    isEditing: Boolean = false
) {
    Surface(
        color           = MaterialTheme.colorScheme.surface,
        tonalElevation  = 4.dp,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEmojiClick) {
                Icon(Icons.Outlined.EmojiEmotions, "Emoji",
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp))
            }
            if (!isEditing) {
                IconButton(onClick = onAttachClick) {
                    Icon(Icons.Default.Add, "Attach",
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp))
                }
            }

            Surface(
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                shape    = RoundedCornerShape(24.dp),
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            ) {
                BasicTextField(
                    value        = text,
                    onValueChange = onTextChange,
                    modifier     = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    textStyle    = TextStyle(
                        color    = MaterialTheme.colorScheme.onSurface,
                        fontSize = 16.sp
                    ),
                    cursorBrush  = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner ->
                        if (text.isEmpty()) {
                            Text(
                                if (isEditing) "Edit message…" else "Type a message…",
                                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontSize = 16.sp
                            )
                        }
                        inner()
                    }
                )
            }

            if (text.isBlank() && !isEditing) {
                // Hold-to-record mic button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                // Wait for initial press down
                                val down = awaitFirstDown()
                                down.consume()
                                onMicHold()   // ← starts recording

                                // Wait for finger up (we won't process release here —
                                // RecordingBar takes over and handles send/cancel)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Hold to record",
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            if (text.isNotBlank() || isEditing) {
                IconButton(
                    onClick  = onSend,
                    modifier = Modifier.scale(sendButtonScale)
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Check else Icons.AutoMirrored.Filled.Send,
                        contentDescription = if (isEditing) "Save edit" else "Send",
                        tint = if (isEditing) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ─── Story Reply Bubble ───────────────────────────────────────────────────────

/**
 * Shows a story thumbnail with the reply/reaction text below it —
 * the same style Instagram uses for story replies in DMs.
 */
@Composable
fun StoryReplyBubble(message: ChatMessage, isMe: Boolean) {
    val contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary
                       else MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .widthIn(min = 120.dp, max = 220.dp)
            .padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Story thumbnail
        if (!message.storyImageUrl.isNullOrBlank()) {
            Box {
                AsyncImage(
                    model = message.storyImageUrl,
                    contentDescription = "Story",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                // "Story" label overlay at top-left
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                    color  = Color.Black.copy(alpha = 0.45f),
                    shape  = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        "Story",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        color    = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Reply text or emoji reaction
        if (message.text.isNotBlank()) {
            Text(
                text     = message.text,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                style    = MaterialTheme.typography.bodyLarge,
                color    = contentColor,
                fontSize = if (message.text.length <= 2) 32.sp else 15.sp
            )
        }
    }
}

// ─── Typing indicator ────────────────────────────────────────────────────────

@Composable
fun TypingIndicator(chat: SpaceChat) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment     = Alignment.Bottom
    ) {
        AsyncImage(
            model = chat.avatarUrl, contentDescription = null,
            modifier = Modifier
                .size(32.dp).clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
        ) {
            Row(
                modifier          = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val dotScale = remember { Animatable(0.5f) }
                    LaunchedEffect(Unit) {
                        delay(index * 200L)
                        while (true) {
                            dotScale.animateTo(1f, tween(300))
                            dotScale.animateTo(0.5f, tween(300))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .scale(dotScale.value)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                    if (index < 2) Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

// ─── Zoomable Image Viewer ───────────────────────────────────────────────────

@Composable
fun ZoomableImageViewer(imageUrl: String, onDismiss: () -> Unit) {
    Box(
        modifier         = Modifier.fillMaxSize().background(Color.Black).clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model        = imageUrl, contentDescription = null,
            modifier     = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

// ─── Reaction picker ─────────────────────────────────────────────────────────

@Composable
fun ChatReactionPicker(onReact: (String) -> Unit) {
    Surface(
        shape           = RoundedCornerShape(40.dp),
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 24.dp
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("❤️", "😂", "😮", "😢", "😡", "🔥", "👍").forEach { emoji ->
                Text(emoji, fontSize = 24.sp,
                    modifier = Modifier.clickable { onReact(emoji) }.padding(4.dp))
            }
        }
    }
}

// ─── Chat profile header ─────────────────────────────────────────────────────

@Composable
fun ChatProfileHeader(chat: SpaceChat, onViewProfile: () -> Unit) {
    Column(
        modifier              = Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment   = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model        = chat.avatarUrl, contentDescription = null,
            modifier     = Modifier.size(100.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(16.dp))
        Text(chat.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onViewProfile) { Text("View Profile") }
    }
}
