package com.genonbeta.android.framework.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.genonbeta.android.framework.io.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.URLConnection
import java.util.*
import kotlin.math.ln

/**
 * created by: veli
 * date: 7/31/18 8:14 AM
 */
object Files {
    val TAG = Files::class.java.simpleName

    @Throws(Exception::class)
    fun copy(
        context: Context, source: DocumentFile, destination: DocumentFile, stoppable: Stoppable?,
        bufferLength: Int, socketTimeout: Int,
    ) {
        // TODO: 2/8/21 DocumentContract has copyDocument feature but it doesn't show the progress
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(source.getUri())
        val outputStream = resolver.openOutputStream(destination.getUri())
        if (inputStream == null || outputStream == null)
            throw IOException("Failed to open streams to start copying")

        val buffer = ByteArray(bufferLength)
        var len = 0
        var time = System.currentTimeMillis()
        var lastRead = time

        while (len != -1) {
            time = System.currentTimeMillis()

            if (inputStream.read(buffer).also { len = it } > 0) {
                outputStream.write(buffer, 0, len)
                outputStream.flush()
                lastRead = time
            }

            if (time - lastRead > socketTimeout || stoppable?.isInterrupted() == true)
                throw Exception("Timed out or interrupted. Exiting!")
        }

        outputStream.close()
        inputStream.close()
    }

    @Throws(IOException::class)
    fun fetchDirectories(directory: DocumentFile, path: String, createIfNeeded: Boolean = true): DocumentFile {
        var current: DocumentFile? = directory
        val pathArray: Array<String> = path.split(File.separator.toRegex()).toTypedArray()

        for (currentPath in pathArray) {
            if (current == null)
                throw IOException("Failed to create directories: $path")

            val existing = current.findFile(currentPath)

            existing?.let {
                if (!existing.isDirectory())
                    throw IOException("A file exists for of directory name: $currentPath ; $path")
            }

            current = if (existing == null && createIfNeeded) current.createDirectory(currentPath) else existing
        }

        return current as DocumentFile
    }

    @Throws(IOException::class)
    fun fetchFile(
        directory: DocumentFile,
        path: String?,
        mimeType: String,
        fileName: String,
        createIfNeeded: Boolean = true,
    ): DocumentFile {
        val documentFile = if (path == null) directory else fetchDirectories(directory, path, createIfNeeded)
        val existing = documentFile.findFile(fileName)

        if (existing != null) {
            if (!existing.isFile())
                throw IOException("An entity in the same directory with the same name already exists.")
            return existing
        } else if (createIfNeeded) {
            val createdFile = documentFile.createFile(mimeType, fileName)
            if (createdFile != null)
                return createdFile
        }

        throw IOException("Failed to create file: $path")
    }

    @Throws(FileNotFoundException::class)
    fun fromUri(context: Context, uri: Uri): DocumentFile {
        val uriType = uri.toString()
        return if (uriType.startsWith("file")) DocumentFile.fromFile(
            File(
                URI.create(
                    uriType
                )
            )
        ) else DocumentFile.fromUri(context, uri, false)
    }

    fun geActionTypeToView(type: String?): String {
        return if ("application/vnd.android.package-archive" == type && Build.VERSION.SDK_INT >= 14)
            Intent.ACTION_INSTALL_PACKAGE
        else
            Intent.ACTION_VIEW
    }

    fun getFileContentType(fileUrl: String?): String {
        val nameMap = URLConnection.getFileNameMap()
        val fileType = nameMap.getContentTypeFor(fileUrl)
        return fileType ?: "*/*"
    }

    fun getFileExtension(fileName: String): String {
        val lastDot = fileName.lastIndexOf('.')

        if (lastDot >= 0) {
            val extension = fileName.substring(lastDot + 1).toLowerCase(Locale.ROOT)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) return ".$extension"
        }

        return ""
    }

    fun getOpenIntent(context: Context, file: DocumentFile): Intent {
        return if (Build.VERSION.SDK_INT >= 24 || Build.VERSION.SDK_INT == 23
            && Intent.ACTION_INSTALL_PACKAGE != geActionTypeToView(file.getType())
        ) {
            getOpenIntent(getSecureUriSilently(context, file), file.getType())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else
            getOpenIntent(file.originalUri, file.getType())
    }

    fun getOpenIntent(url: Uri?, type: String?): Intent {
        return Intent(geActionTypeToView(type)).setDataAndType(url, type)
    }

    @Throws(IOException::class)
    fun getSecureUri(context: Context, documentFile: DocumentFile): Uri {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || documentFile is TreeDocumentFile)
            return documentFile.getUri()

        if (documentFile is StreamDocumentFile)
            return getSecureUri(context, documentFile.stream)
        if (documentFile is LocalDocumentFile)
            return getSelfProviderFile(context, documentFile.file)
        throw IOException("Cannot gather right method to create uri")
    }

    fun getSecureUriSilently(context: Context, documentFile: DocumentFile): Uri {
        try {
            return getSecureUri(context, documentFile)
        } catch (e: Throwable) {
            // do nothing
            Log.d(
                TAG, String.format(
                    Locale.US, "Cannot create secure uri for the file %s with error message '%s'",
                    documentFile.getName(), e.message
                )
            )
        }
        return documentFile.getUri()
    }

    fun getSecureUri(context: Context, streamInfo: StreamInfo): Uri {
        return if (streamInfo.file != null) getSelfProviderFile(context, streamInfo.file) else streamInfo.uri
    }

    fun getSelfProviderFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.applicationContext.packageName}.fileprovider", file)
    }

    fun getUniqueFileName(directory: DocumentFile, fileName: String, tryOriginalFirst: Boolean): String {
        if (tryOriginalFirst && directory.findFile(fileName) == null)
            return fileName

        val pathStartPosition = fileName.lastIndexOf(".")
        var mergedName = if (pathStartPosition != -1) fileName.substring(0, pathStartPosition) else fileName
        var fileExtension = if (pathStartPosition != -1) fileName.substring(pathStartPosition) else ""
        if (mergedName.isEmpty() && fileExtension.isNotEmpty()) {
            mergedName = fileExtension
            fileExtension = ""
        }
        for (exceed in 1..998) {
            val newName = "$mergedName ($exceed)$fileExtension"
            if (directory.findFile(newName) == null) return newName
        }
        return fileName
    }

    @Throws(Exception::class)
    fun move(
        context: Context, target: DocumentFile, destination: DocumentFile,
        stoppable: Stoppable, bufferLength: Int, socketTimeout: Int,
    ): Boolean {
        if (target !is LocalDocumentFile || destination !is LocalDocumentFile
            || target.file.renameTo(destination.file)
        )
            copy(context, target, destination, stoppable, bufferLength, socketTimeout)

        // syncs the file with latest data if it is database based
        destination.sync()

        if (target.getLength() == destination.getLength()) {
            target.delete()
            return true
        }
        return false
    }

    fun openUri(context: Context, file: DocumentFile): Boolean {
        return openUri(context, getOpenIntent(context, file))
    }

    fun openUri(context: Context, uri: Uri): Boolean {
        return openUri(context, getOpenIntent(uri, context.getContentResolver().getType(uri)))
    }

    fun openUri(context: Context, intent: Intent): Boolean {
        try {
            context.startActivity(intent)
            return true
        } catch (e: Throwable) {
            Log.d(TAG, String.format(Locale.US, "Open uri request failed with error message '%s'", e.message))
        }
        return false
    }

    fun sizeExpression(bytes: Long, notUseByte: Boolean): String {
        val unit = if (notUseByte) 1000 else 1024
        if (bytes < unit) return "$bytes B"
        val expression = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val prefix =
            (if (notUseByte) "kMGTPE" else "KMGTPE")[expression - 1].toString() + if (notUseByte) "i" else ""
        return String.format(
            Locale.getDefault(),
            "%.1f %sB",
            bytes / Math.pow(unit.toDouble(), expression.toDouble()),
            prefix
        )
    }
}