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
import android.support.v4.app.FragmentStatePagerAdapter;
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
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.Fragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.dialog.TransactionGroupInfoDialog;
import com.genonbeta.TrebleShot.fragment.TransactionListFragment;
import com.genonbeta.TrebleShot.io.DocumentFile;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.PowerfulActionModeSupported;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TitleSupport;
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

	public static final int REQUEST_CHOOSE_FOLDER = 1;

	private TransactionObject.Group mGroup;
	private NetworkDevice mDevice;
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

	private TransactionGroupInfoDialog mInfoDialog;
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
		final TransactionFileExplorer transactionFragment = new TransactionFileExplorer();

		tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);

		pagerAdapter.add(transactionFragment, tabLayout);
		pagerAdapter.add(new TransactionInfo(), tabLayout);

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
			public void onTabUnselected(TabLayout.Tab tab)
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


		if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_GROUP_ID)) {
			TransactionObject.Group group = new TransactionObject.Group(getIntent().getIntExtra(EXTRA_GROUP_ID, -1));

			try {
				getDatabase().reconstruct(group);

				NetworkDevice networkDevice = new NetworkDevice(group.deviceId);
				getDatabase().reconstruct(networkDevice);

				mGroup = group;
				mDevice = networkDevice;
				mInfoDialog = new TransactionGroupInfoDialog(this, getDatabase(), getDefaultPreferences(), mGroup);

				if (getSupportActionBar() != null)
					getSupportActionBar().setTitle(mDevice.nickname);

				transactionFragment.goPath(mGroup.groupId, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (mGroup == null)
			finish();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
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
								AlertDialog.Builder builder = new AlertDialog.Builder(TransactionActivity.this);

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
										WorkerService.run(TransactionActivity.this, new WorkerService.NotifiableRunningTask(TAG, JOB_FILE_FIX)
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
												ArrayList<TransactionObject> checkList = getDatabase().
														castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
																.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
																				+ AccessDatabase.FIELD_TRANSFER_TYPE + "=? AND "
																				+ AccessDatabase.FIELD_TRANSFER_FLAG + " != ?",
																		String.valueOf(mGroup.groupId), TransactionObject.Type.INCOMING.toString(), TransactionObject.Flag.PENDING.toString()), TransactionObject.class);

												TransactionObject.Group pseudoGroup = new TransactionObject.Group(mGroup.groupId);

												try {
													// Illustrate new change to build the structure accordingly
													getDatabase().reconstruct(pseudoGroup);
													pseudoGroup.savePath = selectedPath.toString();

													for (TransactionObject transactionObject : checkList) {
														if (getInterrupter().interrupted())
															break;

														try {
															DocumentFile file = FileUtils.getIncomingPseudoFile(getApplicationContext(), getDefaultPreferences(), transactionObject, mGroup, false);
															DocumentFile pseudoFile = FileUtils.getIncomingPseudoFile(getApplicationContext(), getDefaultPreferences(), transactionObject, pseudoGroup, true);

															if (file.canRead())
																FileUtils.move(TransactionActivity.this, file, pseudoFile, getInterrupter());

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

		updateCalculations();

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_transaction_resume_all) {
			try {
				getDatabase().reconstruct(new NetworkDevice.Connection(mDevice.deviceId, mGroup.connectionAdapter));

				AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class)
						.setAction(CommunicationService.ACTION_SEAMLESS_RECEIVE)
						.putExtra(CommunicationService.EXTRA_GROUP_ID, mGroup.groupId));
			} catch (Exception e) {
				e.printStackTrace();

				createSnackbar(R.string.mesg_transferConnectionNotSetUpFix)
						.setAction(R.string.butn_setUp, new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								changeConnection();
							}
						}).show();
			}
		} else if (id == R.id.actions_transaction_retry_all) {
			ContentValues contentValues = new ContentValues();

			contentValues.put(AccessDatabase.FIELD_TRANSFER_FLAG, TransactionObject.Flag.PENDING.toString());

			getDatabase().update(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
					.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
									+ AccessDatabase.FIELD_TRANSFER_FLAG + "=? AND "
									+ AccessDatabase.FIELD_TRANSFER_TYPE + "=?",
							String.valueOf(mGroup.groupId),
							TransactionObject.Flag.INTERRUPTED.toString(),
							TransactionObject.Type.INCOMING.toString()), contentValues);

			createSnackbar(R.string.mesg_retryAllInfo)
					.show();
		} else if (id == R.id.actions_transaction_show_info)
			mInfoDialog.show();
		else
			return super.onOptionsItemSelected(item);

		return true;
	}

	// FIXME: 05.04.2018 though
	public boolean onNavigationItemSelected(@NonNull MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.drawer_transaction_saveTo) {
			startActivityForResult(new Intent(TransactionActivity.this, FilePickerActivity.class)
					.setAction(FilePickerActivity.ACTION_CHOOSE_DIRECTORY), REQUEST_CHOOSE_FOLDER);
		} else if (id == R.id.drawer_transaction_delete) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(TransactionActivity.this);

			dialog.setTitle(R.string.ques_removeAll);
			dialog.setMessage(R.string.text_removeCertainPendingTransfersSummary);

			dialog.setNegativeButton(R.string.butn_cancel, null);
			dialog.setPositiveButton(R.string.butn_removeAll, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					getDatabase().remove(new TransactionObject.Group(mGroup.groupId));
				}
			});

			dialog.show();
		} else if (id == R.id.drawer_transaction_show_files) {
			startActivity(new Intent(this, HomeActivity.class)
					.setAction(HomeActivity.ACTION_OPEN_RECEIVED_FILES)
					.putExtra(HomeActivity.EXTRA_FILE_PATH, FileUtils.getSavePath(this, getDefaultPreferences(), mGroup).getUri()));
		} else if (id == R.id.drawer_transaction_connection) {
			changeConnection();
		} else
			return false;

		return true;
	}

	private void changeConnection()
	{
		new ConnectionChooserDialog(this, getDatabase(), mDevice, new ConnectionChooserDialog.OnDeviceSelectedListener()
		{
			@Override
			public void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> connectionList)
			{
				mGroup.connectionAdapter = connection.adapterName;
				getDatabase().publish(mGroup);

				createSnackbar(R.string.mesg_connectionUpdated, TextUtils.getAdapterName(getApplicationContext(), connection))
						.show();
			}
		}, false).show();
	}

	private Snackbar createSnackbar(int resId, Object... objects)
	{
		return Snackbar.make(findViewById(R.id.activity_transaction_view_pager), getString(resId, objects), Snackbar.LENGTH_LONG);
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

	public void updateSavePath(String selectedPath)
	{
		mGroup.savePath = selectedPath;
		getDatabase().publish(mGroup);

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				createSnackbar(R.string.mesg_pathSaved).show();
			}
		});
	}

	public void updateCalculations()
	{
		if (mInfoMenu != null) {
			new Handler(Looper.myLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					if (!mInfoDialog.calculateSpace()) {
						mInfoMenu.setTitle(R.string.mesg_notEnoughSpace);
						MenuItemCompat.setIconTintList(mInfoMenu, ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.notEnoughSpaceMenuTint)));
					}

					boolean hasIncoming = mInfoDialog.getIndex().incomingCount > 0;

					mStartMenu.setVisible(hasIncoming);
					mRetryMenu.setVisible(hasIncoming);
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
		public void goTo(String[] paths)
		{
			getList().clear();

			StringBuilder mergedPath = new StringBuilder();

			getList().add(new Holder.Index<>("Home", R.drawable.ic_home_black_24dp, (String) null));

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

	public class PagerAdapter extends FragmentStatePagerAdapter
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

	public static class TransactionInfo
			extends Fragment
			implements TitleSupport
	{
		@Nullable
		@Override
		public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
		{
			return inflater.inflate(R.layout.layout_device_info, container, false);
		}

		@Override
		public CharSequence getTitle(Context context)
		{
			return context.getString(R.string.text_transactionDetails);
		}
	}

	public static class TransactionFileExplorer
			extends Fragment
			implements TransactionListAdapter.PathChangedListener, TitleSupport
	{
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
			mPathAdapter = new TransactionPathResolverRecyclerAdapter();

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
			mPathAdapter.goTo(null);
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
			return context.getString(R.string.text_pendingTransfers);
		}

		public void goPath(int groupId, String path)
		{
			getTransactionListFragment().getAdapter().setGroupId(groupId);
			getTransactionListFragment().getAdapter().setPath(path);

			mTransactionListFragment.refreshList();
		}
	}
}
