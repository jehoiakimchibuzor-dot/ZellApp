package com.example.zell

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage

/**
 * PinDetailScreen - High-fidelity view of a discovery item
 * 🔧 REFACTORED: Improved responsiveness and switched to full MaterialTheme support (doneby Gemini)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinDetailScreen(
    item: DiscoverItem,
    onBack: () -> Unit,
    discoverViewModel: DiscoverViewModel = viewModel()
) {
    val relatedPins = discoverViewModel.discoverItems.take(6)
    val error by discoverViewModel.error

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = onBack, 
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    IconButton(onClick = {}) { Icon(Icons.Outlined.Share, null, tint = MaterialTheme.colorScheme.onSurface) }
                    IconButton(onClick = {}) { Icon(Icons.Default.MoreHoriz, null, tint = MaterialTheme.colorScheme.onSurface) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp, 
                modifier = Modifier.imePadding(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.creatorAvatar, 
                        contentDescription = null, 
                        modifier = Modifier.size(36.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(12.dp))
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    ) {
                        Text(
                            text = "Add a comment", 
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), 
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), 
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // 🔧 REFACTORED: Global error display for related content (doneby Gemini)
            if (error != null) {
                ErrorBanner(error = error, onDismiss = { discoverViewModel.clearError() })
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())
            ) {
                // 1. Large Hero Image
                item {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)),
                        contentScale = ContentScale.FillWidth
                    )
                }

                // 2. Main Interaction Row
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            IconButton(onClick = {}) { 
                                Icon(Icons.Default.FavoriteBorder, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurface) 
                            }
                            IconButton(onClick = {}) { 
                                Icon(Icons.Outlined.Download, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurface) 
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { /* External Visit */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant, 
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Icon(Icons.Default.Language, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Visit", fontWeight = FontWeight.Bold)
                            }
                            
                            Button(
                                onClick = { /* Save */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE60023)), // Pinterest Red
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("Save", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                // 3. Info Section
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(
                            text = item.title.ifEmpty { "Inspiration for your next project" }, 
                            style = MaterialTheme.typography.headlineMedium, 
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Discover more ideas in ${item.category}", 
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), 
                            fontSize = 15.sp
                        )
                        
                        Spacer(Modifier.height(32.dp))
                        
                        // Creator Profile
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = item.creatorAvatar, 
                                contentDescription = null, 
                                modifier = Modifier.size(50.dp).clip(CircleShape), 
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.creatorName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("842K followers", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 13.sp)
                            }
                            Button(
                                onClick = {},
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant, 
                                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text("Follow", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // 4. "More like this" Section
                item {
                    Spacer(Modifier.height(40.dp))
                    Text(
                        text = "More like this", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 20.sp, 
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(16.dp))
                    
                    // 🔧 REFACTORED: Using BoxWithConstraints for responsive related grid (doneby Gemini)
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        val columns = if (maxWidth < 600.dp) 2 else 4
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(columns),
                            modifier = Modifier.heightIn(max = 2000.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing = 10.dp,
                            userScrollEnabled = false
                        ) {
                            items(relatedPins) { pin ->
                                PinterestPinCard(pin = pin, onClick = { /* Navigate */ }, onSaveClick = {})
                            }
                        }
                    }
                    Spacer(Modifier.height(40.dp))
                }
            }
        }
    }
}
