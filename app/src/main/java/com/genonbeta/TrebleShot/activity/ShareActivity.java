package com.genonbeta.TrebleShot.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog;
import com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment;
import com.genonbeta.TrebleShot.io.DocumentFile;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.Selectable;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AddressedInterface;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.Interrupter;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;

public class ShareActivity extends Activity
{
	public static final String TAG = "ShareActivity";

	public static final int WORKER_TASK_LOAD_ITEMS = 1;
	public static final int WORKER_TASK_CONNECT_SERVER = 2;
	public static final int WORKER_TASK_CONNECT_TS_NETWORK = 4;

	public static final int REQUEST_CODE_EDIT_BOX = 1;

	public static final String ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND";
	public static final String ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE";

	public static final String EXTRA_FILENAME_LIST = "extraFileNames";
	public static final String EXTRA_DEVICE_ID = "extraDeviceId";

	private boolean mQRScanRequested = false;
	private ArrayList<SelectableStream> mFiles = new ArrayList<>();
	private String mSharedText;
	private AccessDatabase mDatabase;
	private ProgressDialog mProgressDialog;
	private Interrupter mInterrupter = new Interrupter();
	private IntentFilter mFilter = new IntentFilter();
	private NetworkDeviceListFragment mDeviceListFragment;
	private FloatingActionButton mFAB;
	private WorkerService mWorkerService;
	private WorkerConnection mWorkerConnection = new WorkerConnection();
	private BroadcastReceiver mWifiStatusReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (mQRScanRequested
					&& WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
					&& WifiManager.WIFI_STATE_ENABLED == intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)) {
				requestQRScan();
			}
		}
	};

	@Override

	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_share);

		if (getSupportActionBar() != null)
			getSupportActionBar().setTitle(R.string.text_shareWithTrebleshot);

		mFAB = findViewById(R.id.content_fab);
		mDatabase = new AccessDatabase(getApplicationContext());
		mDeviceListFragment = (NetworkDeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_share_fragment);

		mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

		mDeviceListFragment.getListView().setPadding(0, 0, 0, 300);
		mDeviceListFragment.getListView().setClipToPadding(false);

		mFAB.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				fabClicked();
			}
		});

		mDeviceListFragment.setOnListClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				NetworkDevice device = (NetworkDevice) mDeviceListFragment.getListAdapter().getItem(position);

				if (device instanceof NetworkDeviceListAdapter.HotspotNetwork)
					doCommunicate((NetworkDeviceListAdapter.HotspotNetwork) device);
				else
					showChooserDialog(device);
			}
		});

		bindService(new Intent(this, WorkerService.class), mWorkerConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.actions_activity_share, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		registerReceiver(mWifiStatusReceiver, mFilter);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unregisterReceiver(mWifiStatusReceiver);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_activity_share_scan_barcode) {
			if (!mDeviceListFragment.getWifiManager().isWifiEnabled()) {
				createSnackbar(R.string.mesg_wifiEnableRequired)
						.setAction(R.string.butn_enable, new View.OnClickListener()
						{
							@Override
							public void onClick(View view)
							{
								mQRScanRequested = true;

								mDeviceListFragment.getWifiManager()
										.setWifiEnabled(true);
							}
						})
						.show();
			} else
				requestQRScan();
		} else
			return super.onOptionsItemSelected(item);

		return true;
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		getDefaultInterrupter().interrupt(false);
		unbindService(mWorkerConnection);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK) {
			if (requestCode == REQUEST_CODE_EDIT_BOX && data != null && data.hasExtra(TextEditorActivity.EXTRA_TEXT_INDEX))
				mSharedText = data.getStringExtra(TextEditorActivity.EXTRA_TEXT_INDEX);
			else {
				IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

				if (result.getContents() != null) {
					try {
						JSONObject jsonObject = new JSONObject(result.getContents());
						NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork = new NetworkDeviceListAdapter.HotspotNetwork();

						hotspotNetwork.SSID = jsonObject.getString(Keyword.NETWORK_NAME);
						hotspotNetwork.qrConnection = true;

						boolean passProtected = jsonObject.has(Keyword.NETWORK_PASSWORD);

						if (passProtected) {
							hotspotNetwork.password = jsonObject.getString(Keyword.NETWORK_PASSWORD);
							hotspotNetwork.keyManagement = jsonObject.getInt(Keyword.NETWORK_KEYMGMT);
						}

						doCommunicate(hotspotNetwork);
					} catch (JSONException e) {
						e.printStackTrace();

						createSnackbar(R.string.mesg_somethingWentWrong)
								.show();
					}
				}
			}
		}
	}

	protected void onRequestReady()
	{
		if (getIntent().hasExtra(EXTRA_DEVICE_ID)) {
			String deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
			NetworkDevice chosenDevice = new NetworkDevice(deviceId);

			try {
				mDatabase.reconstruct(chosenDevice);
				showChooserDialog(chosenDevice);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	protected void createFolderStructure(DocumentFile file, String folderName)
	{
		DocumentFile[] files = file.listFiles();

		if (files != null) {
			for (DocumentFile thisFile : files) {
				if (getDefaultInterrupter().interrupted())
					break;

				getProgressDialog().setMax(getProgressDialog().getMax() + 1);
				getProgressDialog().setProgress(getProgressDialog().getMax() + 1);

				if (thisFile.isDirectory()) {
					createFolderStructure(thisFile, (folderName != null ? folderName + File.separator : null) + thisFile.getName());
					continue;
				}

				try {
					mFiles.add(new SelectableStream(thisFile, folderName));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

	protected Snackbar createSnackbar(int resId, Object... objects)
	{
		return Snackbar.make(mDeviceListFragment.getListView(), getString(resId, objects), Snackbar.LENGTH_LONG);
	}

	protected void doCommunicate(final NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork)
	{
		resetProgressItems();

		getProgressDialog().setMessage(getString(R.string.mesg_connectingToSelfHotspot));
		getProgressDialog().setMax(20);

		getProgressDialog().show();

		runOnWorkerService(new WorkerService.RunningTask(TAG, WORKER_TASK_CONNECT_TS_NETWORK)
		{
			private boolean mConnected = false;
			private long mStartTime = System.currentTimeMillis();
			private String mRemoteAddress;

			@Override
			public void onRun()
			{

				while (mRemoteAddress == null) {
					int passedTime = (int) (System.currentTimeMillis() - mStartTime);

					if (!mDeviceListFragment.getWifiManager().isWifiEnabled()) {
						if (!mDeviceListFragment.getWifiManager().setWifiEnabled(true))
							break; // failed to start Wireless
					} else if (!mDeviceListFragment.isConnectedToNetwork(hotspotNetwork)) {
						mDeviceListFragment.toggleConnection(hotspotNetwork);
					} else {
						for (AddressedInterface addressedInterface : NetworkUtils.getInterfaces(true, null)) {
							if (addressedInterface.getNetworkInterface().getDisplayName().startsWith(AppConfig.NETWORK_INTERFACE_WIFI)) {
								String remoteAddress = NetworkUtils.getAddressPrefix(addressedInterface.getAssociatedAddress()) + "1";

								if (NetworkUtils.ping(remoteAddress, 1000)) {
									mRemoteAddress = remoteAddress;
									break;
								}
							}
						}
					}

					if (breakerCheck(passedTime))
						break;
				}

				if (mRemoteAddress != null) {
					try {
						NetworkDeviceLoader.load(true, mDatabase, mRemoteAddress, new NetworkDeviceLoader.OnDeviceRegisteredErrorListener()
						{
							@Override
							public void onError(Exception error)
							{
								getProgressDialog().dismiss();
							}

							@Override
							public void onDeviceRegistered(AccessDatabase database, NetworkDevice device, final NetworkDevice.Connection connection)
							{
								mConnected = true;

								try {
									hotspotNetwork.deviceId = device.deviceId;
									mDatabase.reconstruct(hotspotNetwork);

									device = hotspotNetwork;
								} catch (Exception e) {
									e.printStackTrace();
								}

								final NetworkDevice finalDevice = device;

								if (!getDefaultInterrupter().interrupted())
									runOnUiThread(new Runnable()
									{
										@Override
										public void run()
										{
											getProgressDialog().dismiss();
											doCommunicate(finalDevice, connection);
										}
									});
							}
						});
					} catch (ConnectException e) {
						e.printStackTrace();
					}
				}

				if (!mConnected) {
					getProgressDialog().dismiss();
					createSnackbar(R.string.mesg_connectionFailure)
							.show();
				}

				// We can't add dialog outside of the else statement as it may close other dialogs as well
			}

			private boolean breakerCheck(int passedTime)
			{
				try {
					Thread.sleep(1000);
					getProgressDialog().setProgress(passedTime / 1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return true;
				} finally {
					if (passedTime > 20000 || getDefaultInterrupter().interrupted())
						return true;
				}

				return false;
			}
		});
	}

	protected void doCommunicate(final NetworkDevice device, final NetworkDevice.Connection connection)
	{
		resetProgressItems();

		getProgressDialog().setMessage(getString(R.string.mesg_communicating));
		getProgressDialog().show();

		runOnWorkerService(new WorkerService.RunningTask(TAG, WORKER_TASK_CONNECT_SERVER)
		{
			@Override
			public void onRun()
			{
				CommunicationBridge.connect(mDatabase, true, new CommunicationBridge.Client.ConnectionHandler()
				{
					@Override
					public void onConnect(CommunicationBridge.Client client)
					{
						try {
							final JSONObject jsonRequest = new JSONObject();
							final TransactionObject.Group groupInstance = new TransactionObject.Group(AppUtils.getUniqueNumber(), device.deviceId, connection.adapterName);
							final ArrayList<TransactionObject> pendingRegistry = new ArrayList<>();

							if (device instanceof NetworkDeviceListAdapter.HotspotNetwork
									&& ((NetworkDeviceListAdapter.HotspotNetwork) device).qrConnection)
								jsonRequest.put(Keyword.FLAG_TRANSFER_QR_CONNECTION, true);

							if (mSharedText == null) {
								jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER);
								jsonRequest.put(Keyword.TRANSFER_GROUP_ID, groupInstance.groupId);

								JSONArray filesArray = new JSONArray();

								getProgressDialog().setMax(mFiles.size());

								for (SelectableStream selectableStream : mFiles) {
									if (getDefaultInterrupter().interrupted())
										throw new InterruptedException("Interrupted by user");

									if (!selectableStream.isSelectableSelected())
										continue;

									getProgressDialog().setSecondaryProgress(getProgressDialog().getSecondaryProgress() + 1);

									int requestId = AppUtils.getUniqueNumber();
									JSONObject thisJson = new JSONObject();

									TransactionObject transactionObject = new TransactionObject(requestId,
											groupInstance.groupId,
											selectableStream.getSelectableFriendlyName(),
											selectableStream.getDocumentFile().getUri().toString(),
											selectableStream.getDocumentFile().getType(),
											selectableStream.getDocumentFile().length(), TransactionObject.Type.OUTGOING);

									if (selectableStream.mDirectory != null)
										transactionObject.directory = selectableStream.mDirectory;

									pendingRegistry.add(transactionObject);

									try {
										thisJson.put(Keyword.INDEX_FILE_NAME, transactionObject.friendlyName);
										thisJson.put(Keyword.INDEX_FILE_SIZE, transactionObject.fileSize);
										thisJson.put(Keyword.TRANSFER_REQUEST_ID, requestId);
										thisJson.put(Keyword.INDEX_FILE_MIME, transactionObject.fileMimeType);

										if (selectableStream.mDirectory != null)
											thisJson.put(Keyword.INDEX_DIRECTORY, selectableStream.mDirectory);

										filesArray.put(thisJson);
									} catch (Exception e) {
										Log.e(TAG, "Sender error on fileUri: " + e.getClass().getName() + " : " + transactionObject.friendlyName);
									}
								}

								jsonRequest.put(Keyword.FILES_INDEX, filesArray);
							} else {
								jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD);
								jsonRequest.put(Keyword.TRANSFER_CLIPBOARD_TEXT, mSharedText);
							}

							final CoolSocket.ActiveConnection activeConnection = client.communicate(device, connection);

							getDefaultInterrupter().addCloser(new Interrupter.Closer()
							{
								@Override
								public void onClose(boolean userAction)
								{
									try {
										activeConnection.getSocket().close();
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							});

							activeConnection.reply(jsonRequest.toString());

							CoolSocket.ActiveConnection.Response response = activeConnection.receive();
							JSONObject clientResponse = new JSONObject(response.response);

							if (clientResponse.has(Keyword.RESULT) && clientResponse.getBoolean(Keyword.RESULT)) {
								if (pendingRegistry.size() > 0) {
									mDatabase.insert(groupInstance);

									getDefaultInterrupter().addCloser(new Interrupter.Closer()
									{
										@Override
										public void onClose(boolean userAction)
										{
											mDatabase.remove(groupInstance);
										}
									});

									for (TransactionObject transactionObject : pendingRegistry) {
										if (getDefaultInterrupter().interrupted())
											throw new InterruptedException("Interrupted by user");

										getProgressDialog().setProgress(mProgressDialog.getProgress() + 1);
										mDatabase.insert(transactionObject);
									}

									TransactionActivity.startInstance(getApplicationContext(), groupInstance.groupId);
								}
							} else {
								if (clientResponse.has(Keyword.ERROR) && clientResponse.getString(Keyword.ERROR).equals(Keyword.ERROR_NOT_ALLOWED))
									createSnackbar(R.string.mesg_notAllowed)
											.setAction(R.string.ques_why, new View.OnClickListener()
											{
												@Override
												public void onClick(View v)
												{
													AlertDialog.Builder builder = new AlertDialog.Builder(ShareActivity.this);

													builder.setMessage(getString(R.string.text_notAllowedHelp,
															device.nickname,
															AppUtils.getLocalDeviceName(ShareActivity.this)));

													builder.setNegativeButton(R.string.butn_close, null);
													builder.show();
												}
											}).show();
								else
									createSnackbar(R.string.mesg_somethingWentWrong).show();
							}
						} catch (Exception e) {
							if (!(e instanceof InterruptedException)) {
								e.printStackTrace();
								createSnackbar(R.string.mesg_fileSendError, getString(R.string.text_connectionProblem))
										.show();
							}
						} finally {
							getProgressDialog().dismiss();
						}
					}
				});
			}
		});
	}

	protected void fabClicked()
	{
		if (mSharedText != null)
			startActivityForResult(new Intent(ShareActivity.this, TextEditorActivity.class)
					.setAction(TextEditorActivity.ACTION_EDIT_TEXT)
					.putExtra(TextEditorActivity.EXTRA_TEXT_INDEX, mSharedText)
					.putExtra(TextEditorActivity.EXTRA_SUPPORT_APPLY, true), REQUEST_CODE_EDIT_BOX);
		else
			new SelectionEditorDialog<>(this, mFiles).show();
	}

	public Interrupter getDefaultInterrupter()
	{
		return mInterrupter;
	}

	public ProgressDialog getProgressDialog()
	{
		if (mProgressDialog == null)
			mProgressDialog = new ProgressDialog(this);

		return mProgressDialog;
	}

	private void initialize()
	{
		if (getIntent() != null && getIntent().getAction() != null) {
			String action = getIntent().getAction();

			switch (action) {
				case ACTION_SEND:
				case Intent.ACTION_SEND:
					if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
						mSharedText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
						onRequestReady();
					} else {
						ArrayList<Uri> fileUris = new ArrayList<>();
						ArrayList<CharSequence> fileNames = null;
						Uri fileUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);

						fileUris.add(fileUri);

						if (getIntent().hasExtra(EXTRA_FILENAME_LIST)) {
							fileNames = new ArrayList<>();
							String fileName = getIntent().getStringExtra(EXTRA_FILENAME_LIST);

							fileNames.add(fileName);
						}

						organizeFiles(fileUris, fileNames);
					}
					break;
				case ACTION_SEND_MULTIPLE:
				case Intent.ACTION_SEND_MULTIPLE:
					ArrayList<Uri> fileUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
					ArrayList<CharSequence> fileNames = getIntent().hasExtra(EXTRA_FILENAME_LIST) ? getIntent().getCharSequenceArrayListExtra(EXTRA_FILENAME_LIST) : null;

					organizeFiles(fileUris, fileNames);
					break;
				default:
					Toast.makeText(this, R.string.mesg_formatNotSupported, Toast.LENGTH_SHORT).show();
					finish();
			}
		}
	}

	protected void organizeFiles(final ArrayList<Uri> fileUris, final ArrayList<CharSequence> fileNames)
	{
		mFiles.clear();

		resetProgressItems();

		getProgressDialog().setMax(fileUris.size());
		getProgressDialog().setMessage(getString(R.string.mesg_organizingFiles));
		getProgressDialog().show();

		runOnWorkerService(new WorkerService.RunningTask(TAG, WORKER_TASK_LOAD_ITEMS)
		{
			@Override
			public void onRun()
			{
				for (int position = 0; position < fileUris.size(); position++) {
					if (getDefaultInterrupter().interrupted())
						break;

					getProgressDialog().setProgress(getProgressDialog().getProgress() + 1);

					Uri fileUri = fileUris.get(position);
					String fileName = fileNames != null ? String.valueOf(fileNames.get(position)) : null;

					try {
						SelectableStream selectableStream = new SelectableStream(ShareActivity.this, fileUri, null);

						if (selectableStream.getDocumentFile().isDirectory())
							createFolderStructure(selectableStream.getDocumentFile(), selectableStream.getDocumentFile().getName());
						else {
							if (fileName != null)
								selectableStream.setFriendlyName(fileName);

							mFiles.add(selectableStream);
						}
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
				}

				if (getDefaultInterrupter().interrupted()) {
					mFiles.clear();

					if (getDefaultInterrupter().interruptedByUser())
						finish();
				} else
					runOnUiThread(new Runnable()
					{
						@Override
						public void run()
						{
							getProgressDialog().dismiss();

							if (getSupportActionBar() != null && mFiles.size() > 0)
								getSupportActionBar().setTitle(mFiles.size() == 1
										? mFiles.get(0).getSelectableFriendlyName()
										: getResources().getQuantityString(R.plurals.text_itemSelected, mFiles.size(), mFiles.size()));

							onRequestReady();
						}
					});
			}
		});
	}

	private void requestQRScan()
	{
		mQRScanRequested = false;

		IntentIntegrator integrator = new IntentIntegrator(this)
				.setCaptureActivity(QRScannerActivity.class)
				.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE_TYPES)
				.setPrompt(getString(R.string.text_scanQRCodeHelp))
				.setOrientationLocked(false)
				.setBarcodeImageEnabled(true)
				.setBeepEnabled(false);

		integrator.initiateScan();
	}

	protected ProgressDialog resetProgressItems()
	{
		getDefaultInterrupter().reset();
		getProgressDialog().dismiss();

		mProgressDialog = new ProgressDialog(this);

		getProgressDialog().setCancelable(false);
		getProgressDialog().setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		getProgressDialog().setMax(0);
		getProgressDialog().setProgress(0);
		getProgressDialog().setSecondaryProgress(0);
		getProgressDialog().setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.butn_cancel), new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialogInterface, int i)
			{
				getDefaultInterrupter().interrupt();
			}
		});

		return getProgressDialog();
	}

	public boolean runOnWorkerService(WorkerService.RunningTask runningTask)
	{
		if (mWorkerService == null)
			return false;

		mWorkerService.run(runningTask.setInterrupter(getDefaultInterrupter()));

		return true;
	}

	protected void showChooserDialog(final NetworkDevice device)
	{
		device.isRestricted = false;
		mDatabase.publish(device);

		new ConnectionChooserDialog(ShareActivity.this, mDatabase, device, new ConnectionChooserDialog.OnDeviceSelectedListener()
		{
			@Override
			public void onDeviceSelected(final NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> availableInterfaces)
			{
				doCommunicate(device, connection);
			}
		}, true).show();
	}

	private class WorkerConnection implements ServiceConnection
	{
		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			mWorkerService = ((WorkerService.LocalBinder) service).getService();
			initialize();
		}

		@Override
		public void onServiceDisconnected(ComponentName name)
		{
			finish();
		}
	}

	private class SelectableStream implements Selectable
	{
		private String mDirectory;
		private String mFriendlyName;
		private DocumentFile mFile;
		private boolean mSelected = true;

		public SelectableStream(DocumentFile documentFile, String directory)
		{
			mFile = documentFile;
			mDirectory = directory;
			mFriendlyName = mFile.getName();
		}

		public SelectableStream(Context context, Uri uri, String directory) throws FileNotFoundException
		{
			this(FileUtils.fromUri(context, uri), directory);
		}

		public String getDirectory()
		{
			return mDirectory;
		}

		public DocumentFile getDocumentFile()
		{
			return mFile;
		}

		@Override
		public String getSelectableFriendlyName()
		{
			return mFriendlyName;
		}

		@Override
		public boolean isSelectableSelected()
		{
			return mSelected;
		}

		public void setFriendlyName(String friendlyName)
		{
			mFriendlyName = friendlyName;
		}

		@Override
		public void setSelectableSelected(boolean selected)
		{
			mSelected = selected;
		}
	}
}
