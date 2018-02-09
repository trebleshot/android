package com.genonbeta.TrebleShot.io;

import android.content.ContentProviderClient;
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
	public Uri uri;
	public Type type;
	public long size;

	public StreamInfo()
	{

	}

	public StreamInfo(Context context, Uri uri, boolean openStreams) throws FileNotFoundException, StreamCorruptedException, FolderStateException
	{
		if (!loadStream(context, uri, openStreams))
			throw new StreamCorruptedException("Content was not able to route a stream. Empty result is returned");
	}

	public static StreamInfo getStreamInfo(Context context, Uri uri, boolean openStreams) throws FileNotFoundException, StreamCorruptedException, FolderStateException
	{
		return new StreamInfo(context, uri, openStreams);
	}

	private boolean loadStream(Context context, Uri uri, boolean openStreams) throws FolderStateException, FileNotFoundException
	{
		String uriType = uri.toString();

		this.uri = uri;

		if (uriType.startsWith("content")) {
			ContentResolver contentResolver = context.getContentResolver();
			Cursor cursor = contentResolver.query(uri, null, null, null, null);

			if (cursor != null) {
				if (cursor.moveToFirst()) {
					int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
					int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

					if (nameIndex != -1 && sizeIndex != -1) {
						this.friendlyName = cursor.getString(nameIndex);
						this.size = cursor.getLong(sizeIndex);
						this.mimeType = contentResolver.getType(uri);
						this.type = Type.STREAM;

						ContentProviderClient client = contentResolver.acquireContentProviderClient(uri);

						if (openStreams)
							this.inputStream = contentResolver.openInputStream(uri);

						return true;
					}
				}

				cursor.close();
			}
		} else if (uriType.startsWith("file")) {
			File file = new File(URI.create(uriType));

			if (file.canRead()) {
				if (file.isDirectory())
					throw new FolderStateException();

				this.friendlyName = file.getName();
				this.size = file.length();
				this.mimeType = FileUtils.getFileContentType(file.getName());
				this.type = Type.FILE;

				if (openStreams)
					this.inputStream = new FileInputStream(file);

				return true;
			}
		}

		return false;
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
