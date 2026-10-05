package org.fossify.documents.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.commons.compose.theme.isSurfaceNotLitWell

internal data class DocumentSearchColors(
    val accent: Color,
    val match: Color,
    val currentMatch: Color,
)

@Composable
internal fun documentSearchColors(darkBackground: Boolean = isSurfaceNotLitWell()): DocumentSearchColors {
    val accent = if (darkBackground == isSurfaceNotLitWell()) {
        SimpleTheme.colorScheme.primary
    } else {
        SimpleTheme.colorScheme.inversePrimary
    }
    return DocumentSearchColors(
        accent = accent,
        match = accent.copy(alpha = 0.18f),
        currentMatch = accent.copy(alpha = 0.5f),
    )
}
