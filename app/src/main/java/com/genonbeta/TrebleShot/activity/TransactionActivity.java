package com.genonbeta.TrebleShot.activity;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter;
import com.genonbeta.TrebleShot.adapter.TransferAssigneeListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.fragment.TransactionListFragment;
import com.genonbeta.TrebleShot.fragment.TransferAssigneeListFragment;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.callback.PowerfulActionModeSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.android.framework.widget.PowerfulActionMode;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/23/17 1:43 PM
 */

public class TransactionActivity
		extends Activity
		implements PowerfulActionModeSupport
{
	public static final String TAG = TransactionActivity.class.getSimpleName();

	public static final String ACTION_LIST_TRANSFERS = "com.genonbeta.TrebleShot.action.LIST_TRANSFERS";
	public static final String EXTRA_GROUP_ID = "extraGroupId";

	public static final int REQUEST_ADD_DEVICES = 5045;

	private OnBackPressedListener mBackPressedListener;
	private TransferGroup mGroup;

	private BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction()) && intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)) {
				if (AccessDatabase.TABLE_TRANSFERGROUP.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME)))
					reconstructGroup();
				else if (intent.hasExtra(AccessDatabase.EXTRA_CHANGE_TYPE)
						&& AccessDatabase.TABLE_TRANSFER.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME))
						&& (AccessDatabase.TYPE_INSERT.equals(intent.getStringExtra(AccessDatabase.EXTRA_CHANGE_TYPE)) || AccessDatabase.TYPE_REMOVE.equals(intent.getStringExtra(AccessDatabase.EXTRA_CHANGE_TYPE)))) {
					updateCalculations();
				}
			} else if (CommunicationService.ACTION_TASK_STATUS_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(CommunicationService.EXTRA_GROUP_ID)) {
				long groupId = intent.getLongExtra(CommunicationService.EXTRA_GROUP_ID, -1);
				String deviceId = intent.getStringExtra(CommunicationService.EXTRA_DEVICE_ID);

				if (groupId == mGroup.groupId) {
					if (intent.getIntExtra(CommunicationService.EXTRA_TASK_CHANGE_TYPE, -1) == CommunicationService.TASK_STATUS_ONGOING) {
						mRunning = true;
						showMenus();
					} else {
						mRunning = false;
						showMenus();
					}
				}
			}
		}
	};

	private TransferGroup.Index mTransactionIndex = new TransferGroup.Index();
	private PowerfulActionMode mMode;
	private MenuItem mStartMenu;
	private MenuItem mRetryMenu;
	private MenuItem mShowFiles;
	private MenuItem mAddDevice;
	private CrunchLatestDataTask mDataCruncher;

	boolean mRunning = false;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_transaction);

		mMode = findViewById(R.id.activity_transaction_action_mode);

		final Toolbar toolbar = findViewById(R.id.toolbar);
		final TabLayout tabLayout = findViewById(R.id.activity_transaction_tab_layout);
		final ViewPager viewPager = findViewById(R.id.activity_transaction_view_pager);

		setSupportActionBar(toolbar);

		if (getSupportActionBar() != null) {
			getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		}

		final SmartFragmentPagerAdapter pagerAdapter = new SmartFragmentPagerAdapter(this, getSupportFragmentManager()) {
			@Override
			public void onItemInstantiated(StableItem item)
			{
				super.onItemInstantiated(item);

				if (viewPager.getCurrentItem() == item.getCurrentPosition())
					attachListeners(item.getInitiatedItem());
			}
		};

		if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_GROUP_ID)) {
			TransferGroup group = new TransferGroup(getIntent().getLongExtra(EXTRA_GROUP_ID, -1));

			try {
				getDatabase().reconstruct(group);
				mGroup = group;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (mGroup == null)
			finish();
		else {
			tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

			Bundle assigneeFragmentArgs = new Bundle();
			assigneeFragmentArgs.putLong(TransferAssigneeListFragment.ARG_GROUP_ID, mGroup.groupId);

			Bundle transactionFragmentArgs = new Bundle();
			transactionFragmentArgs.putLong(TransactionExplorerFragment.ARG_GROUP_ID, mGroup.groupId);
			transactionFragmentArgs.putString(TransactionExplorerFragment.ARG_PATH, null);

			pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(0, TransactionExplorerFragment.class, transactionFragmentArgs));
			pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(1, TransferAssigneeListFragment.class, assigneeFragmentArgs));

			pagerAdapter.createTabs(tabLayout);
			viewPager.setAdapter(pagerAdapter);
			viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

			tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
			{
				@Override
				public void onTabSelected(TabLayout.Tab tab)
				{
					viewPager.setCurrentItem(tab.getPosition());
					attachListeners(pagerAdapter.getItem(tab.getPosition()));
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

			mMode.setOnSelectionTaskListener(new PowerfulActionMode.OnSelectionTaskListener()
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

		IntentFilter filter = new IntentFilter();

		filter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
		filter.addAction(CommunicationService.ACTION_TASK_STATUS_CHANGE);

		registerReceiver(mReceiver, filter);
		reconstructGroup();

		requestTaskStateUpdate();
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
		super.onCreateOptionsMenu(menu);
		getMenuInflater().inflate(R.menu.actions_transaction, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		mStartMenu = menu.findItem(R.id.actions_transaction_resume);
		mRetryMenu = menu.findItem(R.id.actions_transaction_retry_all);
		mShowFiles = menu.findItem(R.id.actions_transaction_show_files);
		mAddDevice = menu.findItem(R.id.actions_transaction_add_device);

		if (!getIndex().calculated)
			updateCalculations();
		else
			showMenus();

		requestTaskStateUpdate();

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == android.R.id.home) {
			finish();
		} else if (id == R.id.actions_transaction_resume) {
			toggleTask();
		} else if (id == R.id.actions_transaction_remove) {
			AlertDialog.Builder dialog = new AlertDialog.Builder(this);

			dialog.setTitle(R.string.ques_removeAll);
			dialog.setMessage(R.string.text_removeCertainPendingTransfersSummary);
			dialog.setNegativeButton(R.string.butn_cancel, null);
			dialog.setPositiveButton(R.string.butn_removeAll, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					getDatabase().remove(mGroup);
				}
			});

			dialog.show();
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
		} else if (id == R.id.actions_transaction_show_files) {
			startActivity(new Intent(this, HomeActivity.class)
					.setAction(HomeActivity.ACTION_OPEN_RECEIVED_FILES)
					.putExtra(HomeActivity.EXTRA_FILE_PATH, FileUtils.getSavePath(this, getDefaultPreferences(), mGroup).getUri()));
		} else if (id == R.id.actions_transaction_add_device) {
			startActivityForResult(new Intent(this, ShareActivity.class)
					.setAction(ShareActivity.ACTION_ADD_DEVICES)
					.putExtra(ShareActivity.EXTRA_GROUP_ID, mGroup.groupId), REQUEST_ADD_DEVICES);
		} else
			return super.onOptionsItemSelected(item);

		return true;
	}

	@Override
	public void onBackPressed()
	{
		if (mBackPressedListener == null || !mBackPressedListener.onBackPressed())
			super.onBackPressed();
	}

	private void attachListeners(Fragment initiatedItem)
	{
		mBackPressedListener = initiatedItem instanceof OnBackPressedListener
				? (OnBackPressedListener) initiatedItem
				: null;
	}

	private Snackbar createSnackbar(int resId, Object... objects)
	{
		return Snackbar.make(findViewById(R.id.activity_transaction_view_pager), getString(resId, objects), Snackbar.LENGTH_LONG);
	}

	@Nullable
	public TransferGroup getGroup()
	{
		return mGroup;
	}

	public TransferGroup.Index getIndex()
	{
		return mTransactionIndex;
	}

	@Override
	public PowerfulActionMode getPowerfulActionMode()
	{
		return mMode;
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

	private void requestTaskStateUpdate()
	{
		AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class)
				.setAction(CommunicationService.ACTION_REQUEST_TASK_STATUS_CHANGE)
				.putExtra(CommunicationService.EXTRA_GROUP_ID, mGroup.groupId));
	}

	private void showMenus()
	{
		boolean hasIncoming = getIndex().incomingCount > 0;
		boolean hasOutgoing = getIndex().outgoingCount > 0;

		if (mStartMenu == null || mRetryMenu == null || mShowFiles == null)
			return;

		mStartMenu.setTitle(mRunning ? R.string.butn_pause : R.string.butn_resume);
		mStartMenu.setIcon(mRunning ? R.drawable.ic_pause_white_24dp : R.drawable.ic_play_arrow_white_24dp);

		// Only show when there
		mAddDevice.setVisible(hasOutgoing);
		mStartMenu.setVisible(hasIncoming || mRunning);
		mRetryMenu.setVisible(hasIncoming);
		mShowFiles.setVisible(hasIncoming);

		setTitle(getResources().getQuantityString(R.plurals.text_files,
				getIndex().incomingCount + getIndex().outgoingCount,
				getIndex().incomingCount + getIndex().outgoingCount));
	}

	private void toggleTask()
	{
		if (mRunning) {
			TransferUtils.pauseTransfer(this, mGroup, null);
		} else {
			SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
					.setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(mGroup.groupId));

			ArrayList<TransferAssigneeListAdapter.ShowingAssignee> assignees = getDatabase().castQuery(select, TransferAssigneeListAdapter.ShowingAssignee.class, new SQLiteDatabase.CastQueryListener<TransferAssigneeListAdapter.ShowingAssignee>()
			{
				@Override
				public void onObjectReconstructed(SQLiteDatabase db, CursorItem item, TransferAssigneeListAdapter.ShowingAssignee object)
				{
					object.device = new NetworkDevice(object.deviceId);
					object.connection = new NetworkDevice.Connection(object);

					try {
						db.reconstruct(object.device);
						db.reconstruct(object.connection);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});

			if (assignees.size() == 0) {
				createSnackbar(R.string.mesg_noReceiverOrSender)
						.show();
				return;
			}

			final TransferAssigneeListAdapter.ShowingAssignee assignee = assignees.get(0);

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
	}

	public synchronized void updateCalculations()
	{
		if (mDataCruncher == null || !mDataCruncher.requestRestart()) {
			mDataCruncher = new CrunchLatestDataTask(new CrunchLatestDataTask.PostExecuteListener()
			{
				@Override
				public void onPostExecute()
				{
					showMenus();
				}
			});

			mDataCruncher.execute(this);
		}
	}

	public static void startInstance(Context context, long groupId)
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

	public static class TransactionExplorerFragment
			extends TransactionListFragment
			implements TitleSupport, SnackbarSupport
	{
		private RecyclerView mPathView;
		private TransactionPathResolverRecyclerAdapter mPathAdapter;

		@Override
		protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
		{
			View adaptedView = getLayoutInflater().inflate(R.layout.layout_transaction_explorer, null, false);
			listViewContainer.addView(adaptedView);

			mPathView = adaptedView.findViewById(R.id.layout_transaction_explorer_recycler);
			mPathAdapter = new TransactionPathResolverRecyclerAdapter(getContext());

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
					goPath(getAdapter().getGroupId(), holder.index.object);
				}
			});

			return super.onListView(mainContainer, (ViewGroup) adaptedView.findViewById(R.id.layout_transaction_explorer_fragment_content));
		}

		@Override
		protected void onListRefreshed()
		{
			super.onListRefreshed();

			String path = getAdapter().getPath();

			mPathAdapter.goTo(path == null ? null : path.split(File.separator));
			mPathAdapter.notifyDataSetChanged();

			if (mPathAdapter.getItemCount() > 0)
				mPathView.smoothScrollToPosition(mPathAdapter.getItemCount() - 1);
		}

		@Override
		public CharSequence getTitle(Context context)
		{
			return context.getString(R.string.text_files);
		}
	}

	public static class CrunchLatestDataTask extends AsyncTask<TransactionActivity, Void, Void>
	{
		private PostExecuteListener mListener;
		private boolean mRestartRequested = false;

		public CrunchLatestDataTask(PostExecuteListener listener)
		{
			mListener = listener;
		}

		/* "possibility of having more than one TransactionActivity" < "sun turning into black hole" */
		@Override
		protected Void doInBackground(TransactionActivity... activities)
		{
			do {
				mRestartRequested = false;

				for (TransactionActivity activity : activities) {
					if (activity.getGroup() != null)
						activity.getDatabase()
								.calculateTransactionSize(activity.getGroup().groupId, activity.getIndex());
				}
			} while (mRestartRequested && !isCancelled());

			return null;
		}

		public boolean requestRestart()
		{
			if (getStatus().equals(Status.RUNNING))
				mRestartRequested = true;

			return mRestartRequested;
		}

		@Override
		protected void onPostExecute(Void aVoid)
		{
			super.onPostExecute(aVoid);
			mListener.onPostExecute();
		}

		/* Should we have used a generic type class for this?
		 * This interface aims to keep its parent class non-anonymous
		 */

		public interface PostExecuteListener
		{
			void onPostExecute();
		}
	}
}
