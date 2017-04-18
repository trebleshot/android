package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */

public class ProcessListAdapter extends BaseAdapter
{
	private Context mContext;
	private LayoutInflater mInflater;
	private MainDatabase mDatabase;
	private ArrayList<CursorItem> mList = new ArrayList<>();

	public ProcessListAdapter(Context context)
	{
		mContext = context;
		mDatabase = new MainDatabase(context);
		mInflater = LayoutInflater.from(mContext);

		mList.addAll(mDatabase.getTable(new SQLQuery.Select(MainDatabase.TABLE_TRANSFER)
				.setGroupBy(MainDatabase.FIELD_TRANSFER_GROUPID)
				.setOrderBy(MainDatabase.FIELD_TRANSFER_GROUPID + " DESC")));
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	@Override
	public Object getItem(int i)
	{
		return mList.get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return 0;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		if (view == null)
			view = mInflater.inflate(R.layout.list_process, viewGroup, false);

		CursorItem thisItem = (CursorItem) getItem(i);
		ImageView typeImage = (ImageView) view.findViewById(R.id.list_process_type_image);
		TextView mainText = (TextView) view.findViewById(R.id.list_process_name_text);

		boolean isIncoming = thisItem.getInt(MainDatabase.FIELD_TRANSFER_TYPE) == MainDatabase.TYPE_TRANSFER_TYPE_INCOMING;

		typeImage.setImageResource(isIncoming ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_upload_black_24dp);
		mainText.setText(thisItem.getString(MainDatabase.FIELD_TRANSFER_FILE));

		return view;
	}
}
