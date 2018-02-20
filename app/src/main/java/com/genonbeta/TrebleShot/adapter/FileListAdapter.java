package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.io.DocumentFile;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.object.WritablePathObject;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class FileListAdapter extends ShareableListAdapter<FileListAdapter.GenericFileHolder>
{
	private boolean mShowDirectories = true;
	private boolean mShowFiles = true;
	private String mFileMatch;
	private DocumentFile mPath;
	private AccessDatabase mDatabase;

	public FileListAdapter(Context context, AccessDatabase database)
	{
		super(context);
		mDatabase = database;
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
						files.add(new FileHolder(file));
				}

				Collections.sort(folders, getDefaultComparator());
				Collections.sort(files, getDefaultComparator());
			}
		} else {
			ArrayList<File> referencedDirectoryList = new ArrayList<>();
			DocumentFile defaultFolder = FileUtils.getApplicationDirectory(getContext());

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

			ArrayList<WritablePathObject> objectList = getDatabase().castQuery(new SQLQuery.Select(AccessDatabase.TABLE_WRITABLEPATH), WritablePathObject.class);

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

	public String buildPath(String[] splitPath, int count)
	{
		StringBuilder stringBuilder = new StringBuilder();

		for (int i = 0; (i < count && i < splitPath.length); i++) {
			stringBuilder.append(File.separator);
			stringBuilder.append(splitPath[i]);
		}

		return stringBuilder.toString();
	}

	@Override
	public int getCount()
	{
		return getItemList().size();
	}

	public AccessDatabase getDatabase()
	{
		return mDatabase;
	}

	@Override
	public Object getItem(int itemId)
	{
		return getItemList().get(itemId);
	}

	public ArrayList<GenericFileHolder> getList()
	{
		return getItemList();
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	public DocumentFile getPath()
	{
		return mPath;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
			convertView = getInflater().inflate(R.layout.list_file, parent, false);

		final GenericFileHolder holder = (GenericFileHolder) getItem(position);

		final View selector = convertView.findViewById(R.id.selector);
		final View layoutImage = convertView.findViewById(R.id.layout_image);
		ImageView image = convertView.findViewById(R.id.image);
		TextView text1 = convertView.findViewById(R.id.text);
		TextView text2 = convertView.findViewById(R.id.text2);

		if (getSelectionConnection() != null) {
			selector.setSelected(holder.isSelectableSelected());

			layoutImage.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					getSelectionConnection().setSelected(holder);
					selector.setSelected(holder.isSelectableSelected());
				}
			});
		}

		image.setImageResource(holder.iconRes);
		text1.setText(holder.friendlyName);
		text2.setText(holder.info);

		return convertView;
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
		public FileHolder(DocumentFile file)
		{
			super(file,
					file.getName(),
					FileUtils.sizeExpression(file.length(), false),
					R.drawable.ic_insert_drive_file_white_24dp,
					file.lastModified(),
					file.length(),
					file.getUri());
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
