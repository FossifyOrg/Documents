package org.fossify.documents.helpers

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fossify.documents.R
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class PdfDocumentAdapter(
    private val context: Context,
    private val uri: Uri,
    private val fileName: String,
) : PrintDocumentAdapter() {
    private val writeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onLayout(
        oldAttributes: PrintAttributes,
        printAttributes: PrintAttributes,
        cancellationSignal: CancellationSignal,
        layoutResultCallback: LayoutResultCallback,
        extras: Bundle
    ) {
        if (cancellationSignal.isCanceled) {
            layoutResultCallback.onLayoutCancelled()
        } else {
            val info = PrintDocumentInfo.Builder(fileName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            layoutResultCallback.onLayoutFinished(info, oldAttributes != printAttributes)
        }
    }

    override fun onWrite(
        pages: Array<PageRange>,
        parcelFileDescriptor: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
        writeResultCallback: WriteResultCallback
    ) {
        if (cancellationSignal.isCanceled) {
            writeResultCallback.onWriteCancelled()
            return
        }
        if (pages.size != 1 || pages.single() != PageRange.ALL_PAGES) {
            writeResultCallback.onWriteFailed(context.getString(R.string.pdf_print_whole_document_only))
            return
        }

        writeScope.launch {
            val result = try {
                writePdf(parcelFileDescriptor, cancellationSignal)
                if (cancellationSignal.isCanceled) WriteResult.Cancelled else WriteResult.Finished
            } catch (error: IOException) {
                WriteResult.Failed(error.message)
            } catch (error: SecurityException) {
                WriteResult.Failed(error.message)
            }

            withContext(Dispatchers.Main) {
                when (result) {
                    WriteResult.Cancelled -> writeResultCallback.onWriteCancelled()
                    WriteResult.Finished -> writeResultCallback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                    is WriteResult.Failed -> writeResultCallback.onWriteFailed(result.message)
                }
            }
        }
    }

    override fun onFinish() {
        writeScope.cancel()
        super.onFinish()
    }

    private fun writePdf(
        parcelFileDescriptor: ParcelFileDescriptor,
        cancellationSignal: CancellationSignal,
    ) {
        val input = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException(fileName)
        input.use { inputStream ->
            FileOutputStream(parcelFileDescriptor.fileDescriptor).use { outputStream ->
                copyToOutput(inputStream, outputStream, cancellationSignal)
            }
        }
    }

    private fun copyToOutput(
        inputStream: InputStream,
        outputStream: OutputStream,
        cancellationSignal: CancellationSignal,
    ) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var size: Int

        while (inputStream.read(buffer).also { size = it } != -1 && !cancellationSignal.isCanceled) {
            outputStream.write(buffer, 0, size)
        }
    }

    private sealed interface WriteResult {
        data object Cancelled : WriteResult
        data object Finished : WriteResult
        data class Failed(val message: String?) : WriteResult
    }
}
