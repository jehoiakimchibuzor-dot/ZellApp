package com.example.zell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/**
 * CreatePinScreen - UI for creating Pinterest-style pins
 * 🔧 REFACTORED: Improved mobile responsiveness and fixed compilation errors (doneby Gemini)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePinScreen(
    onDismiss: () -> Unit,
    boardViewModel: BoardViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val currentUser by userViewModel.currentUserProfile
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBoard by remember { mutableStateOf<PinBoard?>(null) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { 
        selectedImageUri = it 
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Create Pin", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onBackground) }
                },
                actions = {
                    Button(
                        onClick = { /* Handle Publish logic */ },
                        enabled = selectedImageUri != null && title.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)), // Pinterest Red preserved for branding
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Publish", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            val isPortrait = maxWidth < 600.dp
            
            if (isPortrait) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    UploadZone(selectedImageUri) { photoPicker.launch("image/*") }
                    Spacer(Modifier.height(32.dp))
                    PinDetailsForm(
                        title = title,
                        onTitleChange = { title = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        link = link,
                        onLinkChange = { link = it },
                        selectedBoard = selectedBoard,
                        currentUser = currentUser
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalArrangement = Arrangement.spacedBy(48.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        UploadZone(selectedImageUri) { photoPicker.launch("image/*") }
                    }
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        PinDetailsForm(
                            title = title,
                            onTitleChange = { title = it },
                            description = description,
                            onDescriptionChange = { description = it },
                            link = link,
                            onLinkChange = { link = it },
                            selectedBoard = selectedBoard,
                            currentUser = currentUser
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun UploadZone(selectedImageUri: Uri?, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 300.dp, max = 500.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selectedImageUri != null) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(48.dp), 
                    shape = CircleShape, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                ) {
                    Icon(
                        Icons.Default.ArrowUpward, 
                        null, 
                        tint = MaterialTheme.colorScheme.onSurface, 
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("Click to upload", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    "Recommendation: High-quality .jpg", 
                    fontSize = 12.sp, 
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun PinDetailsForm(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    link: String,
    onLinkChange: (String) -> Unit,
    selectedBoard: PinBoard?,
    currentUser: StoryUser
) {
    Text("Board", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    Spacer(Modifier.height(8.dp))
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable { },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = selectedBoard?.name ?: "Choose a board", 
                modifier = Modifier.weight(1f), 
                color = if(selectedBoard == null) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
            )
            Icon(Icons.Default.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }

    Spacer(Modifier.height(32.dp))

    BasicTextField(
        value = title,
        onValueChange = onTitleChange,
        textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.fillMaxWidth(),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            if (title.isEmpty()) Text("Add your title", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f), fontSize = 32.sp, fontWeight = FontWeight.Black)
            inner()
        }
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

    Spacer(Modifier.height(16.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = currentUser.avatarUrl, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape))
        Spacer(Modifier.width(12.dp))
        Text(currentUser.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }

    Spacer(Modifier.height(24.dp))

    BasicTextField(
        value = description,
        onValueChange = onDescriptionChange,
        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.fillMaxWidth(),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            if (description.isEmpty()) Text("Tell everyone what your Pin is about", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 16.sp)
            inner()
        }
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

    Spacer(Modifier.height(24.dp))

    BasicTextField(
        value = link,
        onValueChange = onLinkChange,
        textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.fillMaxWidth(),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = { inner ->
            if (link.isEmpty()) Text("Add a destination link", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 16.sp)
            inner()
        }
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
}
