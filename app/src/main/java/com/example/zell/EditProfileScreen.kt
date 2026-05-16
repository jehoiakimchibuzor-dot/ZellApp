package com.example.zell

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

/**
 * LinkedIn-style Edit Profile screen.
 *
 * Sections:
 *  • Intro card  — name, bio, location, website (the "top card" on LinkedIn)
 *  • About card  — long-form description
 *  • Education   — institution
 *  • Skills      — comma-separated chips
 *  • Theme       — accent colour picker
 *
 * Compose rule: rememberLauncherForActivityResult lives at the TOP level of the
 * composable, never inside a nested layout or condition.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    initialUser: StoryUser,
    onBack: () -> Unit,
    onSave: (
        name: String,
        bio: String,
        location: String,
        website: String,
        about: String?,
        skills: String?,
        institution: String?,
        themeColor: String?,
        avatarUri: Uri?
    ) -> Unit
) {
    // ── Form state ────────────────────────────────────────────────────────────
    var name          by remember { mutableStateOf(initialUser.name) }
    var bio           by remember { mutableStateOf(initialUser.bio) }
    var location      by remember { mutableStateOf(initialUser.location) }
    var website       by remember { mutableStateOf(initialUser.website) }
    var about         by remember { mutableStateOf(initialUser.about ?: "") }
    var skills        by remember { mutableStateOf(initialUser.skills ?: "") }
    var institution   by remember { mutableStateOf(initialUser.institution ?: "") }
    var avatarUri     by remember { mutableStateOf<Uri?>(null) }
    var selectedTheme by remember { mutableStateOf(initialUser.themeColor ?: "#0A66C2") }
    var isSaving      by remember { mutableStateOf(false) }

    // ── Avatar picker — MUST be at the top level of the composable ────────────
    val avatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) avatarUri = uri }

    // ── Derive theme colour for preview ──────────────────────────────────────
    val themeColorObj = remember(selectedTheme) {
        runCatching { Color(android.graphics.Color.parseColor(selectedTheme)) }
            .getOrDefault(Color(0xFF0A66C2))
    }

    // ── Colour palette ────────────────────────────────────────────────────────
    val themeColors = listOf(
        "#0A66C2" to Color(0xFF0A66C2),
        "#FF6B6B" to Color(0xFFFF6B6B),
        "#4ECDC4" to Color(0xFF4ECDC4),
        "#FFE66D" to Color(0xFFFFE66D),
        "#A8E6CF" to Color(0xFFA8E6CF),
        "#FF8B94" to Color(0xFFFF8B94),
        "#AA96DA" to Color(0xFFAA96DA),
        "#FCBAD3" to Color(0xFFFCBAD3),
        "#A8D8EA" to Color(0xFFA8D8EA),
        "#95E1D3" to Color(0xFF95E1D3)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            isSaving = true
                            onSave(
                                name,
                                bio,
                                location,
                                website,
                                about.ifBlank { null },
                                skills.ifBlank { null },
                                institution.ifBlank { null },
                                selectedTheme,
                                avatarUri
                            )
                        },
                        enabled  = !isSaving && name.isNotBlank(),
                        shape    = RoundedCornerShape(20.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = themeColorObj),
                        modifier = Modifier.padding(end = 8.dp).height(36.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(16.dp),
                                color       = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Save", fontWeight = FontWeight.Bold, fontSize = 14.sp)
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
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
        ) {

            // ── 1. INTRO CARD ─────────────────────────────────────────────────
            EditCard(title = "Intro") {
                // Taller banner — 160dp so the avatar is clearly visible
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(themeColorObj.copy(alpha = 0.85f), themeColorObj.copy(alpha = 0.35f))
                            )
                        )
                ) {
                    // "Tap to change cover" hint
                    Text(
                        "Tap photo to change ↓",
                        modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
                        fontSize = 11.sp,
                        color    = Color.White.copy(alpha = 0.7f)
                    )
                }

                // Avatar — 120dp, clearly tappable, floats below banner
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.TopStart
                ) {
                    Box(
                        modifier = Modifier
                            .offset(x = 12.dp, y = (-52).dp)
                            .size(120.dp)
                            .clip(CircleShape)
                            .border(4.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clickable { avatarLauncher.launch("image/*") }
                    ) {
                        AsyncImage(
                            model              = avatarUri ?: initialUser.avatarUrl,
                            contentDescription = "Profile photo",
                            modifier           = Modifier.fillMaxSize(),
                            contentScale       = ContentScale.Crop
                        )
                        // Semi-dark overlay so camera icon is always visible
                        Box(
                            modifier         = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.30f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, "Change photo", tint = Color.White, modifier = Modifier.size(28.dp))
                                Text("Change", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(0.dp)) // avatar offset handles the spacing

                ProfileField(
                    label         = "Name *",
                    value         = name,
                    onValueChange = { name = it },
                    hint          = "Your full name"
                )
                ProfileField(
                    label         = "Headline / Bio",
                    value         = bio,
                    onValueChange = { bio = it },
                    singleLine    = false,
                    hint          = "Software engineer • Designer • Founder…"
                )
                ProfileField(
                    label         = "Location",
                    value         = location,
                    onValueChange = { location = it },
                    hint          = "City, Country"
                )
                ProfileField(
                    label         = "Website / Portfolio",
                    value         = website,
                    onValueChange = { website = it },
                    hint          = "https://yoursite.com"
                )
            }

            // ── 2. ABOUT CARD ─────────────────────────────────────────────────
            EditCard(title = "About") {
                Text(
                    "Share your story — projects, values, what drives you.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                ProfileField(
                    label         = "",
                    value         = about,
                    onValueChange = { about = it },
                    singleLine    = false,
                    hint          = "I'm a product designer passionate about building tools that matter…",
                    minLines      = 5
                )
            }

            // ── 3. EDUCATION CARD (with auto-suggest) ─────────────────────────
            EditCard(title = "Education") {
                SuggestField(
                    label         = "Institution",
                    value         = institution,
                    onValueChange = { institution = it },
                    hint          = "e.g. University of Lagos, Google, Andela",
                    suggestions   = listOf(
                        "University of Lagos", "University of Ibadan", "Covenant University",
                        "Obafemi Awolowo University", "University of Benin",
                        "Google", "Microsoft", "Meta", "Amazon", "Apple",
                        "Andela", "Flutterwave", "Paystack", "Interswitch", "MTN"
                    ),
                    accentColor   = themeColorObj
                )
            }

            // ── 4. SKILLS CARD (with auto-suggest + live chips) ───────────────
            EditCard(title = "Skills") {
                Text(
                    "Separate skills with commas. Tap a suggestion to add.",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                SuggestField(
                    label         = "",
                    value         = skills,
                    onValueChange = { skills = it },
                    hint          = "e.g. Kotlin, Product Design, Marketing",
                    singleLine    = false,
                    suggestions   = listOf(
                        "Kotlin", "Jetpack Compose", "Android Development", "iOS Development",
                        "Flutter", "React Native", "JavaScript", "TypeScript", "Python",
                        "Product Design", "UI/UX Design", "Figma", "Graphic Design",
                        "Product Management", "Project Management", "Data Analysis",
                        "Machine Learning", "Backend Development", "DevOps",
                        "Marketing", "Content Creation", "Copywriting", "Sales"
                    ),
                    // For skills, appending comma-separated
                    onSuggestionPick = { suggestion ->
                        skills = if (skills.isBlank()) suggestion
                                 else "${skills.trimEnd().trimEnd(',')}, $suggestion"
                    },
                    accentColor   = themeColorObj
                )
                // Live chip preview
                if (skills.isNotBlank()) {
                    val chips = skills.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    if (chips.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement   = Arrangement.spacedBy(6.dp)
                        ) {
                            chips.forEach { chip ->
                                Surface(
                                    shape  = RoundedCornerShape(20.dp),
                                    color  = themeColorObj.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, themeColorObj.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        chip,
                                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        fontSize   = 12.sp,
                                        color      = themeColorObj,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 5. THEME COLOUR CARD ──────────────────────────────────────────
            EditCard(title = "Profile Theme Colour") {
                Text(
                    "Sets your banner and accent colour across your profile.",
                    fontSize = 12.sp,
                    color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    themeColors.forEach { (hex, color) ->
                        val isSelected = selectedTheme == hex
                        Box(
                            modifier         = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(color)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedTheme = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    Icons.Default.Check, null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(48.dp))
        }
    }
}

// ── Section card wrapper ──────────────────────────────────────────────────────

@Composable
private fun EditCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(0.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (title.isNotEmpty()) {
                Text(
                    title,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 17.sp,
                    color      = MaterialTheme.colorScheme.onSurface,
                    modifier   = Modifier.padding(bottom = 12.dp)
                )
            }
            content()
        }
    }
}

// ── Form field ────────────────────────────────────────────────────────────────

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    hint: String = "",
    minLines: Int = 1
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        if (label.isNotEmpty()) {
            Text(
                label,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier   = Modifier.padding(bottom = 4.dp)
            )
        }
        OutlinedTextField(
            value         = value,
            onValueChange = onValueChange,
            placeholder   = {
                Text(hint, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f))
            },
            modifier      = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = if (singleLine) 52.dp else (52 + (minLines - 1) * 24).dp),
            singleLine    = singleLine,
            minLines      = minLines,
            maxLines      = if (singleLine) 1 else 10,
            shape         = RoundedCornerShape(10.dp),
            colors        = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            )
        )
    }
}

// ── Auto-suggest field ────────────────────────────────────────────────────────

/**
 * OutlinedTextField that shows a filtered suggestion dropdown while the user types.
 *
 * @param onSuggestionPick  Optional custom handler when a suggestion is tapped.
 *                          If null, the suggestion replaces the whole field value.
 *                          The Skills card passes a lambda that appends comma-separated.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    hint: String = "",
    singleLine: Boolean = true,
    suggestions: List<String> = emptyList(),
    onSuggestionPick: ((String) -> Unit)? = null,
    accentColor: Color = Color(0xFF0A66C2)
) {
    // For skills (comma-separated), filter by the last segment after the final comma
    val query = value.split(",").lastOrNull()?.trim() ?: value.trim()

    val filtered = remember(query) {
        if (query.isEmpty()) emptyList()
        else suggestions.filter { it.contains(query, ignoreCase = true) }.take(5)
    }

    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(filtered) { expanded = filtered.isNotEmpty() }

    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        if (label.isNotEmpty()) {
            Text(
                label,
                fontSize   = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier   = Modifier.padding(bottom = 4.dp)
            )
        }

        ExposedDropdownMenuBox(
            expanded         = expanded,
            onExpandedChange = { /* controlled by filtered list */ }
        ) {
            OutlinedTextField(
                value         = value,
                onValueChange = onValueChange,
                placeholder   = {
                    Text(
                        hint,
                        fontSize = 14.sp,
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                    )
                },
                modifier   = Modifier
                    .fillMaxWidth()
                    .menuAnchor(MenuAnchorType.PrimaryEditable),
                singleLine = singleLine,
                maxLines   = if (singleLine) 1 else 6,
                shape      = RoundedCornerShape(10.dp),
                colors     = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = accentColor,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                )
            )

            if (filtered.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded         = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    filtered.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(suggestion, fontSize = 14.sp) },
                            onClick = {
                                if (onSuggestionPick != null) {
                                    onSuggestionPick(suggestion)
                                } else {
                                    onValueChange(suggestion)
                                }
                                expanded = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint     = accentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

// ── Public alias so any existing callers (e.g. SettingsScreen) still compile ─

@Composable
fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    singleLine: Boolean = true,
    hint: String = ""
) {
    ProfileField(label = label, value = value, onValueChange = onValueChange, singleLine = singleLine, hint = hint)
}
