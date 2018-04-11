package com.genonbeta.TrebleShot.activity;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.adapter.TransactionListAdapter;
import com.genonbeta.TrebleShot.adapter.TransferAssigneeListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.Fragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.TransactionGroupInfoDialog;
import com.genonbeta.TrebleShot.fragment.TransactionListFragment;
import com.genonbeta.TrebleShot.fragment.TransferAssigneeListFragment;
import com.genonbeta.TrebleShot.io.DocumentFile;
import com.genonbeta.TrebleShot.io.LocalDocumentFile;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.PowerfulActionModeSupported;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;
import com.genonbeta.android.database.SQLQuery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/23/17 1:43 PM
 */

public class TransactionActivity
		extends Activity
		implements PowerfulActionModeSupported
{
	public static final String TAG = TransactionActivity.class.getSimpleName();
	public static final int JOB_FILE_FIX = 1;

	public static final String ACTION_LIST_TRANSFERS = "com.genonbeta.TrebleShot.action.LIST_TRANSFERS";
	public static final String EXTRA_GROUP_ID = "extraGroupId";

	private TransferGroup mGroup;
	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
					&& intent.hasExtra(AccessDatabase.EXTRA_CHANGE_TYPE)
					&& AccessDatabase.TABLE_TRANSFERGROUP.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME))
					&& AccessDatabase.TYPE_REMOVE.equals(intent.getStringExtra(AccessDatabase.EXTRA_CHANGE_TYPE))) {
				reconstructGroup();
				updateCalculations();
			}
		}
	};

	private TransferAssigneeListFragment mAssigneeFragment = new TransferAssigneeListFragment();
	private TransactionDetailsFragment mDetailsFragment = new TransactionDetailsFragment();

	private TransferGroup.Index mTransactionIndex = new TransferGroup.Index();
	private PowerfulActionMode mPowafulActionMode;
	private MenuItem mInfoMenu;
	private MenuItem mStartMenu;
	private MenuItem mRetryMenu;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_transaction);

		mPowafulActionMode = findViewById(R.id.activity_transaction_action_mode);

		final Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		final TabLayout tabLayout = findViewById(R.id.activity_transaction_tab_layout);
		final ViewPager viewPager = findViewById(R.id.activity_transaction_view_pager);
		final PagerAdapter pagerAdapter = new PagerAdapter(getSupportFragmentManager());
		final TransactionExplorerFragment transactionFragment = new TransactionExplorerFragment();

		if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_GROUP_ID)) {
			TransferGroup group = new TransferGroup(getIntent().getIntExtra(EXTRA_GROUP_ID, -1));

			try {
				getDatabase().reconstruct(group);

				mGroup = group;

				mAssigneeFragment.setGroup(mGroup);
				mDetailsFragment.setGroup(mGroup);
				mDetailsFragment.setIndex(getIndex());

				transactionFragment.goPath(mGroup.groupId, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (mGroup == null)
			finish();
		else {
			Bundle args = new Bundle();

			args.putInt(TransactionExplorerFragment.ARG_GROUP_ID, mGroup.groupId);
			args.putString(TransactionExplorerFragment.ARG_PATH, null);

			transactionFragment.setArguments(args);

			tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

			pagerAdapter.add(mDetailsFragment, tabLayout);
			pagerAdapter.add(transactionFragment, tabLayout);
			pagerAdapter.add(mAssigneeFragment, tabLayout);

			viewPager.setAdapter(pagerAdapter);
			viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

			tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
			{
				@Override
				public void onTabSelected(TabLayout.Tab tab)
				{
					viewPager.setCurrentItem(tab.getPosition());
				}

				@Override
				public void onTabUnselected(final TabLayout.Tab tab)
				{

				}

				@Override
				public void onTabReselected(TabLayout.Tab tab)
				{

				}
			});

			mPowafulActionMode.setOnSelectionTaskListener(new PowerfulActionMode.OnSelectionTaskListener()
			{
				@Override
				public void onSelectionTask(boolean started, PowerfulActionMode actionMode)
				{
					toolbar.setVisibility(!started ? View.VISIBLE : View.GONE);
				}
			});
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		registerReceiver(mReceiver, new IntentFilter(AccessDatabase.ACTION_DATABASE_CHANGE));
		reconstructGroup();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unregisterReceiver(mReceiver);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.actions_transaction, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		mInfoMenu = menu.findItem(R.id.actions_transaction_show_info);
		mStartMenu = menu.findItem(R.id.actions_transaction_resume_all);
		mRetryMenu = menu.findItem(R.id.actions_transaction_retry_all);

		if (!getIndex().calculated)
			updateCalculations();
		else
			showMenus();

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_transaction_resume_all) {
			resumeReceiving();
		} else if (id == R.id.actions_transaction_retry_all) {
			ContentValues contentValues = new ContentValues();

			contentValues.put(AccessDatabase.FIELD_TRANSFER_FLAG, TransferObject.Flag.PENDING.toString());

			getDatabase().update(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
					.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
									+ AccessDatabase.FIELD_TRANSFER_FLAG + "=? AND "
									+ AccessDatabase.FIELD_TRANSFER_TYPE + "=?",
							String.valueOf(mGroup.groupId),
							TransferObject.Flag.INTERRUPTED.toString(),
							TransferObject.Type.INCOMING.toString()), contentValues);

			createSnackbar(R.string.mesg_retryAllInfo)
					.show();
		} else if (id == R.id.actions_transaction_show_info)
			new TransactionGroupInfoDialog(this, getDatabase(), getDefaultPreferences(), mGroup, getIndex())
					.show();
		else
			return super.onOptionsItemSelected(item);

		return true;
	}

	public boolean calculateSpace()
	{
		DocumentFile documentFile = FileUtils.getSavePath(this, getDefaultPreferences(), mGroup);

		long freeSpace = documentFile instanceof LocalDocumentFile
				? ((LocalDocumentFile) documentFile).getFile().getFreeSpace()
				: -1;

		return freeSpace == -1
				|| freeSpace >= getIndex().incoming;
	}

	private Snackbar createSnackbar(int resId, Object... objects)
	{
		return Snackbar.make(findViewById(R.id.activity_transaction_view_pager), getString(resId, objects), Snackbar.LENGTH_LONG);
	}

	public TransferGroup.Index getIndex()
	{
		return mTransactionIndex;
	}

	@Override
	public PowerfulActionMode getPowerfulActionMode()
	{
		return mPowafulActionMode;
	}

	public void reconstructGroup()
	{
		try {
			getDatabase().reconstruct(mGroup);
		} catch (Exception e) {
			e.printStackTrace();
			finish();
		}
	}

	private void showMenus()
	{
		if (!calculateSpace()) {
			mInfoMenu.setTitle(R.string.mesg_notEnoughSpace);
			MenuItemCompat.setIconTintList(mInfoMenu, ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.notEnoughSpaceMenuTint)));
		}

		boolean hasIncoming = getIndex().incomingCount > 0;

		mStartMenu.setVisible(hasIncoming);
		mRetryMenu.setVisible(hasIncoming);

		if (mDetailsFragment != null && mDetailsFragment.isAdded())
			mDetailsFragment.updateViewState();
	}

	private void resumeReceiving()
	{
		if (mAssigneeFragment.getAdapter() == null || mAssigneeFragment.getAdapter().getItemCount() == 0) {
			createSnackbar(R.string.mesg_noReceiverOrSender)
					.show();
			return;
		}

		final TransferAssigneeListAdapter.ShowingAssignee assignee = mAssigneeFragment.getAdapter().getList().get(0);

		try {
			getDatabase().reconstruct(new NetworkDevice.Connection(assignee));
			TransferUtils.resumeTransfer(getApplicationContext(), mGroup, assignee);
		} catch (Exception e) {
			e.printStackTrace();

			createSnackbar(R.string.mesg_transferConnectionNotSetUpFix)
					.setAction(R.string.butn_setUp, new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							TransferUtils.changeConnection(TransactionActivity.this, getDatabase(), mGroup, assignee.device, new TransferUtils.ConnectionUpdatedListener()
							{
								@Override
								public void onConnectionUpdated(NetworkDevice.Connection connection, TransferGroup.Assignee assignee)
								{
									createSnackbar(R.string.mesg_connectionUpdated, TextUtils.getAdapterName(getApplicationContext(), connection))
											.show();
								}
							});
						}
					}).show();
		}
	}

	public void updateCalculations()
	{
		if (mInfoMenu != null) {
			new Handler(Looper.myLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					getDatabase().calculateTransactionSize(mGroup.groupId, getIndex());
					showMenus();
				}
			});
		}
	}

	public static void startInstance(Context context, int groupId)
	{
		context.startActivity(new Intent(context, TransactionActivity.class)
				.setAction(ACTION_LIST_TRANSFERS)
				.putExtra(EXTRA_GROUP_ID, groupId)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

	private static class TransactionPathResolverRecyclerAdapter extends PathResolverRecyclerAdapter<String>
	{
		public TransactionPathResolverRecyclerAdapter(Context context)
		{
			super(context);
		}

		public void goTo(String[] paths)
		{
			getList().clear();

			StringBuilder mergedPath = new StringBuilder();

			getList().add(new Holder.Index<>(getContext().getString(R.string.text_home), R.drawable.ic_home_black_24dp, (String) null));

			if (paths != null)
				for (String path : paths) {
					if (path.length() == 0)
						continue;

					if (mergedPath.length() > 0)
						mergedPath.append(File.separator);

					mergedPath.append(path);

					getList().add(new Holder.Index<>(path, mergedPath.toString()));
				}
		}

	}

	public class PagerAdapter extends FragmentPagerAdapter
	{
		private ArrayList<Fragment> mFragments = new ArrayList<>();

		public PagerAdapter(FragmentManager fm)
		{
			super(fm);
		}

		public void add(Fragment fragment)
		{
			mFragments.add(fragment);
		}

		public void add(Fragment fragment, TabLayout tabLayout)
		{
			add(fragment);

			if (fragment instanceof TitleSupport)
				tabLayout.addTab(tabLayout.newTab().setText(((TitleSupport) fragment).getTitle(getApplicationContext())));
		}

		@Override
		public Fragment getItem(int position)
		{
			return mFragments.get(position);
		}

		@Override
		public int getCount()
		{
			return mFragments.size();
		}
	}

	public static class TransactionDetailsFragment
			extends Fragment
			implements TitleSupport
	{
		public static final int REQUEST_CHOOSE_FOLDER = 1;

		private TransferGroup mGroup;
		private TransferGroup.Index mIndex;

		private View mRemoveView;
		private View mShowFiles;
		private View mSaveTo;

		@Nullable
		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
		{
			View view = inflater.inflate(R.layout.layout_transaction_details, container, false);

			mRemoveView = view.findViewById(R.id.layout_transaction_details_remove);
			mShowFiles = view.findViewById(R.id.layout_transaction_details_show_files);
			mSaveTo = view.findViewById(R.id.layout_transaction_details_save_to);

			mRemoveView.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

					dialog.setTitle(R.string.ques_removeAll);
					dialog.setMessage(R.string.text_removeCertainPendingTransfersSummary);

					dialog.setNegativeButton(R.string.butn_cancel, null);
					dialog.setPositiveButton(R.string.butn_removeAll, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialog, int which)
						{
							getDatabase().remove(new TransferGroup(mGroup.groupId));
						}
					});

					dialog.show();
				}
			});

			mSaveTo.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					startActivityForResult(new Intent(getActivity(), FilePickerActivity.class)
							.setAction(FilePickerActivity.ACTION_CHOOSE_DIRECTORY), REQUEST_CHOOSE_FOLDER);
				}
			});

			mShowFiles.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					startActivity(new Intent(getActivity(), HomeActivity.class)
							.setAction(HomeActivity.ACTION_OPEN_RECEIVED_FILES)
							.putExtra(HomeActivity.EXTRA_FILE_PATH, FileUtils.getSavePath(getContext(), getDefaultPreferences(), mGroup).getUri()));
				}
			});

			return view;
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

								if (selectedPath.toString().equals(mGroup.savePath)) {
									createSnackbar(R.string.mesg_pathSameError).show();
								} else {
									AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

									builder.setTitle(R.string.ques_checkOldFiles);
									builder.setMessage(R.string.text_checkOldFiles);

									builder.setNeutralButton(R.string.butn_cancel, null);
									builder.setNegativeButton(R.string.butn_reject, new DialogInterface.OnClickListener()
									{
										@Override
										public void onClick(DialogInterface dialogInterface, int i)
										{
											updateSavePath(selectedPath.toString());
										}
									});

									builder.setPositiveButton(R.string.butn_accept, new DialogInterface.OnClickListener()
									{
										@Override
										public void onClick(DialogInterface dialogInterface, int i)
										{
											WorkerService.run(getContext(), new WorkerService.NotifiableRunningTask(TAG, JOB_FILE_FIX)
											{
												@Override
												public void onUpdateNotification(DynamicNotification dynamicNotification, UpdateType updateType)
												{
													switch (updateType) {
														case Started:
															dynamicNotification.setSmallIcon(R.drawable.ic_compare_arrows_white_24dp)
																	.setContentText(getString(R.string.mesg_organizingFiles));
															break;
														case Done:
															dynamicNotification.setContentText(getString(R.string.text_movedCacheFiles));
															break;
													}
												}

												@Override
												public void onRun()
												{
													ArrayList<TransferObject> checkList = getDatabase().
															castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
																	.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
																					+ AccessDatabase.FIELD_TRANSFER_TYPE + "=? AND "
																					+ AccessDatabase.FIELD_TRANSFER_FLAG + " != ?",
																			String.valueOf(mGroup.groupId), TransferObject.Type.INCOMING.toString(), TransferObject.Flag.PENDING.toString()), TransferObject.class);

													TransferGroup pseudoGroup = new TransferGroup(mGroup.groupId);

													try {
														// Illustrate new change to build the structure accordingly
														getDatabase().reconstruct(pseudoGroup);
														pseudoGroup.savePath = selectedPath.toString();

														for (TransferObject transferObject : checkList) {
															if (getInterrupter().interrupted())
																break;

															try {
																DocumentFile file = FileUtils.getIncomingPseudoFile(getContext(), getDefaultPreferences(), transferObject, mGroup, false);
																DocumentFile pseudoFile = FileUtils.getIncomingPseudoFile(getContext(), getDefaultPreferences(), transferObject, pseudoGroup, true);

																if (file.canRead())
																	FileUtils.move(getContext(), file, pseudoFile, getInterrupter());

																file.delete();
															} catch (IOException e) {
																e.printStackTrace();
															}
														}

														updateSavePath(selectedPath.toString());
													} catch (Exception e) {
														e.printStackTrace();
													}
												}
											});
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

		private Snackbar createSnackbar(int resId, Object... objects)
		{
			return Snackbar.make(getView(), getString(resId, objects), Snackbar.LENGTH_LONG);
		}

		@Override
		public CharSequence getTitle(Context context)
		{
			return context.getString(R.string.text_transactionDetails);
		}

		public TransactionDetailsFragment setGroup(TransferGroup group)
		{
			mGroup = group;
			return this;
		}

		public void setIndex(TransferGroup.Index index)
		{
			mIndex = index;
		}

		public void updateSavePath(String selectedPath)
		{
			mGroup.savePath = selectedPath;
			getDatabase().publish(mGroup);

			getActivity().runOnUiThread(new Runnable()
			{
				@Override
				public void run()
				{
					createSnackbar(R.string.mesg_pathSaved).show();
				}
			});
		}

		public void updateViewState()
		{
			boolean isIncoming = mIndex.incomingCount > 0;

			mShowFiles.setVisibility(isIncoming ? View.VISIBLE : View.GONE);
			mSaveTo.setVisibility(isIncoming ? View.VISIBLE : View.GONE);
		}
	}

	public static class TransactionExplorerFragment
			extends Fragment
			implements TransactionListAdapter.PathChangedListener, TitleSupport
	{
		public static final String ARG_GROUP_ID = "argGroupId";
		public static final String ARG_PATH = "path";

		private RecyclerView mPathView;
		private TransactionPathResolverRecyclerAdapter mPathAdapter;
		private TransactionListFragment mTransactionListFragment;

		@Nullable
		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
		{
			View view = inflater.inflate(R.layout.layout_transaction_explorer, container, false);

			mPathView = view.findViewById(R.id.layout_transaction_explorer_recycler);
			mTransactionListFragment = (TransactionListFragment) getChildFragmentManager().findFragmentById(R.id.layout_transaction_explorer_fragment_transaction);
			mPathAdapter = new TransactionPathResolverRecyclerAdapter(getContext());

			mTransactionListFragment.getAdapter().setPathChangedListener(this);

			LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
			layoutManager.setStackFromEnd(true);

			mPathView.setHasFixedSize(true);
			mPathView.setLayoutManager(layoutManager);
			mPathView.setAdapter(mPathAdapter);

			mPathAdapter.setOnClickListener(new PathResolverRecyclerAdapter.OnClickListener<String>()
			{
				@Override
				public void onClick(PathResolverRecyclerAdapter.Holder<String> holder)
				{
					goPath(getTransactionListFragment().getAdapter().getGroupId(), holder.index.object);
				}
			});

			return view;
		}

		@Override
		public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
		{
			super.onViewCreated(view, savedInstanceState);

			Bundle args = getArguments();

			if (args != null && args.containsKey(ARG_GROUP_ID))
				goPath(args.getInt(ARG_GROUP_ID), args.getString(ARG_PATH));
			else
				mPathAdapter.goTo(null);

			getTransactionListFragment().setMenuVisibility(isMenuShown());
		}

		@Override
		public void onPathChange(String path)
		{
			mPathAdapter.goTo(path == null ? null : path.split(File.separator));
			mPathAdapter.notifyDataSetChanged();

			if (mPathAdapter.getItemCount() > 0)
				mPathView.smoothScrollToPosition(mPathAdapter.getItemCount() - 1);
		}

		public TransactionListFragment getTransactionListFragment()
		{
			return mTransactionListFragment;
		}

		@Override
		public CharSequence getTitle(Context context)
		{
			return context.getString(R.string.textFiles);
		}

		public void goPath(int groupId, String path)
		{
			getTransactionListFragment().getAdapter().setGroupId(groupId);
			getTransactionListFragment().getAdapter().setPath(path);

			getTransactionListFragment().refreshList();
		}

		@Override
		public void setMenuVisibility(boolean menuVisible)
		{
			super.setMenuVisibility(menuVisible);

			if (getTransactionListFragment() != null)
				getTransactionListFragment().setMenuVisibility(menuVisible);
		}
	}
}
