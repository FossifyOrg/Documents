package org.fossify.documents.data

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import org.fossify.commons.extensions.getRealPathFromURI
import java.io.File

internal class DocumentLocationResolver(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun resolveDocument(uri: Uri): String {
        val realPath = appContext.getRealPathFromURI(uri)
        return if (!realPath.isNullOrBlank()) {
            File(realPath).parent.orEmpty()
        } else {
            resolveExternalStorage(uri, includeDocumentName = false) ?: when (uri.authority) {
                DOWNLOADS_AUTHORITY -> "Downloads"
                else -> providerLabel(uri)
            }
        }
    }

    fun resolveFolder(
        treeUri: Uri,
        documentId: String,
        fallbackName: String,
    ): String {
        val documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)
        return resolveExternalStorage(documentUri, includeDocumentName = true)
            ?: resolveDocumentPath(documentUri)
            ?: "Downloads".takeIf { documentUri.authority == DOWNLOADS_AUTHORITY }
            ?: providerLabel(documentUri).takeIf { it.isNotBlank() }
            ?: fallbackName
    }

    private fun resolveDocumentPath(uri: Uri): String? {
        return if (DocumentsContract.isTreeUri(uri)) {
            runCatching {
                DocumentsContract.findDocumentPath(appContext.contentResolver, uri)
                    ?.path
                    ?.joinToString(separator = File.separator)
                    ?.takeIf { it.isNotBlank() }
            }.getOrNull()
        } else {
            null
        }
    }

    private fun resolveExternalStorage(
        uri: Uri,
        includeDocumentName: Boolean,
    ): String? {
        val documentId = uri.documentIdForLocation()
        return if (uri.authority != EXTERNAL_STORAGE_AUTHORITY || documentId == null) {
            null
        } else {
            val volume = documentId.substringBefore(':')
            var relativePath = documentId.substringAfter(':', missingDelimiterValue = "")
            if (!includeDocumentName && relativePath.isNotBlank()) {
                relativePath = relativePath.substringBeforeLast('/', missingDelimiterValue = "")
            }
            val root = when (volume.lowercase()) {
                "primary" -> Environment.getExternalStorageDirectory().absolutePath
                "home" -> File(
                    Environment.getExternalStorageDirectory(),
                    Environment.DIRECTORY_DOCUMENTS,
                ).absolutePath

                else -> File("/storage", volume).absolutePath
            }
            if (relativePath.isBlank()) root else File(root, relativePath).absolutePath
        }
    }

    private fun providerLabel(uri: Uri): String {
        val authority = uri.authority ?: return ""
        return runCatching {
            val provider = appContext.packageManager.resolveContentProvider(authority, 0)
            provider?.applicationInfo?.loadLabel(appContext.packageManager)?.toString().orEmpty()
        }.getOrDefault("")
    }

    private companion object {
        const val DOWNLOADS_AUTHORITY = "com.android.providers.downloads.documents"
        const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
    }
}

private fun Uri.documentIdForLocation(): String? {
    return runCatching {
        DocumentsContract.getDocumentId(this)
    }.getOrNull() ?: runCatching {
        DocumentsContract.getTreeDocumentId(this)
    }.getOrNull()
}
