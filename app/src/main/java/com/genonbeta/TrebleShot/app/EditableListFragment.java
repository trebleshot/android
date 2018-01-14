package com.genonbeta.TrebleShot.app;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Selectable;
import com.genonbeta.TrebleShot.util.DetachListener;
import com.genonbeta.TrebleShot.util.PowerfulActionModeSupported;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.TrebleShot.widget.ListAdapter;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 21.11.2017 10:12
 */

abstract public class EditableListFragment<T extends Selectable, E extends EditableListAdapter<T>>
		extends com.genonbeta.TrebleShot.app.ListFragment<T, E>
		implements PowerfulActionMode.Callback<T>, DetachListener
{
	private MenuItem mMultiSelect;
	private PowerfulActionMode.SelectorConnection<T> mSelectionConnection;
	private boolean mRefreshRequested = false;

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		getListView().setDividerHeight(0);

		if (getPowerfulActionMode() != null) {
			mSelectionConnection = new PowerfulActionMode.SelectorConnection<>(getPowerfulActionMode(), this);

			getAdapter().setSelectionConnection(getSelectionConnection());
			getPowerfulActionMode().enableFor(getSelectionConnection());

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
	public void onItemChecked(Context context, PowerfulActionMode actionMode, T selectable)
	{
		int selectedSize = getSelectionConnection()
				.getSelectedItemList()
				.size();

		actionMode.setTitle(String.valueOf(selectedSize));

		if (mMultiSelect != null)
			mMultiSelect.setIcon(selectedSize != getAdapter().getCount() ? R.drawable.ic_select : R.drawable.ic_select_undo);
	}

	@Override
	public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.action_mode_abs_editable_multi_select)
			setSelection();

		return false;
	}

	@Override
	public void onFinish(Context context, PowerfulActionMode actionMode)
	{
		getListView().setPadding(0, 0, 0, 0);
		getListView().setClipToPadding(true);

		setSelection(false);

		loadIfRequested();
	}

	@Override
	public AbsListView getActionModeListView()
	{
		return getListView();
	}

	@Override
	public ArrayList<T> getSelectableList()
	{
		return getAdapter().getList();
	}

	public PowerfulActionMode.SelectorConnection<T> getSelectionConnection()
	{
		return mSelectionConnection;
	}

	public PowerfulActionMode getPowerfulActionMode()
	{
		return getActivity() != null && getActivity() instanceof PowerfulActionModeSupported
				? ((PowerfulActionModeSupported) getActivity()).getPowerfulActionMode()
				: null;
	}

	public boolean isSelectionActivated()
	{
		return getPowerfulActionMode() != null
				&& getPowerfulActionMode().hasActive(this)
				&& getSelectionConnection() != null;
	}

	public boolean isRefreshLocked()
	{
		return false;
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

	public boolean setItemSelected(int position)
	{
		if (isSelectionActivated()) {
			getSelectionConnection().setSelected(getSelectableList().get(position));
			getAdapter().notifyDataSetChanged();

			return true;
		}

		return false;
	}

	public boolean setSelection()
	{
		boolean allSelected = getSelectionConnection().getSelectedItemList().size() != getSelectableList().size();

		setSelection(allSelected);

		return allSelected;
	}

	public void setSelection(boolean selection)
	{
		for (T selectable : getSelectableList())
			getSelectionConnection().setSelected(selectable, selection);

		getAdapter().notifyDataSetChanged();
	}
}
