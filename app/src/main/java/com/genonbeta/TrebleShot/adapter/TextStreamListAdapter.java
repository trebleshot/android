package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.RecyclerViewFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;
import java.util.Collections;

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */

public class TextStreamListAdapter extends EditableListAdapter<TextStreamObject, RecyclerViewAdapter.ViewHolder>
{
	private AccessDatabase mDatabase;

	public TextStreamListAdapter(Context context)
	{
		super(context);

		mDatabase = new AccessDatabase(getContext());
	}

	@Override
	public ArrayList<TextStreamObject> onLoad()
	{
		ArrayList<TextStreamObject> list = mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_CLIPBOARD), TextStreamObject.class);

		Collections.sort(list, getDefaultComparator());

		return list;
	}

	public AccessDatabase getDatabase()
	{
		return mDatabase;
	}

	@NonNull
	@Override
	public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return new RecyclerViewAdapter.ViewHolder(getInflater().inflate(R.layout.list_text_stream, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position)
	{
		View parentView = holder.getView();
		TextStreamObject object = getItem(position);

		View selector = parentView.findViewById(R.id.selector);
		TextView text1 = parentView.findViewById(R.id.text);
		TextView text2 = parentView.findViewById(R.id.text2);

		if (getSelectionConnection() != null)
			selector.setSelected(object.isSelectableSelected());

		text1.setText(object.text);
		text2.setText(AppUtils.formatDateTime(getContext(), object.time));
	}
}