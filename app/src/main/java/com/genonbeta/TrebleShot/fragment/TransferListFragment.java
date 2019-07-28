/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.FilePickerActivity;
import com.genonbeta.TrebleShot.adapter.TransferListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DialogUtils;
import com.genonbeta.TrebleShot.dialog.SelectAssigneeDialog;
import com.genonbeta.TrebleShot.dialog.TransferInfoDialog;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.widget.PowerfulActionMode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

public class TransferListFragment
		extends GroupEditableListFragment<TransferListAdapter.AbstractGenericItem, GroupEditableListAdapter.GroupViewHolder, TransferListAdapter>
		implements TitleSupport, Activity.OnBackPressedListener
{
	public static final String TAG = "TransferListFragment";

	public static final String ARG_DEVICE_ID = "argDeviceId";
	public static final String ARG_GROUP_ID = "argGroupId";
	public static final String ARG_TYPE = "argType";
	public static final String ARG_PATH = "argPath";

	public static final int REQUEST_CHOOSE_FOLDER = 1;

	private TransferGroup mHeldGroup;
	private String mLastKnownPath;

	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
				AccessDatabase.BroadcastData data = AccessDatabase.toData(intent);
				if (AccessDatabase.TABLE_TRANSFER.equals(data.tableName)
						|| AccessDatabase.TABLE_TRANSFERGROUP.equals(data.tableName))
					refreshList();
			}
		}
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setFilteringSupported(true);
		setDefaultOrderingCriteria(TransferListAdapter.MODE_SORT_ORDER_ASCENDING);
		setDefaultSortingCriteria(TransferListAdapter.MODE_SORT_BY_NAME);
		setDefaultGroupingCriteria(TransferListAdapter.MODE_GROUP_BY_DEFAULT);
		setDefaultSelectionCallback(new SelectionCallback(this));
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_compare_arrows_white_24dp);

		Bundle args = getArguments();

		if (args != null && args.containsKey(ARG_GROUP_ID)) {
			goPath(args.getString(ARG_PATH), args.getLong(ARG_GROUP_ID),
					args.getString(ARG_DEVICE_ID), args.getString(ARG_TYPE));
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mReceiver, new IntentFilter(AccessDatabase.ACTION_DATABASE_CHANGE));
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mReceiver);
	}

	@Override
	public TransferListAdapter onAdapter()
	{
		final AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder> quickActions = new AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder>()
		{
			@Override
			public void onQuickActions(final GroupEditableListAdapter.GroupViewHolder clazz)
			{
				if (!clazz.isRepresentative()) {
					registerLayoutViewClicks(clazz);

					clazz.getView().findViewById(R.id.layout_image).setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							if (getSelectionConnection() != null)
								getSelectionConnection().setSelected(clazz.getAdapterPosition());
						}
					});
				}
			}
		};

		return new TransferListAdapter(getActivity())
		{
			@NonNull
			@Override
			public GroupEditableListAdapter.GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
			{
				return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
			}
		};
	}

	@Override
	public int onGridSpanSize(int viewType, int currentSpanSize)
	{
		return viewType == TransferListAdapter.VIEW_TYPE_REPRESENTATIVE ? currentSpanSize
				: super.onGridSpanSize(viewType, currentSpanSize);
	}

	@Override
	public boolean onDefaultClickAction(GroupEditableListAdapter.GroupViewHolder holder)
	{
		try {
			final TransferObject transferObject = getAdapter().getItem(holder);
			new TransferInfoDialog(getActivity(), getTransferGroup(), transferObject,
					getAdapter().getDeviceId()).show();

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	@Override
	protected void onListRefreshed()
	{
		super.onListRefreshed();

		String pathOnTrial = getAdapter().getPath();

		if (!(mLastKnownPath == null && getAdapter().getPath() == null)
				&& (mLastKnownPath != null && !mLastKnownPath.equals(pathOnTrial)))
			getListView().scrollToPosition(0);

		mLastKnownPath = pathOnTrial;
	}

	@Override
	public boolean onBackPressed()
	{
		String path = getAdapter().getPath();

		if (path == null) {
			if (getSelectionCallback() != null
					&& getSelectionConnection() != null
					&& getSelectionConnection().getMode().hasActive(getSelectionCallback())) {
				getSelectionConnection().getMode().finish(getSelectionCallback());
				return true;
			} else
				return false;
		}

		int slashPos = path.lastIndexOf(File.separator);

		goPath(slashPos == -1 && path.length() > 0 ? null : path.substring(0, slashPos));

		return true;
	}

	public void changeSavePath(String initialPath)
	{
		startActivityForResult(new Intent(getActivity(), FilePickerActivity.class)
				.setAction(FilePickerActivity.ACTION_CHOOSE_DIRECTORY)
				.putExtra(FilePickerActivity.EXTRA_START_PATH, initialPath)
				.putExtra(FilePickerActivity.EXTRA_ACTIVITY_TITLE, getString(R.string.butn_saveTo)), REQUEST_CHOOSE_FOLDER);
	}


	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_transfers);
	}

	@Override
	public boolean performLayoutClick(GroupEditableListAdapter.GroupViewHolder holder)
	{
		try {
			final TransferObject transferObject = getAdapter().getItem(holder);

			if (transferObject instanceof TransferListAdapter.DetailsTransferFolder) {
				final List<ShowingAssignee> list = TransferUtils.loadAssigneeList(getContext(),
						getTransferGroup().id, null);

				DialogInterface.OnClickListener listClickListener = new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						getAdapter().setAssignee(list.get(which));
						getAdapter().setPath(getAdapter().getPath());
						refreshList();
					}
				};

				DialogInterface.OnClickListener noLimitListener = new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						getAdapter().setAssignee(null);
						getAdapter().setPath(getAdapter().getPath());
						refreshList();
					}
				};

				SelectAssigneeDialog dialog = new SelectAssigneeDialog(getActivity(), list,
						listClickListener);

				dialog.setTitle(R.string.text_limitTo)
						.setNeutralButton(R.string.butn_none, noLimitListener)
						.show();
			} else if (transferObject instanceof TransferListAdapter.StorageStatusItem) {
				final TransferListAdapter.StorageStatusItem statusItem = (TransferListAdapter.StorageStatusItem) transferObject;

				if (statusItem.hasIssues(getAdapter())) {
					AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

					builder.setMessage(getContext().getString(R.string.mesg_notEnoughSpace));
					builder.setNegativeButton(R.string.butn_close, null);

					builder.setPositiveButton(R.string.butn_saveTo, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							changeSavePath(statusItem.directory);
						}
					});

					builder.show();
				} else
					changeSavePath(statusItem.directory);
			} else if (transferObject instanceof TransferListAdapter.TransferFolder) {
				getAdapter().setPath(transferObject.directory);
				refreshList();

				if (getSelectionCallback() != null && getSelectionCallback().isSelectionActivated() && !AppUtils.getDefaultPreferences(getContext()).getBoolean("helpFolderSelection", false))
					createSnackbar(R.string.mesg_helpFolderSelection)
							.setAction(R.string.butn_gotIt, new View.OnClickListener()
							{
								@Override
								public void onClick(View v)
								{
									AppUtils.getDefaultPreferences(getContext())
											.edit()
											.putBoolean("helpFolderSelection", true)
											.apply();
								}
							})
							.show();
			} else
				return super.performLayoutClick(holder);

			return true;
		} catch (Exception e) {

		}

		return false;
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (data != null) {
			if (resultCode == Activity.RESULT_OK) {
				switch (requestCode) {
					case REQUEST_CHOOSE_FOLDER:
						if (data.hasExtra(FilePickerActivity.EXTRA_CHOSEN_PATH)) {
							final Uri selectedPath = data.getParcelableExtra(FilePickerActivity.EXTRA_CHOSEN_PATH);

							if (selectedPath.toString().equals(getTransferGroup().savePath)) {
								createSnackbar(R.string.mesg_pathSameError).show();
							} else {
								AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

								builder.setTitle(R.string.ques_checkOldFiles);
								builder.setMessage(R.string.text_checkOldFiles);

								builder.setNeutralButton(R.string.butn_cancel, null);
								builder.setNegativeButton(R.string.butn_skip, new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface dialogInterface, int i)
									{
										updateSavePath(selectedPath.toString());
									}
								});

								builder.setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface dialogInterface, int i)
									{
										new WorkerService.RunningTask()
										{
											@Override
											public void onRun()
											{
												TransferUtils.pauseTransfer(getContext(), mHeldGroup,
														TransferObject.Type.INCOMING);

												List<TransferObject> checkList = AppUtils.getDatabase(getService()).
														castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
																.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
																				+ AccessDatabase.FIELD_TRANSFER_TYPE + "=?",
																		String.valueOf(getTransferGroup().id), TransferObject.Type.INCOMING.toString()), TransferObject.class);

												TransferGroup pseudoGroup = new TransferGroup(getTransferGroup().id);

												try {
													// Illustrate new change to build the structure accordingly
													AppUtils.getDatabase(getService()).reconstruct(pseudoGroup);
													pseudoGroup.savePath = selectedPath.toString();

													for (TransferObject transferObject : checkList) {
														if (getInterrupter().interrupted())
															throw new InterruptedException();

														DocumentFile file = null;
														DocumentFile pseudoFile = null;

														publishStatusText(transferObject.name);

														try {
															file = FileUtils.getIncomingPseudoFile(getService(), transferObject, getTransferGroup(), false);
															pseudoFile = FileUtils.getIncomingPseudoFile(getService(), transferObject, pseudoGroup, true);
														} catch (Exception e) {
															continue;
														}

														if (file != null && pseudoFile != null) {
															if (file.canWrite())
																FileUtils.move(getService(), file, pseudoFile, getInterrupter());
															else
																throw new IOException("Failed to access: " + file.getUri());
														}

													}

													updateSavePath(selectedPath.toString());
												} catch (Exception e) {
													e.printStackTrace();
												}
											}
										}.setTitle(getString(R.string.mesg_organizingFiles))
												.setIconRes(R.drawable.ic_compare_arrows_white_24dp_static)
												.run(getActivity());
									}
								});

								builder.show();
							}
						}

						break;
				}
			}
		}
	}

	public TransferGroup getTransferGroup()
	{
		if (mHeldGroup == null) {
			mHeldGroup = new TransferGroup(getArguments().getLong(ARG_GROUP_ID, -1));

			try {
				AppUtils.getDatabase(getContext()).reconstruct(mHeldGroup);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		return mHeldGroup;
	}

	public void goPath(String path, long groupId, String deviceId, String type)
	{
		if (deviceId != null && type != null)
			try {
				ShowingAssignee assignee = new ShowingAssignee(groupId, deviceId,
						TransferObject.Type.valueOf(type));

				AppUtils.getDatabase(getContext()).reconstruct(assignee);
				TransferUtils.loadAssigneeInfo(getContext(), assignee);

				getAdapter().setAssignee(assignee);
			} catch (Exception ignored) {
			}

		goPath(path, groupId);
	}

	public void goPath(String path, long groupId)
	{
		getAdapter().setGroupId(groupId);
		goPath(path);
	}

	public void goPath(String path)
	{
		getAdapter().setPath(path);
		refreshList();
	}

	public void updateSavePath(String selectedPath)
	{
		TransferGroup group = getTransferGroup();

		group.savePath = selectedPath;
		AppUtils.getDatabase(getContext()).publish(group);

		if (getActivity() != null && isAdded())
			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					createSnackbar(R.string.mesg_pathSaved).show();
				}
			});
	}

	private static class SelectionCallback extends EditableListFragment.SelectionCallback<TransferListAdapter.AbstractGenericItem>
	{
		private TransferListAdapter mAdapter;

		public SelectionCallback(TransferListFragment fragment)
		{
			super(fragment);
			mAdapter = fragment.getAdapter();
		}

		@Override
		public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
		{
			super.onCreateActionMenu(context, actionMode, menu);
			actionMode.getMenuInflater().inflate(R.menu.action_mode_transfer, menu);
			return true;
		}

		@Override
		public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
		{
			int id = item.getItemId();

			final ArrayList<TransferListAdapter.AbstractGenericItem> selectionList = new ArrayList<>(getFragment().getSelectionConnection().getSelectedItemList());

			if (id == R.id.action_mode_transfer_delete)
				DialogUtils.showRemoveTransferObjectListDialog(getFragment().getActivity(), selectionList);
			else
				return super.onActionMenuItemSelected(context, actionMode, item);

			return true;
		}
	}
}
