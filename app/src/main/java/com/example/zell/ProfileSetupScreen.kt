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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val accentOptions = listOf(
    Color(0xFF00F2FF), Color(0xFF0066FF), Color(0xFFFF007A), 
    Color(0xFF7000FF), Color(0xFF00FF85), Color(0xFFFFD600)
)

@Composable
fun ProfileSetupScreen(onFinish: () -> Unit) {
    val auth = FirebaseAuth.getInstance()
    val db = FirebaseFirestore.getInstance()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var skill by remember { mutableStateOf("") }
    var selectedAccent by remember { mutableStateOf(accentOptions[0]) }
    var isSaving by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var appError by remember { mutableStateOf<AppError?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
    }

    val canFinish = username.length >= 3 && !isSaving

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ZELL", fontSize = 24.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
            Spacer(Modifier.height(32.dp))
            Text("Create Your Identity", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(32.dp))

            ErrorBanner(error = appError, onDismiss = { appError = null })

            Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.clickable { photoLauncher.launch("image/*") }) {
                Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, border = BorderStroke(2.dp, selectedAccent)) {
                    if (selectedImageUri != null) {
                        AsyncImage(model = selectedImageUri, contentDescription = null, contentScale = ContentScale.Crop)
                    } else {
                        Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, null, modifier = Modifier.size(50.dp)) }
                    }
                }
                Surface(modifier = Modifier.size(32.dp), shape = CircleShape, color = selectedAccent) {
                    Icon(Icons.Default.CameraAlt, null, modifier = Modifier.padding(6.dp), tint = Color.Black)
                }
            }

            Spacer(Modifier.height(32.dp))
            SetupField(label = "Username", value = username, onValueChange = { username = it }, accent = selectedAccent)
            Spacer(Modifier.height(16.dp))
            SetupField(label = "Bio", value = bio, onValueChange = { bio = it }, accent = selectedAccent)
            Spacer(Modifier.height(16.dp))
            SetupField(label = "Primary Skill", value = skill, onValueChange = { skill = it }, accent = selectedAccent)

            Spacer(Modifier.height(48.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            isSaving = true
                            appError = null
                            val uid = auth.currentUser?.uid ?: throw Exception("Not authenticated")
                            
                            // 🔧 STEP 1: Upload Avatar if selected
                            var finalAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200"
                            if (selectedImageUri != null) {
                                // ✅ Fixed: path must match storage.rules avatars/{userId}/**
                                finalAvatarUrl = RetryHelper.firebaseRetry {
                                    FirebaseUtils.uploadImage(context, selectedImageUri!!, "avatars/$uid")
                                }
                            }

                            // ✅ Fixed: correct field names + all StoryUser fields present
                            val userMap = mapOf(
                                "id"          to uid,
                                "name"        to username,
                                "bio"         to bio,
                                "skills"      to skill,   // was "skill" — Firestore field is "skills"
                                "about"       to "",
                                "institution" to "",
                                "location"    to "",
                                "website"     to "",
                                "themeColor"  to "#FF6B6B",
                                "avatarUrl"   to finalAvatarUrl,
                                "stories"     to emptyList<Any>(),
                                "hasUnread"   to false,
                                "isYou"       to true
                            )

                            // ✅ Fixed: SetOptions.merge() so subsequent edits never wipe unrelated fields
                            RetryHelper.firebaseRetry {
                                db.collection("users").document(uid)
                                    .set(userMap, SetOptions.merge()).await()
                            }
                            
                            isSaving = false
                            onFinish()
                        } catch (e: Exception) {
                            isSaving = false
                            appError = ErrorHandler.classifyException(e)
                            CrashlyticsLogger.e("ProfileSetup", "Failed to save identity", e)
                        }
                    }
                },
                enabled = canFinish,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = selectedAccent, contentColor = Color.Black)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                else Text("Activate Identity", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SetupField(label: String, value: String, onValueChange: (String) -> Unit, accent: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(12.dp)).padding(16.dp),
            decorationBox = { inner -> 
                if (value.isEmpty()) Text("Type here...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                inner() 
            }
        )
    }
}
