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
package com.genonbeta.TrebleShot.service

import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.TrebleShot.util.*
import com.genonbeta.android.framework.io.DocumentFile
import fi.iki.elonen.NanoHTTPD
import java.io.*
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * created by: veli
 * date: 4/7/19 12:41 AM
 */
class WebShareServer(private val mContext: Context, port: Int) : NanoHTTPD(port) {
    private val mAssetManager: AssetManager
    private val mNotificationUtils: NotificationUtils
    private val mMediaScanner: MediaScannerConnection
    private val mThisDevice: Device?
    private var mHadClients = false
    override fun stop() {
        super.stop()
        mMediaScanner.disconnect()
    }

    override fun serve(session: IHTTPSession): Response {
        mHadClients = true
        val files: Map<String, String> = ArrayMap()
        val method: Method = session.getMethod()
        var receiveTimeElapsed = System.currentTimeMillis()
        val notificationId = AppUtils.getUniqueNumber().toLong()
        var notification: DynamicNotification? = null
        val clientAddress: String = session.getHeaders().get("http-client-ip")
        val device = Device(clientAddress)
        try {
            AppUtils.getKuick(mContext).reconstruct(device)
        } catch (e: ReconstructionFailedException) {
            device.brand = "TrebleShot"
            device.model = "Web"
            device.versionCode = mThisDevice!!.versionCode
            device.versionName = mThisDevice.versionName
            device.username = clientAddress
            device.type = Device.Type.Web
        }
        device.lastUsageTime = System.currentTimeMillis()
        AppUtils.getKuick(mContext).publish(device)
        AppUtils.getKuick(mContext).broadcast()
        if (device.isBlocked) return newFixedLengthResponse(
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
                notification = mNotificationUtils.buildDynamicNotification(
                    notificationId, NotificationUtils.NOTIFICATION_CHANNEL_LOW
                )
                notification.setSmallIcon(android.R.drawable.stat_sys_download)
                    .setContentInfo(mContext.getString(R.string.text_webShare))
                    .setContentTitle(mContext.getString(R.string.text_receiving))
                    .setContentText(device.username)
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
        if (notification != null && session.getParms().containsKey("file")) {
            val fileName: String = session.getParms().get("file")
            val filePath = files["file"]
            if (fileName == null || filePath == null || fileName.length < 1) {
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
                val savePath = Files.getApplicationDirectory(mContext)
                val stoppable: Stoppable = StoppableImpl()
                val sourceFile = DocumentFile.fromFile(tmpFile)
                val destFile = savePath!!.createFile(
                    null,
                    com.genonbeta.android.framework.util.Files.getUniqueFileName(savePath, fileName, true)
                )
                run {
                    notification.setSmallIcon(R.drawable.ic_compare_arrows_white_24dp_static)
                        .setContentInfo(mContext.getString(R.string.text_webShare))
                        .setContentTitle(mContext.getString(R.string.text_preparingFiles))
                        .setContentText(fileName)
                    notification.show()
                    try {
                        val resolver: ContentResolver = mContext.contentResolver
                        val inputStream: InputStream = resolver.openInputStream(sourceFile.uri)
                        val outputStream: OutputStream = resolver.openOutputStream(destFile.uri)
                        if (inputStream == null || outputStream == null) throw IOException("Failed to open streams to start copying")
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
                                || stoppable.isInterrupted()
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
                    val kuick = AppUtils.getKuick(mContext)
                    val webTransfer: Transfer = Transfer(AppConfig.ID_GROUP_WEB_SHARE)
                    webTransfer.dateCreated = System.currentTimeMillis()
                    try {
                        kuick.reconstruct(webTransfer)
                    } catch (e: ReconstructionFailedException) {
                        webTransfer.savePath = savePath.uri.toString()
                    }
                    val transferItem = TransferItem(
                        AppUtils.getUniqueNumber(), webTransfer.id,
                        destFile.name, destFile.name, destFile.type, destFile.getLength(),
                        TransferItem.Type.INCOMING
                    )
                    transferItem.flag = TransferItem.Flag.DONE
                    val address = DeviceAddress(
                        device.uid, InetAddress.getByName(clientAddress),
                        System.currentTimeMillis()
                    )
                    val member = TransferMember(webTransfer, device, TransferItem.Type.INCOMING)
                    kuick.publish(webTransfer)
                    kuick.publish<Transfer, TransferMember>(member)
                    kuick.publish<Device, DeviceAddress>(address)
                    kuick.publish(transferItem)
                    kuick.broadcast()
                    notification = mNotificationUtils.buildDynamicNotification(
                        notificationId,
                        NotificationUtils.NOTIFICATION_CHANNEL_HIGH
                    )
                    notification
                        .setSmallIcon(android.R.drawable.stat_sys_download_done)
                        .setContentInfo(mContext.getString(R.string.text_webShare))
                        .setAutoCancel(true)
                        .setContentTitle(fileName)
                        .setDefaults(mNotificationUtils.notificationSettings)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setContentText(
                            mContext.getString(
                                R.string.text_receivedTransfer,
                                com.genonbeta.android.framework.util.Files.sizeExpression(destFile.getLength(), false),
                                TimeUtils.getFriendlyElapsedTime(mContext, receiveTimeElapsed)
                            )
                        )
                        .addAction(
                            R.drawable.ic_folder_white_24dp_static,
                            mContext.getString(R.string.butn_showFiles), PendingIntent.getActivity(
                                mContext, AppUtils.getUniqueNumber(),
                                Intent(mContext, FileExplorerActivity::class.java)
                                    .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.uri), 0
                            )
                        )
                    try {
                        val openIntent =
                            com.genonbeta.android.framework.util.Files.getOpenIntent(mContext, destFile)
                        notification.setContentIntent(
                            PendingIntent.getActivity(
                                mContext,
                                AppUtils.getUniqueNumber(), openIntent, 0
                            )
                        )
                    } catch (ignored: Exception) {
                    }
                    notification.show()
                    mContext.sendBroadcast(
                        Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
                            .putExtra(FileListFragment.EXTRA_FILE_PARENT, savePath.uri)
                            .putExtra(FileListFragment.EXTRA_FILE_NAME, destFile.name)
                    )
                    if (mMediaScanner.isConnected() && destFile is LocalDocumentFile) mMediaScanner.scanFile(
                        (destFile as LocalDocumentFile).getFile().getAbsolutePath(),
                        destFile.type
                    ) else Log.d(
                        TAG, "Could not save file to the media database: scanner="
                                + mMediaScanner.isConnected() + " localFile=" + (destFile is LocalDocumentFile)
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                } else notification.cancel()
            }
        }
        val args: Array<String> = if (session.getUri().length > 1) session.getUri().substring(1).split("/".toRegex())
            .toTypedArray() else arrayOfNulls(0)
        return try {
            when (if (args.size >= 1) args[0] else "") {
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
    }

    private fun serveAPK(): Response {
        try {
            val file = File(mContext.applicationInfo.sourceDir)
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
            if ("download" == args[0]) {
                if (args.size < 3) throw Exception("Expected 3 args, " + args.size + " given")
                val transfer = Transfer(args[1].toLong())
                val item = TransferItem(
                    transfer.id, args[2].toLong(),
                    TransferItem.Type.OUTGOING
                )
                AppUtils.getKuick(mContext).reconstruct(transfer)
                AppUtils.getKuick(mContext).reconstruct(item)
                if (!transfer.isServedOnWeb) throw Exception("The group is not checked as served on the Web")
                val streamInfo: StreamInfo = StreamInfo.getStreamInfo(
                    mContext, Uri.parse(
                        item.file
                    )
                )
                val stream: InputStream = streamInfo.openInputStream()
                run {
                    val positionString: String = session.getHeaders().get("Accept-Ranges")
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
                AppUtils.getKuick(mContext).reconstruct(transfer)
                if (!transfer.isServedOnWeb) throw Exception("The group is not checked as served on the Web")
                val transferList = AppUtils.getKuick(mContext)
                    .castQuery(
                        SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                            .setWhere(
                                Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=? AND "
                                        + Kuick.FIELD_TRANSFERITEM_TYPE + "=?", transfer.id.toString(),
                                TransferItem.Type.OUTGOING.toString()
                            ), TransferItem::class.java
                    )
                if (transferList.size < 1) throw Exception("No files to send")
                return ZipBundleResponse(
                    Response.Status.ACCEPTED, "application/force-download",
                    transferList
                )
            }
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
        val groupList: List<TransferIndex> = AppUtils.getKuick(mContext).castQuery<Device, TransferIndex>(
            SQLQuery.Select(Kuick.TABLE_TRANSFER)
                .setOrderBy(Kuick.FIELD_TRANSFER_DATECREATED + " DESC"), TransferIndex::class.java
        )
        for (index in groupList) {
            if (!index.transfer.isServedOnWeb) continue
            loadTransferInfo(mContext, index)
            if (!index.hasOutgoing()) continue
            contentBuilder.append(
                makeContent(
                    "list_transfer_group", mContext.getString(
                        R.string.mode_itemCountedDetailed, mContext.resources.getQuantityString(
                            R.plurals.text_files, index.numberOfOutgoing, index.numberOfOutgoing
                        ),
                        com.genonbeta.android.framework.util.Files.sizeExpression(index.bytesOutgoing, false)
                    ),
                    R.string.butn_show, "show", index.transfer.id
                )
            )
        }
        if (contentBuilder.length == 0) contentBuilder.append(
            makeNotFoundTemplate(
                R.string.text_listEmptyTransfer,
                R.string.text_webShareNoContentNotice
            )
        )
        return makePage("icon.png", R.string.text_transfers, contentBuilder.toString())
    }

    private fun serveTransferPage(args: Array<String>): String {
        try {
            if (args.size < 2) throw Exception("Expected 2 args, " + args.size + " given")
            val transfer = Transfer(args[1].toLong())
            AppUtils.getKuick(mContext).reconstruct(transfer)
            if (!transfer.isServedOnWeb) throw Exception("The group is not checked as served on the Web")
            val contentBuilder = StringBuilder()
            val groupList = AppUtils.getKuick(mContext).castQuery(
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
                        mContext.getString(R.string.butn_downloadAllAsZip),
                        R.string.butn_download,
                        "download-zip",
                        transfer.id,
                        mContext.resources.getQuantityString(
                            R.plurals.text_files,
                            groupList.size, groupList.size
                        ) + ".zip"
                    )
                )
                for (item in groupList) contentBuilder.append(
                    makeContent(
                        "list_transfer",
                        item.name + " " + com.genonbeta.android.framework.util.Files.sizeExpression(
                            item.comparableSize,
                            false
                        ), R.string.butn_download, "download", item.transferId,
                        item.id, item.name!!
                    )
                )
            }
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
        values["help_title"] = mContext.getString(R.string.text_help)
        values["licence_text"] = Tools.escapeHtml(mContext.getString(R.string.conf_licence))
        try {
            val pm = mContext.packageManager
            val packageInfo: PackageInfo = pm.getPackageInfo(
                mContext.applicationInfo.packageName,
                0
            )
            val fileName: String = (packageInfo.applicationInfo.loadLabel(pm).toString() + "_"
                    + packageInfo.versionName + ".apk")
            values["apk_link"] = "/trebleshot/$fileName"
            values["apk_filename"] = mContext.getString(R.string.text_dowloadTrebleshotAndroid)
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
        vararg objects: Any
    ): String {
        val actionUrlBuilder = StringBuilder()
        val values: MutableMap<String, String?> = ArrayMap()
        values["content"] = content
        values["action_layout"] = mContext.getString(buttonRes)
        for (item in objects) {
            if (actionUrlBuilder.length > 0) actionUrlBuilder.append("/")
            actionUrlBuilder.append(item)
        }
        values["actionUrl"] = actionUrlBuilder.toString()
        return applyPattern(getFieldPattern(), readPage("$pageName.html"), values)
    }

    private fun makeNotFoundTemplate(@StringRes msg: Int, @StringRes detail: Int): String {
        val values: MutableMap<String, String?> = ArrayMap()
        values["content"] = mContext.getString(msg)
        values["detail"] = mContext.getString(detail)
        return applyPattern(
            getFieldPattern(), readPage("layout_not_found.html"),
            values
        )
    }

    private fun makePage(image: String, @StringRes titleRes: Int, content: String): String {
        val title = mContext.getString(titleRes)
        val appName = mContext.getString(R.string.text_appName)
        val values: MutableMap<String, String?> = ArrayMap()
        values["title"] = String.format("%s - %s", title, appName)
        values["header_logo"] = "/image/$image"
        values["header"] = mContext.getString(R.string.text_appName)
        values["title_header"] = title
        values["main_content"] = content
        values["help_icon"] = "/image/help-circle.svg"
        values["help_alt"] = mContext.getString(R.string.butn_help)
        values["username"] = AppUtils.getLocalDeviceName(mContext)
        values["footer_text"] = mContext.getString(R.string.text_aboutSummary)
        return applyPattern(getFieldPattern(), readPage("home.html"), values)
    }

    fun hadClients(): Boolean {
        return mHadClients
    }

    @Throws(IOException::class)
    private fun openFile(fileName: String): InputStream {
        return mAssetManager.open("webshare" + File.separator + fileName)
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
            return mRequestMethod
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
            val mime = getMimeType()
            val gmtFormat = SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US)
            gmtFormat.timeZone = TimeZone.getTimeZone("GMT")
            try {
                if (getStatus() == null) throw Error("sendResponse(): Status can't be null.")
                val pw = PrintWriter(BufferedWriter(OutputStreamWriter(outputStream, "UTF-8")), false)
                pw.print(
                    """HTTP/1.1 ${getStatus().getDescription()} 
"""
                )
                if (mime != null) pw.print("Content-Type: $mime\r\n")
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
            val chunkedOutputStream: ChunkedOutputStream = ChunkedOutputStream(outputStream)
            val zipOutputStream = ZipOutputStream(chunkedOutputStream)
            zipOutputStream.setLevel(0)
            //zipOutputStream.setMethod(ZipEntry.STORED);
            for (item in mFiles) {
                try {
                    val streamInfo: StreamInfo = StreamInfo.getStreamInfo(mContext, Uri.parse(item.file))
                    val inputStream: InputStream = streamInfo.openInputStream()
                    val thisEntry =
                        ZipEntry((if (item.directory != null) item.directory + File.pathSeparator else "") + item.name)
                    thisEntry.time = item.comparableDate
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
     * A backport for [Html]
     */
    object Tools {
        fun escapeHtml(text: CharSequence): String {
            val out = StringBuilder()
            withinStyle(out, text, 0, text.length)
            return out.toString()
        }

        private fun withinStyle(
            out: StringBuilder, text: CharSequence,
            start: Int, end: Int
        ) {
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
    }

    init {
        mAssetManager = mContext.assets
        mMediaScanner = MediaScannerConnection(mContext, null)
        mNotificationUtils = NotificationUtils(
            mContext, AppUtils.getKuick(mContext),
            AppUtils.getDefaultPreferences(mContext)
        )
        mThisDevice = AppUtils.getLocalDevice(mContext)
    }
}