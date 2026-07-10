@file:Suppress("FunctionNaming", "MagicNumber")

package org.fossify.documents.ui.screens

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.commons.extensions.formatSize
import org.fossify.documents.R
import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentFilter
import org.fossify.documents.models.DocumentKind

@Composable
internal fun DocumentKindIcon(kind: DocumentKind) {
    val iconSpec = kind.iconSpec()

    Surface(
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(8.dp),
        color = iconSpec.containerColor,
        contentColor = iconSpec.contentColor,
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (kind == DocumentKind.MARKDOWN) {
                Text(
                    text = "M↓",
                    style = SimpleTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
            } else {
                Icon(
                    imageVector = iconSpec.icon,
                    contentDescription = null,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}

@Composable
internal fun FolderIcon() {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = RoundedCornerShape(8.dp),
        color = SimpleTheme.colorScheme.primary.copy(alpha = primaryTintAlpha()),
        contentColor = SimpleTheme.colorScheme.primary,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

@Composable
internal fun DocumentEntry.metaLine(showOpenedFallback: Boolean): String {
    val context = LocalContext.current
    val dateText = when {
        lastModified != null && lastModified > 0L -> DateUtils.formatDateTime(
            context,
            lastModified,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_SHOW_YEAR,
        )

        showOpenedFallback && lastOpened > 0L -> stringResource(
            id = R.string.last_opened_value,
            DateUtils.getRelativeTimeSpanString(lastOpened).toString(),
        )

        else -> null
    }
    val sizeText = size?.formatSize()

    return listOfNotNull(dateText, sizeText).joinToString(" · ").ifBlank {
        kind.shortLabel()
    }
}

@Composable
private fun DocumentKind.iconSpec(): DocumentIconSpec {
    val isDark = isDocumentsDarkTheme()
    return when (this) {
        DocumentKind.PDF -> themedIconSpec(
            icon = Icons.Filled.PictureAsPdf,
            isDark = isDark,
            lightContainer = Color(0xFFF8DCDC),
            lightContent = Color(0xFFE52620),
            darkContainer = Color(0xFF55302F),
            darkContent = Color(0xFFFFDAD7),
        )

        DocumentKind.DOCX -> themedIconSpec(
            icon = Icons.AutoMirrored.Filled.Article,
            isDark = isDark,
            lightContainer = Color(0xFFDCEAFF),
            lightContent = Color(0xFF185ABD),
            darkContainer = Color(0xFF243F60),
            darkContent = Color(0xFFD5E3FF),
        )

        DocumentKind.TEXT -> DocumentIconSpec(
            icon = Icons.AutoMirrored.Filled.TextSnippet,
            containerColor = SimpleTheme.colorScheme.primary.copy(alpha = primaryTintAlpha()),
            contentColor = SimpleTheme.colorScheme.onSurface,
        )

        DocumentKind.MARKDOWN -> DocumentIconSpec(
            icon = Icons.Filled.Description,
            containerColor = SimpleTheme.colorScheme.primary.copy(alpha = primaryTintAlpha()),
            contentColor = SimpleTheme.colorScheme.primary,
        )

        DocumentKind.CSV -> themedIconSpec(
            icon = Icons.Filled.TableChart,
            isDark = isDark,
            lightContainer = Color(0xFFD8F3E2),
            lightContent = Color(0xFF167044),
            darkContainer = Color(0xFF1E4935),
            darkContent = Color(0xFFB7E9C8),
        )

        DocumentKind.HTML -> themedIconSpec(
            icon = Icons.Filled.Code,
            isDark = isDark,
            lightContainer = Color(0xFFFFE2D3),
            lightContent = Color(0xFFB84218),
            darkContainer = Color(0xFF5A3525),
            darkContent = Color(0xFFFFDBCA),
        )

        DocumentKind.OTHER -> DocumentIconSpec(
            icon = Icons.Filled.Description,
            containerColor = SimpleTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.18f else 0.08f),
            contentColor = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.72f),
        )
    }
}

private fun themedIconSpec(
    icon: ImageVector,
    isDark: Boolean,
    lightContainer: Color,
    lightContent: Color,
    darkContainer: Color,
    darkContent: Color,
): DocumentIconSpec {
    return DocumentIconSpec(
        icon = icon,
        containerColor = if (isDark) darkContainer else lightContainer,
        contentColor = if (isDark) darkContent else lightContent,
    )
}

private fun DocumentKind.shortLabel(): String {
    return when (this) {
        DocumentKind.PDF -> "PDF"
        DocumentKind.DOCX -> "DOCX"
        DocumentKind.TEXT -> "TXT"
        DocumentKind.MARKDOWN -> "MD"
        DocumentKind.CSV -> "CSV"
        DocumentKind.HTML -> "HTML"
        DocumentKind.OTHER -> "DOC"
    }
}

@Composable
internal fun DocumentFilter.iconTint(): Color {
    val isDark = isDocumentsDarkTheme()
    return when (this) {
        DocumentFilter.PDF -> if (isDark) Color(0xFFFFDAD7) else Color(0xFFE52620)
        DocumentFilter.DOCX -> if (isDark) Color(0xFFD5E3FF) else Color(0xFF185ABD)
        DocumentFilter.CSV -> if (isDark) Color(0xFFB7E9C8) else Color(0xFF167044)
        DocumentFilter.HTML -> if (isDark) Color(0xFFFFDBCA) else Color(0xFFB84218)
        DocumentFilter.TEXT,
        DocumentFilter.MARKDOWN -> SimpleTheme.colorScheme.onSurface
    }
}

private data class DocumentIconSpec(
    val icon: ImageVector,
    val containerColor: Color,
    val contentColor: Color,
)
