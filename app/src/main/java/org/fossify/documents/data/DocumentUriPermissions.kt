package org.fossify.documents.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Process
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

        val hasWritePermission = appContext.checkUriPermission(
            uri,
            Process.myPid(),
            Process.myUid(),
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasWritePermission) {
            return false
        }

        return queryDocumentSupportsWrite(uri) ?: true
    }

    private fun queryDocumentSupportsWrite(uri: Uri): Boolean? {
        if (!DocumentsContract.isDocumentUri(appContext, uri)) {
            return null
        }

        return runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) {
                    return@use null
                }

                val flagsIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)
                if (flagsIndex < 0) {
                    null
                } else {
                    cursor.getInt(flagsIndex) and DocumentsContract.Document.FLAG_SUPPORTS_WRITE != 0
                }
            }
        }.getOrNull()
    }
}

private fun Uri.covers(target: Uri): Boolean {
    val treeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(this) }.getOrNull()
    val targetTreeDocumentId = runCatching { DocumentsContract.getTreeDocumentId(target) }.getOrNull()
    val coversTreeDocument = authority == target.authority &&
            DocumentsContract.isTreeUri(this) &&
            DocumentsContract.isTreeUri(target) &&
            treeDocumentId != null &&
            targetTreeDocumentId == treeDocumentId

    return this == target || coversTreeDocument
}
