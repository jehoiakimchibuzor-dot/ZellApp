package com.example.zell

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ─── Data Models ──────────────────────────────────────────────────────────────

enum class MessageType { TEXT, IMAGE, VIDEO, VOICE, STICKER, GIF }

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val time: String = "",
    val timestamp: Long = 0,
    val isRead: Boolean = false,
    val type: MessageType = MessageType.TEXT,
    val imageUrl: String? = null,
    val reaction: String? = null,
    val reactionCount: Int = 1
)

// ─── Chat Screen ──────────────────────────────────────────────────────────────

@Composable
fun ChatScreen(
    chat: SpaceChat,
    onBack: () -> Unit,
    onViewProfile: (String) -> Unit = {}
) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val currentUserId = auth.currentUser?.uid ?: "me"
    val haptic = LocalHapticFeedback.current
    
    val messages = remember { mutableStateListOf<ChatMessage>() }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isTyping by remember { mutableStateOf(true) } 

    var zoomImageUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(chat.id) {
        val chatId = if (currentUserId < chat.id) "${currentUserId}_${chat.id}" else "${chat.id}_$currentUserId"
        db.collection("chats").document(chatId).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, _ ->
                if (value != null) {
                    messages.clear()
                    messages.addAll(value.toObjects(ChatMessage::class.java))
                    coroutineScope.launch {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                }
            }
    }

    fun sendMessage(type: MessageType = MessageType.TEXT, text: String = inputText, imageUrl: String? = null) {
        if (text.isBlank() && imageUrl == null && type == MessageType.TEXT) return
        val chatId = if (currentUserId < chat.id) "${currentUserId}_${chat.id}" else "${chat.id}_$currentUserId"
        
        val newMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = currentUserId,
            text = text.trim(),
            time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
            timestamp = System.currentTimeMillis(),
            type = type,
            imageUrl = imageUrl,
            isRead = false,
        )
        
        db.collection("chats").document(chatId).collection("messages")
            .document(newMsg.id)
            .set(newMsg)
            
        if (type == MessageType.TEXT) inputText = ""
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .imePadding(),
        ) {
            ChatTopBar(chat = chat, onBack = onBack)

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                item { ChatProfileHeader(chat = chat, onViewProfile = { onViewProfile(chat.id) }) }
                item { DateHeader("Today") }

                items(messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message, 
                        chat = chat, 
                        currentUserId = currentUserId,
                        onImageClick = { url -> zoomImageUrl = url },
                        onReact = { reaction ->
                            val chatId = if (currentUserId < chat.id) "${currentUserId}_${chat.id}" else "${chat.id}_$currentUserId"
                            db.collection("chats").document(chatId).collection("messages")
                                .document(message.id).update("reaction", reaction)
                        }
                    )
                }
                
                if (isTyping) { item { TypingIndicator(chat.avatarUrl) } }
            }

            ChatInputBar(
                text = inputText,
                onTextChange = { inputText = it },
                onSend = { sendMessage() },
                onAttachClick = { },
                onVoiceRecord = { }
            )
        }

        if (zoomImageUrl != null) {
            ZoomableImageViewer(imageUrl = zoomImageUrl!!, onDismiss = { zoomImageUrl = null })
        }
    }
}

@Composable
fun ZoomableImageViewer(imageUrl: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onDismiss() })
            }
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale.coerceIn(1f, 4f)
                    scaleY = scale.coerceIn(1f, 4f)
                    translationX = offset.x
                    translationY = offset.y
                }
                .transformable(state = state),
            contentScale = ContentScale.Fit
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(Icons.Default.Close, null, tint = Color.White)
        }
    }
}

@Composable
fun TypingIndicator(avatarUrl: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        AsyncImage(model = avatarUrl, contentDescription = null, modifier = Modifier.size(24.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(8.dp))
        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), shape = RoundedCornerShape(16.dp)) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "dot")
                    val alpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600, delayMillis = index * 200), RepeatMode.Reverse))
                    val dotOffset by infiniteTransition.animateFloat(0f, -6f, infiniteRepeatable(tween(600, delayMillis = index * 200), RepeatMode.Reverse))
                    Box(Modifier.size(6.dp).offset(y = dotOffset.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)))
                }
            }
        }
    }
}

@Composable
fun ChatTopBar(chat: SpaceChat, onBack: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), shadowElevation = 1.dp) {
        Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
            Box(modifier = Modifier.size(40.dp)) {
                AsyncImage(model = chat.avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                if (chat.isOnline) Box(modifier = Modifier.size(12.dp).background(MaterialTheme.colorScheme.background, CircleShape).padding(2.dp).background(Color(0xFF4CAF50), CircleShape).align(Alignment.BottomEnd))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(chat.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Black)
                Text(if (chat.isOnline) "Active now" else "Offline", style = MaterialTheme.typography.labelSmall, color = if (chat.isOnline) Color(0xFF4CAF50) else Color.Gray)
            }
            IconButton(onClick = { }) { Icon(Icons.Default.Videocam, null, tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = { }) { Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun DateHeader(label: String) {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), letterSpacing = 1.5.sp)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(message: ChatMessage, chat: SpaceChat, currentUserId: String, onImageClick: (String) -> Unit, onReact: (String) -> Unit) {
    val isMe = message.senderId == currentUserId
    val haptic = LocalHapticFeedback.current
    var showReactions by remember { mutableStateOf(false) }
    
    val bubbleEntrance = remember { Animatable(0.8f) }
    val bubbleTranslate = remember { Animatable(20f) }
    LaunchedEffect(Unit) {
        launch { bubbleEntrance.animateTo(1f, spring(dampingRatio = Spring.DampingRatioLowBouncy)) }
        launch { bubbleTranslate.animateTo(0f, spring(stiffness = Spring.StiffnessLow)) }
    }

    val bubbleShape = if (isMe) {
        RoundedCornerShape(12.dp, 12.dp, 4.dp, 12.dp)
    } else {
        RoundedCornerShape(12.dp, 12.dp, 12.dp, 4.dp)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp).graphicsLayer { 
            scaleX = bubbleEntrance.value; scaleY = bubbleEntrance.value; translationY = bubbleTranslate.value 
        },
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start, 
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            AsyncImage(model = chat.avatarUrl, contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(8.dp))
        }
        
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start, modifier = Modifier.widthIn(max = 280.dp)) {
            Surface(
                color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                shape = bubbleShape,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); showReactions = true },
                        onTap = { if (message.type == MessageType.IMAGE && message.imageUrl != null) onImageClick(message.imageUrl) }
                    )
                }
            ) {
                Column(modifier = Modifier.padding(if (message.type == MessageType.TEXT) PaddingValues(horizontal = 14.dp, vertical = 10.dp) else PaddingValues(4.dp))) {
                    when (message.type) {
                        MessageType.IMAGE -> AsyncImage(model = message.imageUrl, contentDescription = null, modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                        MessageType.VOICE -> VoiceNoteBubble(isMe)
                        else -> Text(message.text, style = MaterialTheme.typography.bodyLarge, color = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            
            if (message.reaction != null) {
                Surface(
                    modifier = Modifier.offset(y = (-10).dp, x = if(isMe) (-8).dp else 8.dp),
                    shape = CircleShape, color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(message.reaction!!, fontSize = 12.sp)
                        if (message.reactionCount > 1) {
                            Spacer(Modifier.width(2.dp))
                            Text(message.reactionCount.toString(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (isMe && message.isRead) {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(100); visible = true }
                AnimatedVisibility(visible, enter = fadeIn() + scaleIn()) {
                    AsyncImage(model = chat.avatarUrl, contentDescription = null, modifier = Modifier.padding(top = 4.dp).size(14.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.background, CircleShape), contentScale = ContentScale.Crop)
                }
            }
        }
    }

    if (showReactions) {
        Popup(alignment = Alignment.TopCenter, onDismissRequest = { showReactions = false }, offset = IntOffset(0, (-80).dp.value.toInt())) {
            ChatReactionPicker(onReact = { onReact(it); showReactions = false })
        }
    }
}

@Composable
fun VoiceNoteBubble(isMe: Boolean) {
    var progress by remember { mutableFloatStateOf(0.3f) }
    val infiniteTransition = rememberInfiniteTransition(label = "voice")
    val waveScale by infiniteTransition.animateFloat(0.8f, 1.2f, infiniteRepeatable(tween(500), RepeatMode.Reverse))

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
        Surface(shape = CircleShape, color = if(isMe) Color.White.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), modifier = Modifier.size(36.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.PlayArrow, null, tint = if(isMe) Color.White else MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.width(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.pointerInput(Unit) {
            detectTapGestures { offset -> progress = (offset.x / size.width.toFloat()).coerceIn(0f, 1f) }
        }) {
            repeat(16) { index ->
                val height = remember { (8..24).random().dp }
                val isActive = (index.toFloat() / 16f) < progress
                Box(Modifier.width(2.5.dp).height(if(index % 3 == 0) height * waveScale else height).background(if(isMe) (if(isActive) Color.White else Color.White.copy(alpha = 0.3f)) else (if(isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f)), CircleShape))
            }
        }
        Spacer(Modifier.width(14.dp))
        Text("0:12", style = MaterialTheme.typography.labelSmall, color = if(isMe) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ChatReactionPicker(onReact: (String) -> Unit) {
    Surface(shape = RoundedCornerShape(40.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 24.dp, tonalElevation = 12.dp) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf("❤️", "😂", "😮", "😢", "😡", "🔥", "👍").forEach { emoji ->
                var h by remember { mutableStateOf(false) }
                val s by animateFloatAsState(if (h) 1.8f else 1f, animationSpec = spring())
                Text(text = emoji, fontSize = 26.sp, modifier = Modifier.scale(s).pointerInput(Unit) { detectTapGestures(onTap = { onReact(emoji) }, onPress = { h = true; tryAwaitRelease(); h = false }) }.padding(4.dp))
            }
        }
    }
}

@Composable
fun ChatInputBar(text: String, onTextChange: (String) -> Unit, onSend: () -> Unit, onAttachClick: () -> Unit, onVoiceRecord: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 4.dp, shadowElevation = 8.dp) {
        Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onAttachClick) { Icon(Icons.Default.Add, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) }
            Surface(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))) {
                Row(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    BasicTextField(value = text, onValueChange = onTextChange, modifier = Modifier.weight(1f), textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary), decorationBox = { inner -> if (text.isEmpty()) Text("Message...", color = Color.Gray, fontSize = 16.sp); inner() })
                    Icon(Icons.Outlined.SentimentSatisfiedAlt, null, tint = Color.Gray, modifier = Modifier.size(22.dp).clickable { })
                }
            }
            val isEnabled = text.isNotBlank()
            val pulseScale by animateFloatAsState(targetValue = if (isEnabled) 1.15f else 1f, animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse))
            if (!isEnabled) { IconButton(onClick = onVoiceRecord) { Icon(Icons.Default.Mic, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp)) } }
            else { IconButton(onClick = onSend, modifier = Modifier.scale(pulseScale), colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)) { Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(28.dp)) } }
        }
    }
}

@Composable
fun ChatProfileHeader(chat: SpaceChat, onViewProfile: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(100.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(brush = Brush.sweepGradient(listOf(Color.Cyan, Color.Magenta, Color.Yellow, Color.Cyan)), radius = size.minDimension / 2 + 5.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx()), alpha = 0.6f)
            }
            AsyncImage(model = chat.avatarUrl, contentDescription = null, modifier = Modifier.fillMaxSize().padding(4.dp).clip(CircleShape), contentScale = ContentScale.Crop)
        }
        Spacer(Modifier.height(20.dp))
        Text(chat.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text("Zell ID: @${chat.name.lowercase().replace(" ", "")}", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        Spacer(Modifier.height(28.dp))
        Button(onClick = onViewProfile, shape = RoundedCornerShape(20.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), contentColor = MaterialTheme.colorScheme.onSurfaceVariant), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))) { Text("View Profile", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp)) }
    }
}
