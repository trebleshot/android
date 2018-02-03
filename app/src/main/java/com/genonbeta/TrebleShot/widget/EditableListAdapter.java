package com.genonbeta.TrebleShot.widget;

import android.content.Context;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.object.Editable;

import java.util.Comparator;

/**
 * created by: Veli
 * date: 12.01.2018 16:55
 */

abstract public class EditableListAdapter<T extends Editable> extends ListAdapter<T>
{
	private EditableListFragment mFragment;
	private PowerfulActionMode.SelectorConnection mSelectionConnection;

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
						sortingResult = (int) (sortingAscending
								? toCompare.getComparableDate() - compareTo.getComparableDate()
								: compareTo.getComparableDate() - toCompare.getComparableDate()
						);
						break;
					case R.id.actions_abs_editable_sort_by_size:
						sortingResult = (int) (sortingAscending
								? toCompare.getComparableSize() - compareTo.getComparableSize()
								: compareTo.getComparableSize() - toCompare.getComparableSize()
						);
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

	public EditableListFragment getFragment()
	{
		return mFragment;
	}

	public PowerfulActionMode.SelectorConnection<T> getSelectionConnection()
	{
		return mSelectionConnection;
	}

	@Override
	public void notifyDataSetChanged()
	{

		super.notifyDataSetChanged();
	}

	public void setSelectionConnection(PowerfulActionMode.SelectorConnection<T> selectionConnection)
	{
		mSelectionConnection = selectionConnection;
	}

	public void setFragment(EditableListFragment fragment)
	{
		mFragment = fragment;
	}
}
