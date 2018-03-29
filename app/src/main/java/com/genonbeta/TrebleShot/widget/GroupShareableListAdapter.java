package com.genonbeta.TrebleShot.widget;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.listing.ComparableMerger;
import com.genonbeta.TrebleShot.util.listing.Lister;

import java.util.ArrayList;

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
	public static final int MODE_GROUP_BY_STRING = 2;

	private int mGroupBy;

	public GroupShareableListAdapter(Context context, int groupBy)
	{
		super(context);
		mGroupBy = groupBy;
	}

	abstract protected void onLoad(@Nullable Lister<T, ComparableMerger<T>> lister, ArrayList<T> loadedList);

	// FIXME: 29.03.2018
	/*
	@Override
	public ArrayList<T> onLoad()
	{
		return null;
	}*/

	@Override
	public int getItemViewType(int position)
	{
		return getItem(position).viewType;
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
}
