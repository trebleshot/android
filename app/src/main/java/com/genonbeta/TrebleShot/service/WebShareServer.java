/*
/*
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

package com.genonbeta.TrebleShot.service;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import androidx.annotation.StringRes;
import androidx.collection.ArrayMap;
import androidx.core.app.NotificationCompat;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.FileExplorerActivity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.fragment.FileListFragment;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.util.*;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.LocalDocumentFile;
import com.genonbeta.android.framework.io.StreamInfo;
import com.genonbeta.android.framework.util.Stoppable;
import com.genonbeta.android.framework.util.StoppableImpl;
import fi.iki.elonen.NanoHTTPD;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * created by: veli
 * date: 4/7/19 12:41 AM
 */
public class WebShareServer extends NanoHTTPD
{
    public static final String TAG = WebShareServer.class.getSimpleName();

    private AssetManager mAssetManager;
    private NotificationUtils mNotificationUtils;
    private Context mContext;
    private MediaScannerConnection mMediaScanner;
    private Device mThisDevice;
    private boolean mHadClients = false;

    public WebShareServer(Context context, int port)
    {
        super(port);
        mContext = context;
        mAssetManager = context.getAssets();
        mMediaScanner = new MediaScannerConnection(context, null);
        mNotificationUtils = new NotificationUtils(context, AppUtils.getKuick(context),
                AppUtils.getDefaultPreferences(context));
        mThisDevice = AppUtils.getLocalDevice(mContext);
    }

    @Override
    public void start(int timeout, boolean daemon) throws IOException
    {
        super.start(timeout, daemon);
        mMediaScanner.connect();
    }

    @Override
    public void stop()
    {
        super.stop();
        mMediaScanner.disconnect();
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        mHadClients = true;

        Map<String, String> files = new ArrayMap<>();
        NanoHTTPD.Method method = session.getMethod();
        long receiveTimeElapsed = System.currentTimeMillis();
        long notificationId = AppUtils.getUniqueNumber();
        DynamicNotification notification = null;
        String clientAddress = session.getHeaders().get("http-client-ip");

        Device device = new Device(clientAddress);

        try {
            AppUtils.getKuick(mContext).reconstruct(device);
        } catch (ReconstructionFailedException e) {
            device.brand = "TrebleShot";
            device.model = "Web";
            device.versionCode = mThisDevice.versionCode;
            device.versionName = mThisDevice.versionName;
            device.nickname = clientAddress;
            device.type = Device.Type.WEB;
            device.secureKey = 0; // It is not required for web browsers
        }

        device.lastUsageTime = System.currentTimeMillis();
        AppUtils.getKuick(mContext).publish(device);
        AppUtils.getKuick(mContext).broadcast();

        if (device.isRestricted)
            return newFixedLengthResponse(Response.Status.ACCEPTED, "text/html",
                    makePage("arrow-left.svg", R.string.text_send,
                            makeNotFoundTemplate(R.string.mesg_somethingWentWrong,
                                    R.string.mesg_notAllowed)));

        if (NanoHTTPD.Method.PUT.equals(method) || NanoHTTPD.Method.POST.equals(method)) {
            try {
                notification = mNotificationUtils.buildDynamicNotification(
                        notificationId, NotificationUtils.NOTIFICATION_CHANNEL_LOW);

                notification.setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentInfo(mContext.getString(R.string.text_webShare))
                        .setContentTitle(mContext.getString(R.string.text_receiving))
                        .setContentText(device.nickname);

                notification.show();

                session.parseBody(files);
                receiveTimeElapsed = System.currentTimeMillis() - receiveTimeElapsed;
            } catch (IOException var5) {
                return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,
                        "text/plain", "SERVER INTERNAL ERROR: IOException: "
                                + var5.getMessage());
            } catch (NanoHTTPD.ResponseException var6) {
                return newFixedLengthResponse(var6.getStatus(), "text/plain",
                        var6.getMessage());
            }
        }

        if (notification != null && session.getParms().containsKey("file")) {
            final String fileName = session.getParms().get("file");
            final String filePath = files.get("file");

            if (fileName == null || filePath == null || fileName.length() < 1) {
                notification.cancel();
                return newFixedLengthResponse(Response.Status.ACCEPTED, "text/html",
                        makePage("arrow-left.svg", R.string.text_send,
                                makeNotFoundTemplate(R.string.mesg_somethingWentWrong,
                                        R.string.text_listEmptyFiles)));
            } else {
                File tmpFile = new File(filePath);
                DocumentFile savePath = FileUtils.getApplicationDirectory(mContext);
                Stoppable stoppable = new StoppableImpl();
                DocumentFile sourceFile = DocumentFile.fromFile(tmpFile);
                DocumentFile destFile = savePath.createFile(null,
                        FileUtils.getUniqueFileName(savePath, fileName, true));

                {
                    notification.setSmallIcon(R.drawable.ic_compare_arrows_white_24dp_static)
                            .setContentInfo(mContext.getString(R.string.text_webShare))
                            .setContentTitle(mContext.getString(R.string.text_preparingFiles))
                            .setContentText(fileName);

                    notification.show();

                    try {
                        ContentResolver resolver = mContext.getContentResolver();

                        InputStream inputStream = resolver.openInputStream(sourceFile.getUri());
                        OutputStream outputStream = resolver.openOutputStream(destFile.getUri());

                        if (inputStream == null || outputStream == null)
                            throw new IOException("Failed to open streams to start copying");

                        byte[] buffer = new byte[AppConfig.BUFFER_LENGTH_DEFAULT];
                        int len = 0;
                        long lastRead = System.currentTimeMillis();
                        long lastNotified = 0;
                        long totalRead = 0;

                        while (len != -1) {
                            if ((len = inputStream.read(buffer)) > 0) {
                                outputStream.write(buffer, 0, len);
                                outputStream.flush();
                                lastRead = System.currentTimeMillis();
                                totalRead += len;
                            }

                            if (sourceFile.length() > 0 && totalRead > 0
                                    && System.currentTimeMillis() - lastNotified > AppConfig.DEFAULT_NOTIFICATION_DELAY) {
                                notification.updateProgress(100,
                                        (int) ((totalRead / sourceFile.length()) * 100), false);
                                lastNotified = System.currentTimeMillis();
                            }

                            if ((System.currentTimeMillis() - lastRead) > AppConfig.DEFAULT_SOCKET_TIMEOUT
                                    || stoppable.isInterrupted())
                                throw new Exception("Timed out or interrupted. Exiting!");
                        }

                        outputStream.close();
                        inputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                try {
                    destFile.sync();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (destFile.length() == tmpFile.length() || tmpFile.length() == 0)
                    try {
                        Kuick kuick = AppUtils.getKuick(mContext);

                        TransferGroup webShareGroup = new TransferGroup(AppConfig.ID_GROUP_WEB_SHARE);
                        webShareGroup.dateCreated = System.currentTimeMillis();

                        try {
                            kuick.reconstruct(webShareGroup);
                        } catch (ReconstructionFailedException e) {
                            webShareGroup.savePath = savePath.getUri().toString();
                        }

                        TransferObject transferObject = new TransferObject(AppUtils.getUniqueNumber(),
                                webShareGroup.id, destFile.getName(), destFile.getName(),
                                destFile.getType(), destFile.length(), TransferObject.Type.INCOMING);
                        transferObject.setFlag(TransferObject.Flag.DONE);

                        DeviceConnection connection = new DeviceConnection(
                                Keyword.Local.NETWORK_INTERFACE_UNKNOWN, clientAddress, device.id,
                                System.currentTimeMillis());
                        AppUtils.applyAdapterName(connection);

                        TransferAssignee assignee = new TransferAssignee(webShareGroup,
                                device, TransferObject.Type.INCOMING);
                        assignee.connectionAdapter = connection.adapterName;


                        kuick.publish(webShareGroup);
                        kuick.publish(assignee);
                        kuick.publish(connection);
                        kuick.publish(transferObject);
                        kuick.broadcast();

                        notification = mNotificationUtils.buildDynamicNotification(notificationId,
                                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);
                        notification
                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                .setContentInfo(mContext.getString(R.string.text_webShare))
                                .setAutoCancel(true)
                                .setContentTitle(fileName)
                                .setDefaults(mNotificationUtils.getNotificationSettings())
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setContentText(mContext.getString(R.string.text_receivedTransfer,
                                        FileUtils.sizeExpression(destFile.length(), false),
                                        TimeUtils.getFriendlyElapsedTime(mContext, receiveTimeElapsed)))
                                .addAction(R.drawable.ic_folder_white_24dp_static,
                                        mContext.getString(R.string.butn_showFiles), PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(),
                                                new Intent(mContext, FileExplorerActivity.class)
                                                        .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0));

                        try {
                            Intent openIntent = FileUtils.getOpenIntent(mContext, destFile);
                            notification.setContentIntent(PendingIntent.getActivity(mContext,
                                    AppUtils.getUniqueNumber(), openIntent, 0));
                        } catch (Exception ignored) {
                        }

                        notification.show();

                        mContext.sendBroadcast(new Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
                                .putExtra(FileListFragment.EXTRA_FILE_PARENT, savePath.getUri())
                                .putExtra(FileListFragment.EXTRA_FILE_NAME, destFile.getName()));

                        if (mMediaScanner.isConnected() && destFile instanceof LocalDocumentFile)
                            mMediaScanner.scanFile(((LocalDocumentFile) destFile).getFile().getAbsolutePath(),
                                    destFile.getType());
                        else
                            Log.d(TAG, "Could not save file to the media database: scanner="
                                    + mMediaScanner.isConnected() + " localFile=" + (destFile instanceof LocalDocumentFile));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                else
                    notification.cancel();
            }
        }

        String[] args = session.getUri().length() > 1 ? session.getUri().substring(1).split("/") : new String[0];

        try {
            switch (args.length >= 1 ? args[0] : "") {
                case "download":
                case "download-zip":
                    return serveFileDownload(args, session);
                case "image":
                    return serveFile(args);
                case "trebleshot":
                    return serveAPK();
                case "show":
                    return newFixedLengthResponse(Response.Status.ACCEPTED, "text/html",
                            serveTransferPage(args));
                case "test":
                    return newFixedLengthResponse(Response.Status.ACCEPTED, "text/plain",
                            "Works");
                case "help":
                    return newFixedLengthResponse(Response.Status.ACCEPTED, "text/html",
                            serveHelpPage());
                default:
                    return newFixedLengthResponse(Response.Status.ACCEPTED, "text/html",
                            serveMainPage());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.NOT_ACCEPTABLE, "text/plain",
                    e.toString());
        }
    }


    private Response serveAPK()
    {
        try {
            File file = new File(mContext.getApplicationInfo().sourceDir);
            FileInputStream inputStream = new FileInputStream(file);

            return newFixedLengthResponse(Response.Status.ACCEPTED, "application/force-download",
                    inputStream, file.length());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.ACCEPTED, "text/html",
                makePage("arrow-left.svg", R.string.text_downloads,
                        makeNotFoundTemplate(R.string.text_empty,
                                R.string.text_webShareNoContentNotice)));
    }

    private Response serveFile(String[] args)
    {
        try {
            if (args.length < 2)
                throw new Exception("Expected 2 args, " + args.length + " given");

            return newFixedLengthResponse(Response.Status.ACCEPTED, getMimeTypeForFile(args[1]),
                    openFile(args[0] + File.separator + args[1]), -1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain",
                "Not found");
    }

    private Response serveFileDownload(String[] args, IHTTPSession session)
    {
        try {
            if ("download".equals(args[0])) {
                if (args.length < 3)
                    throw new Exception("Expected 3 args, " + args.length + " given");

                TransferGroup group = new TransferGroup(Long.parseLong(args[1]));
                TransferObject object = new TransferObject(group.id, Long.parseLong(args[2]),
                        TransferObject.Type.OUTGOING);

                AppUtils.getKuick(mContext).reconstruct(group);
                AppUtils.getKuick(mContext).reconstruct(object);

                if (!group.isServedOnWeb)
                    throw new Exception("The group is not checked as served on the Web");

                StreamInfo streamInfo = StreamInfo.getStreamInfo(mContext, Uri.parse(
                        object.file));

                InputStream stream = streamInfo.openInputStream();

                {
                    String positionString = session.getHeaders().get("Accept-Ranges");

                    if (positionString != null)
                        try {
                            long position = Long.parseLong(positionString);

                            if (position < streamInfo.size)
                                stream.skip(position);
                        } catch (Exception e) {
                            // do nothing, formatting issue.
                        }
                }

                return newFixedLengthResponse(Response.Status.ACCEPTED, "application/force-download",
                        stream, streamInfo.size);
            } else if ("download-zip".equals(args[0])) {
                if (args.length < 2)
                    throw new Exception("Expected 2 args, " + args.length + " given");

                TransferGroup group = new TransferGroup(Long.parseLong(args[1]));
                AppUtils.getKuick(mContext).reconstruct(group);

                if (!group.isServedOnWeb)
                    throw new Exception("The group is not checked as served on the Web");

                List<TransferObject> transferList = AppUtils.getKuick(mContext)
                        .castQuery(new SQLQuery.Select(Kuick.TABLE_TRANSFER)
                                .setWhere(Kuick.FIELD_TRANSFER_GROUPID + "=? AND "
                                                + Kuick.FIELD_TRANSFER_TYPE + "=?",
                                        String.valueOf(group.id),
                                        TransferObject.Type.OUTGOING.toString()), TransferObject.class);

                if (transferList.size() < 1)
                    throw new Exception("No files to send");

                return new ZipBundleResponse(Response.Status.ACCEPTED, "application/force-download",
                        transferList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.ACCEPTED, "text/html",
                makePage("arrow-left.svg", R.string.text_downloads,
                        makeNotFoundTemplate(R.string.text_empty,
                                R.string.text_webShareNoContentNotice)));
    }

    private String serveMainPage()
    {
        StringBuilder contentBuilder = new StringBuilder();

        List<IndexOfTransferGroup> groupList = AppUtils.getKuick(mContext).castQuery(
                new SQLQuery.Select(Kuick.TABLE_TRANSFERGROUP)
                        .setOrderBy(Kuick.FIELD_TRANSFERGROUP_DATECREATED + " DESC"), IndexOfTransferGroup.class);

        for (IndexOfTransferGroup index : groupList) {
            if (!index.group.isServedOnWeb)
                continue;

            TransferUtils.loadGroupInfo(mContext, index);

            if (!index.hasOutgoing())
                continue;

            contentBuilder.append(makeContent("list_transfer_group", mContext.getString(
                    R.string.mode_itemCountedDetailed, mContext.getResources().getQuantityString(
                            R.plurals.text_files, index.numberOfOutgoing, index.numberOfOutgoing),
                    FileUtils.sizeExpression(index.bytesOutgoing, false)),
                    R.string.butn_show, "show", index.group.id));
        }

        if (contentBuilder.length() == 0)
            contentBuilder.append(makeNotFoundTemplate(R.string.text_listEmptyTransfer,
                    R.string.text_webShareNoContentNotice));

        return makePage("icon.png", R.string.text_transfers, contentBuilder.toString());
    }

    private String serveTransferPage(String[] args)
    {
        try {
            if (args.length < 2)
                throw new Exception("Expected 2 args, " + args.length + " given");

            TransferGroup group = new TransferGroup(Long.parseLong(args[1]));
            AppUtils.getKuick(mContext).reconstruct(group);

            if (!group.isServedOnWeb)
                throw new Exception("The group is not checked as served on the Web");

            StringBuilder contentBuilder = new StringBuilder();
            List<TransferObject> groupList = AppUtils.getKuick(mContext).castQuery(
                    new SQLQuery.Select(Kuick.TABLE_TRANSFER)
                            .setWhere(String.format("%s = ?", Kuick.FIELD_TRANSFER_GROUPID),
                                    String.valueOf(group.id))
                            .setOrderBy(Kuick.FIELD_TRANSFER_NAME + " ASC"),
                    TransferObject.class);

            if (groupList.size() > 0) {
                contentBuilder.append(makeContent("list_transfer",
                        mContext.getString(R.string.butn_downloadAllAsZip), R.string.butn_download, "download-zip", group.id,
                        mContext.getResources().getQuantityString(R.plurals.text_files,
                                groupList.size(), groupList.size()) + ".zip"));

                for (TransferObject object : groupList)
                    contentBuilder.append(makeContent("list_transfer",
                            object.name + " " + FileUtils.sizeExpression(object.size,
                                    false), R.string.butn_download, "download", object.groupId,
                            object.id, object.name));
            }

            return makePage("arrow-left.svg", R.string.text_files, contentBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return makePage("arrow-left.svg", R.string.text_files,
                makeNotFoundTemplate(R.string.text_listEmptyFiles,
                        R.string.text_webShareNoContentNotice));
    }

    private String serveHelpPage()
    {
        Map<String, String> values = new ArrayMap<>();
        values.put("help_title", mContext.getString(R.string.text_help));
        values.put("licence_text", Tools.escapeHtml(mContext.getString(R.string.conf_licence)));

        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo packageInfo = pm.getPackageInfo(mContext.getApplicationInfo().packageName,
                    0);
            String fileName = packageInfo.applicationInfo.loadLabel(pm) + "_"
                    + packageInfo.versionName + ".apk";

            values.put("apk_link", "/trebleshot/" + fileName);
            values.put("apk_filename", mContext.getString(R.string.text_dowloadTrebleshotAndroid));
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return makePage("arrow-left.svg", R.string.text_help, applyPattern(
                getFieldPattern(), readPage("help.html"), values));
    }

    private String makeContent(String pageName, String content, @StringRes int buttonRes,
                               Object... objects)
    {
        StringBuilder actionUrlBuilder = new StringBuilder();
        Map<String, String> values = new ArrayMap<>();
        values.put("content", content);
        values.put("action_layout", mContext.getString(buttonRes));

        for (Object object : objects) {
            if (actionUrlBuilder.length() > 0)
                actionUrlBuilder.append("/");

            actionUrlBuilder.append(object);
        }

        values.put("actionUrl", actionUrlBuilder.toString());

        return applyPattern(getFieldPattern(), readPage(pageName + ".html"), values);
    }

    private String makeNotFoundTemplate(@StringRes int msg, @StringRes int detail)
    {
        Map<String, String> values = new ArrayMap<>();
        values.put("content", mContext.getString(msg));
        values.put("detail", mContext.getString(detail));

        return applyPattern(getFieldPattern(), readPage("layout_not_found.html"),
                values);
    }

    private String makePage(String image, @StringRes int titleRes, String content)
    {
        String title = mContext.getString(titleRes);
        String appName = mContext.getString(R.string.text_appName);

        Map<String, String> values = new ArrayMap<>();
        values.put("title", String.format("%s - %s", title, appName));
        values.put("header_logo", "/image/" + image);
        values.put("header", mContext.getString(R.string.text_appName));
        values.put("title_header", title);
        values.put("main_content", content);
        values.put("help_icon", "/image/help-circle.svg");
        values.put("help_alt", mContext.getString(R.string.butn_help));
        values.put("username", AppUtils.getLocalDeviceName(mContext));
        values.put("footer_text", mContext.getString(R.string.text_aboutSummary));

        return applyPattern(getFieldPattern(), readPage("home.html"), values);
    }

    private static String applyPattern(Pattern pattern, String template, Map<String, String> values)
    {
        StringBuilder builder = new StringBuilder();
        Matcher matcher = pattern.matcher(template);
        int previousLocation = 0;

        while (matcher.find()) {
            builder.append(template, previousLocation, matcher.start());
            builder.append(values.get(matcher.group(1)));

            previousLocation = matcher.end();
        }

        if (previousLocation > -1 && previousLocation < template.length())
            builder.append(template, previousLocation, template.length());

        return builder.toString();
    }

    private static Pattern getFieldPattern()
    {
        // Android Studio may say the escape characters at the end are redundant.
        // They are not in Java 1.7.
        return Pattern.compile("\\$\\{([a-zA-Z_]+)\\}");
    }

    public boolean hadClients()
    {
        return mHadClients;
    }

    private InputStream openFile(String fileName) throws IOException
    {
        return mAssetManager.open("webshare" + File.separator + fileName);
    }

    private String readPage(String pageName)
    {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();

        try {
            InputStream inputStream = openFile(pageName);
            int len;

            while ((len = inputStream.read()) != -1) {
                stream.write(len);
                stream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return stream.toString();
    }

    public static class BoundRunner implements NanoHTTPD.AsyncRunner
    {
        private ExecutorService executorService;
        private final List<ClientHandler> running = Collections.synchronizedList(new ArrayList<>());

        public BoundRunner(ExecutorService executorService)
        {
            this.executorService = executorService;
        }

        @Override
        public void closeAll()
        {
            // copy of the list for concurrency
            for (NanoHTTPD.ClientHandler clientHandler : new ArrayList<>(this.running)) {
                clientHandler.close();
            }
        }

        @Override
        public void closed(NanoHTTPD.ClientHandler clientHandler)
        {
            this.running.remove(clientHandler);
        }

        @Override
        public void exec(NanoHTTPD.ClientHandler clientHandler)
        {
            executorService.submit(clientHandler);
            this.running.add(clientHandler);
        }
    }

    /**
     * Most of the members of the parent {@link fi.iki.elonen.NanoHTTPD.Response}
     * class had private access, which made impossible to create concurrent zip streams.
     * The biggest problem is that {@link fi.iki.elonen.NanoHTTPD.Response} is not an interface, but
     * a class. To overcome these issues, I created a wrapper that imitates the similar behaviour.
     */
    protected class ZipBundleResponse extends NanoHTTPD.Response
    {
        private class ChunkedOutputStream extends FilterOutputStream
        {

            public ChunkedOutputStream(OutputStream out)
            {
                super(out);
            }

            @Override
            public void write(int b) throws IOException
            {
                byte[] data = {
                        (byte) b
                };
                write(data, 0, 1);
            }

            @Override
            public void write(byte[] b) throws IOException
            {
                write(b, 0, b.length);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException
            {
                if (len == 0)
                    return;
                out.write(String.format("%x\r\n", len).getBytes());
                out.write(b, off, len);
                out.write("\r\n".getBytes());
            }

            public void finish() throws IOException
            {
                out.write("0\r\n\r\n".getBytes());
            }

        }

        private List<TransferObject> mFiles;

        private IStatus mStatus;

        private String mMimeType;

        private InputStream mData;

        private final Map<String, String> mHeader = new ArrayMap<>();

        private Method mRequestMethod;

        private boolean mEncodeAsGzip;

        private boolean mKeepAlive;

        protected ZipBundleResponse(IStatus status, String mimeType, List<TransferObject> files)
        {
            super(status, mimeType, new InputStream()
            {
                @Override
                public int read() throws IOException
                {
                    return -1;
                }
            }, -1);

            mStatus = status;
            mMimeType = mimeType;
            mFiles = files;
            mKeepAlive = true;
        }

        public void addHeader(String name, String value)
        {
            mHeader.put(name, value);
        }

        @Override
        public String getMimeType()
        {
            return mMimeType;
        }

        @Override
        public Method getRequestMethod()
        {
            return mRequestMethod;
        }

        @Override
        public IStatus getStatus()
        {
            return mStatus;
        }

        @Override
        public void setGzipEncoding(boolean encodeAsGzip)
        {
            mEncodeAsGzip = encodeAsGzip;
        }

        @Override
        public void setKeepAlive(boolean useKeepAlive)
        {
            mKeepAlive = useKeepAlive;
        }

        @Override
        public void setMimeType(String mimeType)
        {
            mMimeType = mimeType;
        }

        @Override
        public void setRequestMethod(Method requestMethod)
        {
            mRequestMethod = requestMethod;
        }

        @Override
        public void setStatus(IStatus status)
        {
            mStatus = status;
        }

        @Override
        protected void send(OutputStream outputStream)
        {
            String mime = this.getMimeType();
            SimpleDateFormat gmtFormat = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

            try {
                if (this.getStatus() == null)
                    throw new Error("sendResponse(): Status can't be null.");

                PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")), false);
                pw.print("HTTP/1.1 " + this.getStatus().getDescription() + " \r\n");

                if (mime != null)
                    pw.print("Content-Type: " + mime + "\r\n");

                if (this.getHeader("Date") == null)
                    pw.print("Date: " + gmtFormat.format(new Date()) + "\r\n");

                for (String key : mHeader.keySet()) {
                    String value = mHeader.get(key);
                    pw.print(key + ": " + value + "\r\n");
                }

                if (!headerAlreadySent(mHeader, "connection"))
                    pw.print("Connection: " + (mKeepAlive ? "keep-alive" : "close") + "\r\n");

                if (mRequestMethod != Method.HEAD)
                    pw.print("Transfer-Encoding: chunked\r\n");

                pw.print("\r\n");
                pw.flush();
                sendBody(outputStream);
                outputStream.flush();
            } catch (IOException ioe) {
                Log.d(WebShareServer.class.getSimpleName(), "Could not send response to the client", ioe);
            }
        }

        private void sendBody(OutputStream outputStream) throws IOException
        {
            int bufferSize = 16 * 1024;
            byte[] buffer = new byte[bufferSize];

            ChunkedOutputStream chunkedOutputStream = new ChunkedOutputStream(outputStream);
            ZipOutputStream zipOutputStream = new ZipOutputStream(chunkedOutputStream);
            zipOutputStream.setLevel(0);
            //zipOutputStream.setMethod(ZipEntry.STORED);

            for (TransferObject object : mFiles) {
                try {
                    StreamInfo streamInfo = StreamInfo.getStreamInfo(mContext, Uri.parse(object.file));
                    InputStream inputStream = streamInfo.openInputStream();

                    ZipEntry thisEntry = new ZipEntry((object.directory != null
                            ? object.directory + File.pathSeparator : "") + object.name);

                    thisEntry.setTime(object.getComparableDate());

                    zipOutputStream.putNextEntry(thisEntry);

                    int len;
                    while ((len = inputStream.read(buffer, 0, bufferSize)) != -1) {
                        if (len > 0) {
                            zipOutputStream.write(buffer, 0, len);
                            zipOutputStream.flush();
                        }
                    }

                    zipOutputStream.closeEntry();
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            zipOutputStream.finish();
            zipOutputStream.flush();
            chunkedOutputStream.finish();
            zipOutputStream.close();
        }
    }

    private static boolean headerAlreadySent(Map<String, String> header, String name)
    {
        boolean alreadySent = false;
        for (String headerName : header.keySet())
            alreadySent |= headerName.equalsIgnoreCase(name);
        return alreadySent;
    }

    /**
     * A backport for {@link Html}
     */
    public static class Tools
    {
        public static String escapeHtml(CharSequence text)
        {
            StringBuilder out = new StringBuilder();
            withinStyle(out, text, 0, text.length());
            return out.toString();
        }

        private static void withinStyle(StringBuilder out, CharSequence text,
                                        int start, int end)
        {
            for (int i = start; i < end; i++) {
                char c = text.charAt(i);

                if (c == '<') {
                    out.append("&lt;");
                } else if (c == '>') {
                    out.append("&gt;");
                } else if (c == '&') {
                    out.append("&amp;");
                } else if (c >= 0xD800 && c <= 0xDFFF) {
                    if (c < 0xDC00 && i + 1 < end) {
                        char d = text.charAt(i + 1);
                        if (d >= 0xDC00 && d <= 0xDFFF) {
                            i++;
                            int codepoint = 0x010000 | (int) c - 0xD800 << 10 | (int) d - 0xDC00;
                            out.append("&#").append(codepoint).append(";");
                        }
                    }
                } else if (c > 0x7E || c < ' ') {
                    out.append("&#").append((int) c).append(";");
                } else if (c == ' ') {
                    while (i + 1 < end && text.charAt(i + 1) == ' ') {
                        out.append("&nbsp;");
                        i++;
                    }

                    out.append(' ');
                } else {
                    out.append(c);
                }
            }
        }
    }
}