package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.LayoutInflater;
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

public class ReceivedFilesListAdapter extends AbstractEditableListAdapter
{

	private ArrayList<FileInfo> mList = new ArrayList<>();
	private String mSearchWord;
	private File mDefaultPath;
	private File mPath;
	private Comparator<FileInfo> mComparator = new Comparator<FileInfo>()
	{
		@Override
		public int compare(ReceivedFilesListAdapter.FileInfo compareFrom, ReceivedFilesListAdapter.FileInfo compareTo)
		{
			return compareFrom.fileName.toLowerCase().compareTo(compareTo.fileName.toLowerCase());
		}
	};

	public ReceivedFilesListAdapter(Context context)
	{
		super(context);
		mPath = mDefaultPath = ApplicationHelper.getApplicationDirectory(mContext);
	}

	@Override
	protected void onSearch(String word)
	{
		mSearchWord = word;
	}

	@Override
	protected void onUpdate()
	{
		mList.clear();

		ArrayList<FileInfo> folders = new ArrayList<>();
		ArrayList<FileInfo> files = new ArrayList<>();

		File[] fileIndex = mPath.listFiles();

		for (File file : fileIndex)
			if ((mSearchWord == null || (mSearchWord != null && ApplicationHelper.searchWord(file.getName(), mSearchWord))) && file.isDirectory() && file.canRead())
				folders.add(new FileInfo(file.getName(), mContext.getString(R.string.folder), file));

		for (File file : fileIndex)
			if ((mSearchWord == null || (mSearchWord != null && ApplicationHelper.searchWord(file.getName(), mSearchWord))) && file.isFile() && file.canRead())
				files.add(new FileInfo(file.getName(), FileUtils.sizeExpression(file.length(), false), file));

		Collections.sort(folders, mComparator);
		Collections.sort(files, mComparator);

		if (mPath.getParentFile() != null && mPath.getParentFile().canRead())
			mList.add(new FileInfo(mContext.getString(R.string.file_manager_go_up), "", mPath.getParentFile()));

		mList.addAll(folders);
		mList.addAll(files);
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
			convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_received_file, parent, false);

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
