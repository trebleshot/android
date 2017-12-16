package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.Shareable;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class FileListAdapter extends ShareableListAdapter<FileListAdapter.FileHolder>
{
	private boolean mShowDirectories = true;
	private boolean mShowFiles = true;
	private String mFileMatch;

	private ArrayList<FileHolder> mList = new ArrayList<>();
	private File mDefaultPath;
	private File mPath;
	private Comparator<FileHolder> mComparator = new Comparator<FileHolder>()
	{
		@Override
		public int compare(FileListAdapter.FileHolder compareFrom, FileListAdapter.FileHolder compareTo)
		{
			return compareFrom.friendlyName.toLowerCase().compareTo(compareTo.friendlyName.toLowerCase());
		}
	};

	public FileListAdapter(Context context)
	{
		super(context);
		mDefaultPath = FileUtils.getApplicationDirectory(mContext);
	}

	@Override
	public ArrayList<FileHolder> onLoad()
	{
		ArrayList<FileHolder> list = new ArrayList<>();
		ArrayList<FileHolder> folders = new ArrayList<>();
		ArrayList<FileHolder> files = new ArrayList<>();

		if (mPath != null) {
			File[] fileIndex = mPath.listFiles();

			if (fileIndex != null && fileIndex.length > 0) {
				for (File file : fileIndex) {
					if ((mFileMatch != null && !file.getName().matches(mFileMatch)))
						continue;

					if (file.isDirectory() && mShowDirectories)
						folders.add(new FileHolder(file.getName(), mContext.getString(R.string.text_folder), file, true));
					else if (file.isFile() && mShowFiles)
						files.add(new FileHolder(file.getName(), FileUtils.sizeExpression(file.length(), false), file, false));
				}

				Collections.sort(folders, mComparator);
				Collections.sort(files, mComparator);
			}
		} else {
			ArrayList<File> referencedDirectoryList = new ArrayList<>();

			File defaultFolder = FileUtils.getApplicationDirectory(getContext());
			folders.add(new FileHolder(defaultFolder.getName(), getContext().getString(R.string.text_defaultFolder), defaultFolder, true));

			if (Build.VERSION.SDK_INT >= 21)
				referencedDirectoryList.addAll(Arrays.asList(getContext().getExternalMediaDirs()));
			else if (Build.VERSION.SDK_INT >= 19)
				referencedDirectoryList.addAll(Arrays.asList(getContext().getExternalFilesDirs(null)));
			else
				referencedDirectoryList.add(Environment.getExternalStorageDirectory());

			for (File mediaDir : referencedDirectoryList) {
				String mediaName = mediaDir.getName();
				String[] splitName = mediaDir.getAbsolutePath().split(File.separator);

				if (splitName.length >= 4 && splitName[2].equals("emulated"))
					mediaName = getContext().getString(R.string.text_emulatedMediaDirectory, splitName[3]);
				else if (splitName.length >= 3)
					mediaName = splitName[2];

				folders.add(new FileHolder(mediaName, getContext().getString(R.string.text_mediaDirectory), mediaDir, true));
			}
		}

		list.addAll(folders);
		list.addAll(files);

		return list;
	}

	@Override
	public void onUpdate(ArrayList<FileHolder> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	public File getDefaultPath()
	{
		return mDefaultPath;
	}

	@Override
	public Object getItem(int itemId)
	{
		return mList.get(itemId);
	}

	public ArrayList<FileHolder> getList()
	{
		return mList;
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	public File getPath()
	{
		return mPath;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		if (convertView == null)
			convertView = getInflater().inflate(R.layout.list_file, parent, false);

		ImageView typeImage = convertView.findViewById(R.id.image);
		TextView fileNameText = convertView.findViewById(R.id.text);
		TextView sizeText = convertView.findViewById(R.id.text2);
		FileHolder fileInfo = (FileHolder) getItem(position);

		typeImage.setImageResource(fileInfo.isFolder ? R.drawable.ic_folder_black_36dp : R.drawable.ic_insert_drive_file_black_36dp);
		fileNameText.setText(fileInfo.friendlyName);
		sizeText.setText(fileInfo.fileInfo);

		return convertView;
	}

	public void goPath(File path)
	{
		mPath = path;
	}

	public void setConfiguration(boolean showDirectories, boolean showFiles, String fileMatch)
	{
		mShowDirectories = showDirectories;
		mShowFiles = showFiles;
		mFileMatch = fileMatch;
	}

	public static class FileHolder extends Shareable
	{
		public String fileInfo;
		public File file;
		public boolean isFolder;

		public FileHolder(String friendlyName, String size, File file, boolean isFolder)
		{
			super(friendlyName, friendlyName, Uri.fromFile(file));

			this.fileInfo = size;
			this.file = file;
			this.isFolder = isFolder;
		}
	}
}
