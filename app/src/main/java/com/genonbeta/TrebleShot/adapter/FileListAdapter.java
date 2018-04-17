package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.io.DocumentFile;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.object.WritablePathObject;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class FileListAdapter
		extends EditableListAdapter<FileListAdapter.GenericFileHolder, EditableListAdapter.EditableViewHolder>
{
	private boolean mShowDirectories = true;
	private boolean mShowFiles = true;
	private String mFileMatch;
	private DocumentFile mPath;
	private AccessDatabase mDatabase;
	private SharedPreferences mPreferences;

	public FileListAdapter(Context context, AccessDatabase database, SharedPreferences sharedPreferences)
	{
		super(context);

		mDatabase = database;
		mPreferences = sharedPreferences;
	}

	@Override
	public ArrayList<GenericFileHolder> onLoad()
	{
		DocumentFile path = getPath();
		ArrayList<GenericFileHolder> list = new ArrayList<>();
		ArrayList<GenericFileHolder> folders = new ArrayList<>();
		ArrayList<GenericFileHolder> files = new ArrayList<>();

		if (path != null) {
			DocumentFile[] fileIndex = path.listFiles();

			if (fileIndex != null && fileIndex.length > 0) {
				for (DocumentFile file : fileIndex) {
					if ((mFileMatch != null && !file.getName().matches(mFileMatch)))
						continue;

					if (file.isDirectory() && mShowDirectories)
						folders.add(new DirectoryHolder(file, mContext.getString(R.string.text_folder), R.drawable.ic_folder_white_24dp));
					else if (file.isFile() && mShowFiles)
						files.add(new FileHolder(getContext(), file));
				}

				Collections.sort(folders, getDefaultComparator());
				Collections.sort(files, getDefaultComparator());
			}
		} else {
			ArrayList<File> referencedDirectoryList = new ArrayList<>();
			DocumentFile defaultFolder = FileUtils.getApplicationDirectory(getContext(), mPreferences);

			folders.add(new DirectoryHolder(defaultFolder, getContext().getString(R.string.text_receivedFiles), R.drawable.ic_whatshot_white_24dp));
			folders.add(new DirectoryHolder(DocumentFile.fromFile(new File(".")),
					mContext.getString(R.string.text_fileRoot),
					mContext.getString(R.string.text_folder),
					R.drawable.ic_folder_white_24dp));

			if (Build.VERSION.SDK_INT >= 21)
				referencedDirectoryList.addAll(Arrays.asList(getContext().getExternalMediaDirs()));
			else if (Build.VERSION.SDK_INT >= 19)
				referencedDirectoryList.addAll(Arrays.asList(getContext().getExternalFilesDirs(null)));
			else
				referencedDirectoryList.add(Environment.getExternalStorageDirectory());

			for (File mediaDir : referencedDirectoryList) {
				if (mediaDir == null || !mediaDir.canWrite())
					continue;

				DirectoryHolder fileHolder = new DirectoryHolder(DocumentFile.fromFile(mediaDir), getContext().getString(R.string.text_storage), R.drawable.ic_save_white_24dp);
				String[] splitPath = mediaDir.getAbsolutePath().split(File.separator);

				if (splitPath.length >= 2 && splitPath[1].equals("storage")) {
					if (splitPath.length >= 4 && splitPath[2].equals("emulated")) {
						File file = new File(buildPath(splitPath, 4));

						if (file.canWrite()) {
							fileHolder.file = DocumentFile.fromFile(file);
							fileHolder.friendlyName = "0".equals(splitPath[3])
									? getContext().getString(R.string.text_internalStorage)
									: getContext().getString(R.string.text_emulatedMediaDirectory, splitPath[3]);
						}
					} else if (splitPath.length >= 3) {
						File file = new File(buildPath(splitPath, 3));

						if (!file.canWrite())
							continue;

						fileHolder.friendlyName = splitPath[2];
						fileHolder.file = DocumentFile.fromFile(file);
					}
				}

				folders.add(fileHolder);
			}

			ArrayList<WritablePathObject> objectList = mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_WRITABLEPATH), WritablePathObject.class);

			if (Build.VERSION.SDK_INT >= 21) {
				for (WritablePathObject pathObject : objectList)
					try {
						folders.add(new WritablePathHolder(DocumentFile.fromUri(getContext(), pathObject.path, true),
								pathObject,
								getContext().getString(R.string.text_storage)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
			}
		}

		list.addAll(folders);
		list.addAll(files);

		return list;
	}

	@NonNull
	@Override
	public EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return new EditableViewHolder(getInflater().inflate(R.layout.list_file, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull final EditableViewHolder holder, final int position)
	{
		final GenericFileHolder object = getItem(position);
		final View parentView = holder.getView();

		ImageView image = parentView.findViewById(R.id.image);
		TextView text1 = parentView.findViewById(R.id.text);
		TextView text2 = parentView.findViewById(R.id.text2);

		holder.getView().setSelected(object.isSelectableSelected());

		text1.setText(object.friendlyName);
		text2.setText(object.info);
		image.setImageResource(object.iconRes);
	}

	public String buildPath(String[] splitPath, int count)
	{
		StringBuilder stringBuilder = new StringBuilder();

		for (int i = 0; (i < count && i < splitPath.length); i++) {
			stringBuilder.append(File.separator);
			stringBuilder.append(splitPath[i]);
		}

		return stringBuilder.toString();
	}

	public DocumentFile getPath()
	{
		return mPath;
	}

	public void goPath(File path)
	{
		goPath(DocumentFile.fromFile(path));
	}

	public void goPath(DocumentFile path)
	{
		mPath = path;
	}

	public void setConfiguration(boolean showDirectories, boolean showFiles, String fileMatch)
	{
		mShowDirectories = showDirectories;
		mShowFiles = showFiles;
		mFileMatch = fileMatch;
	}

	public static class GenericFileHolder extends Shareable
	{
		public DocumentFile file;
		public String info;
		public int iconRes;

		public GenericFileHolder(DocumentFile file, String friendlyName, String info, int iconRes, long date, long size, Uri uri)
		{
			super(friendlyName, friendlyName, file.getType(), date, size, uri);

			this.file = file;
			this.info = info;
			this.iconRes = iconRes;
		}
	}

	public static class FileHolder extends GenericFileHolder
	{
		public FileHolder(Context context, DocumentFile file)
		{
			super(file,
					file.getName(),
					FileUtils.sizeExpression(file.length(), false),
					R.drawable.ic_insert_drive_file_white_24dp,
					file.lastModified(),
					file.length(),
					FileUtils.getSecureUriSilently(context, file));
		}
	}

	public static class DirectoryHolder extends GenericFileHolder
	{
		public DirectoryHolder(DocumentFile file, String info, int iconRes)
		{
			this(file, file.getName(), info, iconRes);
		}

		public DirectoryHolder(DocumentFile file, String friendlyName, String info, int iconRes)
		{
			super(file, friendlyName, info, iconRes, file.lastModified(), 0, file.getUri());
		}
	}

	public static class WritablePathHolder extends GenericFileHolder
	{
		public WritablePathObject pathObject;

		public WritablePathHolder(DocumentFile file, WritablePathObject object, String info)
		{
			super(file, file.getName(), info, R.drawable.ic_save_white_24dp, 0, 0, object.path);

			this.pathObject = object;
		}
	}
}
