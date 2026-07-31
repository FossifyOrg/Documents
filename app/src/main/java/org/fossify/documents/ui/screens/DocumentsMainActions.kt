package org.fossify.documents.ui.screens

import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentFilter
import org.fossify.documents.models.DocumentFolder
import org.fossify.documents.models.DocumentSort
import org.fossify.documents.models.DocumentViewMode

internal data class DocumentsMainActions(
    val openDocument: () -> Unit,
    val openFolder: () -> Unit,
    val newTextFile: () -> Unit,
    val newMarkdownFile: () -> Unit,
    val openSettings: () -> Unit,
    val openAbout: () -> Unit,
    val clearRecentDocuments: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onFilterSelected: (DocumentFilter) -> Unit,
    val onSortSelected: (DocumentSort) -> Unit,
    val onViewModeSelected: (DocumentViewMode) -> Unit,
    val onBack: () -> Unit,
    val onShowHome: () -> Unit,
    val onShowRecent: () -> Unit,
    val onShowFolders: () -> Unit,
    val onShowFavorites: () -> Unit,
    val onBreadcrumbClick: (Int) -> Unit,
    val onRetryFolder: () -> Unit,
    val onFolderClick: (DocumentFolder) -> Unit,
    val onDocumentClick: (DocumentEntry) -> Unit,
    val onToggleDocumentSelection: (selectionKey: String, document: DocumentEntry) -> Unit,
    val onToggleFolderSelection: (DocumentFolder) -> Unit,
    val onSelectAll: () -> Unit,
    val clearSelection: () -> Unit,
    val onOpenWith: (DocumentEntry) -> Unit,
    val onShareDocuments: (List<DocumentEntry>) -> Unit,
    val onRemoveSelection: () -> Unit,
    val onToggleSelectedFavorites: () -> Unit,
)
