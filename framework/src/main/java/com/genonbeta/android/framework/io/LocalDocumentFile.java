package com.genonbeta.android.framework.io;

import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * created by: Veli
 * date: 17.02.2018 23:39
 */

public class LocalDocumentFile extends DocumentFile
{
	private File mFile;

	public LocalDocumentFile(DocumentFile parent, File file)
	{
		super(parent);
		mFile = file;
	}

	@Override
	public DocumentFile createFile(String mimeType, String displayName)
	{
		final String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);

		if (extension != null)
			displayName += "." + extension;

		final File target = new File(mFile, displayName);

		try {
			target.createNewFile();
			return new LocalDocumentFile(this, target);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public DocumentFile createDirectory(String displayName)
	{
		final File target = new File(mFile, displayName);

		if (target.isDirectory() || target.mkdir())
			return new LocalDocumentFile(this, target);

		return null;
	}

	public File getFile()
	{
		return mFile;
	}

	@Override
	public Uri getUri()
	{
		return Uri.fromFile(mFile);
	}

	@Override
	public String getName()
	{
		return mFile.getName();
	}

	@Override
	public DocumentFile getParentFile()
	{
		File parentFile = mFile.getParentFile();

		return parentFile == null || File.separator.equals(parentFile.getAbsolutePath()) // hide root
				? null
				: new LocalDocumentFile(null, parentFile);
	}

	@Override
	public String getType()
	{
		if (mFile.isDirectory())
			return "*/*";

		return getTypeForName(mFile.getName());
	}

	@Override
	public boolean isDirectory()
	{
		return mFile.isDirectory();
	}

	@Override
	public boolean isFile()
	{
		return mFile.isFile();
	}

	@Override
	public boolean isVirtual()
	{
		return false;
	}

	@Override
	public long lastModified()
	{
		return mFile.lastModified();
	}

	@Override
	public long length()
	{
		return mFile.length();
	}

	@Override
	public boolean canRead()
	{
		return mFile.canRead();
	}

	@Override
	public boolean canWrite()
	{
		return mFile.canWrite();
	}

	@Override
	public boolean delete()
	{
		deleteContents(mFile);
		return mFile.delete();
	}

	@Override
	public boolean exists()
	{
		return mFile.exists();
	}

	@Override
	public DocumentFile findFile(String displayName)
	{
		File file = new File(mFile, displayName);
		return file.exists() ? new LocalDocumentFile(this, file) : null;
	}

	@Override
	public DocumentFile[] listFiles()
	{
		final ArrayList<DocumentFile> results = new ArrayList<DocumentFile>();
		final File[] files = mFile.listFiles();

		if (files != null)
			for (File file : files)
				results.add(new LocalDocumentFile(this, file));

		return results.toArray(new DocumentFile[results.size()]);
	}

	@Override
	public boolean renameTo(String displayName)
	{
		final File target = new File(mFile.getParentFile(), displayName);

		if (mFile.renameTo(target)) {
			mFile = target;
			return true;
		}

		return false;
	}

	@Override
	public void sync()
	{
	}

	private static String getTypeForName(String name)
	{
		final int lastDot = name.lastIndexOf('.');

		if (lastDot >= 0) {
			final String extension = name.substring(lastDot + 1).toLowerCase();
			final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);

			if (mime != null)
				return mime;
		}

		return "application/octet-stream";
	}

	private static boolean deleteContents(File dir)
	{
		File[] files = dir.listFiles();
		boolean success = true;
		if (files != null) {
			for (File file : files) {
				if (file.isDirectory()) {
					success &= deleteContents(file);
				}
				if (!file.delete()) {
					Log.w(TAG, "Failed to delete " + file);
					success = false;
				}
			}
		}

		return success;
	}
}
