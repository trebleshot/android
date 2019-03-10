package com.genonbeta.TrebleShot.widget;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.View;

import androidx.annotation.NonNull;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.util.AppUtils;
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

abstract public class EditableListAdapter<T extends Editable, V extends EditableListAdapter.EditableViewHolder>
        extends RecyclerViewAdapter<T, V>
        implements EditableListAdapterImpl<T>, SectionTitleProvider
{
    public static final int VIEW_TYPE_DEFAULT = 0;

    public static final int MODE_SORT_BY_NAME = 100;
    public static final int MODE_SORT_BY_DATE = 110;
    public static final int MODE_SORT_BY_SIZE = 120;

    public static final int MODE_SORT_ORDER_ASCENDING = 100;
    public static final int MODE_SORT_ORDER_DESCENDING = 110;

    private EditableListFragmentImpl<T> mFragment;
    private List<T> mItemList = new ArrayList<>();
    private int mSortingCriteria = MODE_SORT_BY_NAME;
    private int mSortingOrderAscending = MODE_SORT_ORDER_ASCENDING;
    private boolean mGridLayoutRequested = false;
    private Comparator<T> mGeneratedComparator;

    public EditableListAdapter(Context context)
    {
        super(context);
        setHasStableIds(true);
    }

    @Override
    public void onUpdate(List<T> passedItem)
    {
        synchronized (getItemList()) {
            mItemList.clear();
            mItemList.addAll(passedItem);

            syncSelectionList(getItemList());
        }
    }

    public int compareItems(int sortingCriteria, int sortingOrder, T objectOne, T objectTwo)
    {
        return 1;
    }

    public boolean filterItem(T item)
    {
        String[] filteringKeywords = getFragment()
                .getFilteringDelegate()
                .getFilteringKeyword(getFragment());

        return filteringKeywords == null
                || filteringKeywords.length <= 0
                || item.applyFilter(filteringKeywords);
    }

    @Override
    public int getCount()
    {
        return getItemList().size();
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

                    // sorting direction is not used, so that the method doesn't have to guess which is used.
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

    public EditableListFragmentImpl<T> getFragment()
    {
        return mFragment;
    }

    public void setFragment(EditableListFragmentImpl<T> fragmentImpl)
    {
        mFragment = fragmentImpl;
    }

    @Override
    public int getItemCount()
    {
        return getCount();
    }

    public T getItem(int position) throws NotReadyException
    {
        if (position >= getCount() || position < 0)
            throw new NotReadyException("The list does not contain  this index: " + position);

        return getList().get(position);
    }

    public T getItem(V holder) throws NotReadyException
    {
        return getItem(holder.getAdapterPosition());
    }

    @Override
    public long getItemId(int position)
    {
        try {
            return getItem(position).getId();
        } catch (NotReadyException e) {
            e.printStackTrace();
        }

        // This may be changed in the future
        return AppUtils.getUniqueNumber();
    }

    public List<T> getItemList()
    {
        return mItemList;
    }

    @Override
    public int getItemViewType(int position)
    {
        return VIEW_TYPE_DEFAULT;
    }

    @Override
    public List<T> getList()
    {
        return getItemList();
    }

    @Override
    public String getSectionTitle(int position)
    {
        try {
            return getSectionName(position, getItem(position));
        } catch (NotReadyException e) {
            e.printStackTrace();
        }

        return getContext().getString(R.string.text_emptySymbol);
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

    public boolean isGridSupported()
    {
        return false;
    }

    public boolean isGridLayoutRequested()
    {
        return mGridLayoutRequested;
    }

    public void notifyAllSelectionChanges()
    {
        syncSelectionList();
        notifyDataSetChanged();
    }

    public boolean notifyGridSizeUpdate(int gridSize, boolean isScreenLarge)
    {
        return mGridLayoutRequested = (!isScreenLarge && gridSize > 1)
                || gridSize > 2;
    }

    public void setSortingCriteria(int sortingCriteria, int sortingOrder)
    {
        mSortingCriteria = sortingCriteria;
        mSortingOrderAscending = sortingOrder;
    }

    public synchronized void syncSelectionList()
    {
        synchronized (getItemList()) {
            syncSelectionList(getItemList());
        }
    }

    public synchronized void syncSelectionList(List<T> itemList)
    {
        if (getFragment() == null || getFragment().getSelectionConnection() == null)
            return;

        for (T item : itemList)
            item.setSelectableSelected(mFragment.getSelectionConnection().isSelected(item));
    }

    public static class EditableViewHolder extends ViewHolder
    {
        private View mClickableView;

        public EditableViewHolder(View itemView)
        {
            super(itemView);
        }

        public View getClickableView()
        {
            return mClickableView == null ? getView() : mClickableView;
        }

        public EditableViewHolder setClickableView(int resId)
        {
            return setClickableView(getView().findViewById(resId));
        }

        public EditableViewHolder setClickableView(View clickableLayout)
        {
            mClickableView = clickableLayout;
            return this;
        }
    }
}
