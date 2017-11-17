package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.genonbeta.TrebleShot.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Locale;

public class FileUtils
{
	public static File getApplicationDirectory(Context context)
	{
		String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + context.getString(R.string.text_appName);
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		File storagePath = new File(sharedPreferences.getString("storage_path", defaultPath));

		if (!storagePath.exists())
			storagePath.mkdirs();

		if (storagePath.canWrite())
			return storagePath;

		return new File(defaultPath);
	}

	public static String getFileContentType(String fileUrl)
	{
		FileNameMap nameMap = URLConnection.getFileNameMap();
		String fileType = nameMap.getContentTypeFor(fileUrl);

		return (fileType == null) ? "*/*" : fileType;
	}

	public static String sizeExpression(long bytes, boolean si)
	{
		int unit = si ? 1000 : 1024;

		if (bytes < unit)
			return bytes + " B";

		int expression = (int) (Math.log(bytes) / Math.log(unit));
		String prefix = (si ? "kMGTPE" : "KMGTPE").charAt(expression - 1) + (si ? "i" : "");

		return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, expression), prefix);
	}

	public static File getUniqueFile(File file, boolean tryActualFile)
	{
		if (tryActualFile && !file.isFile())
			return file;

		String fileName = file.getName();
		int pathStartPosition = fileName.lastIndexOf(".");

		String mergedFileName = file.getParent() + File.separator + (pathStartPosition != -1 ? fileName.substring(0, pathStartPosition) : fileName);
		String fileExtension = pathStartPosition != -1 ? fileName.substring(pathStartPosition) : "";

		for (int exceed = 1; exceed < 999; exceed++) {
			File newFile = new File(mergedFileName + " (" + exceed + ")" + fileExtension);

			if (!newFile.exists())
				return newFile;
		}

		return null;
	}

	public static File getIncomingTransactionFile(Context context, TransactionObject transactionObject, TransactionObject.Group group) throws IOException
	{
		String transactionFile = (transactionObject.directory != null ? transactionObject.directory + File.separator : "") + transactionObject.file;
		boolean useCustom = false;

		if (group.savePath != null) {
			File customPath = new File(group.savePath);
			useCustom = (!customPath.exists() && customPath.mkdirs()) || customPath.canWrite();
		}

		File file = new File((!useCustom ? FileUtils.getApplicationDirectory(context).getAbsolutePath() : group.savePath) + File.separator + transactionFile);

		File parent = file.getParentFile();

		if ((!parent.exists() && !parent.mkdirs()) || !parent.canWrite())
			throw new IOException("Parent dir " + parent.getAbsolutePath() + " cannot be used");

		if (!file.createNewFile() && (!file.isFile() || !file.canWrite()))
			throw new IOException("File cannot be created or you don't have permission write on it");

		return file;
	}
}
