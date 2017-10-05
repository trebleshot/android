package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class FileListAdapter extends AbstractEditableListAdapter<FileListAdapter.FileInfo>
{
	private boolean mShowDirectories = true;
	private boolean mShowFiles = true;
	private String mFileMatch;

	private ArrayList<FileInfo> mList = new ArrayList<>();
	private File mDefaultPath;
	private File mPath;
	private Comparator<FileInfo> mComparator = new Comparator<FileInfo>()
	{
		@Override
		public int compare(FileListAdapter.FileInfo compareFrom, FileListAdapter.FileInfo compareTo)
		{
			return compareFrom.fileName.toLowerCase().compareTo(compareTo.fileName.toLowerCase());
		}
	};

	public FileListAdapter(Context context)
	{
		super(context);
		mPath = mDefaultPath = ApplicationHelper.getApplicationDirectory(mContext);
	}

	@Override
	public ArrayList<FileInfo> onLoad()
	{
		ArrayList<FileInfo> list = new ArrayList<>();
		ArrayList<FileInfo> folders = new ArrayList<>();
		ArrayList<FileInfo> files = new ArrayList<>();

		if (mPath != null) {
			File[] fileIndex = mPath.listFiles();

			if (mShowDirectories) {
				for (File file : fileIndex)
					if (applySearch(file.getName()) && file.isDirectory() && file.canRead())
						folders.add(new FileInfo(file.getName(), mContext.getString(R.string.text_folder), file));

				Collections.sort(folders, mComparator);
			}

			if (mShowFiles) {
				for (File file : fileIndex)
					if ((mFileMatch == null || file.getName().matches(mFileMatch)) && applySearch(file.getName()) && file.isFile() && file.canRead())
						files.add(new FileInfo(file.getName(), FileUtils.sizeExpression(file.length(), false), file));

				Collections.sort(files, mComparator);
			}
		} else {
			ArrayList<File> paths = new ArrayList<>();

			File defaultFolder = ApplicationHelper.getApplicationDirectory(getContext());
			folders.add(new FileInfo(defaultFolder.getName(), "Default Folder", defaultFolder));

			paths.add(Environment.getExternalStorageDirectory());

			for (File storage : paths)
				folders.add(new FileInfo(storage.getName(), "Storage", storage));
		}

		list.addAll(folders);
		list.addAll(files);

		return list;
	}

	@Override
	public void onUpdate(ArrayList<FileInfo> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	@Override
	public Object getItem(int itemId)
	{
		return mList.get(itemId);
	}

	public ArrayList<FileInfo> getList()
	{
		return mList;
	}

	@Override
	public long getItemId(int p1)
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
			convertView = getInflater().inflate(R.layout.list_received_file, parent, false);

		TextView fileNameText = (TextView) convertView.findViewById(R.id.text);
		TextView sizeText = (TextView) convertView.findViewById(R.id.text2);
		FileInfo fileInfo = (FileInfo) getItem(position);

		fileNameText.setText(fileInfo.fileName);
		sizeText.setText(fileInfo.fileInfo);

		return convertView;
	}

	public void goDefault()
	{
		goPath(mDefaultPath);
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

	public static class FileInfo
	{
		public String fileName;
		public String fileInfo;
		public File file;

		public FileInfo(String name, String size, File file)
		{
			this.fileName = name;
			this.fileInfo = size;
			this.file = file;
		}
	}
}
