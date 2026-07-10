package org.fossify.documents.data

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import org.fossify.documents.helpers.Config
import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentFolder
import org.json.JSONArray

internal class DocumentsStore(
    private val config: Config,
) {
    val documentsFlow: Flow<List<DocumentEntry>> = config.documentsJsonFlow
        .onStart { emit(config.documentsJson) }
        .distinctUntilChanged()
        .map(::decodeDocuments)
        .flowOn(Dispatchers.Default)

    val foldersFlow: Flow<List<DocumentFolder>> = config.documentFoldersJsonFlow
        .onStart { emit(config.documentFoldersJson) }
        .distinctUntilChanged()
        .map(::decodeFolders)

    val showFileLocationsFlow: Flow<Boolean> = config.showFileLocationsFlow
        .onStart { emit(config.showFileLocations) }
        .distinctUntilChanged()

    fun getDocuments(): List<DocumentEntry> = synchronized(DOCUMENTS_LOCK) {
        decodeDocuments(config.documentsJson)
    }

    fun getFolders(): List<DocumentFolder> = synchronized(FOLDERS_LOCK) {
        decodeFolders(config.documentFoldersJson)
    }

    fun getDocument(uri: Uri): DocumentEntry? {
        return getDocuments().firstOrNull { it.uri == uri.toString() }
    }

    fun updateDocuments(transform: (List<DocumentEntry>) -> List<DocumentEntry>) {
        synchronized(DOCUMENTS_LOCK) {
            writeDocumentsLocked(transform(decodeDocuments(config.documentsJson)))
        }
    }

    fun updateFolders(transform: (List<DocumentFolder>) -> List<DocumentFolder>) {
        synchronized(FOLDERS_LOCK) {
            writeFoldersLocked(transform(decodeFolders(config.documentFoldersJson)))
        }
    }

    private fun writeDocumentsLocked(documents: List<DocumentEntry>) {
        val sorted = documents
            .distinctBy { it.uri }
            .sortedWith(compareByDescending<DocumentEntry> { it.lastOpened }.thenBy { it.name.lowercase() })

        config.documentsJson = JSONArray().apply {
            sorted.forEach { put(it.toJson()) }
        }.toString()
    }

    private fun writeFoldersLocked(folders: List<DocumentFolder>) {
        val sorted = folders
            .distinctBy { it.uri }
            .sortedWith(compareByDescending<DocumentFolder> { it.lastOpened }.thenBy { it.name.lowercase() })

        config.documentFoldersJson = JSONArray().apply {
            sorted.forEach { put(it.toJson()) }
        }.toString()
    }

    private fun decodeDocuments(json: String): List<DocumentEntry> {
        return runCatching {
            val array = JSONArray(json)
            buildList {
                repeat(array.length()) { index ->
                    val entry = array.optJSONObject(index)?.let(DocumentEntry::fromJson)
                    if (entry != null) {
                        add(entry)
                    }
                }
            }.sortedWith(compareByDescending<DocumentEntry> { it.lastOpened }.thenBy { it.name.lowercase() })
        }.getOrDefault(emptyList())
    }

    private fun decodeFolders(json: String): List<DocumentFolder> {
        return runCatching {
            val array = JSONArray(json)
            buildList {
                repeat(array.length()) { index ->
                    val folder = array.optJSONObject(index)?.let(DocumentFolder::fromJson)
                    if (folder != null) {
                        add(folder)
                    }
                }
            }.sortedWith(compareByDescending<DocumentFolder> { it.lastOpened }.thenBy { it.name.lowercase() })
        }.getOrDefault(emptyList())
    }

    private companion object {
        val DOCUMENTS_LOCK = Any()
        val FOLDERS_LOCK = Any()
    }
}
