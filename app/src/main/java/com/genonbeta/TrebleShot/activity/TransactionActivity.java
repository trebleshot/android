package com.genonbeta.TrebleShot.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.adapter.TransactionListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.fragment.TransactionListFragment;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransactionObject;
import com.genonbeta.android.database.SQLQuery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/23/17 1:43 PM
 */

public class TransactionActivity extends Activity implements NavigationView.OnNavigationItemSelectedListener, TransactionListAdapter.PathChangedListener
{
	public static final String ACTION_LIST_TRANSFERS = "com.genonbeta.TrebleShot.action.LIST_TRANSFERS";

	public static final String EXTRA_GROUP_ID = "extraGroupId";

	public static final int REQUEST_CHOOSE_FOLDER = 1;

	private AccessDatabase mDatabase;
	private TransactionListFragment mTransactionFragment;
	private TransactionObject.Group mGroup;
	private NetworkDevice mDevice;
	private IntentFilter mFilter = new IntentFilter();
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
			}
		}
	};

	private RecyclerView mRecyclerView;
	private ImageView mHomeButton;
	private LinearLayoutManager mLayoutManager;
	private PathResolverRecyclerAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_transaction);

		mDatabase = new AccessDatabase(this);
		mTransactionFragment = (TransactionListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_transaction_listfragment_transaction);
		mRecyclerView = (RecyclerView) findViewById(R.id.activity_transaction_explorer_recycler);
		mHomeButton = (ImageView) findViewById(R.id.activity_transaction_explorer_image_home);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.text_navigationDrawerOpen, R.string.text_navigationDrawerClose);
		drawer.addDrawerListener(toggle);
		toggle.syncState();

		NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		mRecyclerView.setHasFixedSize(true);
		mFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);

		mLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
		mAdapter = new PathResolverRecyclerAdapter();

		mRecyclerView.setLayoutManager(mLayoutManager);
		mLayoutManager.setStackFromEnd(true);
		mRecyclerView.setAdapter(mAdapter);

		mAdapter.setOnClickListener(new PathResolverRecyclerAdapter.OnClickListener()
		{
			@Override
			public void onClick(PathResolverRecyclerAdapter.Holder holder)
			{
				goPath(holder.index.path);
			}
		});

		mHomeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				goPath(null);
			}
		});

		if (ACTION_LIST_TRANSFERS.equals(getIntent().getAction()) && getIntent().hasExtra(EXTRA_GROUP_ID)) {
			TransactionObject.Group group = new TransactionObject.Group(getIntent().getIntExtra(EXTRA_GROUP_ID, -1));

			try {
				mDatabase.reconstruct(group);

				NetworkDevice networkDevice = new NetworkDevice(group.deviceId);
				mDatabase.reconstruct(networkDevice);

				mGroup = group;
				mDevice = networkDevice;

				mTransactionFragment.getAdapter().setPathChangedListener(this);

				applyPath(null);

				View view = navigationView.getHeaderView(0);
				ImageView imageView = view.findViewById(R.id.header_transaction_image);
				TextView deviceNameText = view.findViewById(R.id.header_transaction_text1);
				TextView versionText = view.findViewById(R.id.header_transaction_text2);

				String firstLetters = TextUtils.getFirstLetters(mDevice.user, 1);
				TextDrawable drawable = TextDrawable.builder().buildRoundRect(firstLetters.length() > 0 ? firstLetters : "?", ContextCompat.getColor(getApplicationContext(), R.color.colorTextDrawable), 100);

				imageView.setImageDrawable(drawable);
				deviceNameText.setText(mDevice.user);
				versionText.setText(mDevice.buildName);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (mGroup == null)
			finish();
	}

	public void applyPath(String path)
	{
		mTransactionFragment.getAdapter().setGroupId(mGroup.groupId);
		mTransactionFragment.getAdapter().setPath(path);
	}

	public void goPath(String path)
	{
		applyPath(path);
		mTransactionFragment.refreshList();
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
							final String selectedPath = data.getStringExtra(FilePickerActivity.EXTRA_CHOSEN_PATH);

							if (selectedPath.equals(mGroup.savePath) || ((mGroup.savePath == null || mGroup.savePath.length() == 0 || new File(mGroup.savePath).canWrite()) && selectedPath.equals(FileUtils.getApplicationDirectory(getApplicationContext()).getAbsolutePath()))) {
								Snackbar.make(findViewById(android.R.id.content), R.string.mesg_pathSameError, Snackbar.LENGTH_SHORT).show();
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
										updateSavePath(selectedPath);
									}
								});

								builder.setPositiveButton(R.string.butn_accept, new DialogInterface.OnClickListener()
								{
									@Override
									public void onClick(DialogInterface dialogInterface, int i)
									{
										final ProgressDialog progressDialog = new ProgressDialog(TransactionActivity.this);

										progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
										progressDialog.setCancelable(false);
										progressDialog.setMessage(getString(R.string.mesg_organizingFiles));

										progressDialog.show();

										new Thread()
										{
											@Override
											public void run()
											{
												super.run();

												ArrayList<TransactionObject> checkList = mDatabase.
														castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
																.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
																				+ AccessDatabase.FIELD_TRANSFER_TYPE + "=? AND "
																				+ AccessDatabase.FIELD_TRANSFER_FLAG + " != ?",
																		String.valueOf(mGroup.groupId), TransactionObject.Type.INCOMING.toString(), TransactionObject.Flag.PENDING.toString()), TransactionObject.class);

												progressDialog.setMax(checkList.size());

												for (TransactionObject transactionObject : checkList) {
													progressDialog.setProgress(progressDialog.getProgress() + 1);

													try {
														File file = FileUtils.getIncomingTransactionFile(getApplicationContext(), transactionObject, mGroup);

														if (file.exists() && file.canWrite())
															file.renameTo(new File(selectedPath + File.separator + transactionObject.file));
													} catch (IOException e) {
														e.printStackTrace();
													}
												}

												progressDialog.cancel();

												updateSavePath(selectedPath);

											}
										}.start();
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

		registerReceiver(mReceiver, mFilter);
		reconstructGroup();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unregisterReceiver(mReceiver);
	}

	@Override
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
					mDatabase.remove(new TransactionObject.Group(mGroup.groupId));
				}
			});

			dialog.show();
		} else if (id == R.id.drawer_transaction_connection) {
			new ConnectionChooserDialog(this, mDatabase, mDevice, new ConnectionChooserDialog.OnDeviceSelectedListener()
			{
				@Override
				public void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> connectionList)
				{
					mGroup.connectionAdapter = connection.adapterName;
					mDatabase.publish(mGroup);
				}
			}).show();
		} else
			return false;

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);

		return true;
	}

	public void reconstructGroup()
	{
		try {
			mDatabase.reconstruct(mGroup);
		} catch (Exception e) {
			e.printStackTrace();
			finish();
		}
	}

	public void updateSavePath(String selectedPath)
	{
		mGroup.savePath = selectedPath;
		mDatabase.publish(mGroup);

		runOnUiThread(new Runnable()
		{
			@Override
			public void run()
			{
				Snackbar.make(findViewById(android.R.id.content), R.string.mesg_pathSaved, Snackbar.LENGTH_SHORT).show();
			}
		});
	}

	public static void startInstance(Context context, int groupId)
	{
		context.startActivity(new Intent(context, TransactionActivity.class)
				.setAction(ACTION_LIST_TRANSFERS)
				.putExtra(EXTRA_GROUP_ID, groupId)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
	}

	@Override
	public void onPathChange(String path)
	{
		mAdapter.goTo(path == null ? null : path.split(File.separator));
		mAdapter.notifyDataSetChanged();

		if (mAdapter.getItemCount() > 0)
			mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
	}
}
