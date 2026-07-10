package org.fossify.documents.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File

internal class DocumentUriPermissions(
    context: Context,
) {
    private val appContext = context.applicationContext

    fun persist(uri: Uri, grantFlags: Int): Boolean {
        val takeFlags = grantFlags and (
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

        if (takeFlags != 0) {
            runCatching {
                appContext.contentResolver.takePersistableUriPermission(uri, takeFlags)
            }
        }

        return hasPersistentReadAccess(uri)
    }

    fun release(uri: Uri) {
        val permission = appContext.contentResolver.persistedUriPermissions
            .firstOrNull { it.uri == uri }
            ?: return
        val flags = (if (permission.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                (if (permission.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)

        if (flags != 0) {
            runCatching {
                appContext.contentResolver.releasePersistableUriPermission(uri, flags)
            }
        }
    }

    fun hasPersistentReadAccess(uri: Uri): Boolean {
        if (uri.scheme == "file") {
            return File(uri.path.orEmpty()).canRead()
        }

        return appContext.contentResolver.persistedUriPermissions.any { permission ->
            permission.isReadPermission && permission.uri.covers(uri)
        }
    }

    fun isReadable(uri: Uri): Boolean {
        if (uri.scheme == "file") {
            return File(uri.path.orEmpty()).canRead()
        }

        return runCatching {
            appContext.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
        }.getOrDefault(false)
    }

    fun isWritable(uri: Uri): Boolean {
        if (uri.scheme == "file") {
            return File(uri.path.orEmpty()).canWrite()
        }

        return runCatching {
            appContext.contentResolver.openFileDescriptor(uri, "rw")?.use { true } ?: false
        }.getOrDefault(false)
    }
}

private fun Uri.covers(target: Uri): Boolean {
    val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(this) }.getOrNull()
    val targetDocumentId = runCatching { DocumentsContract.getDocumentId(target) }.getOrNull()
    val coversTreeDocument = authority == target.authority &&
            DocumentsContract.isTreeUri(this) &&
            DocumentsContract.isTreeUri(target) &&
            treeDocumentId != null &&
            targetDocumentId != null &&
            (targetDocumentId == treeDocumentId || targetDocumentId.startsWith("$treeDocumentId/"))

    return this == target || coversTreeDocument
}
