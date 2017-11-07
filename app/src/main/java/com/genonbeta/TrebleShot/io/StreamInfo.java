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
	public InputStream inputStream;
	public long size;
	public Uri uri;

	public static StreamInfo getStreamInfo(Context context, Uri uri) throws FileNotFoundException, StreamCorruptedException
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
						streamInfo.inputStream = contentResolver.openInputStream(uri);
						streamInfo.mimeType = contentResolver.getType(uri);

						return streamInfo;
					}
				}

				cursor.close();
			}
		} else if (uriType.startsWith("file")) {
			File file = new File(URI.create(uriType));

			if (file.canRead()) {
				streamInfo.friendlyName = file.getName();
				streamInfo.size = file.length();
				streamInfo.inputStream = new FileInputStream(file);
				streamInfo.mimeType = FileUtils.getFileContentType(file.getName());

				return streamInfo;
			}
		}

		throw new StreamCorruptedException("Content was not able to route a stream. Empty result is returned");
	}
}
