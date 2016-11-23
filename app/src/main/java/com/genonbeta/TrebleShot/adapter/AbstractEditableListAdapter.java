package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.genonbeta.TrebleShot.R;

abstract public class AbstractEditableListAdapter extends BaseAdapter
{
    protected boolean mLockRequested = false;
    protected Context mContext;
    protected LayoutInflater mInflater;

    protected abstract void onSearch(String word);
    protected abstract void onUpdate();

    public AbstractEditableListAdapter(Context context)
    {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    public Context getContext()
    {
        return this.mContext;
    }
    public LayoutInflater getInflater() { return this.mInflater; }

    public boolean search(String word)
    {
        this.onSearch(word);
        return true;
    }

    public boolean update()
    {
        if (mLockRequested)
            return false;

        mLockRequested = true;

        this.onUpdate();

        mLockRequested = false;

        return true;
    }
}
