package com.genonbeta.TrebleShot.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;
import android.view.View;

import com.futuremind.recyclerviewfastscroll.SectionTitleProvider;
import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.MathUtils;
import com.genonbeta.TrebleShot.util.TextUtils;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * created by: Veli
 * date: 12.01.2018 16:55
 */

abstract public class EditableListAdapter<T extends Editable, V extends EditableListAdapter.EditableViewHolder>
		extends RecyclerViewAdapter<T, V>
		implements SectionTitleProvider, EditableListAdapterImpl<T>
{
	public static final int VIEW_TYPE_DEFAULT = 0;

	public static final int MODE_SORT_BY_NAME = 100;
	public static final int MODE_SORT_BY_DATE = 110;
	public static final int MODE_SORT_BY_SIZE = 120;

	public static final int MODE_SORT_ORDER_ASCENDING = 100;
	public static final int MODE_SORT_ORDER_DESCENDING = 110;

	private EditableListFragmentImpl<T> mFragment;
	private ArrayList<T> mItemList = new ArrayList<>();
	private int mSortingCriteria = MODE_SORT_BY_NAME;
	private int mSortingOrderAscending = MODE_SORT_ORDER_ASCENDING;
	private boolean mGridLayoutRequested = false;
	private Comparator<T> mGeneratedComparator;

	public EditableListAdapter(Context context)
	{
		super(context);
	}

	@Override
	public void onUpdate(ArrayList<T> passedItem)
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
				@Override
				public int compare(T toCompare, T compareTo)
				{
					boolean sortingAscending = getSortingOrder() == MODE_SORT_ORDER_ASCENDING;

					T objectFirst = sortingAscending ? toCompare : compareTo;
					T objectSecond = sortingAscending ? compareTo : toCompare;

					switch (getSortingCriteria()) {
						case MODE_SORT_BY_DATE:
							return MathUtils.compare(objectFirst.getComparableDate(), objectSecond.getComparableDate());
						case MODE_SORT_BY_SIZE:
							return MathUtils.compare(objectFirst.getComparableSize(), objectSecond.getComparableSize());
						case MODE_SORT_BY_NAME:
							return objectFirst.getComparableName().compareToIgnoreCase(objectSecond.getComparableName());
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

	@Override
	public int getItemCount()
	{
		return getCount();
	}

	public T getItem(int position)
	{
		return getList().get(position);
	}

	public T getItem(V holder)
	{
		return getList().get(holder.getAdapterPosition());
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	public ArrayList<T> getItemList()
	{
		return mItemList;
	}

	@Override
	public int getItemViewType(int position)
	{
		return VIEW_TYPE_DEFAULT;
	}

	@Override
	public ArrayList<T> getList()
	{
		return getItemList();
	}

	@Override
	public String getSectionTitle(int position)
	{
		return getSectionName(position, getItem(position));
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

	public int getSortingCriteria()
	{
		return mSortingCriteria;
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

	public void setFragment(EditableListFragmentImpl<T> fragmentImpl)
	{
		mFragment = fragmentImpl;
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

	public synchronized void syncSelectionList(ArrayList<T> itemList)
	{
		if (getFragment() == null || getFragment().getSelectionConnection() == null)
			return;

		for (T item : itemList)
			item.setSelectableSelected(mFragment.getSelectionConnection().isSelected(item));
	}

	public static class EditableViewHolder extends ViewHolder
	{
		private View mClickableLayout;

		public EditableViewHolder(View itemView)
		{
			super(itemView);
		}

		public View getClickableView()
		{
			return mClickableLayout == null ? getView() : mClickableLayout;
		}

		public EditableViewHolder setClickableLayout(int resId)
		{
			return setClickableLayout(getView().findViewById(resId));
		}

		public EditableViewHolder setClickableLayout(View clickableLayout)
		{
			mClickableLayout = clickableLayout;
			return this;
		}
	}
}
