package com.genonbeta.TrebleShot.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.util.DetachListener;
import com.genonbeta.TrebleShot.util.PowerfulActionModeSupported;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 21.11.2017 10:12
 */

abstract public class EditableListFragment<T extends Editable, E extends EditableListAdapter<T>>
		extends com.genonbeta.TrebleShot.app.ListFragment<T, E>
		implements PowerfulActionMode.Callback<T>, DetachListener
{
	private PowerfulActionMode.SelectorConnection<T> mSelectionConnection;
	private SharedPreferences mSortingPreferences;
	private Snackbar mRefreshDelayedSnackbar;
	private boolean mRefreshRequested = false;
	private boolean mSortingSupported = true;
	private boolean mDefaultOrderingAscending = true;
	private int mDefaultSortingCriteria = R.id.actions_abs_editable_sort_by_name;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		getAdapter().setFragment(this);
	}

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

		mSortingPreferences = getActivity().getSharedPreferences(Keyword.Local.SETTINGS_SORTING, Context.MODE_PRIVATE);
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
	public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		menu.findItem(R.id.actions_abs_editable_sort_by)
				.setEnabled(isSortingSupported());

		MenuItem multiSelect = menu.findItem(R.id.actions_abs_editable_multi_select);

		if (getSelectionConnection() == null
				&& multiSelect != null)
			multiSelect.setVisible(false);

		MenuItem sortingItem = menu.findItem(getSortingCriteria());

		if (sortingItem == null)
			sortingItem = menu.findItem(R.id.actions_abs_editable_sort_by_name);

		sortingItem.setChecked(true);

		MenuItem orderingItem = menu.findItem(isSortingAscending()
				? R.id.actions_abs_editable_sort_order_ascending
				: R.id.actions_abs_editable_sort_order_descending);

		if (orderingItem == null)
			orderingItem = menu.findItem(R.id.actions_abs_editable_sort_order_ascending);

		orderingItem.setChecked(true);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_abs_editable_multi_select) {
			getPowerfulActionMode().start(this);
		} else if (id == R.id.actions_abs_editable_sort_by_name
				|| id == R.id.actions_abs_editable_sort_by_date
				|| id == R.id.actions_abs_editable_sort_by_size)
			changeSortingCriteria(id);
		else if (id == R.id.actions_abs_editable_sort_order_ascending
				|| id == R.id.actions_abs_editable_sort_order_descending)
			changeOrderingCriteria(id);

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

		getAdapter().notifySelectionChanges();
		return false;
	}

	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		actionMode.getMenuInflater().inflate(R.menu.action_mode_abs_editable, menu);
		return false;
	}

	@Override
	public void onItemChecked(Context context, PowerfulActionMode actionMode, T selectable)
	{
		int selectedSize = getSelectionConnection()
				.getSelectedItemList()
				.size();

		actionMode.setTitle(String.valueOf(selectedSize));
	}

	@Override
	public boolean onActionMenuItemSelected(final Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.action_mode_abs_editable_select_all)
			setSelection(true);
		else if (id == R.id.action_mode_abs_editable_select_none)
			setSelection(false);
		else if (id == R.id.action_mode_abs_editable_preview_selections)
			new SelectionEditorDialog<>(getActivity(), getSelectionConnection().getSelectedItemList())
					.setOnDismissListener(new DialogInterface.OnDismissListener()
					{
						@Override
						public void onDismiss(DialogInterface dialog)
						{
							ArrayList<T> selectedItems = new ArrayList<>(getSelectionConnection().getSelectedItemList());

							for (T selectable : selectedItems)
								if (!selectable.isSelectableSelected())
									getSelectionConnection().setSelected(selectable, false);

							getAdapter().notifySelectionChanges();
						}

					})
					.show();

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

	public void changeSortingCriteria(int id)
	{
		mSortingPreferences.edit().putInt(getClass().getSimpleName() + "SortBy", id).apply();
		refreshList();
	}

	public void changeOrderingCriteria(int id)
	{
		mSortingPreferences.edit().putBoolean(getClass().getSimpleName() + "SortOrder", id == R.id.actions_abs_editable_sort_order_ascending).apply();
		refreshList();
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

	public int getSortingCriteria()
	{
		return mSortingPreferences.getInt(getClass().getSimpleName() + "SortBy", mDefaultSortingCriteria);
	}

	public PowerfulActionMode getPowerfulActionMode()
	{
		return getActivity() != null && getActivity() instanceof PowerfulActionModeSupported
				? ((PowerfulActionModeSupported) getActivity()).getPowerfulActionMode()
				: null;
	}

	public boolean isRefreshLocked()
	{
		return false;
	}

	public boolean isRefreshRequested()
	{
		return mRefreshRequested;
	}

	public boolean isSelectionActivated()
	{
		return getPowerfulActionMode() != null
				&& getPowerfulActionMode().hasActive(this)
				&& getSelectionConnection() != null;
	}

	public boolean isSortingAscending()
	{
		return mSortingPreferences.getBoolean(getClass().getSimpleName() + "SortOrder", mDefaultOrderingAscending);
	}

	public boolean isSortingSupported()
	{
		return mSortingSupported;
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
		if (isRefreshLocked()) {
			setRefreshRequested(true);

			if (mRefreshDelayedSnackbar == null) {
				mRefreshDelayedSnackbar = createSnackbar(R.string.mesg_listRefreshSnoozed);
				mRefreshDelayedSnackbar.setDuration(Snackbar.LENGTH_LONG);
			}

			mRefreshDelayedSnackbar.show();
		} else {
			super.refreshList();

			if (mRefreshDelayedSnackbar != null) {
				mRefreshDelayedSnackbar.dismiss();
				mRefreshDelayedSnackbar = null;
			}
		}
	}

	public boolean setItemSelected(int position)
	{
		if (isSelectionActivated()) {
			getSelectionConnection().setSelected(getSelectableList().get(position));
			getAdapter().notifySelectionChanges();

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

	public void setSortingSupported(boolean sortingSupported)
	{
		mSortingSupported = sortingSupported;
	}

	public void setDefaultSortingCriteria(int criteria)
	{
		mDefaultSortingCriteria = criteria;
	}

	public void setDefaultOrderingAscending(boolean ascending)
	{
		mDefaultOrderingAscending = ascending;
	}
}
