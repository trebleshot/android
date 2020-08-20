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

package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.object.Transfer;
import com.genonbeta.TrebleShot.object.TransferItem;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.util.Stoppable;

import java.io.File;
import java.io.IOException;

public class FileUtils extends com.genonbeta.android.framework.util.FileUtils
{
    public static void copy(Context context, DocumentFile source, DocumentFile destination, Stoppable stoppable)
            throws Exception
    {
        copy(context, source, destination, stoppable, AppConfig.BUFFER_LENGTH_DEFAULT,
                AppConfig.DEFAULT_TIMEOUT_SOCKET);
    }

    public static DocumentFile getApplicationDirectory(Context context)
    {
        File defaultPath = getDefaultApplicationDirectoryPath(context);
        SharedPreferences defaultPreferences = AppUtils.getDefaultPreferences(context);

        if (defaultPreferences.contains("storage_path")) {
            try {
                DocumentFile savePath = fromUri(context, Uri.parse(defaultPreferences.getString("storage_path",
                        null)));

                if (savePath.isDirectory() && savePath.canWrite())
                    return savePath;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (defaultPath.isFile())
            defaultPath.delete();

        if (!defaultPath.isDirectory())
            defaultPath.mkdirs();

        return DocumentFile.fromFile(defaultPath);
    }

    @SuppressWarnings("deprecation")
    public static File getDefaultApplicationDirectoryPath(Context context)
    {
        if (Build.VERSION.SDK_INT >= 29)
            return context.getExternalCacheDir();

        File primaryDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        if ((!primaryDir.isDirectory() && !primaryDir.mkdirs()) || !primaryDir.canWrite())
            primaryDir = Environment.getExternalStorageDirectory();

        return new File(primaryDir + File.separator + context.getString(R.string.text_appName));
    }

    public static String getFileFormat(String fileName)
    {
        final int lastDot = fileName.lastIndexOf('.');
        return lastDot >= 0 ? fileName.substring(lastDot + 1).toLowerCase() : null;
    }

    public static DocumentFile getIncomingPseudoFile(Context context, TransferItem transferItem,
                                                     Transfer transfer, boolean createIfNotExists) throws IOException
    {
        return fetchFile(getSavePath(context, transfer), transferItem.directory, transferItem.file, createIfNotExists);
    }

    public static DocumentFile getIncomingFile(Context context, TransferItem transferItem, Transfer transfer)
            throws IOException
    {
        DocumentFile pseudoFile = getIncomingPseudoFile(context, transferItem, transfer, true);

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
                               Stoppable stoppable) throws Exception
    {
        return move(context, targetFile, destinationFile, stoppable, AppConfig.BUFFER_LENGTH_DEFAULT,
                AppConfig.DEFAULT_TIMEOUT_SOCKET);
    }

    public static DocumentFile getSavePath(Context context, Transfer transfer)
    {
        DocumentFile defaultFolder = FileUtils.getApplicationDirectory(context);

        if (transfer.savePath != null) {
            try {
                DocumentFile savePath = fromUri(context, Uri.parse(transfer.savePath));

                if (savePath.isDirectory() && savePath.canWrite())
                    return savePath;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            transfer.savePath = defaultFolder.getUri().toString();
            AppUtils.getKuick(context).publish(transfer);
        }

        return defaultFolder;
    }

    public static boolean openUriForeground(Context context, DocumentFile file)
    {
        if (!openUri(context, file)) {
            Toast.makeText(context, context.getString(R.string.mesg_openFailure, file.getName()), Toast.LENGTH_SHORT)
                    .show();
            return false;
        }

        return true;
    }

    /**
     * When the transfer is done, this saves the uniquely named file to its actual name held in {@link TransferItem}.
     *
     * @param savePath     The save path that contains currentFile
     * @param currentFile  The file that should be renamed
     * @param transferItem The transfer request
     * @return File moved to its actual name
     * @throws IOException Thrown when rename fails
     */
    public static DocumentFile saveReceivedFile(DocumentFile savePath, DocumentFile currentFile,
                                                TransferItem transferItem) throws Exception
    {
        String uniqueName = FileUtils.getUniqueFileName(savePath, transferItem.name, true);

        // FIXME: 7/30/19 The rename always fails when renaming TreeDocumentFile
        if (!currentFile.renameTo(uniqueName))
            throw new IOException("Failed to rename object: " + currentFile);

        transferItem.file = uniqueName;
        savePath.sync();

        return savePath.findFile(uniqueName);
    }
}
