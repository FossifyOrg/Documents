@file:OptIn(ExperimentalMaterial3Api::class)
@file:Suppress("FunctionNaming", "MagicNumber")

package org.fossify.documents.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.fossify.commons.compose.components.SimpleDropDownMenuItem
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.R
import org.fossify.documents.models.DocumentFilter
import org.fossify.documents.models.DocumentSort

@Composable
internal fun DocumentSearchSurface(
    query: String,
    hasDocuments: Boolean,
    selectedSort: DocumentSort,
    actions: DocumentsMainActions,
) {
    val contentColor = SimpleTheme.colorScheme.onSurface
    val containerColor = SimpleTheme.colorScheme.primary.copy(
        alpha = if (isDocumentsDarkTheme()) SEARCH_CONTAINER_DARK_ALPHA else SEARCH_CONTAINER_LIGHT_ALPHA
    )
    val inputFieldColors = SearchBarDefaults.inputFieldColors(
        focusedTextColor = contentColor,
        unfocusedTextColor = contentColor,
        cursorColor = SimpleTheme.colorScheme.primary,
        focusedLeadingIconColor = contentColor,
        unfocusedLeadingIconColor = contentColor,
        focusedTrailingIconColor = contentColor,
        unfocusedTrailingIconColor = contentColor,
        focusedPlaceholderColor = contentColor.copy(alpha = 0.56f),
        unfocusedPlaceholderColor = contentColor.copy(alpha = 0.56f),
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
    )

    @Suppress("DEPRECATION")
    SearchBar(
        query = query,
        onQueryChange = actions.onQueryChange,
        onSearch = {},
        active = false,
        onActiveChange = {},
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = {
            Text(
                text = stringResource(id = R.string.search_documents),
                style = SimpleTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
            )
        },
        trailingIcon = {
            SearchBarActions(
                query = query,
                hasDocuments = hasDocuments,
                selectedSort = selectedSort,
                actions = actions,
            )
        },
        colors = SearchBarDefaults.colors(
            containerColor = containerColor,
            inputFieldColors = inputFieldColors,
        ),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {}
}

@Composable
private fun SearchBarActions(
    query: String,
    hasDocuments: Boolean,
    selectedSort: DocumentSort,
    actions: DocumentsMainActions,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (query.isNotBlank()) {
            IconButton(onClick = { actions.onQueryChange("") }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(id = R.string.clear_search),
                )
            }
        }
        HomeSortMenu(
            selectedSort = selectedSort,
            onSortSelect = actions.onSortSelected,
        )
        DocumentsOverflowMenu(
            hasDocuments = hasDocuments,
            actions = actions,
        )
    }
}

@Composable
private fun HomeSortMenu(
    selectedSort: DocumentSort,
    onSortSelect: (DocumentSort) -> Unit,
) {
    var visible by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { visible = true }) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Sort,
                contentDescription = stringResource(id = org.fossify.commons.R.string.sort_by),
            )
        }
        DropdownMenu(
            expanded = visible,
            onDismissRequest = { visible = false },
            modifier = Modifier.widthIn(min = DocumentsMenuMinWidth),
        ) {
            DocumentSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(text = sort.label()) },
                    onClick = {
                        visible = false
                        onSortSelect(sort)
                    },
                    trailingIcon = sort.checkIcon(selectedSort),
                )
            }
        }
    }
}

@Composable
internal fun DocumentsOverflowMenu(
    hasDocuments: Boolean,
    actions: DocumentsMainActions,
) {
    var visible by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { visible = true }) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(id = org.fossify.commons.R.string.more_options),
            )
        }
        DropdownMenu(
            expanded = visible,
            onDismissRequest = { visible = false },
            modifier = Modifier.widthIn(min = DocumentsMenuMinWidth),
            offset = DocumentsEndMenuOffset,
        ) {
            SimpleDropDownMenuItem(
                text = org.fossify.commons.R.string.settings,
                onClick = {
                    visible = false
                    actions.openSettings()
                },
            )
            SimpleDropDownMenuItem(
                text = org.fossify.commons.R.string.about,
                onClick = {
                    visible = false
                    actions.openAbout()
                },
            )
            if (hasDocuments) {
                SimpleDropDownMenuItem(
                    text = R.string.clear_recent_documents,
                    onClick = {
                        visible = false
                        actions.clearRecentDocuments()
                    },
                )
            }
        }
    }
}

@Composable
internal fun DocumentFilterChips(
    availableFilters: Set<DocumentFilter>,
    selectedFilters: Set<DocumentFilter>,
    onFilterSelect: (DocumentFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        filterChipOrder().filter { it in availableFilters }.forEach { filter ->
            val selected = filter in selectedFilters

            FilterChip(
                selected = selected,
                onClick = { onFilterSelect(filter) },
                label = {
                    Text(
                        text = filter.label(),
                        style = SimpleTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                    )
                },
                leadingIcon = {
                    FilterChipIcon(filter = filter, selected = selected)
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SimpleTheme.colorScheme.surface,
                    labelColor = SimpleTheme.colorScheme.onSurface,
                    iconColor = SimpleTheme.colorScheme.onSurface,
                    selectedContainerColor = SimpleTheme.colorScheme.primary.copy(
                        alpha = if (isDocumentsDarkTheme()) 0.28f else 0.14f
                    ),
                    selectedLabelColor = SimpleTheme.colorScheme.onSurface,
                    selectedLeadingIconColor = SimpleTheme.colorScheme.onSurface,
                ),
            )
        }
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = SimpleTheme.colorScheme.onSurface,
            style = SimpleTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (actionLabel != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionLabel,
                    color = SimpleTheme.colorScheme.primary,
                    style = SimpleTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
            }
        }
    }
}

@Composable
internal fun DocumentFilter.label(): String {
    return when (this) {
        DocumentFilter.PDF -> stringResource(id = R.string.filter_pdf)
        DocumentFilter.DOCX -> stringResource(id = R.string.filter_docx)
        DocumentFilter.TEXT -> stringResource(id = org.fossify.commons.R.string.text)
        DocumentFilter.MARKDOWN -> stringResource(id = R.string.filter_markdown)
        DocumentFilter.CSV -> stringResource(id = R.string.filter_csv)
        DocumentFilter.HTML -> stringResource(id = R.string.filter_html)
    }
}

@Composable
private fun FilterChipIcon(
    filter: DocumentFilter,
    selected: Boolean,
) {
    if (selected) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            modifier = Modifier.size(FilterChipDefaults.IconSize),
        )
        return
    }

    val imageVector = when (filter) {
        DocumentFilter.PDF -> Icons.Filled.PictureAsPdf
        DocumentFilter.DOCX -> Icons.AutoMirrored.Filled.Article
        DocumentFilter.TEXT -> Icons.AutoMirrored.Filled.Article
        DocumentFilter.MARKDOWN -> Icons.Filled.Description
        DocumentFilter.CSV -> Icons.Filled.TableChart
        DocumentFilter.HTML -> Icons.Filled.Code
    }
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        modifier = Modifier.size(FilterChipDefaults.IconSize),
        tint = filter.iconTint(),
    )
}

private fun DocumentSort.checkIcon(selectedSort: DocumentSort): @Composable (() -> Unit)? {
    if (this != selectedSort) {
        return null
    }

    return {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
        )
    }
}

internal fun filterChipOrder(): List<DocumentFilter> {
    return listOf(
        DocumentFilter.PDF,
        DocumentFilter.DOCX,
        DocumentFilter.TEXT,
        DocumentFilter.MARKDOWN,
        DocumentFilter.CSV,
        DocumentFilter.HTML,
    )
}

private const val SEARCH_CONTAINER_LIGHT_ALPHA = 0.16f
private const val SEARCH_CONTAINER_DARK_ALPHA = 0.26f
