package com.example.zell

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.zell.ui.theme.AppTheme
import com.example.zell.ui.theme.LocalAppTheme
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val themeState = LocalAppTheme.current
    var showSkillsOnPosts by remember { mutableStateOf(true) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(Locale.getDefault().displayLanguage) }
    var notificationsEnabled by remember { mutableStateOf(true) }
    var privateAccount by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // ── Profile Card ──────────────────────────────────────
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .clickable { }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Amara Osei", fontWeight = FontWeight.Black, fontSize = 17.sp)
                            Text("@amaraosei · View profile", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // ── Account ───────────────────────────────────────────
            item {
                SettingsSectionCard {
                    SettingsSectionHeader("Account")
                    SettingsRowItem(Icons.Outlined.Person, Color(0xFF5C6BC0), "Personal information", showDivider = true) {}
                    SettingsRowItem(Icons.Outlined.Lock, Color(0xFF26A69A), "Password & security", showDivider = true) {}
                    SettingsRowItem(Icons.Outlined.Notifications, Color(0xFFEF6C00), "Notifications", showDivider = true) {}
                    SettingsToggleRow(Icons.Outlined.Lock, Color(0xFF8E24AA), "Private account", privateAccount, showDivider = false) { privateAccount = it }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Content & Display ─────────────────────────────────
            item {
                SettingsSectionCard {
                    SettingsSectionHeader("Content & Display")
                    SettingsToggleRow(Icons.Outlined.DarkMode, Color(0xFF37474F), "Dark mode", themeState.value == AppTheme.DARK, showDivider = true) { isDark ->
                        themeState.value = if (isDark) AppTheme.DARK else AppTheme.LIGHT
                    }
                    SettingsToggleRow(Icons.Outlined.Badge, Color(0xFFC5A059), "Show skill badges on posts", showSkillsOnPosts, showDivider = true) { showSkillsOnPosts = it }
                    SettingsRowItem(Icons.Outlined.Language, Color(0xFF1565C0), "Language", subtitle = currentLanguage, showDivider = true) { showLanguageDialog = true }
                    SettingsRowItem(Icons.Outlined.ColorLens, Color(0xFFAD1457), "App theme colour", showDivider = false) {}
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Your Activity ─────────────────────────────────────
            item {
                SettingsSectionCard {
                    SettingsSectionHeader("Your Activity")
                    SettingsRowItem(Icons.Outlined.Bookmarks, Color(0xFF00838F), "Saved posts", showDivider = true) {}
                    SettingsRowItem(Icons.Outlined.History, Color(0xFF558B2F), "Recently viewed", showDivider = true) {}
                    SettingsRowItem(Icons.Outlined.Block, Color(0xFFD32F2F), "Blocked accounts", showDivider = false) {}
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Support ───────────────────────────────────────────
            item {
                SettingsSectionCard {
                    SettingsSectionHeader("Support")
                    SettingsRowItem(Icons.AutoMirrored.Outlined.HelpOutline, Color(0xFF039BE5), "Help centre", showDivider = true) {}
                    SettingsRowItem(Icons.Outlined.Info, Color(0xFF546E7A), "About Zell", showDivider = true) {}
                    SettingsRowItem(Icons.Outlined.Shield, Color(0xFF43A047), "Privacy policy", showDivider = false) {}
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── Danger Zone ───────────────────────────────────────
            item {
                SettingsSectionCard {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onLogout)
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFD32F2F).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Text("Log out", color = Color(0xFFD32F2F), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Version ───────────────────────────────────────────
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Text("Zell", fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = (-0.5).sp, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("Version 1.0.0 Alpha", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
            }
        }

        if (showLanguageDialog) {
            LanguageSelectionDialog(
                onDismiss = { showLanguageDialog = false },
                onLanguageSelected = { langCode, langName ->
                    currentLanguage = langName
                    showLanguageDialog = false
                    updateLocale(context, langCode)
                }
            )
        }
    }
}

// ── Reusable Section Wrappers ─────────────────────────────────────────────────

@Composable
fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.06f))
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        letterSpacing = 1.2.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingsRowItem(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String? = null,
    showDivider: Boolean = true,
    onClick: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f), modifier = Modifier.size(20.dp))
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 66.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    checked: Boolean,
    showDivider: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(start = 66.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.08f)
            )
        }
    }
}

// ── Keep existing helpers ─────────────────────────────────────────────────────

@Composable
fun LanguageSelectionDialog(
    onDismiss: () -> Unit,
    onLanguageSelected: (String, String) -> Unit
) {
    val languages = listOf(
        "en" to "English",
        "es" to "Español",
        "fr" to "Français",
        "de" to "Deutsch",
        "yo" to "Yorùbá",
        "ig" to "Igbo",
        "ha" to "Hausa",
        "ar" to "العربية"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Language", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(250.dp)) {
                items(languages) { language ->
                    val name = language.second
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLanguageSelected(language.first, name) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(name, modifier = Modifier.weight(1f), fontSize = 16.sp)
                        RadioButton(selected = false, onClick = { onLanguageSelected(language.first, name) })
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun updateLocale(context: Context, languageCode: String) {
    val locale = Locale(languageCode)
    Locale.setDefault(locale)
    val resources = context.resources
    val configuration = resources.configuration
    configuration.setLocale(locale)
    @Suppress("DEPRECATION")
    resources.updateConfiguration(configuration, resources.displayMetrics)
    val activity = context.findActivity()
    activity?.recreate()
}

fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

// kept for backward compat, no longer used in UI but referenced nowhere — safe to keep
@Composable
fun SettingsHeader(title: String) {
    Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, value: String? = null, onClick: () -> Unit = {}) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp)
        if (value != null) Text(value, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(end = 8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun SettingsToggleItem(icon: ImageVector, title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onCheckedChange(!checked) }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}
