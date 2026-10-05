package org.fossify.documents.viewmodels

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.documents.R
import org.fossify.documents.data.DocumentsRepository
import java.io.File
import java.io.IOException

class PdfDocumentViewModel(application: Application) : AndroidViewModel(application) {
    private val resolver = application.contentResolver
    private val repository = DocumentsRepository(application)
    private val messages = Channel<Int>(Channel.BUFFERED)
    val copyMessages = messages.receiveAsFlow()
    var isCopying by mutableStateOf(false)
        private set

    fun saveCopy(source: Uri, destination: Uri, grantFlags: Int) {
        if (isCopying) return
        isCopying = true
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    copyDocument(source, destination)
                    repository.rememberDocument(destination, grantFlags)
                }
                messages.send(org.fossify.commons.R.string.file_saved)
            } catch (_: IOException) {
                messages.send(R.string.could_not_save_document)
            } catch (_: SecurityException) {
                messages.send(R.string.could_not_save_document)
            } finally {
                isCopying = false
            }
        }
    }

    private fun copyDocument(source: Uri, destination: Uri) {
        val temporary = File.createTempFile("pdf-copy-", ".pdf", getApplication<Application>().cacheDir)
        try {
            val input = resolver.openInputStream(source) ?: throw IOException()
            input.use { sourceStream ->
                temporary.outputStream().use { sourceStream.copyTo(it) }
            }
            temporary.inputStream().use { sourceStream ->
                val output = resolver.openOutputStream(destination, "wt") ?: throw IOException()
                output.use { sourceStream.copyTo(it) }
            }
        } finally {
            temporary.delete()
        }
    }
}
