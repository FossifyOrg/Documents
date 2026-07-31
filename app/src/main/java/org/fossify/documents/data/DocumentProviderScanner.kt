package org.fossify.documents.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.core.net.toUri
import org.fossify.commons.extensions.getFilenameFromUri
import org.fossify.commons.extensions.getMimeTypeFromUri
import org.fossify.documents.models.DocumentEntry
import org.fossify.documents.models.DocumentFolder
import org.fossify.documents.models.DocumentKind
import java.io.IOException

internal class DocumentProviderScanner(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val locationResolver = DocumentLocationResolver(appContext)

    fun readDocument(uri: Uri, previous: DocumentEntry?): DocumentEntry {
        val queried = queryMetadata(uri)
        val mimeType = appContext.getMimeTypeFromUri(uri).ifBlank { previous?.mimeType.orEmpty() }
        val name = queried.name
            ?: previous?.name
            ?: appContext.getFilenameFromUri(uri).takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment.orEmpty()
        val kind = DocumentKind.fromName(name, mimeType)

        return DocumentEntry(
            uri = uri.toString(),
            name = name,
            mimeType = mimeType,
            kind = kind,
            location = locationResolver.resolveDocument(uri).ifBlank { previous?.location.orEmpty() },
            size = queried.size ?: previous?.size,
            lastModified = queried.lastModified ?: previous?.lastModified,
            lastOpened = previous?.lastOpened ?: 0L,
            lastPage = previous?.lastPage ?: 0,
            isFavorite = previous?.isFavorite ?: false,
        )
    }

    fun readFolder(uri: Uri, previous: DocumentFolder?): DocumentFolder {
        val name = queryFolderName(uri)
            ?: previous?.name
            ?: fallbackFolderName(uri)

        return DocumentFolder(
            uri = uri.toString(),
            name = name,
            itemCount = appContext.getFolderItemCount(uri.toString()),
            lastOpened = previous?.lastOpened ?: 0L,
        )
    }

    fun readFolderContent(uri: Uri, previous: DocumentFolder?): DocumentFolderContent {
        val folderName = queryFolderName(uri)
            ?: previous?.name
            ?: fallbackFolderName(uri)
        val parentDocumentId = uri.getDocumentIdForChildren()
            ?: throw IOException("Could not access this folder.")

        val children = queryFolderChildren(
            treeUri = uri,
            parentDocumentId = parentDocumentId,
            folderLocation = locationResolver.resolveFolder(uri, parentDocumentId, folderName),
        )
        return DocumentFolderContent(
            folder = DocumentFolder(
                uri = uri.toString(),
                name = folderName,
                itemCount = children.folders.size + children.documents.size,
                lastOpened = previous?.lastOpened ?: 0L,
            ),
            childFolders = children.folders,
            documents = children.documents,
        )
    }

    private fun queryFolderChildren(
        treeUri: Uri,
        parentDocumentId: String,
        folderLocation: String,
    ): FolderChildren {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

        return appContext.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            val folders = mutableListOf<DocumentFolder>()
            val documents = mutableListOf<DocumentEntry>()
            while (cursor.moveToNext()) {
                val mimeType = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_MIME_TYPE).orEmpty()
                val documentId = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                    ?: continue
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    folders += DocumentFolder(
                        uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString(),
                        name = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_DISPLAY_NAME).orEmpty(),
                    )
                } else {
                    cursor.toFolderDocument(treeUri, folderLocation)?.let(documents::add)
                }
            }

            FolderChildren(
                folders = folders.sortedBy { it.name.lowercase() },
                documents = documents.sortedBy { it.name.lowercase() },
            )
        } ?: throw IOException("Could not access this folder.")
    }

    fun refreshFolderCount(folder: DocumentFolder): DocumentFolder {
        return folder.copy(itemCount = appContext.getFolderItemCount(folder.uri))
    }

    private fun Cursor.toFolderDocument(
        treeUri: Uri,
        folderLocation: String,
    ): DocumentEntry? {
        val mimeType = getStringOrNull(DocumentsContract.Document.COLUMN_MIME_TYPE).orEmpty()
        val name = getStringOrNull(DocumentsContract.Document.COLUMN_DISPLAY_NAME).orEmpty()
        val kind = DocumentKind.fromName(name, mimeType)

        return getStringOrNull(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            ?.takeIf {
                mimeType != DocumentsContract.Document.MIME_TYPE_DIR &&
                        kind != DocumentKind.OTHER
            }
            ?.let { documentId ->
                DocumentEntry(
                    uri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId).toString(),
                    name = name,
                    mimeType = mimeType,
                    kind = kind,
                    location = folderLocation,
                    size = getLongOrNull(DocumentsContract.Document.COLUMN_SIZE),
                    lastModified = getLongOrNull(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                    lastOpened = 0L,
                )
            }
    }

    private fun queryFolderName(uri: Uri): String? {
        val documentId = uri.getDocumentIdForChildren() ?: return null
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(uri, documentId)
        val projection = arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME)

        return runCatching {
            appContext.contentResolver.query(documentUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    cursor.getStringOrNull(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                } else {
                    null
                }
            }
        }.getOrNull()
    }

    private fun queryMetadata(uri: Uri): QueriedMetadata {
        val projection = arrayOf(
            OpenableColumns.DISPLAY_NAME,
            OpenableColumns.SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        )

        return runCatching {
            appContext.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    QueriedMetadata(
                        name = cursor.getStringOrNull(OpenableColumns.DISPLAY_NAME),
                        size = cursor.getLongOrNull(OpenableColumns.SIZE),
                        lastModified = cursor.getLongOrNull(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
                    )
                } else {
                    QueriedMetadata()
                }
            } ?: QueriedMetadata()
        }.getOrDefault(QueriedMetadata())
    }

    private fun fallbackFolderName(uri: Uri): String {
        return Uri.decode(uri.lastPathSegment.orEmpty())
            .substringAfterLast(':')
            .ifBlank { uri.lastPathSegment.orEmpty() }
    }

    private data class QueriedMetadata(
        val name: String? = null,
        val size: Long? = null,
        val lastModified: Long? = null,
    )

    private data class FolderChildren(
        val folders: List<DocumentFolder> = emptyList(),
        val documents: List<DocumentEntry> = emptyList(),
    )

}

private fun Uri.getDocumentIdForChildren(): String? {
    return runCatching {
        DocumentsContract.getDocumentId(this)
    }.getOrNull() ?: runCatching {
        DocumentsContract.getTreeDocumentId(this)
    }.getOrNull()
}

private fun Context.getFolderItemCount(folderUri: String): Int {
    val treeUri = folderUri.toUri()
    val parentDocumentId = treeUri.getDocumentIdForChildren() ?: return 0
    val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
    val projection = arrayOf(
        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        DocumentsContract.Document.COLUMN_MIME_TYPE,
    )

    return runCatching {
        contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
            var itemCount = 0
            while (cursor.moveToNext()) {
                val name = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_DISPLAY_NAME).orEmpty()
                val mimeType = cursor.getStringOrNull(DocumentsContract.Document.COLUMN_MIME_TYPE).orEmpty()
                val isFolder = mimeType == DocumentsContract.Document.MIME_TYPE_DIR
                if (isFolder || DocumentKind.fromName(name, mimeType) != DocumentKind.OTHER) {
                    itemCount++
                }
            }
            itemCount
        } ?: 0
    }.getOrDefault(0)
}
