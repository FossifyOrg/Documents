package org.fossify.documents.data

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.fossify.documents.extensions.config
import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentFolder

@Suppress("TooManyFunctions")
class DocumentsRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val config = appContext.config
    private val store = DocumentsStore(config)
    private val scanner = DocumentProviderScanner(appContext)
    private val permissions = DocumentUriPermissions(appContext)

    val documentsFlow: Flow<List<DocumentEntry>> = store.documentsFlow

    val foldersFlow: Flow<List<DocumentFolder>> = store.foldersFlow
        .map { folders -> folders.map(scanner::refreshFolderCount) }
        .flowOn(Dispatchers.IO)

    val showFileLocationsFlow: Flow<Boolean> = store.showFileLocationsFlow

    fun getDocument(uri: Uri): DocumentEntry? = store.getDocument(uri)

    fun rememberDocument(uri: Uri, grantFlags: Int = 0): DocumentEntry? {
        if (!permissions.persist(uri, grantFlags)) {
            return null
        }

        var remembered: DocumentEntry? = null
        store.updateDocuments { existing ->
            val previous = existing.firstOrNull { it.uri == uri.toString() }
            val metadata = scanner.readDocument(uri, previous)
            val updated = metadata.copy(
                lastOpened = System.currentTimeMillis(),
                lastPage = previous?.lastPage ?: metadata.lastPage,
                isFavorite = previous?.isFavorite ?: metadata.isFavorite,
            )
            remembered = updated
            listOf(updated) + existing.filterNot { it.uri == updated.uri }
        }
        return remembered
    }

    fun refreshDocumentMetadata(uri: Uri): DocumentEntry? {
        var refreshed: DocumentEntry? = null
        store.updateDocuments { existing ->
            val previous = existing.firstOrNull { it.uri == uri.toString() } ?: return@updateDocuments existing
            val updated = scanner.readDocument(uri, previous).copy(
                lastOpened = previous.lastOpened,
                lastPage = previous.lastPage,
                isFavorite = previous.isFavorite,
            )
            refreshed = updated
            listOf(updated) + existing.filterNot { it.uri == updated.uri }
        }
        return refreshed
    }

    fun isDocumentReadable(uri: Uri): Boolean = permissions.isReadable(uri)

    fun isDocumentWritable(uri: Uri): Boolean = permissions.isWritable(uri)

    fun cleanUpStoredItems() {
        val refreshLocations = !config.wereDocumentLocationsRefreshed
        var removedDocuments = emptyList<DocumentEntry>()
        store.updateDocuments { documents ->
            val retained = documents
                .filter { permissions.hasPersistentReadAccess(it.uri.toUri()) }
                .filter { it.lastOpened > 0L || it.isFavorite }
                .map { document ->
                    if (refreshLocations) {
                        scanner.readDocument(document.uri.toUri(), document)
                    } else {
                        document
                    }
                }
            val retainedUris = retained.mapTo(hashSetOf(), DocumentEntry::uri)
            removedDocuments = documents.filterNot { it.uri in retainedUris }
            retained
        }
        removedDocuments.forEach { permissions.release(it.uri.toUri()) }
        store.updateFolders { folders ->
            folders.filter { permissions.hasPersistentReadAccess(it.uri.toUri()) }
        }
        config.wereDocumentLocationsRefreshed = true
    }

    fun updateLastPage(uri: Uri, page: Int) {
        if (!appContext.config.rememberPdfPage) {
            return
        }

        store.updateDocuments { documents ->
            documents.map { entry ->
                if (entry.uri == uri.toString()) entry.copy(lastPage = page.coerceAtLeast(0)) else entry
            }
        }
    }

    fun removeDocument(uri: String) {
        removeDocuments(listOf(uri))
    }

    fun removeDocuments(uris: Collection<String>) {
        val removedUris = uris.toSet()
        if (removedUris.isEmpty()) {
            return
        }

        removedUris.forEach { permissions.release(it.toUri()) }
        store.updateDocuments { documents ->
            documents.filterNot { it.uri in removedUris }
        }
    }

    fun setFavorites(documents: Collection<DocumentEntry>, favorite: Boolean) {
        if (documents.isEmpty()) {
            return
        }

        var removed = emptyList<DocumentEntry>()
        store.updateDocuments { existing ->
            updateFavoriteEntries(existing, documents, favorite).also { result ->
                removed = result.removed
            }.retained
        }
        removed.forEach { permissions.release(it.uri.toUri()) }
    }

    fun clearRecentDocuments() {
        var removed = emptyList<DocumentEntry>()
        store.updateDocuments { documents ->
            clearRecentEntries(documents).also { result ->
                removed = result.removed
            }.retained
        }
        removed.forEach { permissions.release(it.uri.toUri()) }
    }

    fun rememberFolder(uri: Uri, grantFlags: Int = 0): DocumentFolder? {
        if (!permissions.persist(uri, grantFlags)) {
            return null
        }

        var remembered: DocumentFolder? = null
        store.updateFolders { existing ->
            val previous = existing.firstOrNull { it.uri == uri.toString() }
            val folder = scanner.readFolder(uri, previous).copy(lastOpened = System.currentTimeMillis())
            remembered = folder
            listOf(folder) + existing.filterNot { it.uri == folder.uri }
        }
        return remembered
    }

    fun removeFolders(uris: Collection<String>) {
        val removedUris = uris.toSet()
        if (removedUris.isEmpty()) {
            return
        }

        removedUris.forEach { permissions.release(it.toUri()) }
        store.updateFolders { folders ->
            folders.filterNot { it.uri in removedUris }
        }
        store.updateDocuments { documents ->
            documents.filter { permissions.hasPersistentReadAccess(it.uri.toUri()) }
        }
    }

    internal fun folderContentFlow(uri: String): Flow<DocumentFolderContent> = flow {
        val previous = store.getFolders().firstOrNull { it.uri == uri }
        emit(scanner.readFolderContent(uri.toUri(), previous))
    }.flowOn(Dispatchers.IO)

}

internal data class DocumentFolderContent(
    val folder: DocumentFolder,
    val childFolders: List<DocumentFolder>,
    val documents: List<DocumentEntry>,
)

internal data class ClearRecentResult(
    val retained: List<DocumentEntry>,
    val removed: List<DocumentEntry>,
)

internal data class FavoriteUpdateResult(
    val retained: List<DocumentEntry>,
    val removed: List<DocumentEntry>,
)

internal fun updateFavoriteEntries(
    existing: List<DocumentEntry>,
    selected: Collection<DocumentEntry>,
    favorite: Boolean,
): FavoriteUpdateResult {
    val selectedByUri = selected.associateBy(DocumentEntry::uri)
    val existingUris = existing.mapTo(hashSetOf(), DocumentEntry::uri)
    val removed = mutableListOf<DocumentEntry>()
    val retained = buildList {
        existing.forEach { entry ->
            if (entry.uri !in selectedByUri) {
                add(entry)
            } else if (favorite || entry.lastOpened > 0L) {
                add(entry.copy(isFavorite = favorite))
            } else {
                removed += entry
            }
        }

        if (favorite) {
            selectedByUri.values
                .filterNot { it.uri in existingUris }
                .forEach { document ->
                    add(document.copy(isFavorite = true, lastOpened = 0L))
                }
        }
    }

    return FavoriteUpdateResult(retained = retained, removed = removed)
}

internal fun clearRecentEntries(documents: List<DocumentEntry>): ClearRecentResult {
    val (favorites, others) = documents.partition { it.isFavorite }
    return ClearRecentResult(
        retained = favorites.map { it.copy(lastOpened = 0L) },
        removed = others,
    )
}
