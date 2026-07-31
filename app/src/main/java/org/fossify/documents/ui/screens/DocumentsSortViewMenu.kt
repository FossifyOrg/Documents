@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("FunctionNaming", "MagicNumber")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R
import org.fossify.documents.models.DocumentSort
import org.fossify.documents.models.DocumentViewMode

@Composable
internal fun SortAndViewRow(
    selectedSort: DocumentSort,
    onSortSelect: (DocumentSort) -> Unit,
    selectedViewMode: DocumentViewMode,
    onViewModeSelect: (DocumentViewMode) -> Unit,
    availableSorts: List<DocumentSort> = DocumentSort.entries,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(PaddingValues(start = 24.dp, top = 8.dp, end = 16.dp, bottom = 8.dp)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SortMenu(
            selectedSort = selectedSort,
            onSortSelect = onSortSelect,
            availableSorts = availableSorts,
            modifier = Modifier.weight(1f),
        )
        ViewModeToggle(
            selectedViewMode = selectedViewMode,
            onViewModeSelect = onViewModeSelect,
        )
    }
}

@Composable
private fun SortMenu(
    selectedSort: DocumentSort,
    onSortSelect: (DocumentSort) -> Unit,
    availableSorts: List<DocumentSort>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        TextButton(
            onClick = { expanded = true },
            modifier = Modifier.heightIn(min = 48.dp),
            contentPadding = PaddingValues(horizontal = 8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "${stringResource(id = org.fossify.commons.R.string.sort_by)}:",
                    style = SimpleTheme.typography.bodyMedium,
                    color = SimpleTheme.colorScheme.onSurface.copy(alpha = 0.76f),
                    maxLines = 1,
                )
                Text(
                    text = selectedSort.label(),
                    style = SimpleTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = SimpleTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = DocumentsMenuMinWidth),
        ) {
            availableSorts.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(text = sort.label()) },
                    onClick = {
                        expanded = false
                        onSortSelect(sort)
                    },
                    trailingIcon = if (sort == selectedSort) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

@Composable
private fun ViewModeToggle(
    selectedViewMode: DocumentViewMode,
    onViewModeSelect: (DocumentViewMode) -> Unit,
) {
    val targetViewMode = when (selectedViewMode) {
        DocumentViewMode.LIST -> DocumentViewMode.GRID
        DocumentViewMode.GRID -> DocumentViewMode.LIST
    }

    IconButton(onClick = { onViewModeSelect(targetViewMode) }) {
        Icon(
            imageVector = when (targetViewMode) {
                DocumentViewMode.LIST -> Icons.AutoMirrored.Filled.ViewList
                DocumentViewMode.GRID -> Icons.Filled.GridView
            },
            contentDescription = stringResource(
                id = when (targetViewMode) {
                    DocumentViewMode.LIST -> org.fossify.commons.R.string.list
                    DocumentViewMode.GRID -> org.fossify.commons.R.string.grid
                }
            ),
        )
    }
}

@Composable
internal fun DocumentSort.label(): String {
    return when (this) {
        DocumentSort.RECENT -> stringResource(id = R.string.sort_recent)
        DocumentSort.NAME_ASCENDING -> stringResource(id = R.string.sort_name_ascending)
        DocumentSort.NAME_DESCENDING -> stringResource(id = R.string.sort_name_descending)
        DocumentSort.SIZE_ASCENDING -> stringResource(id = R.string.sort_size_ascending)
        DocumentSort.SIZE_DESCENDING -> stringResource(id = R.string.sort_size_descending)
    }
}
