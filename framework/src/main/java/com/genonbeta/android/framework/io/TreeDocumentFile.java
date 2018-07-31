package com.genonbeta.android.framework.io;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.DocumentsContract;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;

/**
 * created by: Veli
 * date: 17.02.2018 22:01
 */

@RequiresApi(21)
public class TreeDocumentFile extends DocumentFile
{
	private Context mContext;
	private Uri mUri;

	private String mId;
	private String mName;
	private String mType;
	private long mLength;
	private long mFlags;
	private long mLastModified;

	private boolean mExists;

	public TreeDocumentFile(DocumentFile parent, Context context, Uri uri) throws Exception
	{
		super(parent);

		mContext = context;
		mUri = uri;

		sync();
	}

	public TreeDocumentFile(DocumentFile parent, Context context, Cursor cursor)
	{
		super(parent);

		mContext = context;

		if (loadFrom(cursor))
			mUri = DocumentsContract.buildDocumentUriUsingTree(parent.getUri(), mId);
	}

	@Override
	public DocumentFile createFile(String mimeType, String displayName)
	{
		try {
			Uri newFile = DocumentsContract.createDocument(mContext.getContentResolver(), mUri, mimeType, displayName);

			if (newFile != null)
				return new TreeDocumentFile(this, mContext, newFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public DocumentFile createDirectory(String displayName)
	{
		return createFile(DocumentsContract.Document.MIME_TYPE_DIR, displayName);
	}

	public long getFlags()
	{
		return mFlags;
	}

	@Override
	public Uri getUri()
	{
		return mUri;
	}

	@Override
	public String getName()
	{
		return mName;
	}

	@Override
	public String getType()
	{
		return mType;
	}

	@Override
	public boolean isDirectory()
	{
		return DocumentsContract.Document.MIME_TYPE_DIR.equals(mType);
	}

	@Override
	public boolean isFile()
	{
		return !DocumentsContract.Document.MIME_TYPE_DIR.equals(mType) || TextUtils.isEmpty(mType);
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	@Override
	public boolean isVirtual()
	{
		return (getFlags() & android.provider.DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT) != 0;
	}

	@Override
	public long lastModified()
	{
		return mLastModified;
	}

	@Override
	public long length()
	{
		return mLength;
	}

	protected boolean loadFrom(Cursor cursor)
	{
		if (cursor == null)
			return false;

		int idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID);
		int nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
		int sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE);
		int typeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE);
		int flagIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS);
		int modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED);

		if (idIndex == -1
				|| nameIndex == -1
				|| sizeIndex == -1
				|| typeIndex == -1
				|| flagIndex == -1
				|| modifiedIndex == -1)
			return false;

		mId = cursor.getString(idIndex);
		mName = cursor.getString(nameIndex);
		mLastModified = cursor.getLong(modifiedIndex);
		mLength = cursor.getLong(sizeIndex);
		mType = cursor.getString(typeIndex);
		mFlags = cursor.getLong(flagIndex);

		mExists = true;

		return true;
	}

	@Override
	public boolean canRead()
	{
		return mContext.checkCallingOrSelfUriPermission(mUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
				== PackageManager.PERMISSION_GRANTED;
	}

	@Override
	public boolean canWrite()
	{
		return mContext.checkCallingOrSelfUriPermission(mUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
				== PackageManager.PERMISSION_GRANTED;
	}

	@Override
	public boolean delete()
	{
		try {
			return DocumentsContract.deleteDocument(mContext.getContentResolver(), mUri);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public boolean exists()
	{
		return mExists;
	}

	@Override
	public DocumentFile[] listFiles()
	{
		try {
			Uri treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(mUri, DocumentsContract.getDocumentId(mUri));
			Cursor cursor = mContext.getContentResolver().query(treeUri, null, null, null, null, null);

			if (cursor == null || !cursor.moveToFirst())
				return new DocumentFile[0];

			final DocumentFile[] resultFiles = new DocumentFile[cursor.getCount()];

			do {
				resultFiles[cursor.getPosition()] = new TreeDocumentFile(this, mContext, cursor);
			} while (cursor.moveToNext());

			closeQuietly(cursor);

			return resultFiles;
		} catch (Exception e) {

		}

		return new DocumentFile[]{};
	}

	@Override
	public boolean renameTo(String displayName)
	{
		try {
			return DocumentsContract.renameDocument(mContext.getContentResolver(), mUri, displayName) != null;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	public void sync() throws Exception
	{
		mExists = false;

		final ContentResolver resolver = mContext.getContentResolver();

		Cursor cursor = null;

		try {
			cursor = resolver.query(mUri, null, null, null, null);

			if (cursor != null && cursor.moveToFirst() && loadFrom(cursor))
				return;
		} catch (Exception e) {
			Log.w(TAG, "Failed query: " + e);
			throw e;
		} finally {
			closeQuietly(cursor);
		}

		throw new Exception("Failed to sync()");
	}
}
