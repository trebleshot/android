package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;

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

	public static DocumentFile getApplicationDirectory(Context context, SharedPreferences defaultPreferences)
	{
		String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + context.getString(R.string.text_appName);

		if (defaultPreferences.contains("storage_path")) {
			try {
				DocumentFile savePath = fromUri(context, Uri.parse(defaultPreferences.getString("storage_path", null)));

				if (savePath != null && savePath.isDirectory() && savePath.canWrite())
					return savePath;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		File defaultFolder = new File(defaultPath);

		if (!defaultFolder.isDirectory())
			defaultFolder.mkdirs();

		return DocumentFile.fromFile(defaultFolder);
	}

	public static DocumentFile getIncomingPseudoFile(Context context, SharedPreferences preferences, TransferObject transferObject, TransferGroup group, boolean createIfNotExists) throws IOException
	{
		return fetchFile(getSavePath(context, preferences, group), transferObject.directory, transferObject.file, createIfNotExists);
	}

	public static DocumentFile getIncomingTransactionFile(Context context, SharedPreferences preferences, TransferObject transferObject, TransferGroup group) throws IOException
	{
		DocumentFile pseudoFile = getIncomingPseudoFile(context, preferences, transferObject, group, true);

		if (!pseudoFile.canWrite())
			throw new IOException("File cannot be created or you don't have permission write on it");

		return pseudoFile;
	}

	public static DocumentFile getSavePath(Context context, SharedPreferences preferences, TransferGroup group)
	{
		if (group.savePath != null) {
			try {
				DocumentFile savePath = fromUri(context, Uri.parse(group.savePath));

				if (savePath.isDirectory() && savePath.canWrite())
					return savePath;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return FileUtils.getApplicationDirectory(context, preferences);
	}

	public static DocumentFile saveReceivedFile(DocumentFile savePath, DocumentFile currentFile, TransferObject transferObject) throws IOException
	{
		String uniqueName = FileUtils.getUniqueFileName(savePath, transferObject.friendlyName, true);

		if (!currentFile.renameTo(uniqueName))
			throw new IOException("Failed to rename object: " + currentFile);

		return savePath.findFile(uniqueName);
	}

	public static boolean move(Context context, DocumentFile targetFile, DocumentFile destinationFile,
							   Interrupter interrupter) throws Exception
	{
		return move(context, targetFile, destinationFile, interrupter, AppConfig.BUFFER_LENGTH_DEFAULT, AppConfig.DEFAULT_SOCKET_TIMEOUT);
	}
}
