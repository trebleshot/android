package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.android.framework.object.Selectable;

import java.util.List;

/**
 * created by: Veli
 * date: 5.01.2018 10:38
 */

public class SelectionEditorDialog<T extends Selectable> extends AlertDialog.Builder
{
    private List<T> mList;
    private LayoutInflater mLayoutInflater;
    private SelfAdapter mAdapter;
    private ListView mListView;

    public SelectionEditorDialog(Context context, List<T> list)
    {
        super(context);

        mList = list;
        mLayoutInflater = LayoutInflater.from(context);

        View view = mLayoutInflater.inflate(R.layout.layout_selection_editor, null, false);

        mListView = view.findViewById(R.id.listView);
        mAdapter = new SelfAdapter();

        mListView.setAdapter(mAdapter);
        mListView.setDividerHeight(0);

        if (mList.size() > 0)
            setView(view);
        else
            setMessage(R.string.text_listEmpty);

        setTitle(R.string.text_previewAndEditList);

        setNeutralButton(R.string.butn_check, null);
        setNegativeButton(R.string.butn_uncheck, null);
        setPositiveButton(R.string.butn_close, null);
    }

    public void checkReversed(View removeSign, Selectable selectable)
    {
        selectable.setSelectableSelected(!selectable.isSelectableSelected());
        removeSign.setVisibility(selectable.isSelectableSelected() ? View.GONE : View.VISIBLE);
    }

    public void massCheck(boolean check)
    {
        for (Selectable selectable : mList)
            selectable.setSelectableSelected(check);

        mAdapter.notifyDataSetChanged();
    }

    @Override
    public AlertDialog show()
    {
        final AlertDialog dialog = super.show();

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                massCheck(true);
            }
        });

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                massCheck(false);
            }
        });

        return dialog;
    }

    private class SelfAdapter extends BaseAdapter
    {
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
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
                convertView = mLayoutInflater.inflate(R.layout.list_selection, parent, false);

            final Selectable selectable = (Selectable) getItem(position);
            final TextView text1 = convertView.findViewById(R.id.text);
            final View removalSignView = convertView.findViewById(R.id.removalSign);

            text1.setText(selectable.getSelectableTitle());
            removalSignView.setVisibility(selectable.isSelectableSelected() ? View.GONE : View.VISIBLE);

            convertView.setClickable(true);
            convertView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    checkReversed(removalSignView, selectable);
                }
            });

            return convertView;
        }
    }
}
