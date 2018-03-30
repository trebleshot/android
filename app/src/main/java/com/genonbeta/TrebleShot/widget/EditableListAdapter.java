package com.genonbeta.TrebleShot.widget;

import android.content.Context;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.util.MathUtils;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * created by: Veli
 * date: 12.01.2018 16:55
 */

abstract public class EditableListAdapter<T extends Editable, V extends RecyclerViewAdapter.ViewHolder> extends RecyclerViewAdapter<T, V>
{
	public static final int VIEW_TYPE_DEFAULT = 0;

	private EditableListFragment mFragment;
	private PowerfulActionMode.SelectorConnection<T> mSelectionConnection;
	private ArrayList<T> mItemList = new ArrayList<>();
	private boolean mGridLayoutRequested = false;

	public EditableListAdapter(Context context)
	{
		super(context);
	}

	public EditableListAdapter(Context context, PowerfulActionMode.SelectorConnection<T> selectorConnection)
	{
		this(context);
		setSelectionConnection(selectorConnection);
	}

	public Comparator<T> getDefaultComparator()
	{
		return new Comparator<T>()
		{
			private int mSortingCriteria = -1;

			@Override
			public int compare(T toCompare, T compareTo)
			{
				boolean sortingAscending = getFragment() == null
						|| getFragment().isSortingAscending();

				int sortingResult = 0;

				switch (getSortingCriteria()) {
					case R.id.actions_abs_editable_sort_by_name:
						sortingResult = sortingAscending
								? toCompare.getComparableName().compareToIgnoreCase(compareTo.getComparableName())
								: compareTo.getComparableName().compareToIgnoreCase(toCompare.getComparableName());
						break;
					case R.id.actions_abs_editable_sort_by_date:
						sortingResult = sortingAscending
								? MathUtils.compare(toCompare.getComparableDate(), compareTo.getComparableDate())
								: MathUtils.compare(compareTo.getComparableDate(), toCompare.getComparableDate());
						break;
					case R.id.actions_abs_editable_sort_by_size:
						sortingResult = sortingAscending
								? MathUtils.compare(toCompare.getComparableSize(), compareTo.getComparableSize())
								: MathUtils.compare(compareTo.getComparableSize(), toCompare.getComparableSize());
						break;
				}

				return sortingResult;
			}

			private int getSortingCriteria()
			{
				if (mSortingCriteria != -1)
					return mSortingCriteria;

				return mSortingCriteria = getFragment() != null
						? getFragment().getSortingCriteria() : R.id.actions_abs_editable_sort_by_name;
			}
		};
	}

	@Override
	public void onUpdate(ArrayList<T> passedItem)
	{
		synchronized (getItemList()) {
			getItemList().clear();
			getItemList().addAll(passedItem);

			syncSelectionList(getItemList());
		}
	}

	@Override
	public int getCount()
	{
		return getItemList().size();
	}

	public EditableListFragment getFragment()
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

	public PowerfulActionMode.SelectorConnection<T> getSelectionConnection()
	{
		return mSelectionConnection;
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

	public void setSelectionConnection(PowerfulActionMode.SelectorConnection<T> selectionConnection)
	{
		mSelectionConnection = selectionConnection;
	}

	public void setFragment(EditableListFragment fragment)
	{
		mFragment = fragment;
	}

	public synchronized void syncSelectionList()
	{
		synchronized (getItemList()) {
			syncSelectionList(getItemList());
		}
	}

	public synchronized void syncSelectionList(ArrayList<T> itemList)
	{
		if (getSelectionConnection() != null)
			for (T item : itemList)
				item.setSelectableSelected(getSelectionConnection().isSelected(item));
	}
}
