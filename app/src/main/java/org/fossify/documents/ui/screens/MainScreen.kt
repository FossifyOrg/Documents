@file:Suppress("FunctionNaming")

package org.fossify.documents.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.fossify.commons.compose.theme.SimpleTheme
import org.fossify.documents.models.DocumentSort
import org.fossify.documents.viewmodels.DocumentsHomeSection
import org.fossify.documents.viewmodels.DocumentsUiState

@Composable
internal fun MainScreen(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    var focusedSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.homeSection) {
        if (!uiState.isCollectionScreen) {
            focusedSearchActive = false
        }
    }

    DocumentsBackHandler(
        uiState = uiState,
        actions = actions,
        focusedSearchActive = focusedSearchActive,
        onFocusedSearchActiveChange = { focusedSearchActive = it },
    )

    DocumentsScaffold(
        uiState = uiState,
        actions = actions,
        focusedSearchActive = focusedSearchActive,
        onFocusedSearchActiveChange = { focusedSearchActive = it },
    )
}

@Composable
private fun DocumentsBackHandler(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
    focusedSearchActive: Boolean,
    onFocusedSearchActiveChange: (Boolean) -> Unit,
) {
    BackHandler(
        enabled = uiState.hasSelection ||
                uiState.isCollectionScreen ||
                uiState.query.isNotBlank() ||
                uiState.selectedFilters.isNotEmpty(),
    ) {
        if (uiState.hasSelection) {
            actions.clearSelection()
        } else if (focusedSearchActive) {
            actions.onQueryChange("")
            onFocusedSearchActiveChange(false)
        } else {
            actions.onBack()
        }
    }
}

@Composable
private fun DocumentsScaffold(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
    focusedSearchActive: Boolean,
    onFocusedSearchActiveChange: (Boolean) -> Unit,
) {
    Scaffold(
        containerColor = SimpleTheme.colorScheme.surface,
        topBar = {
            when {
                uiState.hasSelection -> {
                    DocumentsSelectionTopBar(uiState = uiState, actions = actions)
                }

                uiState.isCollectionScreen -> {
                    DocumentsFocusedTopBar(
                        uiState = uiState,
                        actions = actions,
                        searchActive = focusedSearchActive,
                        onSearchActiveChange = onFocusedSearchActiveChange,
                    )
                }

                else -> {
                    Box(modifier = Modifier.windowInsetsPadding(TopAppBarDefaults.windowInsets)) {
                        DocumentSearchSurface(
                            query = uiState.query,
                            hasDocuments = uiState.hasRecentDocuments,
                            selectedSort = uiState.selectedSort,
                            actions = actions,
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!uiState.hasSelection) {
                ImportAction(actions = actions)
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(SimpleTheme.colorScheme.surface)
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 104.dp),
        ) {
            mainScreenItems(
                uiState = uiState,
                actions = actions,
                isCollectionScreen = uiState.isCollectionScreen,
            )
        }
    }
}

private fun LazyListScope.mainScreenItems(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
    isCollectionScreen: Boolean,
) {
    if (isCollectionScreen) {
        focusedScreenItems(uiState, actions)
    } else {
        homeScreenItems(uiState, actions)
    }
}

private fun LazyListScope.homeScreenItems(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    if (uiState.showsFilterChips) {
        item(key = "filters") {
            DocumentFilterChips(
                availableFilters = uiState.availableFilters,
                selectedFilters = uiState.selectedFilters,
                onFilterSelect = actions.onFilterSelected,
            )
        }
    }
    homeContent(uiState, actions)
}

private fun LazyListScope.focusedScreenItems(
    uiState: DocumentsUiState,
    actions: DocumentsMainActions,
) {
    if (uiState.showsFolderBreadcrumb) {
        item(key = "breadcrumb") {
            DocumentsBreadcrumb(uiState = uiState, actions = actions)
        }
    }
    if (uiState.showsFilterChips) {
        item(key = "filters") {
            DocumentFilterChips(
                availableFilters = uiState.availableFilters,
                selectedFilters = uiState.selectedFilters,
                onFilterSelect = actions.onFilterSelected,
            )
        }
    }
    item(key = "sort") {
        SortAndViewRow(
            selectedSort = uiState.selectedSort,
            onSortSelect = actions.onSortSelected,
            selectedViewMode = uiState.selectedViewMode,
            onViewModeSelect = actions.onViewModeSelected,
            availableSorts = if (uiState.homeSection == DocumentsHomeSection.FOLDERS) {
                listOf(DocumentSort.NAME_ASCENDING, DocumentSort.NAME_DESCENDING)
            } else {
                DocumentSort.entries
            },
        )
    }
    focusedContent(uiState, actions)
}

private val DocumentsUiState.showsFolderBreadcrumb: Boolean
    get() = homeSection == DocumentsHomeSection.FOLDERS || homeSection == DocumentsHomeSection.FOLDER

private val DocumentsUiState.isCollectionScreen: Boolean
    get() = homeSection != DocumentsHomeSection.HOME
