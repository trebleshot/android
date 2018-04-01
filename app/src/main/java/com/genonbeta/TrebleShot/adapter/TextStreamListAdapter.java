package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.widget.GroupShareableListAdapter;
import com.genonbeta.android.database.SQLQuery;

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */

public class TextStreamListAdapter
		extends GroupShareableListAdapter<TextStreamObject, GroupShareableListAdapter.ViewHolder>
{
	private AccessDatabase mDatabase;

	public TextStreamListAdapter(Context context, AccessDatabase database)
	{
		super(context, MODE_GROUP_BY_DATE);
		mDatabase = database;
	}

	@Override
	protected void onLoad(GroupLister<TextStreamObject> lister)
	{
		for (TextStreamObject object : mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_CLIPBOARD), TextStreamObject.class))
			lister.offer(object);
	}

	@Override
	protected TextStreamObject onGenerateRepresentative(String representativeText)
	{
		return new TextStreamObject(representativeText);
	}

	public AccessDatabase getDatabase()
	{
		return mDatabase;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return viewType == VIEW_TYPE_REPRESENTATIVE
				? new ViewHolder(getInflater().inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text)
				: new ViewHolder(getInflater().inflate(R.layout.list_text_stream, parent, false));
	}


	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position)
	{
		View parentView = holder.getView();
		TextStreamObject object = getItem(position);

		if (!holder.tryBinding(object)) {
			View selector = parentView.findViewById(R.id.selector);
			TextView text1 = parentView.findViewById(R.id.text);
			TextView text2 = parentView.findViewById(R.id.text2);

			if (getSelectionConnection() != null)
				selector.setSelected(object.isSelectableSelected());

			text1.setText(object.text);
			text2.setText(DateUtils.formatDateTime(getContext(), object.date, DateUtils.FORMAT_SHOW_TIME));
		}
	}
}