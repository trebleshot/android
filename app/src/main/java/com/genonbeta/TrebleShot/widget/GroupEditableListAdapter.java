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

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.IEditableListFragment;
import com.genonbeta.TrebleShot.dataobject.Editable;
import com.genonbeta.TrebleShot.dataobject.Shareable;
import com.genonbeta.android.framework.util.date.DateMerger;
import com.genonbeta.android.framework.util.listing.ComparableMerger;
import com.genonbeta.android.framework.util.listing.Lister;
import com.genonbeta.android.framework.util.listing.Merger;
import com.genonbeta.android.framework.util.listing.merger.StringMerger;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * created by: Veli
 * date: 29.03.2018 08:00
 */
abstract public class GroupEditableListAdapter<T extends GroupEditableListAdapter.GroupEditable,
        V extends GroupEditableListAdapter.GroupViewHolder> extends EditableListAdapter<T, V>
{
    public static final int VIEW_TYPE_REPRESENTATIVE = 100;
    public static final int VIEW_TYPE_ACTION_BUTTON = 110;

    public static final int MODE_GROUP_BY_NOTHING = 100;
    public static final int MODE_GROUP_BY_DATE = 110;

    private int mGroupBy;

    public GroupEditableListAdapter(IEditableListFragment<T, V> fragment, int groupBy)
    {
        super(fragment);
        mGroupBy = groupBy;
    }

    protected abstract void onLoad(GroupLister<T> lister);

    protected abstract T onGenerateRepresentative(String text, Merger<T> merger);

    @Override
    public List<T> onLoad()
    {
        List<T> loadedList = new ArrayList<>();
        GroupLister<T> groupLister = createLister(loadedList, getGroupBy());

        onLoad(groupLister);

        if (groupLister.getList().size() > 0) {
            Collections.sort(groupLister.getList(), (o1, o2) -> o2.compareTo(o1));

            for (ComparableMerger<T> thisMerger : groupLister.getList()) {
                Collections.sort(thisMerger.getBelongings(), this);

                T generated = onGenerateRepresentative(getRepresentativeText(thisMerger), thisMerger);
                T firstEditable = thisMerger.getBelongings().get(0);

                if (generated != null) {
                    loadedList.add(generated);
                    generated.setSize(thisMerger.getBelongings().size());
                    generated.setDate(firstEditable.getComparableDate());
                    generated.setId(~generated.getRepresentativeText().hashCode());
                }

                loadedList.addAll(thisMerger.getBelongings());
            }
        } else
            Collections.sort(loadedList, this);

        return loadedList;
    }

    public GroupLister<T> createLister(List<T> loadedList, int groupBy)
    {
        return new GroupLister<>(loadedList, groupBy);
    }

    protected GroupViewHolder createDefaultViews(ViewGroup parent, int viewType, boolean noPadding)
    {
        if (viewType == VIEW_TYPE_REPRESENTATIVE)
            return new GroupViewHolder(getInflater().inflate(noPadding ? R.layout.layout_list_title_no_padding
                    : R.layout.layout_list_title, parent, false), R.id.layout_list_title_text);
        else if (viewType == VIEW_TYPE_ACTION_BUTTON)
            return new GroupViewHolder(getInflater().inflate(R.layout.layout_list_action_button, parent,
                    false), R.id.text);

        throw new IllegalArgumentException(viewType + " is not defined in defaults");
    }

    public int getGroupBy()
    {
        return mGroupBy;
    }

    public void setGroupBy(int groupBy)
    {
        mGroupBy = groupBy;
    }

    @Override
    public int getItemViewType(int position)
    {
        return getItem(position).getViewType();
    }

    public String getRepresentativeText(Merger<? extends T> merger)
    {
        if (merger instanceof DateMerger)
            return String.valueOf(getSectionNameDate(((DateMerger<?>) merger).getTime()));
        else if (merger instanceof StringMerger)
            return ((StringMerger<?>) merger).getString();

        return merger.toString();
    }

    @NonNull
    @Override
    public String getSectionName(int position, T object)
    {
        if (object.isGroupRepresentative())
            return object.getRepresentativeText();

        if (getGroupBy() == MODE_GROUP_BY_DATE)
            return getSectionNameDate(object.getComparableDate());

        return super.getSectionName(position, object);
    }

    public interface GroupEditable extends Editable
    {
        int getViewType();

        int getRequestCode();

        String getRepresentativeText();

        void setRepresentativeText(CharSequence text);

        boolean isGroupRepresentative();

        void setDate(long date);

        void setId(long id);

        void setSize(long size);
    }

    abstract public static class GroupShareable extends Shareable implements GroupEditable
    {
        private int mViewType = EditableListAdapter.VIEW_TYPE_DEFAULT;

        public GroupShareable()
        {
            super();
        }

        public GroupShareable(int viewType, String representativeText)
        {
            mViewType = viewType;
            friendlyName = representativeText;
        }

        @Override
        public int getRequestCode()
        {
            return 0;
        }

        @Override
        public int getViewType()
        {
            return mViewType;
        }

        @Override
        public String getRepresentativeText()
        {
            return friendlyName;
        }

        @Override
        public void setRepresentativeText(CharSequence text)
        {
            friendlyName = String.valueOf(text);
        }

        public boolean isGroupRepresentative()
        {
            return mViewType == VIEW_TYPE_REPRESENTATIVE || mViewType == VIEW_TYPE_ACTION_BUTTON;
        }

        @Override
        public void setDate(long date)
        {
            this.date = date;
        }

        @Override
        public void setSize(long size)
        {
            this.size = size;
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            return !isGroupRepresentative() && super.setSelectableSelected(selected);
        }
    }

    public static class GroupViewHolder extends RecyclerViewAdapter.ViewHolder
    {
        private TextView mRepresentativeTextView;
        private int mRequestCode;

        public GroupViewHolder(View itemView, TextView textView)
        {
            super(itemView);
            mRepresentativeTextView = textView;
        }

        public GroupViewHolder(View itemView, int resRepresentativeText)
        {
            this(itemView, itemView.findViewById(resRepresentativeText));
        }

        public GroupViewHolder(View itemView)
        {
            super(itemView);
        }

        public TextView getRepresentativeTextView()
        {
            return mRepresentativeTextView;
        }

        public int getRequestCode()
        {
            return mRequestCode;
        }

        public GroupViewHolder setRequestCode(int requestCode)
        {
            mRequestCode = requestCode;
            return this;
        }

        public boolean isRepresentative()
        {
            return mRepresentativeTextView != null;
        }

        public boolean tryBinding(GroupEditable editable)
        {
            if (getRepresentativeTextView() == null || editable.getRepresentativeText() == null)
                return false;

            getRepresentativeTextView().setText(editable.getRepresentativeText());
            setRequestCode(editable.getRequestCode());

            return true;
        }
    }

    public static class GroupLister<T extends GroupEditable> extends Lister<T, ComparableMerger<T>>
    {
        private int mMode;
        private List<T> mNoGroupingList;
        private CustomGroupLister<T> mCustomLister;

        public GroupLister(List<T> noGroupingList, int mode)
        {
            mNoGroupingList = noGroupingList;
            mMode = mode;
        }

        public GroupLister(List<T> noGroupingList, int mode, CustomGroupLister<T> customList)
        {
            this(noGroupingList, mode);
            mCustomLister = customList;
        }

        public void offerObliged(EditableListAdapterBase<T> adapter, T object)
        {
            if (adapter.filterItem(object))
                offer(object);
        }

        public void offer(T object)
        {
            if (mCustomLister == null || !mCustomLister.onCustomGroupListing(this, mMode, object)) {
                if (mMode == MODE_GROUP_BY_DATE)
                    offer(object, new DateMerger<>(object.getComparableDate()));
                else
                    mNoGroupingList.add(object);
            }
        }

        public GroupLister<T> setCustomLister(CustomGroupLister<T> customLister)
        {
            mCustomLister = customLister;
            return this;
        }

        public interface CustomGroupLister<T extends GroupEditable>
        {
            boolean onCustomGroupListing(GroupLister<T> lister, int mode, T object);
        }
    }
}
