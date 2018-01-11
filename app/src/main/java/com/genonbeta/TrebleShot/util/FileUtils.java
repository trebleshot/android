package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.TransactionObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.util.Locale;

public class FileUtils
{
	public static void copyFile(File source, File destination) throws IOException
	{
		FileChannel sourceChannel = null;
		FileChannel destChannel = null;

		sourceChannel = new FileInputStream(source).getChannel();
		destChannel = new FileOutputStream(destination).getChannel();

		destChannel.transferFrom(sourceChannel, 0, sourceChannel.size());

		sourceChannel.close();
		destChannel.close();
	}

	public static File getApplicationDirectory(Context context)
	{
		String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + context.getString(R.string.text_appName);
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		File storagePath = new File(sharedPreferences.getString("storage_path", defaultPath));

		if ((!storagePath.exists() && storagePath.mkdirs()) || storagePath.canWrite())
			return storagePath;

		return new File(defaultPath);
	}

	public static String getFileContentType(String fileUrl)
	{
		FileNameMap nameMap = URLConnection.getFileNameMap();
		String fileType = nameMap.getContentTypeFor(fileUrl);

		return (fileType == null) ? "*/*" : fileType;
	}

	public static File getIncomingTransactionFile(Context context, TransactionObject transactionObject, TransactionObject.Group group) throws IOException
	{
		String transactionFile = (transactionObject.directory != null ? transactionObject.directory + File.separator : "") + transactionObject.file;

		File file = new File(getSavePath(context, group) + File.separator + transactionFile);
		File parentFile = file.getParentFile();

		if (parentFile != null)
			if ((!parentFile.exists() && parentFile.mkdirs()) || parentFile.canWrite())

				if (!file.createNewFile() && (!file.isFile() || !file.canWrite()))
					throw new IOException("File cannot be created or you don't have permission write on it");

		return file;
	}

	public static File getSavePath(Context context, TransactionObject.Group group)
	{
		if (group.savePath != null) {
			File customPath = new File(group.savePath);

			if ((!customPath.exists() && customPath.mkdirs()) || customPath.canWrite())
				return customPath;
		}

		return FileUtils.getApplicationDirectory(context);
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

	public static Uri getUriForFile(Context context, File file, @Nullable Intent intent)
	{
		if (intent != null)
			intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				? FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file)
				: Uri.fromFile(file);
	}

	public static String sizeExpression(long bytes, boolean notUseByte)
	{
		int unit = notUseByte ? 1000 : 1024;

		if (bytes < unit)
			return bytes + " B";

		int expression = (int) (Math.log(bytes) / Math.log(unit));
		String prefix = (notUseByte ? "kMGTPE" : "KMGTPE").charAt(expression - 1) + (notUseByte ? "i" : "");

		return String.format(Locale.getDefault(), "%.1f %sB", bytes / Math.pow(unit, expression), prefix);
	}
}
