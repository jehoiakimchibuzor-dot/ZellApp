package com.example.zell

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun PinDetailScreen(
    item: DiscoverItem,
    onBack: () -> Unit
) {
    var isSaved by remember { mutableStateOf(false) }
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableIntStateOf(4218) }

    val relatedItems = remember(item.category) {
        mockDiscover.filter { it.category == item.category && it.id != item.id }
            .ifEmpty { mockDiscover.take(4) }
    }

    // Extended mock related for "More like this" section
    val moreItems = remember {
        (mockDiscover + mockDiscover).take(8)
    }

    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Main Pin Image (full-width, no crop height limit) ──
            Box {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    contentScale = ContentScale.FillWidth
                )
                // Top gradient for back button visibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.4f), Color.Transparent)))
                )
                // Back + More buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(Icons.Outlined.Share, null, tint = Color.White)
                        }
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.White)
                        }
                    }
                }
            }

            // ── Pin Content ──────────────────────────────────────────
            Column(modifier = Modifier.padding(20.dp)) {
                // Category chip
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        item.category.uppercase(),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    item.title,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 32.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Discover the best of ${item.category} on Zell. Inspiring layouts, colours, and creative energy for your digital lifestyle.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                    lineHeight = 23.sp
                )

                Spacer(Modifier.height(20.dp))

                // ── Creator Row ───────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Zell Curations", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("12.4K followers", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = {},
                        shape = RoundedCornerShape(20.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onBackground,
                            contentColor = MaterialTheme.colorScheme.background
                        )
                    ) {
                        Text("Follow", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Action Row ────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Like
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
                            .clickable {
                                isLiked = !isLiked
                                likeCount += if (isLiked) 1 else -1
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            null,
                            tint = if (isLiked) Color.Red else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("$likeCount", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    // Save button — Pinterest red style
                    Button(
                        onClick = { isSaved = !isSaved },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSaved) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFE60023),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(42.dp)
                    ) {
                        Icon(
                            if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            null,
                            modifier = Modifier.size(18.dp),
                            tint = if (isSaved) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isSaved) "Saved" else "Save",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = if (isSaved) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                    }

                    // Share
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .size(42.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f), RoundedCornerShape(12.dp))
                    ) {
                        Icon(Icons.Outlined.Share, null, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(Modifier.height(28.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                Spacer(Modifier.height(24.dp))

                // ── More Like This ────────────────────────────────────
                Text("More like this", fontWeight = FontWeight.Black, fontSize = 20.sp, letterSpacing = (-0.3).sp)
                Spacer(Modifier.height(16.dp))
            }

            // Pinterest-style staggered grid of related pins
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 1200.dp),  // bounded height within scroll
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalItemSpacing = 10.dp
            ) {
                items(moreItems) { related ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { }
                    ) {
                        AsyncImage(
                            model = related.imageUrl,
                            contentDescription = related.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(related.height),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                                    )
                                )
                        )
                        // Title
                        Text(
                            related.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        )
                        // Mini save button
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE60023))
                                .clickable { }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(120.dp))
        }
    }
}
