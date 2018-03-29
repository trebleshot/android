package com.genonbeta.TrebleShot.widget;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.TextView;

import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.date.DateMerger;
import com.genonbeta.TrebleShot.util.listing.ComparableMerger;
import com.genonbeta.TrebleShot.util.listing.Lister;
import com.genonbeta.TrebleShot.util.listing.Merger;
import com.genonbeta.TrebleShot.util.listing.merger.StringMerger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * created by: Veli
 * date: 29.03.2018 08:00
 */
abstract public class GroupShareableListAdapter<T extends GroupShareableListAdapter.GroupShareable, V extends GroupShareableListAdapter.ViewHolder>
		extends ShareableListAdapter<T, V>
{
	public static final int VIEW_TYPE_REPRESENTATIVE = 1;

	public static final int MODE_GROUP_BY_NOTHING = 0;
	public static final int MODE_GROUP_BY_DATE = 1;

	private int mGroupBy;

	public GroupShareableListAdapter(Context context, int groupBy)
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
		GroupLister<T> groupLister = new GroupLister<>(loadedList, mGroupBy);

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

			for (ComparableMerger<T> thisMerger : groupLister.getList()) {
				Collections.sort(thisMerger.getBelongings(), getDefaultComparator());

				T generated = onGenerateRepresentative(getRepresentativeText(thisMerger));

				if (generated != null)
					loadedList.add(generated);

				loadedList.addAll(thisMerger.getBelongings());
			}
		}

		return loadedList;
	}

	public int getGroupBy()
	{
		return mGroupBy;
	}

	@Override
	public int getItemViewType(int position)
	{
		return getItem(position).viewType;
	}

	public String getRepresentativeText(Merger merger)
	{
		if (merger instanceof DateMerger)
			return String.valueOf(DateUtils.formatDateTime(getContext(), ((DateMerger) merger).getTime(), DateUtils.FORMAT_SHOW_DATE));
		else if (merger instanceof StringMerger)
			return ((StringMerger) merger).getString();

		return merger.toString();
	}

	public static class GroupShareable extends Shareable
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

		public GroupShareable(String friendlyName, String fileName, String mimeType, long date, long size, Uri uri)
		{
			super(friendlyName, fileName, mimeType, date, size, uri);
		}

		public boolean isGroupRepresentative(boolean isGroupRepresentative, int viewType)
		{
			return isGroupRepresentative;
		}

		@Override
		public boolean setSelectableSelected(boolean selected)
		{
			return viewType != VIEW_TYPE_REPRESENTATIVE && super.setSelectableSelected(selected);
		}

		@Override
		public boolean searchMatches(String searchWord)
		{
			return viewType != VIEW_TYPE_REPRESENTATIVE && super.searchMatches(searchWord);
		}
	}

	public static class ViewHolder extends RecyclerViewAdapter.ViewHolder
	{
		private TextView mRepresentativeText;

		public ViewHolder(View itemView, TextView representativeText)
		{
			super(itemView);
			mRepresentativeText = representativeText;
		}

		public ViewHolder(View itemView, int resRepresentativeText)
		{
			this(itemView, (TextView) itemView.findViewById(resRepresentativeText));
		}

		public ViewHolder(View itemView)
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

		public boolean tryBinding(GroupShareable shareable)
		{
			if (mRepresentativeText == null || shareable.representativeText == null)
				return false;

			mRepresentativeText.setText(shareable.representativeText);

			return true;
		}
	}

	public static class GroupLister<T extends GroupShareable> extends Lister<T, ComparableMerger<T>>
	{
		private int mMode;
		private List<T> mNoGroupingList;

		public GroupLister(List<T> noGroupingList, int mode)
		{
			mNoGroupingList = noGroupingList;
			mMode = mode;
		}

		public void offer(T object)
		{
			if (mMode == MODE_GROUP_BY_DATE)
				super.offer(object, new DateMerger<T>(object.date * 1000));
			else
				mNoGroupingList.add(object);
		}
	}
}
