package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */

public class TextStreamListAdapter extends ShareableListAdapter<TextStreamObject>
{
	private ArrayList<TextStreamObject> mList = new ArrayList<>();
	private AccessDatabase mDatabase;

	public TextStreamListAdapter(Context context)
	{
		super(context);

		mDatabase = new AccessDatabase(getContext());
	}

	@Override
	public ArrayList<TextStreamObject> onLoad()
	{
		return mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_CLIPBOARD)
				.setOrderBy(AccessDatabase.FIELD_CLIPBOARD_TIME + " DESC"), TextStreamObject.class);
	}

	@Override
	public void onUpdate(ArrayList<TextStreamObject> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	public AccessDatabase getDatabase()
	{
		return mDatabase;
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mList.get(position);
	}

	@Override
	public long getItemId(int position)
	{
		return 0;
	}

	@Override
	public ArrayList<TextStreamObject> getList()
	{
		return mList;
	}

	@Override
	public View getView(int position, View view, ViewGroup viewGroup)
	{
		if (view == null)
			view = getInflater().inflate(R.layout.list_text_stream, viewGroup, false);

		TextStreamObject info = (TextStreamObject) getItem(position);
		TextView text1 = view.findViewById(R.id.text);
		TextView text2 = view.findViewById(R.id.text2);

		text1.setText(info.text);
		text2.setText(TimeUtils.getTimeAgo(getContext(), info.time));

		return view;
	}
}