package com.example.zell

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pixel-Perfect Pinterest Discover Engine.
 * 🔧 REFACTORED: Added MaterialTheme support and Error handling (doneby Gemini)
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DiscoverTab(
    onPinClick: (DiscoverItem) -> Unit,
    onCreatePinClick: () -> Unit = {},
    viewModel: DiscoverViewModel = viewModel(),
    boardViewModel: BoardViewModel = viewModel(),
    userViewModel: UserViewModel = viewModel()
) {
    val discoverItems = viewModel.discoverItems
    val isLoading = viewModel.isLoading
    val isLoadingMore = viewModel.isLoadingMore
    val error by viewModel.error
    val gridState = rememberLazyStaggeredGridState()
    val currentUser by userViewModel.currentUserProfile
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var selectedPinToSave by remember { mutableStateOf<DiscoverItem?>(null) }
    val sheetState = rememberModalBottomSheetState()
    var showBoardSheet by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val columnCount = when {
            this.maxWidth < 600.dp -> 2
            this.maxWidth < 1200.dp -> 4
            else -> 6
        }

        val shouldLoadMore = remember {
            derivedStateOf {
                val totalItems = gridState.layoutInfo.totalItemsCount
                val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                lastVisibleItem >= totalItems - 4 && totalItems > 0 && !isLoadingMore
            }
        }
        LaunchedEffect(shouldLoadMore.value) {
            if (shouldLoadMore.value) viewModel.loadMore()
        }

        Scaffold(
            topBar = {
                PinterestTopNav(
                    userAvatar = currentUser.avatarUrl,
                    onCreateClick = onCreatePinClick
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                
                // 🔧 REFACTORED: Integrated global ErrorBanner for Discover feed (doneby Gemini)
                if (error != null) {
                    ErrorBanner(error = error, onDismiss = { viewModel.clearError() })
                }

                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Fixed(columnCount),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 16.dp
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Column {
                            TodayStoriesRow()
                            DiscoverFilterRow()
                        }
                    }

                    if (isLoading && discoverItems.isEmpty()) {
                        items(10) { PinSkeletonCard() }
                    } else {
                        itemsIndexed(discoverItems, key = { _, item -> item.id }) { index, item ->
                            var visible by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) { delay(index % 10 * 30L); visible = true }

                            AnimatedVisibility(
                                visible = visible,
                                enter = scaleIn(initialScale = 0.9f) + fadeIn(),
                                label = "pin_entrance"
                            ) {
                                PinterestPinCard(
                                    pin = item,
                                    onClick = { onPinClick(item) },
                                    onSaveClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        selectedPinToSave = item
                                        showBoardSheet = true
                                    }
                                )
                            }
                        }
                    }

                    if (isLoadingMore) {
                        item(span = StaggeredGridItemSpan.FullLine) {
                            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 3.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBoardSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBoardSheet = false },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            BoardSelectorContent(
                boards = boardViewModel.boards,
                onBoardSelected = { board ->
                    selectedPinToSave?.let { pin ->
                        boardViewModel.savePinToBoard(board.id, pin.id, pin.imageUrl)
                    }
                    scope.launch { sheetState.hide() }.invokeOnCompletion { showBoardSheet = false }
                },
                onCreateBoard = { name -> boardViewModel.createBoard(name) }
            )
        }
    }
}

@Composable
fun PinterestTopNav(userAvatar: String, onCreateClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 2.dp) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(color = Color(0xFFE60023), shape = CircleShape, modifier = Modifier.size(34.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("P", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }

                Surface(
                    modifier = Modifier.weight(1f).height(44.dp), 
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), 
                    shape = RoundedCornerShape(24.dp), 
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Search", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.CameraAlt, "Visual Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
                    }
                }

                IconButton(onClick = {}) { Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.onBackground) }
                
                Surface(
                    color = Color(0xFFE60023),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(36.dp).clickable { onCreateClick() }
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text("Create", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                AsyncImage(
                    model = userAvatar, 
                    contentDescription = "Profile", 
                    modifier = Modifier.size(32.dp).clip(CircleShape), 
                    contentScale = ContentScale.Crop
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), 
                horizontalArrangement = Arrangement.Center, 
                verticalAlignment = Alignment.CenterVertically
            ) {
                PinterestTabPill("Home", true)
                Spacer(Modifier.width(12.dp))
                PinterestTabPill("Today", false)
                Spacer(Modifier.width(12.dp))
                PinterestTabPill("Following", false)
            }
        }
    }
}

@Composable
fun PinterestTabPill(text: String, isSelected: Boolean) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Transparent, 
        shape = RoundedCornerShape(24.dp)
    ) {
        Text(
            text = text, 
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), 
            color = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground, 
            fontWeight = FontWeight.Bold, 
            fontSize = 14.sp
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PinterestPinCard(pin: DiscoverItem, onClick: () -> Unit, onSaveClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "bounce")

    Column(
        modifier = Modifier.fillMaxWidth().scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onSaveClick
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            AsyncImage(
                model = pin.imageUrl,
                contentDescription = pin.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f / pin.aspectRatio),
                contentScale = ContentScale.Crop
            )

            Box(modifier = Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.Black.copy(0.02f), Color.Transparent, Color.Black.copy(0.12f)))))

            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(10.dp).clickable { onSaveClick() }, 
                color = Color(0xFFE60023), 
                shape = RoundedCornerShape(24.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
            
            Surface(modifier = Modifier.align(Alignment.TopStart).padding(10.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f), shape = CircleShape) {
                Icon(Icons.Default.MoreHoriz, null, modifier = Modifier.size(24.dp).padding(4.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        Column(modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)) {
            if (pin.title.isNotBlank()) {
                Text(text = pin.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = pin.creatorAvatar, contentDescription = null, modifier = Modifier.size(22.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(8.dp))
                Text(text = pin.creatorName, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.width(4.dp))
                    Text(pin.likesCount.toString(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Outlined.ChatBubbleOutline, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun TodayStoriesRow() {
    val items = listOf(
        "Today's Picks" to "https://images.unsplash.com/photo-1506744038136-46273834b3fb?w=200",
        "Home Decor" to "https://images.unsplash.com/photo-1484154218962-a197022b5858?w=200",
        "Autumn Fashion" to "https://images.unsplash.com/photo-1483985988355-763728e1935b?w=200",
        "Minimalism" to "https://images.unsplash.com/photo-1500835595337-f7560799480d?w=200"
    )
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), 
        contentPadding = PaddingValues(horizontal = 8.dp), 
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { (label, url) ->
            Box(modifier = Modifier.size(width = 150.dp, height = 90.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                AsyncImage(model = url, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                Text(text = label, modifier = Modifier.align(Alignment.Center), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun PinSkeletonCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f, 
        targetValue = 0.6f, 
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), 
        label = "alpha"
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().height((180..300).random().dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.width(100.dp).height(12.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha)))
    }
}

@Composable
fun DiscoverFilterRow() {
    val filters = listOf("All", "Interior", "Design", "Food", "Fashion", "Travel", "Cars")
    var selected by remember { mutableStateOf("All") }
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
        contentPadding = PaddingValues(horizontal = 8.dp), 
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filters) { filter ->
            FilterChip(
                selected = selected == filter, 
                onClick = { selected = filter }, 
                label = { Text(filter) }, 
                shape = RoundedCornerShape(24.dp), 
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.onBackground, 
                    selectedLabelColor = MaterialTheme.colorScheme.background,
                    labelColor = MaterialTheme.colorScheme.onBackground
                ), 
                border = null
            )
        }
    }
}

@Composable
fun BoardSelectorContent(boards: List<PinBoard>, onBoardSelected: (PinBoard) -> Unit, onCreateBoard: (String) -> Unit) {
    var newBoardName by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp)) {
        Text("Save to board", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 24.dp), color = MaterialTheme.colorScheme.onSurface)
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(boards) { board ->
                Row(modifier = Modifier.fillMaxWidth().clickable { onBoardSelected(board) }.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                        if (board.thumbnails.isNotEmpty()) AsyncImage(model = board.thumbnails.first(), contentDescription = null, contentScale = ContentScale.Crop)
                        else Icon(Icons.Default.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(board.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text("${board.pinCount} pins", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 32.dp)) {
            Box(modifier = Modifier.weight(1f).clip(RoundedCornerShape(24.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 16.dp, vertical = 12.dp)) {
                BasicTextField(
                    value = newBoardName, 
                    onValueChange = { newBoardName = it }, 
                    modifier = Modifier.fillMaxWidth(), 
                    textStyle = TextStyle(fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface), 
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { inner -> if (newBoardName.isEmpty()) Text("Create board...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)); inner() }
                )
            }
            Spacer(Modifier.width(12.dp))
            IconButton(
                onClick = { if(newBoardName.isNotBlank()) onCreateBoard(newBoardName); newBoardName = "" }, 
                modifier = Modifier.background(Color(0xFFE60023), CircleShape).size(40.dp)
            ) { Icon(Icons.Default.Add, null, tint = Color.White) }
        }
    }
}
