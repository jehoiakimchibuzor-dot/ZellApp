package com.example.zell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun CreateStoryScreen(
    onDismiss: () -> Unit,
    onStoryPosted: (Uri?) -> Unit
) {
    val auth    = FirebaseAuth.getInstance()
    val db      = FirebaseFirestore.getInstance()
    val scope   = rememberCoroutineScope()
    val context = LocalContext.current

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading      by remember { mutableStateOf(false) }
    var appError         by remember { mutableStateOf<AppError?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

        // ── Preview image ─────────────────────────────────────────────────────
        if (selectedImageUri != null) {
            AsyncImage(
                model              = selectedImageUri,
                contentDescription = null,
                modifier           = Modifier.fillMaxSize(),
                contentScale       = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CameraAlt, null,
                        tint     = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Pick a photo to share",
                        color      = Color.White.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                        fontSize   = 16.sp
                    )
                }
            }
        }

        // ── Error banner ──────────────────────────────────────────────────────
        if (appError != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 72.dp, start = 16.dp, end = 16.dp)
            ) {
                ErrorBanner(error = appError, onDismiss = { appError = null })
            }
        }

        // ── Top bar — close + share ───────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(
                onClick  = { if (selectedImageUri != null) selectedImageUri = null else onDismiss() },
                modifier = Modifier.background(Color.Black.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(Icons.Default.Close, null, tint = Color.White)
            }

            if (selectedImageUri != null) {
                Button(
                    onClick = {
                        scope.launch {
                            try {
                                isUploading = true
                                appError    = null

                                val uid = auth.currentUser?.uid
                                    ?: throw Exception("Not logged in. Please sign in and try again.")

                                // Upload to stories/{uid}/ — matches rule: match /stories/{userId}/{fileName}
                                val url = RetryHelper.firebaseRetry {
                                    FirebaseUtils.uploadImage(
                                        context, selectedImageUri!!, "stories/$uid"
                                    )
                                }

                                val newStory = StoryItem(url = url)

                                // Use set(merge) — safe whether stories field exists or not.
                                // update() throws NOT_FOUND if the field or doc is missing.
                                RetryHelper.firebaseRetry {
                                    db.collection("users").document(uid)
                                        .set(
                                            mapOf("stories" to
                                                // Read current stories then append
                                                (db.collection("users").document(uid)
                                                    .get().await()
                                                    .toObject(StoryUser::class.java)
                                                    ?.stories ?: emptyList()) + newStory
                                            ),
                                            SetOptions.merge()
                                        ).await()
                                }

                                // Keep mockStories in sync so the stories row refreshes
                                val idx = mockStories.indexOfFirst { it.id == uid }
                                if (idx != -1) {
                                    mockStories[idx] = mockStories[idx].copy(
                                        stories = mockStories[idx].stories + newStory
                                    )
                                }

                                isUploading = false
                                onStoryPosted(selectedImageUri)
                            } catch (e: Exception) {
                                isUploading = false
                                appError    = ErrorHandler.classifyException(e)
                                CrashlyticsLogger.e("CreateStory", "Story upload failed", e)
                            }
                        }
                    },
                    enabled = !isUploading,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor   = Color.Black
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(18.dp),
                            color       = Color.Black,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Uploading…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share Story", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Photo library button (when no image selected) ─────────────────────
        if (selectedImageUri == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 40.dp)
                    .align(Alignment.BottomCenter),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { photoPickerLauncher.launch("image/*") },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.15f),
                        contentColor   = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Choose Photo", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // ── Story reactions row (when image is selected) ──────────────────────
        if (selectedImageUri != null) {
            StoryReactionBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Story Reaction Bar — floating emoji row at the bottom of a story
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StoryReactionBar(
    modifier: Modifier = Modifier,
    onReact: ((String) -> Unit)? = null
) {
    val reactions = listOf("❤️", "🔥", "😂", "😮", "😢", "👏", "🙌", "😍")

    // Track which emoji was last tapped for the bounce animation
    var lastReacted by remember { mutableStateOf<String?>(null) }

    Row(
        modifier              = modifier
            .background(Color.Black.copy(alpha = 0.40f), RoundedCornerShape(32.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        reactions.forEach { emoji ->
            val isActive = lastReacted == emoji
            val scale by animateFloatAsState(
                targetValue   = if (isActive) 1.5f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness    = Spring.StiffnessLow
                ),
                label = "reactionScale"
            )

            if (isActive) {
                LaunchedEffect(lastReacted) {
                    delay(400)
                    lastReacted = null
                }
            }

            Text(
                text     = emoji,
                fontSize = 26.sp,
                modifier = Modifier
                    .scale(scale)
                    .clickable {
                        lastReacted = emoji
                        onReact?.invoke(emoji)
                    }
                    .padding(4.dp)
            )
        }
    }
}
