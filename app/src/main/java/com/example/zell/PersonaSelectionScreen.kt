package com.example.zell

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Persona Selection Screen - Choose user personality/role
 * Extracted from SignUpFlow for better maintainability
 * 🔧 REFACTORED: Fixed CrashlyticsLogger usage (doneby Gemini)
 */

data class Persona(val title: String, val icon: String, val description: String)

data class PersonaState(
    val selected: Persona? = null,
    val appError: AppError? = null
)

@Composable
fun PersonaSelectionScreen(
    state: PersonaState,
    onStateChange: (PersonaState) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().testTag("persona_step")) {
        Text(
            text = "Persona",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Choose your starting class.",
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(personas) { persona ->
                PersonaCard(
                    persona = persona,
                    isSelected = state.selected == persona,
                    onClick = {
                        onStateChange(state.copy(selected = persona))
                        // 🔧 REFACTORED: Corrected logger call to use logUserAction for metadata (doneby Gemini)
                        CrashlyticsLogger.logUserAction("Persona selected", mapOf("persona" to persona.title))
                    },
                    testTag = "persona_${persona.title.lowercase()}"
                )
            }
        }
    }
}

@Composable
fun PersonaCard(
    persona: Persona,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String = "persona_card"
) {
    val borderColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        label = "border"
    )
    val bgColor by animateColorAsState(
        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        else MaterialTheme.colorScheme.surface,
        label = "bg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
            .height(140.dp)
            .testTag(testTag)
    ) {
        Column {
            Text(persona.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                persona.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                persona.description,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

val personas = listOf(
    Persona("Builder", "⚡", "I build systems and solve complex problems."),
    Persona("Creator", "✦", "I design beautiful and intuitive experiences."),
    Persona("Analyst", "◎", "I find insights in data and drive decisions."),
    Persona("Explorer", "◈", "I discover new markets and opportunities.")
)
