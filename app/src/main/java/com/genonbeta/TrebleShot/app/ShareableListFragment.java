package com.genonbeta.TrebleShot.app;

import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class ShareableListFragment<T extends Shareable, V extends EditableListAdapter.EditableViewHolder, E extends EditableListAdapter<T, V>>
        extends EditableListFragment<T, V, E>
{
    private List<T> mCachedList = new ArrayList<>();
    private boolean mSearchSupport = true;
    private boolean mSearchActive = false;
    private String mDefaultEmptyText = null;
    private Toast mToastNoResult = null;

    private SearchView.OnQueryTextListener mSearchComposer = new SearchView.OnQueryTextListener()
    {
        @Override
        public boolean onQueryTextSubmit(String word)
        {
            return search(word);
        }

        @Override
        public boolean onQueryTextChange(String word)
        {
            return search(word);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);

        if (getSearchSupport()) {
            inflater.inflate(R.menu.actions_search, menu);

            SearchView searchView = ((SearchView) menu.findItem(R.id.search).getActionView());

            searchView.setOnQueryTextListener(mSearchComposer);
            searchView.setMaxWidth(500);
        }
    }

    public List<T> getCachedList()
    {
        return mCachedList;
    }

    public boolean getSearchSupport()
    {
        return mSearchSupport;
    }

    public void setSearchSupport(boolean searchSupport)
    {
        mSearchSupport = searchSupport;
    }

    @Override
    public boolean isRefreshLocked()
    {
        return super.isRefreshLocked() || mSearchActive;
    }

    public boolean search(String word)
    {
        mSearchActive = word != null && word.length() > 0;

        if (mSearchActive) {
            if (mCachedList.size() == 0)
                mCachedList.addAll(getAdapter().getList());

            List<T> searchableList = new ArrayList<>();

            for (T shareable : mCachedList)
                if (shareable.searchMatches(word))
                    searchableList.add(shareable);

            if (searchableList.size() > 0) {
                getAdapter().onUpdate(searchableList);
                getAdapter().notifyDataSetChanged();

                if (mToastNoResult != null)
                    mToastNoResult.cancel();
            } else {
                String text = getString(R.string.text_emptySearchResult, word);

                if (mToastNoResult == null) {
                    mToastNoResult = Toast.makeText(getContext(), text, Toast.LENGTH_SHORT);
                    mToastNoResult.setGravity(Gravity.TOP, mToastNoResult.getXOffset(), mToastNoResult.getYOffset());
                } else
                    mToastNoResult.setText(text);

                mToastNoResult.show();
            }
        } else if (!loadIfRequested() && mCachedList.size() != 0) {
            getAdapter().onUpdate(mCachedList);
            getAdapter().notifyDataSetChanged();

            mCachedList.clear();
        }

        if (mDefaultEmptyText == null)
            mDefaultEmptyText = String.valueOf(getEmptyText().getText());

        setEmptyText(mSearchActive
                ? getString(R.string.text_emptySearchResult, word)
                : mDefaultEmptyText);

        return getAdapter().getCount() > 0;
    }

    public static class MIMEGrouper
    {
        public static final String TYPE_GENERIC = "*";

        private String mMajor;
        private String mMinor;
        private boolean mLocked;

        public MIMEGrouper()
        {

        }

        public boolean isLocked()
        {
            return mLocked;
        }

        public String getMajor()
        {
            return mMajor == null ? TYPE_GENERIC : mMajor;
        }

        public String getMinor()
        {
            return mMinor == null ? TYPE_GENERIC : mMinor;
        }

        public void process(String mimeType)
        {
            if (mimeType == null || mimeType.length() < 3 || !mimeType.contains(File.separator))
                return;

            String[] splitMIME = mimeType.split(File.separator);

            process(splitMIME[0], splitMIME[1]);
        }

        public void process(String major, String minor)
        {
            if (mMajor == null || mMinor == null) {
                mMajor = major;
                mMinor = minor;
            } else if (getMajor().equals(TYPE_GENERIC))
                mLocked = true;
            else if (!getMajor().equals(major)) {
                mMajor = TYPE_GENERIC;
                mMinor = TYPE_GENERIC;

                mLocked = true;
            } else if (!getMinor().equals(minor)) {
                mMinor = TYPE_GENERIC;
            }
        }

        @Override
        public String toString()
        {
            return getMajor() + File.separator + getMinor();
        }
    }
}
