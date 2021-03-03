/*
/ *
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.monora.uprotocol.client.android.service

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.util.Log
import androidx.annotation.StringRes
import androidx.collection.ArrayMap
import androidx.core.app.NotificationCompat
import androidx.preference.PreferenceManager
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.util.Files.getOpenIntent
import com.genonbeta.android.framework.util.Stoppable
import com.genonbeta.android.framework.util.StoppableImpl
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.iki.elonen.NanoHTTPD
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.FileExplorerActivity
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.WebClient
import org.monora.uprotocol.client.android.fragment.FileListFragment
import org.monora.uprotocol.client.android.util.AppUtils
import org.monora.uprotocol.client.android.util.Files
import org.monora.uprotocol.client.android.util.Notifications
import org.monora.uprotocol.client.android.util.Time
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.transfer.TransferItem
import org.monora.uprotocol.core.transfer.TransferItem.Type.Incoming
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.regex.Pattern
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * created by: veli
 * date: 4/7/19 12:41 AM
 */
@Singleton
class WebShareServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val persistenceProvider: PersistenceProvider
) : NanoHTTPD(AppConfig.SERVER_PORT_WEBSHARE) {
    private val assetManager = context.assets

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    private val notifications: Notifications = Notifications(context)

    private val mediaScanner = MediaScannerConnection(context, null)

    private val client = persistenceProvider.client

    var hadClients = true
        private set

    override fun stop() {
        super.stop()
        mediaScanner.disconnect()
    }

    // TODO: 2/26/21 Fix web server
    /**
    override fun serve(session: IHTTPSession): Response {
        try {
            hadClients = true
            val files: Map<String, String> = ArrayMap()
            val method: Method = session.method
            var receiveTimeElapsed = System.currentTimeMillis()
            val notificationId = AppUtils.uniqueNumber
            val notification = notifications.buildDynamicNotification(
                notificationId, Notifications.NOTIFICATION_CHANNEL_LOW
            )
            val address: String = session.headers["http-client-ip"]!!
            val webClient = appDatabase.webClientDao().get(address) ?: WebClient(address, address).also {
                appDatabase.webClientDao().insert(it)
            }

            if (webClient.blocked) return newFixedLengthResponse(
                Response.Status.ACCEPTED, "text/html",
                makePage(
                    "arrow-left.svg", R.string.text_send,
                    makeNotFoundTemplate(
                        R.string.mesg_somethingWentWrong,
                        R.string.mesg_notAllowed
                    )
                )
            )
            if (Method.PUT == method || Method.POST == method) {
                try {
                    notification.setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentInfo(context.getString(R.string.text_webShare))
                        .setContentTitle(context.getString(R.string.text_receiving))
                        .setContentText(webClient.title)
                    notification.show()
                    session.parseBody(files)
                    receiveTimeElapsed = System.currentTimeMillis() - receiveTimeElapsed
                } catch (var5: IOException) {
                    return newFixedLengthResponse(
                        Response.Status.INTERNAL_ERROR,
                        "text/plain", "SERVER INTERNAL ERROR: IOException: "
                                + var5.message
                    )
                } catch (var6: ResponseException) {
                    return newFixedLengthResponse(
                        var6.getStatus(), "text/plain",
                        var6.message
                    )
                }
            }
            if (session.parms.containsKey("file")) {
                val fileName = session.parms["file"]
                val filePath = files["file"]
                if (fileName == null || filePath == null || fileName.isEmpty()) {
                    notification.cancel()
                    return newFixedLengthResponse(
                        Response.Status.ACCEPTED, "text/html",
                        makePage(
                            "arrow-left.svg", R.string.text_send,
                            makeNotFoundTemplate(
                                R.string.mesg_somethingWentWrong,
                                R.string.text_listEmptyFiles
                            )
                        )
                    )
                } else {
                    val tmpFile = File(filePath)
                    val savePath = Files.getApplicationDirectory(context)
                    val stoppable: Stoppable = StoppableImpl()
                    val sourceFile = DocumentFile.fromFile(tmpFile)
                    val destFile = savePath.createFile(
                        sourceFile.getType(),
                        com.genonbeta.android.framework.util.Files.getUniqueFileName(savePath, fileName, true)
                    )!!
                    run {
                        notification.setSmallIcon(R.drawable.ic_compare_arrows_white_24dp_static)
                            .setContentInfo(context.getString(R.string.text_webShare))
                            .setContentTitle(context.getString(R.string.text_preparingFiles))
                            .setContentText(fileName)
                        notification.show()
                        try {
                            val resolver: ContentResolver = context.contentResolver
                            val inputStream: InputStream = resolver.openInputStream(sourceFile.getUri())!!
                            val outputStream: OutputStream = resolver.openOutputStream(destFile.getUri())!!
                            val buffer = ByteArray(AppConfig.BUFFER_LENGTH_DEFAULT)
                            var len = 0
                            var lastRead = System.currentTimeMillis()
                            var lastNotified: Long = 0
                            var totalRead: Long = 0
                            while (len != -1) {
                                if (inputStream.read(buffer).also { len = it } > 0) {
                                    outputStream.write(buffer, 0, len)
                                    outputStream.flush()
                                    lastRead = System.currentTimeMillis()
                                    totalRead += len.toLong()
                                }
                                if (sourceFile.getLength() > 0 && totalRead > 0 && System.currentTimeMillis() - lastNotified > AppConfig.DELAY_DEFAULT_NOTIFICATION) {
                                    notification.updateProgress(
                                        100,
                                        (totalRead / sourceFile.getLength() * 100).toInt(), false
                                    )
                                    lastNotified = System.currentTimeMillis()
                                }
                                if (System.currentTimeMillis() - lastRead > AppConfig.DEFAULT_TIMEOUT_SOCKET
                                    || stoppable.interrupted()
                                ) throw Exception("Timed out or interrupted. Exiting!")
                            }
                            outputStream.close()
                            inputStream.close()
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    try {
                        destFile.sync()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    if (destFile.getLength() == tmpFile.length() || tmpFile.length() == 0L) try {
                        val webTransfer = appDatabase.transferDao().get(AppConfig.ID_GROUP_WEB_SHARE) ?: Transfer(
                            AppConfig.ID_GROUP_WEB_SHARE, Incoming, savePath.getUri().toString(),
                        ).also { appDatabase.transferDao().insertAll(it) }

                        // TODO: 2/26/21 Save in to the database
                        /*val transferItem = TransferItem(
                            AppUtils.uniqueNumber.toLong(), webTransfer.id,
                            destFile.getName(), destFile.getName(), destFile.getType(), destFile.getLength(),
                            TransferItem.Type.INCOMING
                        )
                        transferItem.flag = TransferItem.Flag.DONE
                        val address = DeviceAddress(
                            webClient.uid, InetAddress.getByName(address),
                            System.currentTimeMillis()
                        )
                        val member = TransferMember(webTransfer, webClient, TransferItem.Type.INCOMING)
                        kuick.publish(webTransfer)
                        kuick.publish(member)
                        kuick.publish(address)
                        kuick.publish(transferItem)
                        kuick.broadcast()*/
                        notification
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .setContentInfo(context.getString(R.string.text_webShare))
                            .setAutoCancel(true)
                            .setContentTitle(fileName)
                            .setDefaults(notifications.notificationSettings)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setContentText(
                                context.getString(
                                    R.string.text_receivedTransfer,
                                    com.genonbeta.android.framework.util.Files.formatLength(
                                        destFile.getLength(),
                                        false
                                    ),
                                    Time.formatElapsedTime(context, receiveTimeElapsed)
                                )
                            )
                            .addAction(
                                R.drawable.ic_folder_white_24dp_static,
                                context.getString(R.string.butn_showFiles), PendingIntent.getActivity(
                                    context, AppUtils.uniqueNumber,
                                    Intent(context, FileExplorerActivity::class.java)
                                        .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0
                                )
                            )
                        try {
                            val openIntent = getOpenIntent(context, destFile)
                            notification.setContentIntent(
                                PendingIntent.getActivity(
                                    context,
                                    AppUtils.uniqueNumber, openIntent, 0
                                )
                            )
                        } catch (ignored: Exception) {
                        }
                        notification.show()
                        context.sendBroadcast(
                            Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
                                .putExtra(FileListFragment.EXTRA_FILE_PARENT, savePath.getUri())
                                .putExtra(FileListFragment.EXTRA_FILE_NAME, destFile.getName())
                        )
                        if (mediaScanner.isConnected && destFile is LocalDocumentFile) mediaScanner.scanFile(
                            destFile.file.absolutePath, destFile.getType()
                        ) else Log.d(
                            TAG, "Could not save file to the media database: scanner="
                                    + mediaScanner.isConnected() + " localFile=" + (destFile is LocalDocumentFile)
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } else notification.cancel()
                }
            }
            val args: Array<String> = if (session.getUri().length > 1) session.uri.substring(1).split("/".toRegex())
                .toTypedArray() else emptyArray()
            return try {
                when (if (args.isNotEmpty()) args[0] else "") {
                    "download", "download-zip" -> serveFileDownload(args, session)
                    "image" -> serveFile(args)
                    "trebleshot" -> serveAPK()
                    "show" -> newFixedLengthResponse(
                        Response.Status.ACCEPTED, "text/html",
                        serveTransferPage(args)
                    )
                    "test" -> newFixedLengthResponse(
                        Response.Status.ACCEPTED, "text/plain",
                        "Works"
                    )
                    "help" -> newFixedLengthResponse(
                        Response.Status.ACCEPTED, "text/html",
                        serveHelpPage()
                    )
                    else -> newFixedLengthResponse(
                        Response.Status.ACCEPTED, "text/html",
                        serveMainPage()
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                newFixedLengthResponse(
                    Response.Status.NOT_ACCEPTABLE, "text/plain",
                    e.toString()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    private fun serveAPK(): Response {
        try {
            val file = File(context.applicationInfo.sourceDir)
            val inputStream = FileInputStream(file)
            return newFixedLengthResponse(
                Response.Status.ACCEPTED, "application/force-download",
                inputStream, file.length()
            )
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return newFixedLengthResponse(
            Response.Status.ACCEPTED, "text/html",
            makePage(
                "arrow-left.svg", R.string.text_downloads,
                makeNotFoundTemplate(
                    R.string.text_empty,
                    R.string.text_webShareNoContentNotice
                )
            )
        )
    }

    private fun serveFile(args: Array<String>): Response {
        try {
            if (args.size < 2) throw Exception("Expected 2 args, " + args.size + " given")
            return newFixedLengthResponse(
                Response.Status.ACCEPTED, getMimeTypeForFile(
                    args[1]
                ),
                openFile(args[0] + File.separator + args[1]), -1
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newFixedLengthResponse(
            Response.Status.NOT_FOUND, "text/plain",
            "Not found"
        )
    }

    private fun serveFileDownload(args: Array<String>, session: IHTTPSession): Response {
        try {
            // TODO: 2/26/21 Reimplement download
            /*if ("download" == args[0]) {
                if (args.size < 3) throw Exception("Expected 3 args, " + args.size + " given")

                val transfer = Transfer(args[1].toLong())
                val item = TransferItem(
                    transfer.id, args[2].toLong(),
                    TransferItem.Type.OUTGOING
                )
                kuick.reconstruct(transfer)
                kuick.reconstruct(item)
                if (!transfer.isServedOnWeb) throw Exception("The group is not checked as served on the Web")
                val streamInfo = StreamInfo.from(context, Uri.parse(item.file))
                val stream: InputStream = streamInfo.openInputStream(context)!!
                run {
                    val positionString: String? = session.getHeaders().get("Accept-Ranges")
                    if (positionString != null) try {
                        val position = positionString.toLong()
                        if (position < streamInfo.size) stream.skip(position)
                    } catch (e: Exception) {
                        // do nothing, formatting issue.
                    }
                }
                return newFixedLengthResponse(
                    Response.Status.ACCEPTED, "application/force-download",
                    stream, streamInfo.size
                )
            } else if ("download-zip" == args[0]) {
                if (args.size < 2) throw Exception("Expected 2 args, " + args.size + " given")
                val transfer = Transfer(args[1].toLong())
                kuick.reconstruct(transfer)
                if (!transfer.isServedOnWeb) throw Exception("The group is not checked as served on the Web")
                val transferList = kuick
                    .castQuery(
                        SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                            .setWhere(
                                Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=? AND "
                                        + Kuick.FIELD_TRANSFERITEM_TYPE + "=?", transfer.id.toString(),
                                TransferItem.Type.OUTGOING.toString()
                            ), TransferItem::class.java
                    )
                if (transferList.isEmpty()) throw Exception("No files to send")
                return ZipBundleResponse(Response.Status.ACCEPTED, "application/force-download", transferList)
            }*/
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return newFixedLengthResponse(
            Response.Status.ACCEPTED, "text/html",
            makePage(
                "arrow-left.svg", R.string.text_downloads,
                makeNotFoundTemplate(
                    R.string.text_empty,
                    R.string.text_webShareNoContentNotice
                )
            )
        )
    }

    private fun serveMainPage(): String {
        val contentBuilder = StringBuilder()
        // TODO: 2/26/21 Serve main page
        /*
        val groupList: List<TransferIndex> = kuick.castQuery<Device, TransferIndex>(
            SQLQuery.Select(Kuick.TABLE_TRANSFER)
                .setOrderBy(Kuick.FIELD_TRANSFER_DATECREATED + " DESC"), TransferIndex::class.java
        )
        for (index in groupList) {
            if (!index.transfer.isServedOnWeb) continue
            loadTransferInfo(context, index)
            if (!index.hasOutgoing()) continue
            contentBuilder.append(
                makeContent(
                    "list_transfer_group", context.getString(
                        R.string.mode_itemCountedDetailed, context.resources.getQuantityString(
                            R.plurals.text_files, index.numberOfOutgoing, index.numberOfOutgoing
                        ),
                        com.genonbeta.android.framework.util.Files.formatLength(index.bytesOutgoing, false)
                    ),
                    R.string.butn_show, "show", index.transfer.id
                )
            )
        }
        if (contentBuilder.isEmpty()) contentBuilder.append(
            makeNotFoundTemplate(
                R.string.text_listEmptyTransfer,
                R.string.text_webShareNoContentNotice
            )
        )*/
        return makePage("icon.png", R.string.text_transfers, contentBuilder.toString())
    }

    private fun serveTransferPage(args: Array<String>): String {
        try {
            if (args.size < 2) throw Exception("Expected 2 args, " + args.size + " given")
            /*
            val transfer = Transfer(args[1].toLong())
            kuick.reconstruct(transfer)
            if (!transfer.isServedOnWeb) throw Exception("The group is not checked as served on the Web")
            val contentBuilder = StringBuilder()
            val groupList = kuick.castQuery(
                SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                    .setWhere(
                        String.format("%s = ?", Kuick.FIELD_TRANSFERITEM_TRANSFERID),
                        transfer.id.toString()
                    )
                    .setOrderBy(Kuick.FIELD_TRANSFERITEM_NAME + " ASC"),
                TransferItem::class.java
            )
            if (groupList.size > 0) {
                contentBuilder.append(
                    makeContent(
                        "list_transfer",
                        context.getString(R.string.butn_downloadAllAsZip),
                        R.string.butn_download,
                        "download-zip",
                        transfer.id,
                        context.resources.getQuantityString(
                            R.plurals.text_files,
                            groupList.size, groupList.size
                        ) + ".zip"
                    )
                )
                for (item in groupList) contentBuilder.append(
                    makeContent(
                        "list_transfer",
                        item.name + " " + com.genonbeta.android.framework.util.Files.formatLength(
                            item.length,
                            false
                        ), R.string.butn_download, "download", item.transferId,
                        item.id, item.name
                    )
                )
            }

             */
            val contentBuilder = StringBuilder()
            return makePage("arrow-left.svg", R.string.text_files, contentBuilder.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return makePage(
            "arrow-left.svg", R.string.text_files,
            makeNotFoundTemplate(
                R.string.text_listEmptyFiles,
                R.string.text_webShareNoContentNotice
            )
        )
    }

    private fun serveHelpPage(): String {
        val values: MutableMap<String, String?> = ArrayMap()
        values["help_title"] = context.getString(R.string.text_help)
        values["licence_text"] = Tools.escapeHtml(context.getString(R.string.conf_licence))
        try {
            val pm = context.packageManager
            val packageInfo: PackageInfo = pm.getPackageInfo(
                context.applicationInfo.packageName,
                0
            )
            val fileName: String = (packageInfo.applicationInfo.loadLabel(pm).toString() + "_"
                    + packageInfo.versionName + ".apk")
            values["apk_link"] = "/trebleshot/$fileName"
            values["apk_filename"] = context.getString(R.string.text_dowloadTrebleshotAndroid)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return makePage(
            "arrow-left.svg", R.string.text_help, applyPattern(
                getFieldPattern(), readPage("help.html"), values
            )
        )
    }

    private fun makeContent(
        pageName: String, content: String, @StringRes buttonRes: Int,
        vararg objects: Any,
    ): String {
        val actionUrlBuilder = StringBuilder()
        val values: MutableMap<String, String?> = ArrayMap()
        values["content"] = content
        values["action_layout"] = context.getString(buttonRes)
        for (item in objects) {
            if (actionUrlBuilder.isNotEmpty()) actionUrlBuilder.append("/")
            actionUrlBuilder.append(item)
        }
        values["actionUrl"] = actionUrlBuilder.toString()
        return applyPattern(getFieldPattern(), readPage("$pageName.html"), values)
    }

    private fun makeNotFoundTemplate(@StringRes msg: Int, @StringRes detail: Int): String {
        val values: MutableMap<String, String?> = ArrayMap()
        values["content"] = context.getString(msg)
        values["detail"] = context.getString(detail)
        return applyPattern(
            getFieldPattern(), readPage("layout_not_found.html"),
            values
        )
    }

    private fun makePage(image: String, @StringRes titleRes: Int, content: String): String {
        val title = context.getString(titleRes)
        val appName = context.getString(R.string.text_appName)
        val values: MutableMap<String, String?> = ArrayMap()
        values["title"] = String.format("%s - %s", title, appName)
        values["header_logo"] = "/image/$image"
        values["header"] = context.getString(R.string.text_appName)
        values["title_header"] = title
        values["main_content"] = content
        values["help_icon"] = "/image/help-circle.svg"
        values["help_alt"] = context.getString(R.string.butn_help)
        values["username"] = persistenceProvider.clientNickname
        values["footer_text"] = context.getString(R.string.text_aboutSummary)
        return applyPattern(getFieldPattern(), readPage("home.html"), values)
    }

    fun hadClients(): Boolean {
        return hadClients
    }

    @Throws(IOException::class)
    private fun openFile(fileName: String): InputStream {
        return assetManager.open("webshare" + File.separator + fileName)
    }

    private fun readPage(pageName: String): String {
        val stream = ByteArrayOutputStream()
        try {
            val inputStream = openFile(pageName)
            var len: Int
            while (inputStream.read().also { len = it } != -1) {
                stream.write(len)
                stream.flush()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return stream.toString()
    }

    class BoundRunner(private val executorService: ExecutorService) : AsyncRunner {
        private val running = Collections.synchronizedList(ArrayList<ClientHandler>())
        override fun closeAll() {
            // copy of the list for concurrency
            for (clientHandler in ArrayList(running)) {
                clientHandler.close()
            }
        }

        override fun closed(clientHandler: ClientHandler) {
            running.remove(clientHandler)
        }

        override fun exec(clientHandler: ClientHandler) {
            executorService.submit(clientHandler)
            running.add(clientHandler)
        }
    }

    /**
     * Most the members of the parent [fi.iki.elonen.NanoHTTPD.Response] class is private, which made it
     * impossible to create concurrent zip streams. For that reason this class copies some of the methods from another
     * class.
     */
    protected inner class ZipBundleResponse(status: IStatus, mimeType: String, files: List<TransferItem>) :
        Response(status, mimeType, object : InputStream() {
            @Throws(IOException::class)
            override fun read(): Int {
                return -1
            }
        }, -1) {
        private inner class ChunkedOutputStream(out: OutputStream?) : FilterOutputStream(out) {
            @Throws(IOException::class)
            override fun write(b: Int) {
                val data = byteArrayOf(
                    b.toByte()
                )
                write(data, 0, 1)
            }

            @Throws(IOException::class)
            override fun write(b: ByteArray) {
                write(b, 0, b.size)
            }

            @Throws(IOException::class)
            override fun write(b: ByteArray, off: Int, len: Int) {
                if (len == 0) return
                out.write(String.format("%x\r\n", len).toByteArray())
                out.write(b, off, len)
                out.write("\r\n".toByteArray())
            }

            @Throws(IOException::class)
            fun finish() {
                out.write("0\r\n\r\n".toByteArray())
            }
        }

        private val mFiles: List<TransferItem>
        private var mStatus: IStatus
        private var mMimeType: String
        private val mData: InputStream? = null
        private val mHeader: MutableMap<String, String> = ArrayMap()
        private var mRequestMethod: Method? = null
        private var mEncodeAsGzip = false
        private var mKeepAlive: Boolean
        override fun addHeader(name: String, value: String) {
            mHeader[name] = value
        }

        override fun getMimeType(): String {
            return mMimeType
        }

        override fun getRequestMethod(): Method {
            return mRequestMethod!!
        }

        override fun getStatus(): IStatus {
            return mStatus
        }

        override fun setGzipEncoding(encodeAsGzip: Boolean) {
            mEncodeAsGzip = encodeAsGzip
        }

        override fun setKeepAlive(useKeepAlive: Boolean) {
            mKeepAlive = useKeepAlive
        }

        override fun setMimeType(mimeType: String) {
            mMimeType = mimeType
        }

        override fun setRequestMethod(requestMethod: Method) {
            mRequestMethod = requestMethod
        }

        override fun setStatus(status: IStatus) {
            mStatus = status
        }

        protected override fun send(outputStream: OutputStream) {
            val mime = mimeType
            val gmtFormat = SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US)
            gmtFormat.timeZone = TimeZone.getTimeZone("GMT")
            try {
                val pw = PrintWriter(BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")), false)
                pw.print(
                    """HTTP/1.1 ${getStatus().getDescription()} 
"""
                )
                pw.print("Content-Type: $mime\r\n")
                if (this.getHeader("Date") == null) pw.print(
                    """
    Date: ${gmtFormat.format(Date())}
    
    """.trimIndent()
                )
                for (key in mHeader.keys) {
                    val value = mHeader[key]
                    pw.print("$key: $value\r\n")
                }
                if (!headerAlreadySent(mHeader, "connection")) pw.print(
                    """
    Connection: ${if (mKeepAlive) "keep-alive" else "close"}
    
    """.trimIndent()
                )
                if (mRequestMethod != Method.HEAD) pw.print("Transfer-Encoding: chunked\r\n")
                pw.print("\r\n")
                pw.flush()
                sendBody(outputStream)
                outputStream.flush()
            } catch (ioe: IOException) {
                Log.d(WebShareServer::class.java.simpleName, "Could not send response to the client", ioe)
            }
        }

        @Throws(IOException::class)
        private fun sendBody(outputStream: OutputStream) {
            val bufferSize = 16 * 1024
            val buffer = ByteArray(bufferSize)
            val chunkedOutputStream = ChunkedOutputStream(outputStream)
            val zipOutputStream = ZipOutputStream(chunkedOutputStream)
            zipOutputStream.setLevel(0)
            //zipOutputStream.setMethod(ZipEntry.STORED);
            // TODO: 2/26/21 Fix zipped download 3
            /*
            for (item in mFiles) {
                try {
                    val streamInfo = StreamInfo.from(context, Uri.parse(item.loc))
                    val inputStream: InputStream = streamInfo.openInputStream(context)!!
                    val thisEntry =
                        ZipEntry((if (item.directory != null) item.directory + File.pathSeparator else "") + item.name)
                    thisEntry.time = item.date
                    zipOutputStream.putNextEntry(thisEntry)
                    var len: Int
                    while (inputStream.read(buffer, 0, bufferSize).also { len = it } != -1) {
                        if (len > 0) {
                            zipOutputStream.write(buffer, 0, len)
                            zipOutputStream.flush()
                        }
                    }
                    zipOutputStream.closeEntry()
                    inputStream.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

             */
            zipOutputStream.finish()
            zipOutputStream.flush()
            chunkedOutputStream.finish()
            zipOutputStream.close()
        }

        init {
            mStatus = status
            mMimeType = mimeType
            mFiles = files
            mKeepAlive = true
        }
    }

    /**
     * A backport for [android.text.Html]
     */
    object Tools {
        fun escapeHtml(text: CharSequence): String {
            val out = StringBuilder()
            withinStyle(out, text, 0, text.length)
            return out.toString()
        }

        private fun withinStyle(out: StringBuilder, text: CharSequence, start: Int, end: Int) {
            var i = start
            while (i < end) {
                val c = text[i]
                if (c == '<') {
                    out.append("&lt;")
                } else if (c == '>') {
                    out.append("&gt;")
                } else if (c == '&') {
                    out.append("&amp;")
                } else if (c.toInt() >= 0xD800 && c.toInt() <= 0xDFFF) {
                    if (c.toInt() < 0xDC00 && i + 1 < end) {
                        val d = text[i + 1]
                        if (d.toInt() >= 0xDC00 && d.toInt() <= 0xDFFF) {
                            i++
                            val codepoint = 0x010000 or (c.toInt() - 0xD800 shl 10) or d.toInt() - 0xDC00
                            out.append("&#").append(codepoint).append(";")
                        }
                    }
                } else if (c.toInt() > 0x7E || c < ' ') {
                    out.append("&#").append(c.toInt()).append(";")
                } else if (c == ' ') {
                    while (i + 1 < end && text[i + 1] == ' ') {
                        out.append("&nbsp;")
                        i++
                    }
                    out.append(' ')
                } else {
                    out.append(c)
                }
                i++
            }
        }
    }

    companion object {
        val TAG = WebShareServer::class.java.simpleName
        private fun applyPattern(pattern: Pattern, template: String, values: Map<String, String?>): String {
            val builder = StringBuilder()
            val matcher = pattern.matcher(template)
            var previousLocation = 0
            while (matcher.find()) {
                builder.append(template, previousLocation, matcher.start())
                builder.append(values[matcher.group(1)])
                previousLocation = matcher.end()
            }
            if (previousLocation > -1 && previousLocation < template.length) builder.append(
                template,
                previousLocation,
                template.length
            )
            return builder.toString()
        }

        private fun getFieldPattern(): Pattern {
            // Android Studio may say the escape characters at the end are redundant. They are not.
            return Pattern.compile("\\$\\{([a-zA-Z_]+)\\}")
        }

        private fun headerAlreadySent(header: Map<String, String>, name: String): Boolean {
            var alreadySent = false
            for (headerName in header.keys) alreadySent = alreadySent or headerName.equals(name, ignoreCase = true)
            return alreadySent
        }
    }**/
}