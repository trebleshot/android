package com.genonbeta.TrebleShot.helper;

import android.content.Context;

import com.genonbeta.TrebleShot.database.Transaction;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.Locale;

public class FileUtils
{
	public enum Conflict
	{
		CURRENTLY_OK,
		SET_PATH_UNAVAILABLE,
		DEFAULT_PATH_FILE_EXISTS
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

	public static File getUniqueFile(File file)
	{
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

	public static Conflict isFileConflicted(Context context, AwaitedFileReceiver receiver)
	{
		if (Transaction.Flag.PENDING.equals(receiver.flag)
				|| receiver.fileAddress == null
				|| new File(receiver.fileAddress).canWrite())
			return Conflict.CURRENTLY_OK;
		else if (!new File(getSaveLocationForFile(context, receiver)).exists())
			return Conflict.SET_PATH_UNAVAILABLE;

		return Conflict.DEFAULT_PATH_FILE_EXISTS;
	}

	public static String getSaveLocationForFile(Context context, AwaitedFileReceiver receiver)
	{
		File tmpAddress = new File(receiver.fileAddress);

		return tmpAddress.getParentFile() == null ? getSaveLocationForFile(context, receiver.fileAddress)
				: (tmpAddress.canWrite() ? receiver.fileAddress : getSaveLocationForFile(context, tmpAddress.getName()));
	}

	public static String getSaveLocationForFile(Context context, String file)
	{
		return ApplicationHelper.getApplicationDirectory(context).getAbsolutePath() + File.separator + file;
	}
}
