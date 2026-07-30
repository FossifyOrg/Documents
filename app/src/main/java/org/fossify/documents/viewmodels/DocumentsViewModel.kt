package org.fossify.documents.viewmodels

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.documents.data.DocumentFolderContent
import org.fossify.documents.data.DocumentsRepository
import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentFilter
import org.fossify.documents.models.DocumentFolder
import org.fossify.documents.models.DocumentSort
import org.fossify.documents.models.DocumentViewMode

private const val UI_STATE_STOP_TIMEOUT_MS = 5_000L

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
class DocumentsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val repository = DocumentsRepository(application)
    private val query = MutableStateFlow("")
    private val selectedFilters = MutableStateFlow<Set<DocumentFilter>>(emptySet())
    private val selectedSort = MutableStateFlow(DocumentSort.RECENT)
    private val selectedViewMode = MutableStateFlow(DocumentViewMode.LIST)
    private val homeSection = MutableStateFlow(DocumentsHomeSection.HOME)
    private val folderPath = MutableStateFlow<List<DocumentFolder>>(emptyList())
    private val folderRefresh = MutableStateFlow(0L)
    private var folderOriginSection = DocumentsHomeSection.HOME
    private val selectedDocuments = MutableStateFlow<Map<String, DocumentEntry>>(emptyMap())
    private val selectedFolders = MutableStateFlow<Map<String, DocumentFolder>>(emptyMap())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanUpStoredItems()
        }
    }

    private val displayOptions = combine(
        selectedSort,
        selectedViewMode,
    ) { sort, viewMode ->
        DocumentsDisplayOptions(sort = sort, viewMode = viewMode)
    }

    val onQueryChange: (String) -> Unit = { value ->
        query.value = value
    }

    private val listInputs = combine(
        repository.showFileLocationsFlow,
        query,
        selectedFilters,
        displayOptions,
        homeSection,
    ) { showFileLocations, query, filters, displayOptions, section ->
        DocumentsUiInputs(
            showFileLocations = showFileLocations,
            query = query,
            selectedFilters = filters,
            selectedSort = displayOptions.sort,
            selectedViewMode = displayOptions.viewMode,
            homeSection = section,
            folderPath = emptyList(),
        )
    }

    private val selectionInputs = combine(
        selectedDocuments,
        selectedFolders,
    ) { documents, folders ->
        DocumentsSelectionInputs(
            documentKeys = documents.keys,
            documents = documents.values.toList(),
            folders = folders.values.toList(),
        )
    }

    private val uiInputs = combine(
        listInputs,
        folderPath,
        selectionInputs,
    ) { inputs, folderPath, selection ->
        inputs.copy(
            folderPath = folderPath,
            selectedDocumentKeys = selection.documentKeys,
            selectedDocuments = selection.documents,
            selectedFolders = selection.folders,
        )
    }

    private val folderContentState = combine(
        homeSection,
        folderPath,
        folderRefresh,
    ) { section, path, refresh ->
        FolderContentRequest(
            uri = path.lastOrNull()?.uri.takeIf { section == DocumentsHomeSection.FOLDER },
            refresh = refresh,
        )
    }.distinctUntilChanged().flatMapLatest { request ->
        val uri = request.uri
        if (uri == null) {
            flowOf(FolderContentState())
        } else {
            repository.folderContentFlow(uri)
                .map { content ->
                    FolderContentState(uri = uri, content = content)
                }
                .onStart {
                    emit(FolderContentState(uri = uri, isLoading = true))
                }
                .catch {
                    emit(FolderContentState(uri = uri))
                }
        }
    }

    val uiState: StateFlow<DocumentsUiState> = combine(
        repository.documentsFlow,
        repository.foldersFlow,
        uiInputs,
        folderContentState,
    ) { documents, folders, inputs, folderState ->
        val normalizedQuery = inputs.query.trim()
        val requestedFolderUri = inputs.folderPath.lastOrNull()?.uri
            ?.takeIf { inputs.homeSection == DocumentsHomeSection.FOLDER }
        val activeFolderContent = folderState.content.takeIf { folderState.uri == requestedFolderUri }
        val activeFolderPath = when {
            inputs.homeSection != DocumentsHomeSection.FOLDER -> emptyList()
            activeFolderContent != null -> inputs.folderPath.dropLast(1) + activeFolderContent.folder
            else -> inputs.folderPath
        }
        val recentDocuments = documents.filter { it.lastOpened > 0L }
        val sectionDocuments = when (inputs.homeSection) {
            DocumentsHomeSection.FAVORITES -> documents.filter { it.isFavorite }
            DocumentsHomeSection.FOLDER -> activeFolderContent?.documents.orEmpty()
            DocumentsHomeSection.RECENT -> recentDocuments
            DocumentsHomeSection.HOME -> documents
            DocumentsHomeSection.FOLDERS -> emptyList()
        }
        val availableFilters = (
                sectionDocuments.mapNotNull { DocumentFilter.from(it.kind) } + inputs.selectedFilters
                ).toSet()
        val visibleDocuments = sectionDocuments
            .filter { entry ->
                inputs.selectedFilters.isEmpty() || inputs.selectedFilters.any { it.accepts(entry.kind) }
            }
            .filter { entry ->
                normalizedQuery.isBlank() ||
                        entry.name.contains(normalizedQuery, ignoreCase = true) ||
                        entry.location.contains(normalizedQuery, ignoreCase = true)
            }
            .sortedWith(inputs.selectedSort.documentComparator())
        val sectionFolders = when (inputs.homeSection) {
            DocumentsHomeSection.FOLDER -> activeFolderContent?.childFolders.orEmpty()
            DocumentsHomeSection.FOLDERS -> folders
            DocumentsHomeSection.FAVORITES,
            DocumentsHomeSection.HOME,
            DocumentsHomeSection.RECENT -> emptyList()
        }
        val visibleFolders = sectionFolders
            .filter { folder ->
                normalizedQuery.isBlank() || folder.name.contains(normalizedQuery, ignoreCase = true)
            }
            .let { filteredFolders ->
                if (inputs.selectedSort == DocumentSort.NAME_DESCENDING) {
                    filteredFolders.sortedByDescending { it.name.lowercase() }
                } else {
                    filteredFolders.sortedBy { it.name.lowercase() }
                }
            }
        val sortedFolders = when (inputs.selectedSort) {
            DocumentSort.NAME_ASCENDING -> folders.sortedBy { it.name.lowercase() }
            DocumentSort.NAME_DESCENDING -> folders.sortedByDescending { it.name.lowercase() }
            else -> folders
        }

        DocumentsUiState(
            documents = documents,
            recentDocuments = recentDocuments.sortedWith(inputs.selectedSort.documentComparator()),
            favoriteDocuments = documents
                .filter { it.isFavorite }
                .sortedWith(inputs.selectedSort.documentComparator()),
            folders = sortedFolders,
            visibleFolders = visibleFolders,
            visibleDocuments = visibleDocuments,
            query = inputs.query,
            selectedFilters = inputs.selectedFilters,
            selectedSort = inputs.selectedSort,
            selectedViewMode = inputs.selectedViewMode,
            homeSection = inputs.homeSection,
            activeFolder = activeFolderContent?.folder ?: inputs.folderPath.lastOrNull(),
            folderPath = activeFolderPath,
            showFileLocations = inputs.showFileLocations,
            selectedDocumentKeys = inputs.selectedDocumentKeys,
            selectedDocuments = inputs.selectedDocuments,
            selectedFolders = inputs.selectedFolders,
            availableFilters = availableFilters,
            isFolderLoading = folderState.isLoading && folderState.uri == requestedFolderUri,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(UI_STATE_STOP_TIMEOUT_MS),
        initialValue = DocumentsUiState()
    )

    fun onFilterSelected(filter: DocumentFilter) {
        selectedFilters.value = selectedFilters.value.toMutableSet().apply {
            if (!add(filter)) {
                remove(filter)
            }
        }
    }

    fun updateDisplayOptions(
        sort: DocumentSort? = null,
        viewMode: DocumentViewMode? = null,
    ) {
        sort?.let { selectedSort.value = it }
        viewMode?.let { selectedViewMode.value = it }
    }

    fun showSection(section: DocumentsHomeSection, folder: DocumentFolder? = null) {
        val previousSection = homeSection.value
        if (section == DocumentsHomeSection.FOLDER && previousSection != DocumentsHomeSection.FOLDER) {
            folderOriginSection = previousSection
        }
        clearSelection()
        query.value = ""
        selectedFilters.value = emptySet()

        selectedSort.value = when (section) {
            DocumentsHomeSection.FAVORITES,
            DocumentsHomeSection.FOLDER,
            DocumentsHomeSection.FOLDERS -> DocumentSort.NAME_ASCENDING

            DocumentsHomeSection.HOME,
            DocumentsHomeSection.RECENT -> DocumentSort.RECENT
        }

        val nextFolderPath = if (section == DocumentsHomeSection.FOLDER) {
            folderPathAfterOpening(
                currentSection = previousSection,
                currentPath = folderPath.value,
                folder = folder,
            )
        } else {
            emptyList()
        }
        folderPath.value = nextFolderPath
        homeSection.value = section
    }

    fun navigateBack() {
        when {
            selectedDocuments.value.isNotEmpty() || selectedFolders.value.isNotEmpty() -> clearSelection()
            query.value.isNotBlank() -> query.value = ""
            selectedFilters.value.isNotEmpty() -> selectedFilters.value = emptySet()
            else -> {
                val destination = documentsBackDestination(
                    currentSection = homeSection.value,
                    currentPath = folderPath.value,
                    folderOriginSection = folderOriginSection,
                )
                if (destination.section == DocumentsHomeSection.FOLDER) {
                    folderPath.value = destination.folderPath
                } else {
                    showSection(destination.section)
                }
            }
        }
    }

    fun navigateToBreadcrumb(index: Int) {
        val path = folderPath.value
        if (index !in path.indices || index == path.lastIndex) {
            return
        }

        clearSelection()
        query.value = ""
        selectedFilters.value = emptySet()
        folderPath.value = path.take(index + 1)
    }

    fun prepareDocument(uri: Uri, resultFlags: Int = 0, onReady: (Boolean) -> Unit) {
        viewModelScope.launch {
            val canRead = withContext(Dispatchers.IO) {
                repository.rememberDocument(uri, resultFlags)
                repository.isDocumentReadable(uri).also { readable ->
                    if (!readable) {
                        repository.removeDocument(uri.toString())
                    }
                }
            }
            onReady(canRead)
        }
    }

    fun validateDocuments(
        documents: List<DocumentEntry>,
        onResult: (readable: List<DocumentEntry>, unavailableCount: Int) -> Unit,
    ) {
        viewModelScope.launch {
            val (readable, unavailable) = withContext(Dispatchers.IO) {
                documents.distinctBy { it.uri }.partition { document ->
                    repository.isDocumentReadable(document.uri.toUri())
                }.also { (_, unavailableDocuments) ->
                    repository.removeDocuments(unavailableDocuments.map(DocumentEntry::uri))
                }
            }
            onResult(readable, unavailable.size)
        }
    }

    fun rememberFolder(uri: Uri, resultFlags: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.rememberFolder(uri, resultFlags)
        }
    }

    fun refreshVisibleFolder() {
        folderRefresh.value += 1L
    }

    fun toggleDocumentSelection(selectionKey: String, document: DocumentEntry) {
        selectedFolders.value = emptyMap()
        selectedDocuments.value = selectedDocuments.value.toMutableMap().apply {
            if (containsKey(selectionKey)) {
                remove(selectionKey)
            } else {
                put(selectionKey, document)
            }
        }
    }

    fun toggleFolderSelection(folder: DocumentFolder) {
        selectedDocuments.value = emptyMap()
        selectedFolders.value = selectedFolders.value.toMutableMap().apply {
            if (containsKey(folder.uri)) {
                remove(folder.uri)
            } else {
                put(folder.uri, folder)
            }
        }
    }

    fun selectAllVisibleItems() {
        when {
            selectedDocuments.value.isNotEmpty() -> {
                selectedDocuments.value = uiState.value.documentsForSelectAll(selectedDocuments.value)
            }

            selectedFolders.value.isNotEmpty() -> {
                uiState.value.foldersForSelectAll()
                    .takeIf { it.isNotEmpty() }
                    ?.let { selectedFolders.value = it }
            }
        }
    }

    fun clearSelection() {
        selectedDocuments.value = emptyMap()
        selectedFolders.value = emptyMap()
    }

    fun toggleSelectedFavorites() {
        val documents = selectedDocuments.value.values.distinctBy { it.uri }
        if (documents.isEmpty()) {
            return
        }

        val favorite = documents.any { !it.isFavorite }
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            repository.setFavorites(documents, favorite)
        }
    }

    fun removeSelectedItems() {
        val documentUris = selectedDocuments.value.values.map { it.uri }.distinct()
        val folderUris = selectedFolders.value.keys.toList()
        clearSelection()
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeDocuments(documentUris)
            repository.removeFolders(folderUris)
        }
    }

    fun clearRecentDocuments() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearRecentDocuments()
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DocumentsViewModel(application) as T
                }
            }
        }

        val openDocumentIntent: Intent
            get() = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(
                    Intent.EXTRA_MIME_TYPES,
                    arrayOf(
                        "application/pdf",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/*",
                        "text/markdown",
                        "text/x-markdown",
                        "text/csv",
                        "text/tab-separated-values",
                        "text/html",
                        "application/xhtml+xml",
                        "application/json",
                        "application/xml",
                        "application/yaml",
                        "application/x-yaml",
                        "application/toml",
                    )
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }

        val openFolderIntent: Intent
            get() = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PREFIX_URI_PERMISSION)
            }

        fun createDocumentIntent(mimeType: String, title: String): Intent {
            return Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                putExtra(Intent.EXTRA_TITLE, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
        }
    }
}

enum class DocumentsHomeSection {
    HOME,
    RECENT,
    FOLDERS,
    FAVORITES,
    FOLDER,
}

private data class DocumentsUiInputs(
    val showFileLocations: Boolean,
    val query: String,
    val selectedFilters: Set<DocumentFilter>,
    val selectedSort: DocumentSort,
    val selectedViewMode: DocumentViewMode,
    val homeSection: DocumentsHomeSection,
    val folderPath: List<DocumentFolder>,
    val selectedDocumentKeys: Set<String> = emptySet(),
    val selectedDocuments: List<DocumentEntry> = emptyList(),
    val selectedFolders: List<DocumentFolder> = emptyList(),
)

private data class DocumentsSelectionInputs(
    val documentKeys: Set<String>,
    val documents: List<DocumentEntry>,
    val folders: List<DocumentFolder>,
)

private data class DocumentsDisplayOptions(
    val sort: DocumentSort,
    val viewMode: DocumentViewMode,
)

private data class FolderContentState(
    val uri: String? = null,
    val content: DocumentFolderContent? = null,
    val isLoading: Boolean = false,
)

private data class FolderContentRequest(
    val uri: String?,
    val refresh: Long,
)

data class DocumentsUiState(
    val documents: List<DocumentEntry> = emptyList(),
    val recentDocuments: List<DocumentEntry> = emptyList(),
    val favoriteDocuments: List<DocumentEntry> = emptyList(),
    val folders: List<DocumentFolder> = emptyList(),
    val visibleFolders: List<DocumentFolder> = emptyList(),
    val visibleDocuments: List<DocumentEntry> = emptyList(),
    val query: String = "",
    val selectedFilters: Set<DocumentFilter> = emptySet(),
    val selectedSort: DocumentSort = DocumentSort.RECENT,
    val selectedViewMode: DocumentViewMode = DocumentViewMode.LIST,
    val homeSection: DocumentsHomeSection = DocumentsHomeSection.HOME,
    val activeFolder: DocumentFolder? = null,
    val folderPath: List<DocumentFolder> = emptyList(),
    val showFileLocations: Boolean = false,
    val selectedDocumentKeys: Set<String> = emptySet(),
    val selectedDocuments: List<DocumentEntry> = emptyList(),
    val selectedFolders: List<DocumentFolder> = emptyList(),
    val availableFilters: Set<DocumentFilter> = emptySet(),
    val isFolderLoading: Boolean = false,
) {
    val selectionCount: Int
        get() = selectedDocumentKeys.size + selectedFolders.size

    val hasSelection: Boolean
        get() = selectionCount > 0

    val selectedDocumentsAreFavorites: Boolean
        get() = selectedDocuments.isNotEmpty() && selectedDocuments.all { it.isFavorite }

    val showsFilterChips: Boolean
        get() = availableFilters.size >= 2 || selectedFilters.isNotEmpty()

    val hasRecentDocuments: Boolean
        get() = recentDocuments.isNotEmpty()
}

private fun DocumentSort.documentComparator(): Comparator<DocumentEntry> {
    return when (this) {
        DocumentSort.RECENT -> compareByDescending<DocumentEntry> {
            maxOf(it.lastOpened, it.lastModified ?: 0L)
        }.thenBy { it.name.lowercase() }

        DocumentSort.NAME_ASCENDING -> compareBy { it.name.lowercase() }
        DocumentSort.NAME_DESCENDING -> compareByDescending { it.name.lowercase() }
        DocumentSort.SIZE_ASCENDING -> compareBy<DocumentEntry> { it.size ?: Long.MAX_VALUE }
            .thenBy { it.name.lowercase() }

        DocumentSort.SIZE_DESCENDING -> compareByDescending<DocumentEntry> { it.size ?: Long.MIN_VALUE }
            .thenBy { it.name.lowercase() }
    }
}
