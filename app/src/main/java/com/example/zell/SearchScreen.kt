package com.example.zell

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onUserClick: (String) -> Unit = {},
    onPostClick: (String) -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Top", "Latest", "People", "Photos", "Spaces")

    val recentSearches = listOf("Design Systems", "Lagos Tech", "Kotlin Multiplatform", "Zell App")
    val suggestedPeople = listOf(
        StoryUser("10", "Tunde Sanusi", "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200"),
        StoryUser("11", "Chinelo A.", "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200"),
        StoryUser("12", "Ibrahim K.", "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200")
    )

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .animateContentSize(spring(dampingRatio = Spring.DampingRatioLowBouncy))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Search, 
                                contentDescription = null, 
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                modifier = Modifier.weight(1f).onFocusChanged { focusState -> isFocused = focusState.isFocused },
                                textStyle = TextStyle(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 16.sp
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                singleLine = true,
                                decorationBox = { inner ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search Zell", 
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    inner()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                    AnimatedVisibility(
                        visible = isFocused || searchQuery.isNotEmpty(),
                        enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
                    ) {
                        TextButton(onClick = { 
                            searchQuery = "" 
                            isFocused = false
                        }) {
                            Text("Cancel", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                
                if (searchQuery.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        edgePadding = 16.dp,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                                color = MaterialTheme.colorScheme.primary,
                                height = 3.dp
                            )
                        },
                        divider = {}
                    ) {
                        tabs.forEachIndexed { index, title ->
                            val isSelected = selectedTabIndex == index
                            Tab(
                                selected = isSelected,
                                onClick = { selectedTabIndex = index },
                                text = { 
                                    Text(
                                        title, 
                                        fontSize = 14.sp, 
                                        fontWeight = if(isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if(isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) 
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        val staggerDelay = 40L
        if (searchQuery.isEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                item { SectionHeader("Recent Searches") }
                itemsIndexed(recentSearches) { index, search ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay(index * staggerDelay); visible = true }
                    AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })) {
                        RecentSearchItem(search) { searchQuery = search }
                    }
                }
                
                item { SectionHeader("People you may know") }
                itemsIndexed(suggestedPeople) { index, user ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) { delay((index + 2) * staggerDelay); visible = true }
                    AnimatedVisibility(visible = visible, enter = fadeIn() + slideInVertically(initialOffsetY = { 20 })) {
                        SuggestedPersonItem(user, onUserClick)
                    }
                }
                
                item {
                    SectionHeader("Trending for you")
                    Column(modifier = Modifier.padding(bottom = 32.dp)) {
                        TrendingItem("1", "Technology · Trending", "#ZellApp", "12.5K posts")
                        TrendingItem("2", "Design · Trending", "Minimalism", "8.2K posts")
                        TrendingItem("3", "Business · Trending", "Startup Ecosystem", "5.1K posts")
                    }
                }
            }
        } else {
            if (searchQuery.length > 20) {
                EmptySearchState(searchQuery)
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(MaterialTheme.colorScheme.background),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(List(30) { it }) { index, _ ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(searchQuery) { 
                            visible = false
                            delay(index * 25L) 
                            visible = true 
                        }
                        AnimatedVisibility(visible = visible, enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()) {
                            DynamicImageCard(query = searchQuery, index = index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptySearchState(query: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(160.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Search, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            }
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "No results for \"$query\"", 
            style = MaterialTheme.typography.titleLarge, 
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "We couldn't find anything matching your search. Try adjusting your filters or search terms.", 
            style = MaterialTheme.typography.bodyMedium, 
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = { }, shape = RoundedCornerShape(16.dp)) {
            Text("Clear Search", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun DynamicImageCard(query: String, index: Int) {
    val keywordUrl = "https://images.unsplash.com/featured/?$query&sig=$index"
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (index % 3 == 0) 240.dp else if (index % 2 == 0) 170.dp else 210.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Box {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(keywordUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = query,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.4f)),
                            startY = 300f
                        )
                    )
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp)
    )
}

@Composable
fun RecentSearchItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(text, fontSize = 16.sp, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
        Icon(Icons.Default.Close, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
    }
}

@Composable
fun SuggestedPersonItem(user: StoryUser, onClick: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(user.id) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = user.avatarUrl,
            contentDescription = null,
            modifier = Modifier.size(52.dp).clip(CircleShape).border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(user.name, fontWeight = FontWeight.Black, fontSize = 16.sp)
            Text("@${user.name.lowercase().replace(" ", "")}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        Button(
            onClick = { /* Follow */ },
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text("Follow", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TrendingItem(rank: String, category: String, title: String, postCount: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        Text(
            text = rank,
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
            fontWeight = FontWeight.Black,
            modifier = Modifier.align(Alignment.CenterStart).offset(x = (-12).dp)
        )
        Column(modifier = Modifier.padding(start = 32.dp)) {
            Text(category, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Black)
            Text(postCount, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
