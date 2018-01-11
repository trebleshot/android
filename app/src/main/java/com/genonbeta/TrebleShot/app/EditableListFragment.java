package com.genonbeta.TrebleShot.app;

import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.util.DetachListener;
import com.genonbeta.TrebleShot.util.PowerfulActionModeSupported;
import com.genonbeta.TrebleShot.widget.ListAdapter;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;

/**
 * created by: Veli
 * date: 21.11.2017 10:12
 */

abstract public class EditableListFragment<T, E extends ListAdapter<T>>
		extends com.genonbeta.TrebleShot.app.ListFragment<T, E>
		implements PowerfulActionMode.Callback, DetachListener
{
	private MenuItem mMultiSelect;
	private boolean mRefreshRequested = false;

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		getListView().setDividerHeight(0);

		if (getPowerfulActionMode() != null) {
			getPowerfulActionMode().enableFor(this);
			setHasOptionsMenu(true);
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		refreshList();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_abs_editable_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_abs_editable_multi_select) {
			getPowerfulActionMode().start(this);
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPrepareDetach()
	{
		if (getPowerfulActionMode() != null)
			getPowerfulActionMode().finish(this);
	}

	@Override
	public boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode)
	{
		getListView().setPadding(0, 0, 0, 200);
		getListView().setClipToPadding(false);

		actionMode.setTitle(String.valueOf(0));

		return false;
	}

	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		actionMode.getMenuInflater().inflate(R.menu.action_mode_abs_editable, menu);
		mMultiSelect = menu.findItem(R.id.action_mode_abs_editable_multi_select);
		return false;
	}

	@Override
	public void onItemChecked(Context context, PowerfulActionMode actionMode, int position, boolean isSelected)
	{
		if (mMultiSelect != null)
			mMultiSelect.setIcon(getListView().getCheckedItemCount() != getAdapter().getCount() ? R.drawable.ic_select : R.drawable.ic_select_undo);
	}

	@Override
	public void onFinish(Context context, PowerfulActionMode actionMode)
	{
		getListView().setPadding(0, 0, 0, 0);
		getListView().setClipToPadding(true);

		loadIfRequested();
	}

	@Override
	public AbsListView getActionModeListView()
	{
		return getListView();
	}

	public PowerfulActionMode getPowerfulActionMode()
	{
		return getActivity() != null && getActivity() instanceof PowerfulActionModeSupported
				? ((PowerfulActionModeSupported) getActivity()).getPowerfulActionMode()
				: null;
	}

	public boolean isRefreshLocked()
	{
		return getPowerfulActionMode() != null
				&& getPowerfulActionMode().hasActive(this);
	}

	public boolean isRefreshRequested()
	{
		return mRefreshRequested;
	}

	protected boolean loadIfRequested()
	{
		boolean refreshed = isRefreshRequested();

		setRefreshRequested(false);

		if (refreshed)
			refreshList();

		return refreshed;
	}

	public void setRefreshRequested(boolean requested)
	{
		mRefreshRequested = requested;
	}

	@Override
	public void refreshList()
	{
		if (isRefreshLocked())
			setRefreshRequested(true);
		else
			super.refreshList();
	}

	public void setSelection(boolean selection)
	{
		PowerfulActionMode powerfulActionMode = getPowerfulActionMode();

		for (int position = 0; position < getAdapter().getCount(); position++)
			powerfulActionMode.check(this, position, selection);
	}
}
