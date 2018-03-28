package com.genonbeta.TrebleShot.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.util.DetachListener;
import com.genonbeta.TrebleShot.util.PowerfulActionModeSupported;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 21.11.2017 10:12
 */

abstract public class EditableListFragment<T extends Editable, V extends RecyclerViewAdapter.ViewHolder, E extends EditableListAdapter<T, V>>
		extends DynamicRecyclerViewFragment<T, V, E>
		implements PowerfulActionMode.Callback<T>, DetachListener
{
	private PowerfulActionMode.SelectorConnection<T> mSelectionConnection;
	private SharedPreferences mViewPreferences;
	private Snackbar mRefreshDelayedSnackbar;
	private boolean mRefreshRequested = false;
	private boolean mSortingSupported = true;
	private boolean mDefaultOrderingAscending = true;
	private int mDefaultSortingCriteria = R.id.actions_abs_editable_sort_by_name;
	private int mDefaultViewingGridSize = 1;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mViewPreferences = getContext().getSharedPreferences(Keyword.Local.SETTINGS_VIEWING, Context.MODE_PRIVATE);
		getAdapter().setFragment(this);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		if (getPowerfulActionMode() != null) {
			mSelectionConnection = new PowerfulActionMode.SelectorConnection<>(getPowerfulActionMode(), this);

			getAdapter().setSelectionConnection(getSelectionConnection());
			getPowerfulActionMode().enableFor(getSelectionConnection());

			setHasOptionsMenu(true);
		}

		updateGridSize();
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

		MenuItem gridSizeItem = menu.findItem(R.id.actions_abs_editable_grid_size);

		if (gridSizeItem != null) {
			Menu gridSizeMenu = gridSizeItem.getSubMenu();

			for (int i = 1; i < 5; i++)
				gridSizeMenu.add(R.id.actions_abs_editable_group_grid_size, 0, i,
						getContext().getResources().getQuantityString(R.plurals.text_gridRow, i, i));

			gridSizeMenu.setGroupCheckable(R.id.actions_abs_editable_group_grid_size, true, true);
		}
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

		if (!getAdapter().isGridSupported())
			menu.findItem(R.id.actions_abs_editable_grid_size)
					.setVisible(false);

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

		MenuItem gridSizeItem = menu.findItem(R.id.actions_abs_editable_grid_size);

		if (gridSizeItem != null) {
			Menu gridRowMenu = gridSizeItem.getSubMenu();
			int currentRow = getViewingGridSize() - 1;

			if (currentRow < gridRowMenu.size())
				gridRowMenu.getItem(currentRow).setChecked(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();
		int groupId = item.getGroupId();

		if (id == R.id.actions_abs_editable_multi_select)
			getPowerfulActionMode().start(this);
		else if (groupId == R.id.actions_abs_editable_group_sorting)
			changeSortingCriteria(id);
		else if (groupId == R.id.actions_abs_editable_group_sort_order)
			changeOrderingCriteria(id);
		else if (groupId == R.id.actions_abs_editable_group_grid_size)
			changeGridViewSize(item.getOrder());

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

		getAdapter().notifyAllSelectionChanges();
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

							getAdapter().notifyAllSelectionChanges();
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

	public void changeGridViewSize(int gridSize)
	{
		mViewPreferences.edit().putInt(getClass().getSimpleName() + "GridSize", gridSize).apply();
		updateGridSize(gridSize);
	}

	public void changeSortingCriteria(int id)
	{
		mViewPreferences.edit().putInt(getClass().getSimpleName() + "SortBy", id).apply();
		refreshList();
	}

	public void changeOrderingCriteria(int id)
	{
		mViewPreferences.edit().putBoolean(getClass().getSimpleName() + "SortOrder", id == R.id.actions_abs_editable_sort_order_ascending).apply();
		refreshList();
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
		return mViewPreferences.getInt(getClass().getSimpleName() + "SortBy", mDefaultSortingCriteria);
	}

	public PowerfulActionMode getPowerfulActionMode()
	{
		return getActivity() != null && getActivity() instanceof PowerfulActionModeSupported
				? ((PowerfulActionModeSupported) getActivity()).getPowerfulActionMode()
				: null;
	}

	public int getViewingGridSize()
	{
		if (mViewPreferences == null)
			return 1;

		return mViewPreferences.getInt(getClass().getSimpleName() + "GridSize", mDefaultViewingGridSize);
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
		return mViewPreferences.getBoolean(getClass().getSimpleName() + "SortOrder", mDefaultOrderingAscending);
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

	public void setDefaultSortingCriteria(int criteria)
	{
		mDefaultSortingCriteria = criteria;
	}

	public void setDefaultOrderingAscending(boolean ascending)
	{
		mDefaultOrderingAscending = ascending;
	}

	public void setDefaultViewingGridSize(int gridSize)
	{
		mDefaultViewingGridSize = gridSize;
	}

	public boolean setItemSelected(V holder)
	{
		return setItemSelected(holder.getAdapterPosition());
	}

	public boolean setItemSelected(int position)
	{
		if (isSelectionActivated()) {
			getSelectionConnection().setSelected(getSelectableList().get(position));

			getAdapter().syncSelectionList();
			getAdapter().notifyItemChanged(position);

			return true;
		}

		return false;
	}

	public void setRefreshRequested(boolean requested)
	{
		mRefreshRequested = requested;
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

	public boolean updateGridSize()
	{
		return updateGridSize(getViewingGridSize());
	}

	public boolean updateGridSize(int gridSize)
	{
		if (!getAdapter().isGridSupported())
			return false;

		getAdapter().notifyGridSizeUpdate();

		getListView().setLayoutManager(gridSize > 1
				? new GridLayoutManager(getContext(), gridSize)
				: getDefaultLayoutManager());

		getListView().setAdapter(getAdapter());

		return true;
	}
}
