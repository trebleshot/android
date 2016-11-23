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
    private String mSearchWord;
    public ArrayList<FileInfo> mList = new ArrayList<FileInfo>();
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
        this.mSearchWord = word;
    }

    @Override
    protected void onUpdate()
    {
        this.mList.clear();

        for (File file : ApplicationHelper.getApplicationDirectory(mContext).listFiles())
        {
            if ((this.mSearchWord == null || (this.mSearchWord != null && ApplicationHelper.searchWord(file.getName(), this.mSearchWord))) && file.isFile())
                this.mList.add(new FileInfo(file.getName(), FileUtils.sizeExpression(file.length(), false), file));
        }

        Collections.sort(mList, this.mComparator);
    }

    @Override
    public int getCount()
    {
        return this.mList.size();
    }

    @Override
    public Object getItem(int itemId)
    {
        return this.mList.get(itemId);
    }

    @Override
    public long getItemId(int p1)
    {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        return getViewAt(convertView == null ? LayoutInflater.from(getContext()).inflate(R.layout.list_received_file, parent, false) : convertView, position);
    }

    public View getViewAt(View view, int position)
    {
        TextView fileNameText = (TextView) view.findViewById(R.id.text);
        TextView sizeText = (TextView) view.findViewById(R.id.text2);
        FileInfo fileInfo = (FileInfo) getItem(position);

        fileNameText.setText(fileInfo.fileName);
        sizeText.setText(fileInfo.fileSize);

        return view;
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
