package com.example.zell

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Interest Selection Screen - Choose topics for personalized feed
 * Extracted from SignUpFlow for better maintainability
 * 🔧 REFACTORED: Fixed CrashlyticsLogger usage (doneby Gemini)
 */

data class Interest(val name: String, val category: String)

data class InterestState(
    val searchQuery: String = "",
    val selectedInterests: List<Interest> = emptyList(),
    val appError: AppError? = null
)

@Composable
fun InterestSelectionScreen(
    state: InterestState,
    onStateChange: (InterestState) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().testTag("interests_step")) {
        Text(
            text = "Interests",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Pick 3+ topics to build your feed.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        ElaraTextField(
            value = state.searchQuery,
            onValueChange = { onStateChange(state.copy(searchQuery = it)) },
            label = "SEARCH TOPICS",
            placeholder = "e.g. Design, Crypto...",
            trailingIcon = {
                Icon(
                    Icons.Default.Search,
                    null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            testTag = "search_interests"
        )
        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            val categories = interestsList
                .filter { it.name.contains(state.searchQuery, ignoreCase = true) }
                .groupBy { it.category }

            if (categories.isEmpty()) {
                Text(
                    "No topics found for \"${state.searchQuery}\"",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(16.dp)
                )
            }

            categories.forEach { (category, items) ->
                Text(
                    category.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .testTag("category_$category")
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { interest ->
                        val isSelected = state.selectedInterests.contains(interest)
                        val bgColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            label = "chip_bg"
                        )
                        val textColor by animateColorAsState(
                            if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            label = "chip_text"
                        )

                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(bgColor)
                                .border(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                    CircleShape
                                )
                                .clickable {
                                    val newInterests = if (isSelected) {
                                        state.selectedInterests - interest
                                    } else {
                                        state.selectedInterests + interest
                                    }
                                    onStateChange(state.copy(selectedInterests = newInterests))
                                    // 🔧 REFACTORED: Using logUserAction for event logging with metadata (doneby Gemini)
                                    CrashlyticsLogger.logUserAction(
                                        if (isSelected) "Interest removed" else "Interest selected",
                                        mapOf("interest" to interest.name)
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .testTag("interest_${interest.name.lowercase()}")
                        ) {
                            Text(
                                interest.name,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// FlowRow for responsive layout
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val rows = mutableListOf<List<Int>>()
        val densityMultiplier = this.density
        val horizontalPadding = with(densityMultiplier) { 8.dp.roundToPx() }
        val verticalPadding = with(densityMultiplier) { 8.dp.roundToPx() }

        var currentRow = mutableListOf<Int>()
        var currentRowWidth = 0

        val placeables = measurables.map { measurable ->
            measurable.measure(constraints)
        }

        placeables.forEachIndexed { index, placeable ->
            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                if (currentRow.isNotEmpty()) {
                    rows.add(currentRow.toList())
                    currentRow = mutableListOf()
                    currentRowWidth = 0
                }
            }
            currentRow.add(index)
            currentRowWidth += placeable.width + horizontalPadding
        }

        if (currentRow.isNotEmpty()) {
            rows.add(currentRow.toList())
        }

        val layoutWidth = constraints.maxWidth
        val layoutHeight = (rows.size * 50).coerceAtLeast(constraints.minHeight)

        layout(layoutWidth, layoutHeight) {
            var yPosition = 0
            rows.forEach { row ->
                var xPosition = 0
                row.forEach { index ->
                    val placeable = placeables[index]
                    placeable.placeRelative(xPosition, yPosition)
                    xPosition += placeable.width + horizontalPadding
                }
                yPosition += 50
            }
        }
    }
}

val interestsList = listOf(
    Interest("Technology", "Tech"), Interest("AI", "Tech"), Interest("Coding", "Tech"),
    Interest("Blockchain", "Tech"), Interest("Gadgets", "Tech"), Interest("Cybersecurity", "Tech"),
    Interest("Design", "Creative"), Interest("Art", "Creative"), Interest("Photography", "Creative"),
    Interest("UI/UX", "Creative"), Interest("Architecture", "Creative"), Interest("Fashion", "Creative"),
    Interest("Finance", "Business"), Interest("Investing", "Business"), Interest("Startup", "Business"),
    Interest("Marketing", "Business"), Interest("Economy", "Business"), Interest("E-commerce", "Business"),
    Interest("Gaming", "Lifestyle"), Interest("Fitness", "Lifestyle"), Interest("Travel", "Lifestyle"),
    Interest("Cooking", "Lifestyle"), Interest("Health", "Lifestyle"), Interest("Sports", "Lifestyle"),
    Interest("Music", "Entertainment"), Interest("Movies", "Entertainment"), Interest("Anime", "Entertainment"),
    Interest("Books", "Entertainment"), Interest("Podcasts", "Entertainment")
)
