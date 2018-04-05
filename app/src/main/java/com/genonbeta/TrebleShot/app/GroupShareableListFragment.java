package com.genonbeta.TrebleShot.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.widget.GroupShareableListAdapter;

import java.util.Map;

/**
 * created by: veli
 * date: 30.03.2018 16:10
 */

public abstract class GroupShareableListFragment<T extends GroupShareableListAdapter.GroupShareable, V extends GroupShareableListAdapter.ViewHolder, E extends GroupShareableListAdapter<T, V>>
		extends ShareableListFragment<T, V, E>
{
	private ArrayMap<String, Integer> mGroupingOptions = new ArrayMap<>();
	private int mDefaultGroupingCriteria = GroupShareableListAdapter.MODE_GROUP_BY_NOTHING;

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		getAdapter().setGroupBy(getGroupingCriteria());
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);

		Map<String, Integer> options = new ArrayMap<>();

		onGroupingOptions(options);

		mGroupingOptions.clear();
		mGroupingOptions.putAll(options);

		if (mGroupingOptions.size() > 0) {
			inflater.inflate(R.menu.actions_abs_group_shareable_list, menu);
			MenuItem groupingItem = menu.findItem(R.id.actions_abs_group_shareable_grouping);

			if (groupingItem != null)
				applyDynamicMenuItems(groupingItem, R.id.actions_abs_group_shareable_group_grouping, mGroupingOptions);
		}
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);
		checkPreferredDynamicItem(menu.findItem(R.id.actions_abs_group_shareable_grouping), getGroupingCriteria(), mGroupingOptions);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getGroupId() == R.id.actions_abs_group_shareable_group_grouping)
			changeGroupingCriteria(item.getOrder());
		else
			return super.onOptionsItemSelected(item);

		return true;
	}

	public void onGroupingOptions(Map<String, Integer> options)
	{
	}

	public void changeGroupingCriteria(int criteria)
	{
		getViewPreferences().edit()
				.putInt(getUniqueSettingKey("GroupBy"), criteria)
				.apply();

		getAdapter().setGroupBy(criteria);

		refreshList();
	}

	public int getGroupingCriteria()
	{
		return getViewPreferences()
				.getInt(getUniqueSettingKey("GroupBy"), mDefaultGroupingCriteria);
	}

	public void setDefaultGroupingCriteria(int groupingCriteria)
	{
		mDefaultGroupingCriteria = groupingCriteria;
	}
}
