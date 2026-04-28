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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// Cool modern color palette
private val accentOptions = listOf(
    Color(0xFF00F2FF), // Zell Cyan
    Color(0xFF0066FF), // Electric Blue
    Color(0xFFFF007A), // Hot Pink
    Color(0xFF7000FF), // Vivid Purple
    Color(0xFF00FF85), // Spring Green
    Color(0xFFFFD600), // Cyber Yellow
    Color(0xFFFFFFFF), // Pure White
    Color(0xFF121212), // Onyx Black
)

val skillSuggestions = listOf(
    "Product Designer", "Software Engineer", "Frontend Developer", "Backend Engineer",
    "Mobile Developer", "UI/UX Designer", "Data Scientist", "Product Manager",
    "Content Creator", "Digital Artist", "Architect", "Venture Capitalist"
)

val institutionSuggestions = listOf(
    "Stanford University", "MIT", "Harvard", "Lagos Business School",
    "University of Lagos", "Covenant University", "Google", "Microsoft", "Zell Inc."
)

enum class UsernameState { IDLE, CHECKING, AVAILABLE, TAKEN }

@Composable
fun ProfileSetupScreen(onFinish: () -> Unit) {
    var username by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var skill by remember { mutableStateOf("") }
    var institution by remember { mutableStateOf("") }
    var showSkillOnProfile by remember { mutableStateOf(true) }
    var selectedAccent by remember { mutableStateOf(accentOptions[0]) }
    var usernameState by remember { mutableStateOf(UsernameState.IDLE) }
    var avatarInitial by remember { mutableStateOf("Z") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    LaunchedEffect(username) {
        if (username.length >= 3) {
            usernameState = UsernameState.CHECKING
            delay(800)
            usernameState = if (username.lowercase() in listOf("admin", "zell", "test", "user")) {
                UsernameState.TAKEN
            } else {
                UsernameState.AVAILABLE
            }
        } else {
            usernameState = UsernameState.IDLE
        }
    }

    LaunchedEffect(username) {
        if (username.isNotEmpty()) {
            avatarInitial = username.first().uppercaseChar().toString()
        }
    }

    val canFinish = username.length >= 3 && usernameState == UsernameState.AVAILABLE

    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.08f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)), // Dark slate background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            selectedAccent.copy(alpha = pulseAlpha),
                            Color.Transparent,
                        ),
                        radius = 1200f,
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "ZELL",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    color = Color.White,
                )
                TextButton(onClick = onFinish) {
                    Text(
                        "Skip",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "Customize Identity",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Define your professional and social presence on Zell.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )

            Spacer(Modifier.height(40.dp))

            ProfilePreviewCard(
                username = username,
                bio = bio,
                skill = if (showSkillOnProfile) skill else null,
                institution = institution,
                accent = selectedAccent,
                avatarInitial = avatarInitial,
                imageUri = selectedImageUri
            )

            Spacer(Modifier.height(36.dp))

            AvatarPickerRow(
                accent = selectedAccent,
                initial = avatarInitial,
                imageUri = selectedImageUri,
                onPickImage = { photoPickerLauncher.launch("image/*") }
            )

            Spacer(Modifier.height(32.dp))

            SetupFieldLabel("Username")
            Spacer(Modifier.height(8.dp))
            UsernameField(
                username,
                { username = it.lowercase().replace(" ", "") },
                usernameState,
                selectedAccent,
            )

            Spacer(Modifier.height(24.dp))

            SetupFieldLabel("Bio  ·  ${bio.length}/240")
            Spacer(Modifier.height(8.dp))
            BioField(
                bio,
                { if (it.length <= 240) bio = it },
                selectedAccent,
            )

            Spacer(Modifier.height(24.dp))

            SetupFieldLabel("Primary Skill (LinkedIn style)")
            Spacer(Modifier.height(8.dp))
            AutocompleteField(
                value = skill,
                onValueChange = { skill = it },
                placeholder = "e.g. Software Engineer",
                suggestions = skillSuggestions,
                accent = selectedAccent
            )

            Spacer(Modifier.height(24.dp))

            SetupFieldLabel("Institution / Organization")
            Spacer(Modifier.height(8.dp))
            AutocompleteField(
                value = institution,
                onValueChange = { institution = it },
                placeholder = "e.g. MIT or Google",
                suggestions = institutionSuggestions,
                accent = selectedAccent
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = showSkillOnProfile,
                    onCheckedChange = { showSkillOnProfile = it },
                    colors = CheckboxDefaults.colors(checkedColor = selectedAccent, uncheckedColor = Color.White.copy(alpha = 0.3f))
                )
                Text("Show professional info on profile", fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
            }

            Spacer(Modifier.height(32.dp))

            SetupFieldLabel("Theme Accent")
            Spacer(Modifier.height(14.dp))
            AccentColorPicker(
                selected = selectedAccent,
                onSelect = { selectedAccent = it },
            )

            Spacer(Modifier.height(48.dp))

            FinishButton(
                enabled = canFinish,
                accent = selectedAccent,
                onClick = onFinish,
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfilePreviewCard(
    username: String,
    bio: String,
    skill: String?,
    institution: String,
    accent: Color,
    avatarInitial: String,
    imageUri: Uri? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, accent.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(accent, accent.copy(alpha = 0.5f), accent)
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        avatarInitial,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (accent == Color.White) Color.Black else Color.White,
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (username.isEmpty()) "your_username" else "@$username",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                if (!skill.isNullOrEmpty()) {
                    Text(
                        text = skill,
                        fontSize = 12.sp,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (institution.isNotEmpty()) {
                    Text(
                        text = institution,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (bio.isEmpty()) "Your bio will appear here..." else bio,
                    fontSize = 13.sp,
                    color = if (bio.isEmpty())
                        Color.White.copy(alpha = 0.25f)
                    else
                        Color.White.copy(alpha = 0.7f),
                    lineHeight = 18.sp,
                    maxLines = 3,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(10.dp)
                .background(accent, CircleShape)
                .align(Alignment.TopEnd),
        )
    }
}

@Composable
fun AutocompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suggestions: List<String>,
    accent: Color
) {
    var isFocused by remember { mutableStateOf(false) }
    val filteredSuggestions = remember(value) {
        if (value.length < 2) emptyList()
        else suggestions.filter { it.contains(value, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { isFocused = it.isFocused }
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A1A1A))
                .border(
                    1.dp,
                    if (isFocused) accent else Color.White.copy(alpha = 0.1f),
                    RoundedCornerShape(14.dp)
                )
                .padding(16.dp),
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = SolidColor(accent),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = Color.White.copy(alpha = 0.25f), fontSize = 15.sp)
                }
                inner()
            }
        )

        if (filteredSuggestions.isNotEmpty() && isFocused) {
            Spacer(modifier = Modifier.height(4.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF252525))
                    .padding(vertical = 8.dp)
            ) {
                filteredSuggestions.forEach { suggestion ->
                    Text(
                        text = suggestion,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onValueChange(suggestion)
                                // Hide suggestions
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AvatarPickerRow(
    accent: Color, 
    initial: String, 
    imageUri: Uri? = null,
    onPickImage: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SetupFieldLabel("Identity Visual")
        Spacer(Modifier.height(16.dp))
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(accent, accent.copy(alpha = 0.4f), accent)
                        )
                    )
                    .padding(3.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1A1A1A)),
                contentAlignment = Alignment.Center,
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        initial,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Black,
                        color = accent,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(accent, CircleShape)
                    .border(2.dp, Color(0xFF0A0A0A), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPickImage
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    null,
                    tint = if (accent == Color.White) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Upload Identity Image",
            fontSize = 12.sp,
            color = accent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable(onClick = onPickImage)
        )
    }
}

@Composable
fun UsernameField(
    value: String,
    onValueChange: (String) -> Unit,
    state: UsernameState,
    accent: Color,
) {
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            UsernameState.AVAILABLE -> Color(0xFF00FF85)
            UsernameState.TAKEN -> Color(0xFFFF007A)
            UsernameState.CHECKING -> accent.copy(alpha = 0.5f)
            else -> Color.White.copy(alpha = 0.1f)
        },
        label = "username_border",
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF1A1A1A))
                .border(1.dp, borderColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "@",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                ),
                cursorBrush = SolidColor(accent),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            "choose_username",
                            color = Color.White.copy(alpha = 0.25f),
                            fontSize = 16.sp,
                        )
                    }
                    inner()
                }
            )

            AnimatedContent(targetState = state, label = "username_icon") { s ->
                when (s) {
                    UsernameState.CHECKING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = accent,
                        )
                    }
                    UsernameState.AVAILABLE -> {
                        Icon(
                            Icons.Default.CheckCircle,
                            null,
                            tint = Color(0xFF00FF85),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    UsernameState.TAKEN -> {
                        Icon(
                            Icons.Default.Cancel,
                            null,
                            tint = Color(0xFFFF007A),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    else -> Spacer(Modifier.size(18.dp))
                }
            }
        }

        AnimatedVisibility(visible = state != UsernameState.IDLE) {
            Column {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = when (state) {
                        UsernameState.CHECKING -> "Checking availability..."
                        UsernameState.AVAILABLE -> "✓  @$value is available"
                        UsernameState.TAKEN -> "✗  @$value is already taken"
                        else -> ""
                    },
                    fontSize = 12.sp,
                    color = when (state) {
                        UsernameState.AVAILABLE -> Color(0xFF00FF85)
                        UsernameState.TAKEN -> Color(0xFFFF007A)
                        else -> Color.White.copy(alpha = 0.4f)
                    },
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}

@Composable
fun BioField(
    value: String,
    onValueChange: (String) -> Unit,
    accent: Color,
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor by animateColorAsState(
        targetValue = if (isFocused) accent
        else Color.White.copy(alpha = 0.1f),
        label = "bio_border",
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused }
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1A1A1A))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(16.dp),
        textStyle = TextStyle(
            color = Color.White,
            fontSize = 15.sp,
            lineHeight = 22.sp,
        ),
        cursorBrush = SolidColor(accent),
        maxLines = 5,
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    "Tell us your story...",
                    color = Color.White.copy(alpha = 0.25f),
                    fontSize = 15.sp,
                )
            }
            inner()
        }
    )
}

@Composable
fun AccentColorPicker(selected: Color, onSelect: (Color) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        accentOptions.forEach { color ->
            val isSelected = color == selected
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.2f else 1f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy),
                label = "color_scale",
            )
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 2.5.dp else 0.dp,
                        color = Color.Black,
                        shape = CircleShape,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onSelect(color) },
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        null,
                        tint = if (color == Color.White) Color.Black else Color.White,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun FinishButton(enabled: Boolean, accent: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "btn_scale",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled)
                    Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.7f)))
                else
                    Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.1f),
                            Color.White.copy(alpha = 0.1f),
                        )
                    )
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Activate Identity  →",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = if (enabled) (if (accent == Color.White) Color.Black else Color.White)
            else Color.White.copy(alpha = 0.3f),
        )
    }
}

@Composable
fun SetupFieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = Color.White.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth().padding(start = 4.dp),
    )
}
