package com.genonbeta.TrebleShot.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.adapter.AbstractFlexibleAdapter;
import com.genonbeta.TrebleShot.helper.GAnimater;

import java.util.ArrayList;
import java.util.HashSet;

public abstract class AbstractMediaListFragment<T extends AbstractFlexibleAdapter> extends ListFragment
{
    private T mAdapter;
    private MediaChoiceListener mChoiceListener;
    private ActionMode mActionMode;
    private boolean mIsLoading = false;

    private Runnable mNotifyListChanges = new Runnable()
    {
        @Override
        public void run()
        {
            getAdapter().notifyDataSetChanged();
            setEmptyText(getString(R.string.list_empty_msg));
        }
    };

    private Runnable mUpdateList = new Runnable()
    {
        @Override
        public void run()
        {
            boolean updateSucceed = getAdapter().update();

            if (updateSucceed && getActivity() != null && !isDetached())
                getActivity().runOnUiThread(mNotifyListChanges);

            AbstractMediaListFragment.this.mIsLoading = false;
        }
    };

    protected abstract T onAdapter();

    protected abstract MediaChoiceListener onChoiceListener();

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        this.mAdapter = this.onAdapter();
        this.mChoiceListener = this.onChoiceListener();

        this.setListAdapter(mAdapter);

        this.getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        this.getListView().setMultiChoiceModeListener(this.mChoiceListener);

        GAnimater.applyLayoutAnimation(getListView(), GAnimater.APPEAR);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        this.updateInBackground();
    }

    protected ActionMode getActionMode()
    {
        return this.mActionMode;
    }

    protected T getAdapter()
    {
        return this.mAdapter;
    }

    public boolean isLoading()
    {
        return mIsLoading;
    }

    public void openFile(Uri uri, String type, String chooserText)
    {
        Intent openIntent = new Intent(Intent.ACTION_VIEW);

        openIntent.setDataAndType(uri, type);

        this.startActivity(Intent.createChooser(openIntent, chooserText));
    }

    public void search(String word)
    {
        if (word.equals(""))
            word = null;
        else
            word = word.toLowerCase();

        getAdapter().search(word);
        updateInBackground();
    }

    public boolean updateInBackground()
    {
        if (getActivity() == null || isLoading())
            return false;

        this.mIsLoading = true;

        setEmptyText(getString(R.string.loading));

        new Thread(this.mUpdateList).start();

        return true;
    }

    public void warnBeforeRemove()
    {
        if (mChoiceListener != null)
            mChoiceListener.setItemsChecked(false);
    }

    protected abstract class MediaChoiceListener implements AbsListView.MultiChoiceModeListener
    {
        protected HashSet<Uri> mCheckedList = new HashSet<Uri>();
        protected MenuItem mSelectAll;

        public abstract void onItemChecked(ActionMode mode, int position, long id, boolean isChecked);

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu)
        {
            mode.getMenuInflater().inflate(R.menu.share_actions, menu);

            mActionMode = mode;
            mSelectAll = menu.findItem(R.id.file_actions_select);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu)
        {
            mCheckedList.clear();
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item)
        {
            if (item.getItemId() == R.id.file_actions_share || item.getItemId() == R.id.file_actions_share_trebleshot)
            {
                Intent shareIntent = null;
                String action = (item.getItemId() == R.id.file_actions_share) ? (mCheckedList.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND) : (mCheckedList.size() > 1 ? ShareActivity.ACTION_SEND_MULTIPLE : ShareActivity.ACTION_SEND);

                if (mCheckedList.size() > 1)
                {
                    ArrayList<Uri> uris = new ArrayList<Uri>();

                    for (Object uri : mCheckedList)
                        uris.add((Uri) uri);

                    shareIntent = new Intent(action);

                    shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
                    shareIntent.setType("*/*");
                }
                else if (mCheckedList.size() == 1)
                {
                    Uri fileUri = (Uri) mCheckedList.toArray()[0];

                    shareIntent = new Intent(action);

                    shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
                    shareIntent.setType("*/*");
                }

                if (shareIntent != null)
                {
                    startActivity((item.getItemId() == R.id.file_actions_share) ? Intent.createChooser(shareIntent, getString(R.string.file_share_app_chooser_msg)) : shareIntent);
                    return true;
                }
            }
            else if (item.getItemId() == R.id.file_actions_select)
            {
                setItemsChecked(mCheckedList.size() != getListView().getCount());
                return true;
            }

            return false;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean isChecked)
        {
            onItemChecked(mode, position, id, isChecked);

            mSelectAll.setIcon((mCheckedList.size() == getListView().getCount()) ? R.drawable.ic_unselect : R.drawable.ic_select);

            mode.setTitle(String.valueOf(getListView().getCheckedItemCount()));
        }

        @Override
        public void onDestroyActionMode(ActionMode p1)
        {
            mActionMode = null;
            mCheckedList.clear();
        }

        public void setItemsChecked(boolean check)
        {
            for (int i = 0; i < getListView().getCount(); i++)
            {
                getListView().setItemChecked(i, check);
            }
        }
    }
}
