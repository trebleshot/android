package com.genonbeta.TrebleShot.service;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;

import androidx.annotation.StringRes;
import androidx.collection.ArrayMap;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.StreamInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.iki.elonen.NanoHTTPD;

/**
 * created by: veli
 * date: 4/7/19 12:41 AM
 */
public class WebShareServer extends NanoHTTPD
{
    private AssetManager mAssetManager;
    private Context mContext;

    public WebShareServer(Context context, int port) throws IOException
    {
        super(port);
        mContext = context;
        mAssetManager = context.getAssets();
    }

    @Override
    public Response serve(IHTTPSession session)
    {
        Map<String, String> files = new HashMap<>();
        NanoHTTPD.Method method = session.getMethod();

        if (NanoHTTPD.Method.PUT.equals(method) || NanoHTTPD.Method.POST.equals(method)) {
            try {
                session.parseBody(files);
            } catch (IOException var5) {
                return newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR,
                        "text/plain", "SERVER INTERNAL ERROR: IOException: "
                                + var5.getMessage());
            } catch (NanoHTTPD.ResponseException var6) {
                return newFixedLengthResponse(var6.getStatus(), "text/plain",
                        var6.getMessage());
            }
        }

        if ("/kill".equals(session.getUri()))
            stop();

        String[] args = new String[]{};

        if (session.getUri().length() > 1) {
            args = session.getUri().substring(1).split("/");
        }

        try {
            switch (args.length >= 1 ? args[0] : "") {
                case "download":
                    return serveFile(args);
                case "image":
                    return serveImage(args);
                case "show":
                    return newFixedLengthResponse(Response.Status.ACCEPTED, "text/html",
                            serveTransferPage(args));
                case "test":
                    return newFixedLengthResponse(Response.Status.ACCEPTED, "text/plain",
                            "Works");
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

    private Response serveImage(String[] args)
    {
        try {
            if (args.length < 2)
                throw new Exception("Expected 2 args, " + args.length + " given");

            return newFixedLengthResponse(Response.Status.ACCEPTED, getMimeTypeForFile(args[1]),
                    openFile("image" + File.separator + args[1]), -1);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain",
                "Not found");
    }

    private Response serveFile(String[] args)
    {
        try {
            if (args.length < 3)
                throw new Exception("Expected 3 args, " + args.length + " given");

            TransferGroup group = new TransferGroup(Long.parseLong(args[1]));
            TransferObject object = new TransferObject(Long.parseLong(args[2]), null,
                    TransferObject.Type.OUTGOING);

            AppUtils.getDatabase(mContext).reconstruct(group);
            AppUtils.getDatabase(mContext).reconstruct(object);

            StreamInfo streamInfo = StreamInfo.getStreamInfo(mContext, Uri.parse(
                    object.file));

            return newFixedLengthResponse(Response.Status.ACCEPTED, "application/force-download",
                    streamInfo.openInputStream(), streamInfo.size);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain",
                "Not found");
    }

    private String serveMainPage()
    {
        StringBuilder contentBuilder = new StringBuilder();
        Map<Long, TransferObject> calcMap = new ArrayMap<>();

        List<TransferGroup> groupList = AppUtils.getDatabase(mContext).castQuery(
                new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP)
                        .setOrderBy(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED + " DESC"),
                TransferGroup.class);

        for (TransferGroup group : groupList) {
            TransferGroup.Index index = new TransferGroup.Index();
            AppUtils.getDatabase(mContext).calculateTransactionSize(group.groupId, index);

            if (index.outgoingCount > 0)
                contentBuilder.append(makeContent(mContext.getString(R.string
                                .mode_itemCountedDetailed, mContext.getResources()
                                .getQuantityString(R.plurals.text_files, index.outgoingCount,
                                        index.outgoingCount), FileUtils.sizeExpression(
                                                index.outgoing, false)),
                        R.string.butn_show, "show", group.groupId));
        }

        return makePage(R.string.text_appName, contentBuilder.toString());
    }

    private String serveTransferPage(String[] args)
    {
        try {
            if (args.length < 2)
                throw new Exception("Expected 2 args, " + args.length + " given");

            long groupId = Long.parseLong(args[1]);

            StringBuilder contentBuilder = new StringBuilder();
            List<TransferObject> groupList = AppUtils.getDatabase(mContext).castQuery(
                    new SQLQuery.Select(AccessDatabase.DIVIS_TRANSFER)
                            .setWhere(String.format("%s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID),
                                    String.valueOf(groupId))
                            .setOrderBy(AccessDatabase.FIELD_TRANSFER_NAME + " ASC"),
                    TransferObject.class);

            for (TransferObject object : groupList)
                contentBuilder.append(makeContent(String.valueOf(object.friendlyName),
                        R.string.butn_download, "download", object.groupId,
                        object.requestId, object.friendlyName));

            return makePage(R.string.text_files, contentBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        return makePage(R.string.text_error, "Not found");
    }

    private String makeContent(String content, @StringRes int buttonRes, Object... objects)
    {
        StringBuilder actionUrlBuilder = new StringBuilder();
        Map<String, String> values = new HashMap<>();
        values.put("content", content);
        values.put("action_layout", mContext.getString(buttonRes));

        for (Object object : objects) {
            if (actionUrlBuilder.length() > 0)
                actionUrlBuilder.append("/");

            actionUrlBuilder.append(object);
        }

        values.put("actionUrl", actionUrlBuilder.toString());

        return applyPattern(getFieldPattern(), readPage("list_transfer.html"), values);
    }

    private String makePage(@StringRes int titleRes, String content)
    {
        String title = mContext.getString(titleRes);
        String appName = mContext.getString(R.string.text_appName);

        Map<String, String> values = new HashMap<>();
        values.put("title", String.format("%s - %s", title, appName));
        values.put("header_logo", "/image/icon.png");
        values.put("header", title);
        values.put("main_content", content);
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
        private final List<ClientHandler> running =
                Collections.synchronizedList(new ArrayList<ClientHandler>());

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
}