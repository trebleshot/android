/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.widget;

import android.text.format.DateUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.genonbeta.TrebleShot.app.IEditableListFragment;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.android.framework.util.MathUtils;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.SectionTitleProvider;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * created by: Veli
 * date: 12.01.2018 16:55
 */

abstract public class EditableListAdapter<T extends Editable, V extends RecyclerViewAdapter.ViewHolder>
        extends RecyclerViewAdapter<T, V> implements EditableListAdapterBase<T>, SectionTitleProvider
{
    public static final int VIEW_TYPE_DEFAULT = 0;

    public static final int MODE_SORT_BY_NAME = 100;
    public static final int MODE_SORT_BY_DATE = 110;
    public static final int MODE_SORT_BY_SIZE = 120;

    public static final int MODE_SORT_ORDER_ASCENDING = 100;
    public static final int MODE_SORT_ORDER_DESCENDING = 110;

    private IEditableListFragment<T, V> mFragment;
    private final List<T> mItemList = new ArrayList<>();
    private int mSortingCriteria = MODE_SORT_BY_NAME;
    private int mSortingOrderAscending = MODE_SORT_ORDER_ASCENDING;
    private boolean mGridLayoutRequested = false;
    private Comparator<T> mGeneratedComparator;

    public EditableListAdapter(IEditableListFragment<T, V> fragment)
    {
        super(fragment.getContext());
        setHasStableIds(true);
        setFragment(fragment);
    }

    @Override
    public void onUpdate(List<T> passedItem)
    {
        synchronized (mItemList) {
            mItemList.clear();
            mItemList.addAll(passedItem);

            syncSelectionList();
        }
    }

    public int compareItems(int sortingCriteria, int sortingOrder, T objectOne, T objectTwo)
    {
        return 1;
    }

    public boolean filterItem(T item)
    {
        String[] filteringKeywords = getFragment().getFilteringDelegate()
                .getFilteringKeyword(getFragment());

        return filteringKeywords == null || filteringKeywords.length <= 0 || item.applyFilter(filteringKeywords);
    }

    public boolean isGridLayoutRequested()
    {
        return mGridLayoutRequested;
    }

    @Override
    public int getCount()
    {
        return getList().size();
    }

    public Comparator<T> getDefaultComparator()
    {
        if (mGeneratedComparator == null)
            mGeneratedComparator = new Comparator<T>()
            {
                Collator mCollator;

                public Collator getCollator()
                {
                    if (mCollator == null) {
                        mCollator = Collator.getInstance();
                        mCollator.setStrength(Collator.TERTIARY);
                    }

                    return mCollator;
                }

                @Override
                public int compare(T toCompare, T compareTo)
                {
                    boolean sortingAscending = getSortingOrder(toCompare, compareTo) == MODE_SORT_ORDER_ASCENDING;

                    T objectFirst = sortingAscending ? toCompare : compareTo;
                    T objectSecond = sortingAscending ? compareTo : toCompare;

                    if (objectFirst.comparisonSupported() == objectSecond.comparisonSupported()
                            && !objectFirst.comparisonSupported())
                        return 1;
                    else if (!toCompare.comparisonSupported())
                        return 1;
                    else if (!compareTo.comparisonSupported())
                        return -1;

                    // sorting direction is not used, so that the method doesn't have to guess which
                    // one is used.
                    switch (getSortingCriteria(toCompare, compareTo)) {
                        case MODE_SORT_BY_DATE:
                            return MathUtils.compare(objectFirst.getComparableDate(), objectSecond.getComparableDate());
                        case MODE_SORT_BY_SIZE:
                            return MathUtils.compare(objectFirst.getComparableSize(), objectSecond.getComparableSize());
                        case MODE_SORT_BY_NAME:
                            return getCollator().compare(objectFirst.getComparableName(), objectSecond.getComparableName());
                        default:
                            return compareItems(getSortingCriteria(), getSortingOrder(), objectFirst, objectSecond);
                    }
                }
            };

        return mGeneratedComparator;
    }

    public IEditableListFragment<T, V> getFragment()
    {
        return mFragment;
    }

    @Override
    public int getItemCount()
    {
        return getCount();
    }

    public T getItem(int position)
    {
        synchronized (mItemList) {
            return mItemList.get(position);
        }
    }

    public T getItem(V holder)
    {
        int position = holder.getAdapterPosition();
        if (position == RecyclerView.NO_POSITION)
            throw new IllegalStateException();
        return getItem(position);
    }

    @Override
    public long getItemId(int position)
    {
        return getItem(position).getId();
    }

    @Override
    public int getItemViewType(int position)
    {
        return VIEW_TYPE_DEFAULT;
    }

    @Override
    public List<T> getList()
    {
        return mItemList;
    }

    @Override
    public List<T> getSelectableList()
    {
        return getList();
    }

    @NonNull
    public String getSectionName(int position, T object)
    {
        switch (getSortingCriteria()) {
            case MODE_SORT_BY_NAME:
                return getSectionNameTrimmedText(object.getComparableName());
            case MODE_SORT_BY_DATE:
                return getSectionNameDate(object.getComparableDate());
            case MODE_SORT_BY_SIZE:
                return FileUtils.sizeExpression(object.getComparableSize(), false);
        }

        return String.valueOf(position);
    }

    public String getSectionNameDate(long date)
    {
        return String.valueOf(DateUtils.formatDateTime(getContext(), date, DateUtils.FORMAT_SHOW_DATE));
    }

    public String getSectionNameTrimmedText(String text)
    {
        return TextUtils.trimText(text, 1).toUpperCase();
    }

    @Override
    public String getSectionTitle(int position)
    {
        return getSectionName(position, getItem(position));
    }

    public int getSortingCriteria(T objectOne, T objectTwo)
    {
        return getSortingCriteria();
    }

    public int getSortingCriteria()
    {
        return mSortingCriteria;
    }

    public int getSortingOrder(T objectOne, T objectTwo)
    {
        return getSortingOrder();
    }

    public int getSortingOrder()
    {
        return mSortingOrderAscending;
    }

    public void notifyGridSizeUpdate(int gridSize, boolean isScreenLarge)
    {
        mGridLayoutRequested = (!isScreenLarge && gridSize > 1) || gridSize > 2;
    }

    public void setFragment(IEditableListFragment<T, V> fragmentImpl)
    {
        mFragment = fragmentImpl;
    }

    public void setSortingCriteria(int sortingCriteria, int sortingOrder)
    {
        mSortingCriteria = sortingCriteria;
        mSortingOrderAscending = sortingOrder;
    }

    @Override
    public void syncAndNotify(int adapterPosition)
    {
        syncSelection(adapterPosition);
        notifyItemChanged(adapterPosition);
    }

    @Override
    public void syncAllAndNotify()
    {
        syncSelectionList();
        notifyDataSetChanged();
    }

    public synchronized void syncSelection(int adapterPosition)
    {
        T item = getItem(adapterPosition);
        item.setSelectableSelected(mFragment.getEngineConnection().isSelectedOnHost(item));
    }

    public synchronized void syncSelectionList()
    {
        List<T> itemList = new ArrayList<>(getList());
        for (T item : itemList)
            item.setSelectableSelected(mFragment.getEngineConnection().isSelectedOnHost(item));
    }
}
