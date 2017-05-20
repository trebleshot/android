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
	public ArrayList<FileInfo> mList = new ArrayList<FileInfo>();
	private String mSearchWord;
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

		for (File file : ApplicationHelper.getApplicationDirectory(mContext).listFiles())
		{
			if ((mSearchWord == null || (mSearchWord != null && ApplicationHelper.searchWord(file.getName(), mSearchWord))) && file.isFile())
				mList.add(new FileInfo(file.getName(), FileUtils.sizeExpression(file.length(), false), file));
		}

		Collections.sort(mList, mComparator);
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

	@Override
	public long getItemId(int p1)
	{
		return 0;
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
		sizeText.setText(fileInfo.fileSize);

		return convertView;
	}

	public static class FileInfo
	{
		public String fileName;
		public String fileSize;
		public File file;

		public FileInfo(String name, String size, File file)
		{
			this.fileName = name;
			this.fileSize = size;
			this.file = file;
		}
	}
}
