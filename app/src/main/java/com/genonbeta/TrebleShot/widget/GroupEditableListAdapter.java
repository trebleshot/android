package com.genonbeta.TrebleShot.widget;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.TextView;

import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.android.framework.util.date.DateMerger;
import com.genonbeta.android.framework.util.listing.ComparableMerger;
import com.genonbeta.android.framework.util.listing.Lister;
import com.genonbeta.android.framework.util.listing.Merger;
import com.genonbeta.android.framework.util.listing.merger.StringMerger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * created by: Veli
 * date: 29.03.2018 08:00
 */
abstract public class GroupEditableListAdapter<T extends GroupEditableListAdapter.GroupEditable, V extends GroupEditableListAdapter.GroupViewHolder>
		extends EditableListAdapter<T, V>
{
	public static final int VIEW_TYPE_REPRESENTATIVE = 100;

	public static final int MODE_GROUP_BY_NOTHING = 100;
	public static final int MODE_GROUP_BY_DATE = 110;

	private int mGroupBy;

	public GroupEditableListAdapter(Context context, int groupBy)
	{
		super(context);
		mGroupBy = groupBy;
	}

	abstract protected void onLoad(GroupLister<T> lister);

	abstract protected T onGenerateRepresentative(String representativeText);

	@Override
	public ArrayList<T> onLoad()
	{
		ArrayList<T> loadedList = new ArrayList<>();
		GroupLister<T> groupLister = createLister(loadedList, getGroupBy());

		onLoad(groupLister);

		if (groupLister.getList().size() > 0) {
			Collections.sort(groupLister.getList(), new Comparator<ComparableMerger<T>>()
			{
				@Override
				public int compare(ComparableMerger<T> o1, ComparableMerger<T> o2)
				{
					return o2.compareTo(o1);
				}
			});

			long representativeIdIterator = -1;

			for (ComparableMerger<T> thisMerger : groupLister.getList()) {
				Collections.sort(thisMerger.getBelongings(), getDefaultComparator());

				T generated = onGenerateRepresentative(getRepresentativeText(thisMerger));
				T firstEditable = thisMerger.getBelongings().get(0);

				if (generated != null)
					loadedList.add(generated);

				generated.setSize(thisMerger.getBelongings().size());
				generated.setDate(firstEditable.getComparableDate());
				generated.setRepresentativeText(generated.getRepresentativeText());
				generated.setId(representativeIdIterator--);

				loadedList.addAll(thisMerger.getBelongings());
			}
		} else
			Collections.sort(loadedList, getDefaultComparator());

		return loadedList;
	}

	public GroupLister<T> createLister(ArrayList<T> loadedList, int groupBy)
	{
		return new GroupLister<>(loadedList, groupBy);
	}

	public int getGroupBy()
	{
		return mGroupBy;
	}

	@Override
	public int getItemViewType(int position)
	{
		return getItem(position).getViewType();
	}

	public String getRepresentativeText(Merger merger)
	{
		if (merger instanceof DateMerger)
			return String.valueOf(getSectionNameDate(((DateMerger) merger).getTime()));
		else if (merger instanceof StringMerger)
			return ((StringMerger) merger).getString();

		return merger.toString();
	}

	@NonNull
	@Override
	public String getSectionName(int position, T object)
	{
		if (object.isGroupRepresentative())
			return object.getRepresentativeText();

		switch (getGroupBy()) {
			case MODE_GROUP_BY_DATE:
				return getSectionNameDate(object.getComparableDate());
			default:
				return super.getSectionName(position, object);
		}
	}

	public void setGroupBy(int groupBy)
	{
		mGroupBy = groupBy;
	}

	public interface GroupEditable extends Editable
	{
		int getViewType();

		String getRepresentativeText();

		boolean isGroupRepresentative();

		void setDate(long date);

		void setSize(long size);

		void setRepresentativeText(CharSequence representativeText);

	}

	public static class GroupShareable extends Shareable implements GroupEditable
	{
		public int viewType = EditableListAdapter.VIEW_TYPE_DEFAULT;
		public String representativeText;

		public GroupShareable()
		{
			super();
		}

		public GroupShareable(int viewType, String representativeText)
		{
			this.viewType = viewType;
			this.representativeText = representativeText;
		}

		public GroupShareable(long id, String friendlyName, String fileName, String mimeType, long date, long size, Uri uri)
		{
			super(id, friendlyName, fileName, mimeType, date, size, uri);
		}

		@Override
		public int getViewType()
		{
			return viewType;
		}

		@Override
		public String getRepresentativeText()
		{
			return representativeText;
		}

		public boolean isGroupRepresentative()
		{
			return representativeText != null;
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
		public void setRepresentativeText(CharSequence representativeText)
		{
			this.representativeText = String.valueOf(representativeText);
		}

		@Override
		public boolean setSelectableSelected(boolean selected)
		{
			return !isGroupRepresentative() && super.setSelectableSelected(selected);
		}

		@Override
		public boolean searchMatches(String searchWord)
		{
			if (isGroupRepresentative())
				return TextUtils.searchWord(representativeText, searchWord);

			return super.searchMatches(searchWord);
		}
	}

	public static class GroupViewHolder extends EditableListAdapter.EditableViewHolder
	{
		private TextView mRepresentativeText;

		public GroupViewHolder(View itemView, TextView representativeText)
		{
			super(itemView);
			mRepresentativeText = representativeText;
		}

		public GroupViewHolder(View itemView, int resRepresentativeText)
		{
			this(itemView, (TextView) itemView.findViewById(resRepresentativeText));
		}

		public GroupViewHolder(View itemView)
		{
			super(itemView);
		}

		public TextView getRepresentativeText()
		{
			return mRepresentativeText;
		}

		public boolean isRepresentative()
		{
			return mRepresentativeText != null;
		}

		public boolean tryBinding(GroupEditable editable)
		{
			if (getRepresentativeText() == null || editable.getRepresentativeText() == null)
				return false;

			getRepresentativeText().setText(editable.getRepresentativeText());

			return true;
		}
	}

	public static class GroupLister<T extends GroupEditable> extends Lister<T, ComparableMerger<T>>
	{
		private int mMode;
		private List<T> mNoGroupingList;
		private CustomGroupListener<T> mCustomLister;

		public GroupLister(List<T> noGroupingList, int mode)
		{
			mNoGroupingList = noGroupingList;
			mMode = mode;
		}

		public GroupLister(List<T> noGroupingListList, int mode, CustomGroupListener<T> customList)
		{
			this(noGroupingListList, mode);
			mCustomLister = customList;
		}

		public void offer(T object)
		{
			if (mMode == MODE_GROUP_BY_DATE)
				offer(object, new DateMerger<T>(object.getComparableDate()));
			else if (mMode == MODE_GROUP_BY_NOTHING
					|| mCustomLister == null
					|| !mCustomLister.onCustomGroupListing(this, mMode, object))
				mNoGroupingList.add(object);
		}

		public GroupLister<T> setCustomLister(CustomGroupListener<T> customLister)
		{
			mCustomLister = customLister;
			return this;
		}

		public interface CustomGroupListener<T extends GroupEditable>
		{
			boolean onCustomGroupListing(GroupLister<T> lister, int mode, T object);
		}
	}
}
