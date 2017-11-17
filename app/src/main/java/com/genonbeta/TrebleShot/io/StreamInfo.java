package com.genonbeta.TrebleShot.io;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.genonbeta.TrebleShot.util.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StreamCorruptedException;
import java.net.URI;

/**
 * created by: Veli
 * date: 4.10.2017 12:36
 */

public class StreamInfo
{
	public String friendlyName;
	public String mimeType;
	public String directory;
	public InputStream inputStream;
	public Uri uri;
	public Type type;
	public long size;

	public static StreamInfo getStreamInfo(Context context, Uri uri, boolean openStreams) throws FileNotFoundException, StreamCorruptedException, FolderStateException
	{
		StreamInfo streamInfo = new StreamInfo();
		String uriType = uri.toString();

		streamInfo.uri = uri;

		if (uriType.startsWith("content")) {
			ContentResolver contentResolver = context.getContentResolver();
			Cursor cursor = contentResolver.query(uri, null, null, null, null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
					int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

					if (nameIndex != -1 && sizeIndex != -1) {
						streamInfo.friendlyName = cursor.getString(nameIndex);
						streamInfo.size = cursor.getLong(sizeIndex);
						streamInfo.mimeType = contentResolver.getType(uri);
						streamInfo.type = Type.STREAM;

						if (openStreams)
							streamInfo.inputStream = contentResolver.openInputStream(uri);

						return streamInfo;
					}
				}

				cursor.close();
			}
		} else if (uriType.startsWith("file")) {
			File file = new File(URI.create(uriType));

			if (file.canRead()) {
				if (file.isDirectory())
					throw new FolderStateException();

				streamInfo.friendlyName = file.getName();
				streamInfo.size = file.length();
				streamInfo.mimeType = FileUtils.getFileContentType(file.getName());
				streamInfo.type = Type.FILE;

				if (openStreams)
					streamInfo.inputStream = new FileInputStream(file);

				return streamInfo;
			}
		}

		throw new StreamCorruptedException("Content was not able to route a stream. Empty result is returned");
	}

	public static StreamInfo getStreamInfo(Context context, Uri uri, boolean openStreams, String directory) throws FileNotFoundException, StreamCorruptedException, FolderStateException
	{
		StreamInfo streamInfo = getStreamInfo(context, uri, openStreams);
		streamInfo.directory = directory;

		return streamInfo;
	}

	public enum Type
	{
		STREAM,
		FILE
	}

	public static class FolderStateException extends Exception
	{
	}
}
