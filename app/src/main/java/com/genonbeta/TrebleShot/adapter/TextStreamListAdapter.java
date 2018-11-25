package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import androidx.annotation.NonNull;

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */

public class TextStreamListAdapter
        extends GroupEditableListAdapter<TextStreamObject, GroupEditableListAdapter.GroupViewHolder>
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
            lister.offerObliged(this, object);
    }

    @Override
    protected TextStreamObject onGenerateRepresentative(String representativeText)
    {
        return new TextStreamObject(representativeText);
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if (viewType == VIEW_TYPE_REPRESENTATIVE)
            return new GroupViewHolder(getInflater().inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text);

        return new GroupViewHolder(getInflater().inflate(R.layout.list_text_stream, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position)
    {
        try {
            TextStreamObject object = getItem(position);

            if (!holder.tryBinding(object)) {
                View parentView = holder.getView();

                TextView text1 = parentView.findViewById(R.id.text);
                TextView text2 = parentView.findViewById(R.id.text2);

                parentView.setSelected(object.isSelectableSelected());

                text1.setText(object.text);
                text2.setText(DateUtils.formatDateTime(getContext(), object.date, DateUtils.FORMAT_SHOW_TIME));
            }
        } catch (Exception e) {

        }
    }
}