package com.genonbeta.android.framework.io;

import android.net.Uri;
import android.support.annotation.Nullable;

import java.io.File;

/**
 * created by: Veli
 * date: 18.02.2018 00:24
 */

public class StreamDocumentFile extends DocumentFile
{
	private StreamInfo mStream;

	public StreamDocumentFile(StreamInfo streamInfo)
	{
		super(null);
		mStream = streamInfo;
	}

	@Override
	public DocumentFile createFile(String mimeType, String displayName)
	{
		return null;
	}

	@Override
	public DocumentFile createDirectory(String displayName)
	{
		return null;
	}

	@Override
	public Uri getUri()
	{
		return mStream.uri;
	}

	@Nullable
	public File getFile()
	{
		return mStream.file;
	}

	@Override
	public String getName()
	{
		return mStream.friendlyName;
	}

	public StreamInfo getStream()
	{
		return mStream;
	}

	@Override
	public String getType()
	{
		return mStream.mimeType;
	}

	@Override
	public boolean isDirectory()
	{
		return false;
	}

	@Override
	public boolean isFile()
	{
		return true;
	}

	@Override
	public boolean isVirtual()
	{
		return false;
	}

	@Override
	public long lastModified()
	{
		return 0;
	}

	@Override
	public long length()
	{
		return mStream.size;
	}

	@Override
	public boolean canRead()
	{
		return true;
	}

	@Override
	public boolean canWrite()
	{
		return true;
	}

	@Override
	public boolean delete()
	{
		return false;
	}

	@Override
	public boolean exists()
	{
		return true;
	}

	@Override
	public DocumentFile[] listFiles()
	{
		return new DocumentFile[0];
	}

	@Override
	public boolean renameTo(String displayName)
	{
		return false;
	}

	@Override
	public void sync()
	{
	}
}
