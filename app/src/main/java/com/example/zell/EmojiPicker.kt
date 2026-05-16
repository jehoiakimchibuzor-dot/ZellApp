package com.example.zell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ZellEmojiPicker — Messenger-style emoji bottom sheet.
 *
 * Slides up from the bottom with category tabs at the top.
 * Tapping any emoji calls onEmojiSelected and stays open
 * so the user can pick multiple emojis before dismissing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZellEmojiPicker(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp) // fixed height so it doesn't take the whole screen
        ) {
            // Category tab row
            ScrollableTabRow(
                selectedTabIndex = selectedCategory,
                edgePadding = 8.dp,
                divider = {}
            ) {
                EMOJI_CATEGORIES.forEachIndexed { index, category ->
                    Tab(
                        selected = selectedCategory == index,
                        onClick = { selectedCategory = index },
                        text = {
                            Text(
                                text = category.icon,
                                fontSize = 20.sp
                            )
                        }
                    )
                }
            }

            // Category label
            Text(
                text = EMOJI_CATEGORIES[selectedCategory].name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Emoji grid — 8 columns like most emoji keyboards
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(EMOJI_CATEGORIES[selectedCategory].emojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clickable { onEmojiSelected(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ─── Emoji Data ───────────────────────────────────────────────────────────────

data class EmojiCategory(val name: String, val icon: String, val emojis: List<String>)

val EMOJI_CATEGORIES = listOf(
    EmojiCategory(
        name = "Smileys",
        icon = "😊",
        emojis = listOf(
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
            "🙂", "🙃", "😉", "😊", "😇", "🥰", "😍", "🤩",
            "😘", "😗", "😚", "😙", "😋", "😛", "😜", "🤪",
            "😝", "🤑", "🤗", "🤭", "🤫", "🤔", "😐", "😑",
            "😶", "😏", "😒", "🙄", "😬", "🤥", "😌", "😔",
            "😪", "😴", "😷", "🤒", "🤕", "🥺", "😢", "😭",
            "😤", "😠", "😡", "🤬", "🥳", "😎", "🤓", "🧐",
            "😈", "👿", "💀", "☠️", "💩", "🤡", "👻", "👽"
        )
    ),
    EmojiCategory(
        name = "Gestures",
        icon = "👋",
        emojis = listOf(
            "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏",
            "✌️", "🤞", "🤟", "🤘", "🤙", "👈", "👉", "👆",
            "👇", "☝️", "👍", "👎", "✊", "👊", "🤛", "🤜",
            "👏", "🙌", "👐", "🤲", "🤝", "🙏", "💪", "🦾",
            "🫀", "🧠", "👀", "👅", "👄", "💋", "🫶", "❤️‍🔥"
        )
    ),
    EmojiCategory(
        name = "Hearts",
        icon = "❤️",
        emojis = listOf(
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍",
            "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖",
            "💘", "💝", "💟", "♥️", "❤️‍🩹", "❤️‍🔥", "💯", "✨",
            "🌟", "⭐", "🔥", "💫", "🌈", "☀️", "🌙", "⚡"
        )
    ),
    EmojiCategory(
        name = "Animals",
        icon = "🐶",
        emojis = listOf(
            "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼",
            "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🙈",
            "🙉", "🙊", "🐔", "🐧", "🐦", "🦆", "🦅", "🦉",
            "🦇", "🐺", "🐴", "🦄", "🐝", "🦋", "🐌", "🐞",
            "🐢", "🐍", "🦎", "🐙", "🦑", "🐠", "🐬", "🐋",
            "🦈", "🦁", "🐘", "🦒", "🦓", "🦊", "🐿️", "🦔"
        )
    ),
    EmojiCategory(
        name = "Food",
        icon = "🍕",
        emojis = listOf(
            "🍏", "🍎", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓",
            "🫐", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅",
            "🍆", "🥑", "🥦", "🌽", "🥕", "🧄", "🧅", "🍄",
            "🍕", "🍔", "🍟", "🌭", "🍿", "🧀", "🍳", "🥞",
            "🧇", "🍗", "🍖", "🌮", "🌯", "🍜", "🍝", "🍛",
            "🍣", "🍱", "🍩", "🍪", "🎂", "🍰", "🧁", "🍫",
            "☕", "🧋", "🍺", "🥂", "🍷", "🥤", "🧃", "🍾"
        )
    ),
    EmojiCategory(
        name = "Activities",
        icon = "🎉",
        emojis = listOf(
            "🎉", "🎊", "🎈", "🎁", "🎀", "🏆", "🥇", "🥈",
            "🥉", "🏅", "🎖️", "🎗️", "🎭", "🎨", "🎬", "🎤",
            "🎧", "🎵", "🎶", "🎸", "🎹", "🎺", "🎻", "🥁",
            "🎯", "🎱", "🎮", "🕹️", "🎲", "🧩", "♟️", "🎳",
            "⚽", "🏀", "🏈", "⚾", "🥎", "🏐", "🏉", "🎾",
            "🏓", "🏸", "🥊", "🛹", "🛷", "🏋️", "🤸", "🏊"
        )
    ),
    EmojiCategory(
        name = "Travel",
        icon = "✈️",
        emojis = listOf(
            "✈️", "🚀", "🛸", "🚁", "🛶", "⛵", "🚢", "🚂",
            "🚃", "🚄", "🚅", "🚆", "🚇", "🚈", "🚉", "🚊",
            "🚝", "🚞", "🚋", "🚌", "🚍", "🚎", "🚐", "🚑",
            "🚒", "🚓", "🚔", "🚕", "🚗", "🚙", "🛻", "🚚",
            "🏠", "🏡", "🏢", "🏣", "🏤", "🏥", "🏦", "🏨",
            "🏩", "🏪", "🏫", "🏬", "🏭", "🏯", "🏰", "🗼",
            "🗽", "⛪", "🌁", "🌃", "🌄", "🌅", "🌆", "🌇"
        )
    ),
    EmojiCategory(
        name = "Symbols",
        icon = "💡",
        emojis = listOf(
            "💡", "🔥", "💧", "🌊", "🌀", "⚡", "❄️", "🌈",
            "☀️", "🌤️", "⛅", "🌥️", "☁️", "🌦️", "🌧️", "⛈️",
            "✅", "❌", "❓", "❗", "💬", "💭", "🗯️", "💤",
            "🔔", "🔕", "🎵", "🎶", "💰", "💳", "📱", "💻",
            "⌨️", "🖥️", "🖨️", "🖱️", "📷", "📸", "📹", "🎥",
            "📞", "☎️", "📟", "📠", "📺", "📻", "🧭", "⏰",
            "⌚", "📅", "📆", "📊", "📈", "📉", "🔑", "🗝️"
        )
    )
)
