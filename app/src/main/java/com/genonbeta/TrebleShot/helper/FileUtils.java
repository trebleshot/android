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
		String path = file.getName();
		int pathStartPosition = path.lastIndexOf(".");

		if (pathStartPosition != -1)
		{
			String fileName = path.substring(0, pathStartPosition);
			String fileExtension = path.substring(pathStartPosition);

			return new File(file.getParent() + File.separator + fileName + " [" + System.currentTimeMillis() + "]" + fileExtension);
		}

		return new File(file.getParent() + File.separator + file.getName() + " [" + System.currentTimeMillis() + "]");
	}

	public static String getSaveLocationForFile(Context context, String file)
	{
		return ApplicationHelper.getApplicationDirectory(context).getAbsolutePath() + "/" + file;
	}
}
