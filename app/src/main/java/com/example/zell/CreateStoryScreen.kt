package com.example.zell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

data class StickerItem(
    val id: String,
    val emoji: String,
    var offset: IntOffset = IntOffset(0, 0),
    var scale: Float = 1f
)

@Composable
fun CreateStoryScreen(
    onDismiss: () -> Unit,
    onStoryPosted: (Uri?) -> Unit
) {
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var stickers by remember { mutableStateOf(listOf<StickerItem>()) }
    var showStickerPicker by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    
    // Camera & Gallery Launchers
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) selectedImageUri = tempImageUri
    }
    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // --- Content Layer ---
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (selectedImageUri != null) {
                AsyncImage(
                    model = selectedImageUri,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                
                // Stickers Layer
                stickers.forEach { sticker ->
                    DraggableSticker(sticker = sticker)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, null, tint = Color.White.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Capture the moment", color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- Top Controls ---
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { if (selectedImageUri != null) selectedImageUri = null else onDismiss() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }
            
            if (selectedImageUri != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(
                        onClick = { showStickerPicker = true },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(Icons.Outlined.Face, null, tint = Color.White)
                    }
                    Button(
                        onClick = { onStoryPosted(selectedImageUri) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Share Story", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // --- Bottom Capture Controls ---
        if (selectedImageUri == null) {
            Box(modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 60.dp), contentAlignment = Alignment.BottomCenter) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { photoPickerLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                    
                    Surface(
                        modifier = Modifier.size(84.dp).clickable { 
                            val file = File(context.cacheDir, "story_${System.currentTimeMillis()}.jpg")
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                            tempImageUri = uri
                            cameraLauncher.launch(uri)
                        },
                        shape = CircleShape, color = Color.Transparent, border = BorderStroke(5.dp, Color.White)
                    ) {
                        Box(modifier = Modifier.padding(6.dp).fillMaxSize().background(Color.White, CircleShape))
                    }

                    IconButton(onClick = {}) {
                        Icon(Icons.Default.FlipCameraIos, null, tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }
            }
        }

        // --- Sticker Picker Popup ---
        if (showStickerPicker) {
            StickerPicker(
                onStickerSelected = { emoji ->
                    stickers = stickers + StickerItem(id = UUID.randomUUID().toString(), emoji = emoji)
                    showStickerPicker = false
                },
                onDismiss = { showStickerPicker = false }
            )
        }
    }
}

@Composable
fun DraggableSticker(sticker: StickerItem) {
    var offset by remember { mutableStateOf(sticker.offset) }
    var scale by remember { mutableFloatStateOf(sticker.scale) }
    
    Box(
        modifier = Modifier
            .offset { offset }
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    offset = IntOffset(
                        (offset.x + pan.x).roundToInt(),
                        (offset.y + pan.y).roundToInt()
                    )
                    scale *= zoom
                }
            }
            .padding(8.dp)
    ) {
        Text(sticker.emoji, fontSize = 64.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StickerPicker(onStickerSelected: (String) -> Unit, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black.copy(alpha = 0.8f)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Stickers", color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = Color.White) }
            }
            Spacer(Modifier.height(24.dp))
            val emojis = listOf("🔥", "❤️", "🙌", "📍", "💯", "✨", "🎉", "😎", "🤩", "⚡️", "🖤", "👑", "🍕", "🌍", "🧸")
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                emojis.forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 50.sp,
                        modifier = Modifier.clickable { onStickerSelected(emoji) }
                    )
                }
            }
        }
    }
}
