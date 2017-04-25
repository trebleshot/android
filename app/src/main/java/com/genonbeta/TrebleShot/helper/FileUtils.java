package com.genonbeta.TrebleShot.helper;

import android.content.Context;

import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;

public class FileUtils
{
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

		return String.format("%.1f %sB", bytes / Math.pow(unit, expression), prefix);
	}

	public static File getUniqueFile(File file)
	{
		String fileName = file.getName();
		int pathStartPosition = fileName.lastIndexOf(".");

		String mergedFileName = file.getParent() + File.separator + (pathStartPosition != -1 ? fileName.substring(0, pathStartPosition) : fileName);
		String fileExtension = pathStartPosition != -1 ? fileName.substring(pathStartPosition) : "";

		for (int exceed = 1; exceed < 999; exceed++)
		{
			File newFile = new File(mergedFileName + " (" + exceed + ")" + fileExtension);

			if (!newFile.isFile())
				return newFile;
		}

		// TODO: 4/25/17 handle this later
		return null;
	}

	public static String getSaveLocationForFile(Context context, String file)
	{
		return ApplicationHelper.getApplicationDirectory(context).getAbsolutePath() + "/" + file;
	}
}
