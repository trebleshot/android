package com.genonbeta.android.framework.util

import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import android.content.Context
import kotlin.Throws
import com.genonbeta.android.framework.io.StreamInfo.FolderStateException
import android.provider.OpenableColumns
import androidx.annotation.RequiresApi
import android.provider.DocumentsContract
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.io.*
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Exception
import java.net.URI
import java.net.URLConnection
import java.util.*

/**
 * created by: veli
 * date: 7/31/18 8:14 AM
 */
open class FileUtils {
    companion object {
        val TAG: String? = FileUtils::class.java.simpleName
        @Throws(Exception::class)
        fun copy(
            context: Context?, source: DocumentFile?, destination: DocumentFile?, stoppable: Stoppable?,
            bufferLength: Int, socketTimeout: Int
        ) {
            val resolver = context.getContentResolver()
            val inputStream = resolver.openInputStream(source.getUri())
            val outputStream = resolver.openOutputStream(destination.getUri())
            if (inputStream == null || outputStream == null) throw IOException("Failed to open streams to start copying")
            val buffer = ByteArray(bufferLength)
            var len = 0
            var lastRead = System.currentTimeMillis()
            while (len != -1) {
                if (inputStream.read(buffer).also { len = it } > 0) {
                    outputStream.write(buffer, 0, len)
                    outputStream.flush()
                    lastRead = System.currentTimeMillis()
                }
                if (System.currentTimeMillis() - lastRead > socketTimeout || stoppable.isInterrupted()) throw Exception(
                    "Timed out or interrupted. Exiting!"
                )
            }
            outputStream.close()
            inputStream.close()
        }

        @JvmOverloads
        @Throws(IOException::class)
        fun fetchDirectories(
            directoryFile: DocumentFile?,
            path: String?,
            createIfNotExists: Boolean = true
        ): DocumentFile? {
            var currentDirectory = directoryFile
            val pathArray: Array<String?> = path.split(File.separator.toRegex()).toTypedArray()
            for (currentPath in pathArray) {
                if (currentDirectory == null) throw IOException("Failed to create directories: $path")
                val existingOne = currentDirectory.findFile(currentPath)
                if (existingOne != null && !existingOne.isDirectory) throw IOException("A file exists for of directory name: $currentPath ; $path")
                currentDirectory =
                    if (existingOne == null && createIfNotExists) currentDirectory.createDirectory(currentPath) else existingOne
            }
            return currentDirectory
        }

        @JvmOverloads
        @Throws(IOException::class)
        fun fetchFile(
            directoryFile: DocumentFile?, path: String?, displayName: String?,
            createIfNotExists: Boolean = true
        ): DocumentFile? {
            val documentFile = if (path == null) directoryFile else fetchDirectories(
                directoryFile, path,
                createIfNotExists
            )
            if (documentFile != null) {
                val existingOne = documentFile.findFile(displayName)
                if (existingOne != null) {
                    if (!existingOne.isFile) throw IOException("A directory exists for of file name")
                    return existingOne
                }
                if (createIfNotExists) {
                    val createdFile = documentFile.createFile(null, displayName)
                    if (createdFile != null) return createdFile
                }
            }
            throw IOException("Failed to create file: $path")
        }

        @Throws(FileNotFoundException::class)
        fun fromUri(context: Context?, uri: Uri?): DocumentFile? {
            val uriType = uri.toString()
            return if (uriType.startsWith("file")) DocumentFile.Companion.fromFile(
                File(
                    URI.create(
                        uriType
                    )
                )
            ) else DocumentFile.Companion.fromUri(context, uri, false)
        }

        fun geActionTypeToView(type: String?): String? {
            return if ("application/vnd.android.package-archive" == type && Build.VERSION.SDK_INT >= 14) Intent.ACTION_INSTALL_PACKAGE else Intent.ACTION_VIEW
        }

        fun getFileContentType(fileUrl: String?): String? {
            val nameMap = URLConnection.getFileNameMap()
            val fileType = nameMap.getContentTypeFor(fileUrl)
            return fileType ?: "*/*"
        }

        fun getFileExtension(fileName: String?): String? {
            val lastDot = fileName.lastIndexOf('.')
            if (lastDot >= 0) {
                val extension = fileName.substring(lastDot + 1).toLowerCase()
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (mime != null) return ".$extension"
            }
            return ""
        }

        fun getOpenIntent(context: Context?, file: DocumentFile?): Intent? {
            return if (Build.VERSION.SDK_INT >= 24 || Build.VERSION.SDK_INT == 23 && Intent.ACTION_INSTALL_PACKAGE != geActionTypeToView(
                    file.getType()
                )
            ) {
                getOpenIntent(
                    getSecureUriSilently(
                        context,
                        file
                    ), file.getType()
                )
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else getOpenIntent(
                file.getOriginalUri(),
                file.getType()
            )
        }

        fun getOpenIntent(url: Uri?, type: String?): Intent? {
            return Intent(geActionTypeToView(type)).setDataAndType(url, type)
        }

        @Throws(IOException::class)
        fun getSecureUri(context: Context?, documentFile: DocumentFile?): Uri? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M || documentFile is TreeDocumentFile) return documentFile.getUri()
            if (documentFile is StreamDocumentFile) return getSecureUri(
                context,
                (documentFile as StreamDocumentFile?).getStream()
            )
            if (documentFile is LocalDocumentFile) return getSelfProviderFile(
                context,
                (documentFile as LocalDocumentFile?).getFile()
            )
            throw IOException("Cannot gather right method to create uri")
        }

        fun getSecureUriSilently(context: Context?, documentFile: DocumentFile?): Uri? {
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

        fun getSecureUri(context: Context?, streamInfo: StreamInfo?): Uri? {
            return if (StreamInfo.Type.File == streamInfo.type) getSelfProviderFile(
                context,
                streamInfo.file
            ) else streamInfo.uri
        }

        fun getSelfProviderFile(context: Context?, file: File?): Uri? {
            return FileProvider.getUriForFile(
                context, context.getApplicationContext().packageName
                        + ".fileprovider", file
            )
        }

        fun getUniqueFileName(documentFolder: DocumentFile?, fileName: String?, tryActualFile: Boolean): String? {
            if (tryActualFile && documentFolder.findFile(fileName) == null) return fileName
            val pathStartPosition = fileName.lastIndexOf(".")
            var mergedName = if (pathStartPosition != -1) fileName.substring(0, pathStartPosition) else fileName
            var fileExtension = if (pathStartPosition != -1) fileName.substring(pathStartPosition) else ""
            if (mergedName.length == 0 && fileExtension.length > 0) {
                mergedName = fileExtension
                fileExtension = ""
            }
            for (exceed in 1..998) {
                val newName = "$mergedName ($exceed)$fileExtension"
                if (documentFolder.findFile(newName) == null) return newName
            }
            return fileName
        }

        @Throws(Exception::class)
        fun move(
            context: Context?, targetFile: DocumentFile?, destinationFile: DocumentFile?,
            stoppable: Stoppable?, bufferLength: Int, socketTimeout: Int
        ): Boolean {
            if (targetFile !is LocalDocumentFile || destinationFile !is LocalDocumentFile
                || !(targetFile as LocalDocumentFile?).getFile()
                    .renameTo((destinationFile as LocalDocumentFile?).getFile())
            ) copy(context, targetFile, destinationFile, stoppable, bufferLength, socketTimeout)

            // syncs the file with latest data if it is database based
            destinationFile.sync()
            if (targetFile.length() == destinationFile.length()) {
                targetFile.delete()
                return true
            }
            return false
        }

        fun openUri(context: Context?, file: DocumentFile?): Boolean {
            return openUri(context, getOpenIntent(context, file))
        }

        fun openUri(context: Context?, uri: Uri?): Boolean {
            return openUri(context, getOpenIntent(uri, context.getContentResolver().getType(uri)))
        }

        fun openUri(context: Context?, intent: Intent?): Boolean {
            try {
                context.startActivity(intent)
                return true
            } catch (e: Throwable) {
                Log.d(TAG, String.format(Locale.US, "Open uri request failed with error message '%s'", e.message))
            }
            return false
        }

        fun sizeExpression(bytes: Long, notUseByte: Boolean): String? {
            val unit = if (notUseByte) 1000 else 1024
            if (bytes < unit) return "$bytes B"
            val expression = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())) as Int
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
}