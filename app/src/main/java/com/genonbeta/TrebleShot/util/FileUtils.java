package com.genonbeta.TrebleShot.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.util.Interrupter;

import java.io.File;
import java.io.IOException;

public class FileUtils extends com.genonbeta.android.framework.util.FileUtils
{
    public static void copy(Context context, DocumentFile source, DocumentFile destination,
                            Interrupter interrupter) throws Exception
    {
        copy(context, source, destination, interrupter, AppConfig.BUFFER_LENGTH_DEFAULT, AppConfig.DEFAULT_SOCKET_TIMEOUT);
    }

    public static DocumentFile getApplicationDirectory(Context context)
    {
        String defaultPath = getDefaultApplicationDirectoryPath(context);
        SharedPreferences defaultPreferences = AppUtils.getDefaultPreferences(context);

        if (defaultPreferences.contains("storage_path")) {
            try {
                DocumentFile savePath = fromUri(context, Uri.parse(defaultPreferences.getString("storage_path", null)));

                if (savePath.isDirectory() && savePath.canWrite())
                    return savePath;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        File defaultFolder = new File(defaultPath);

        if (defaultFolder.isFile())
            defaultFolder.delete();

        if (!defaultFolder.isDirectory())
            defaultFolder.mkdirs();

        return DocumentFile.fromFile(defaultFolder);
    }

    public static String getDefaultApplicationDirectoryPath(Context context)
    {
        return Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator
                + context.getString(R.string.text_appName);
    }

    public static String getFileFormat(String fileName)
    {
        final int lastDot = fileName.lastIndexOf('.');

        if (lastDot >= 0)
            return fileName.substring(lastDot + 1).toLowerCase();

        return null;
    }

    public static DocumentFile getIncomingPseudoFile(Context context, TransferObject transferObject, TransferGroup group, boolean createIfNotExists) throws IOException
    {
        return fetchFile(getSavePath(context, group), transferObject.directory, transferObject.file, createIfNotExists);
    }

    public static DocumentFile getIncomingTransactionFile(Context context, TransferObject transferObject, TransferGroup group) throws IOException
    {
        DocumentFile pseudoFile = getIncomingPseudoFile(context, transferObject, group, true);

        if (!pseudoFile.canWrite())
            throw new IOException("File cannot be created or you don't have permission write on it");

        return pseudoFile;
    }

    public static String getReadableUri(String uri)
    {
        return getReadableUri(Uri.parse(uri), uri);
    }

    public static String getReadableUri(Uri uri)
    {
        return getReadableUri(uri, uri.toString());
    }

    public static String getReadableUri(Uri uri, @Nullable String defaultValue)
    {
        return uri.getPath() == null ? defaultValue : uri.getPath();
    }

    public static boolean move(Context context, DocumentFile targetFile, DocumentFile destinationFile,
                               Interrupter interrupter) throws Exception
    {
        return move(context, targetFile, destinationFile, interrupter, AppConfig.BUFFER_LENGTH_DEFAULT, AppConfig.DEFAULT_SOCKET_TIMEOUT);
    }

    /**
     * The available path to save {@link TransferGroup} with fallback check
     *
     * @param context
     * @param preferences
     * @param group
     * @return
     */
    public static DocumentFile getSavePath(Context context, TransferGroup group)
    {
        DocumentFile defaultFolder = FileUtils.getApplicationDirectory(context);

        if (group.savePath != null) {
            try {
                DocumentFile savePath = fromUri(context, Uri.parse(group.savePath));

                if (savePath.isDirectory() && savePath.canWrite())
                    return savePath;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            group.savePath = defaultFolder.getUri().toString();
            AppUtils.getDatabase(context).publish(group);
        }

        return defaultFolder;
    }

    /**
     * Tries to start an activity to view {@param file} using {@link DocumentFile}
     *
     * @param activity Theme supplied {@link Context}
     * @param file
     * @return true
     */
    public static boolean openUriForeground(Activity activity, DocumentFile file)
    {
        if (!openUri(activity, file)) {
            Toast.makeText(activity, activity.getString(R.string.mesg_openFailure, file.getName()), Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        return true;
    }

    /**
     * When the transfer is done, this saves the uniquely named file to its actual name held in {@link TransferObject}.
     *
     * @param savePath       The save path that contains currentFile
     * @param currentFile    The file that should be renamed
     * @param transferObject The transfer request
     * @return File moved to its actual name
     * @throws IOException Thrown when rename fails
     */
    public static DocumentFile saveReceivedFile(DocumentFile savePath, DocumentFile currentFile, TransferObject transferObject) throws IOException
    {
        String uniqueName = FileUtils.getUniqueFileName(savePath, transferObject.friendlyName, true);

        if (!currentFile.renameTo(uniqueName))
            throw new IOException("Failed to rename object: " + currentFile);

        return savePath.findFile(uniqueName);
    }
}
