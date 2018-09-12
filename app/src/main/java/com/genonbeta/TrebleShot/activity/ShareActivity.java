package com.genonbeta.TrebleShot.activity;

import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ConnectionChooserDialog;
import com.genonbeta.TrebleShot.fragment.ConnectDevicesFragment;
import com.genonbeta.TrebleShot.fragment.inner.SelectionListFragment;
import com.genonbeta.TrebleShot.fragment.inner.TextViewerFragment;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.ui.UITask;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.object.Selectable;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.android.framework.util.Interrupter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

public class ShareActivity extends Activity
		implements SnackbarSupport, SelectionListFragment.ReadyLoadListener, TextViewerFragment.ReadyLoadListener, Activity.OnPreloadArgumentWatcher
{
	public static final String TAG = "ShareActivity";

	public static final int WORKER_TASK_LOAD_ITEMS = 1;
	public static final int WORKER_TASK_CONNECT_SERVER = 2;

	public static final int REQUEST_CODE_EDIT_BOX = 1;

	public static final String ACTION_ADD_DEVICES = "genonbeta.intent.action.ADD_DEVICES";
	public static final String ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND";
	public static final String ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE";

	public static final String EXTRA_FILENAME_LIST = "extraFileNames";
	public static final String EXTRA_DEVICE_ID = "extraDeviceId";
	public static final String EXTRA_GROUP_ID = "extraGroupId";

	private Toolbar mToolbar;
	private String mAction;
	private long mGroupId;
	private ConnectDevicesFragment mConnectDevicesFragment;
	private ArrayList<SelectableStream> mFiles = new ArrayList<>();
	private String mSharedText;
	private ProgressDialog mProgressDialog;
	private Interrupter mInterrupter = new Interrupter();
	private WorkerService mWorkerService;
	private WorkerConnection mWorkerConnection = new WorkerConnection();
	private Bundle mPreLoadingBundle = new Bundle();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		bindService(new Intent(this, WorkerService.class), mWorkerConnection, Context.BIND_AUTO_CREATE);
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
		}
	}

	protected void onRequestReady()
	{
		if (getIntent().hasExtra(EXTRA_DEVICE_ID)) {
			String deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
			NetworkDevice chosenDevice = new NetworkDevice(deviceId);

			try {
				getDatabase().reconstruct(chosenDevice);
				showChooserDialog(chosenDevice);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		Fragment fragment = mConnectDevicesFragment.getPagerAdapter().getItem(0);

		if (fragment instanceof SelectionListFragment)
			((SelectionListFragment) fragment).refreshList();
		else if (fragment instanceof TextViewerFragment)
			((TextViewerFragment) fragment).updateText();
	}

	@Override
	public ArrayList<? extends Selectable> onSelectionReadyLoad()
	{
		return mFiles;
	}

	@Override
	public CharSequence onTextViewerReadyLoad()
	{
		return mSharedText;
	}

	@Override
	public void onTextViewerEditRequested()
	{
		startActivityForResult(new Intent(ShareActivity.this, TextEditorActivity.class)
				.setAction(TextEditorActivity.ACTION_EDIT_TEXT)
				.putExtra(TextEditorActivity.EXTRA_TEXT_INDEX, mSharedText)
				.putExtra(TextEditorActivity.EXTRA_SUPPORT_APPLY, true), REQUEST_CODE_EDIT_BOX);
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

	public Snackbar createSnackbar(int resId, Object... objects)
	{
		return Snackbar.make(mConnectDevicesFragment.getViewPager(), getString(resId, objects), Snackbar.LENGTH_LONG);
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
				publishStatusText(getString(R.string.mesg_communicating));

				CommunicationBridge.connect(getDatabase(), true, new CommunicationBridge.Client.ConnectionHandler()
				{
					@Override
					public void onConnect(CommunicationBridge.Client client)
					{
						client.setDevice(device);

						try {
							final JSONObject jsonRequest = new JSONObject();
							final TransferGroup groupInstance = new TransferGroup(AppUtils.getUniqueNumber());
							final TransferGroup.Assignee assignee = new TransferGroup.Assignee(groupInstance, device, connection);
							final ArrayList<TransferObject> pendingRegistry = new ArrayList<>();

							if (device instanceof NetworkDeviceListAdapter.HotspotNetwork
									&& ((NetworkDeviceListAdapter.HotspotNetwork) device).qrConnection)
								jsonRequest.put(Keyword.FLAG_TRANSFER_QR_CONNECTION, true);

							switch (mAction) {
								case Intent.ACTION_SEND:
								case Intent.ACTION_SEND_MULTIPLE:
								case ACTION_SEND:
								case ACTION_SEND_MULTIPLE:
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

											long requestId = AppUtils.getUniqueNumber();
											JSONObject thisJson = new JSONObject();

											TransferObject transferObject = new TransferObject(requestId,
													groupInstance.groupId,
													selectableStream.getSelectableTitle(),
													selectableStream.getDocumentFile().getUri().toString(),
													selectableStream.getDocumentFile().getType(),
													selectableStream.getDocumentFile().length(), TransferObject.Type.OUTGOING);

											if (selectableStream.mDirectory != null)
												transferObject.directory = selectableStream.mDirectory;

											pendingRegistry.add(transferObject);

											try {
												thisJson.put(Keyword.INDEX_FILE_NAME, transferObject.friendlyName);
												thisJson.put(Keyword.INDEX_FILE_SIZE, transferObject.fileSize);
												thisJson.put(Keyword.TRANSFER_REQUEST_ID, requestId);
												thisJson.put(Keyword.INDEX_FILE_MIME, transferObject.fileMimeType);

												if (selectableStream.mDirectory != null)
													thisJson.put(Keyword.INDEX_DIRECTORY, selectableStream.mDirectory);

												filesArray.put(thisJson);
											} catch (Exception e) {
												Log.e(TAG, "Sender error on fileUri: " + e.getClass().getName() + " : " + transferObject.friendlyName);
											}
										}

										jsonRequest.put(Keyword.FILES_INDEX, filesArray);
									} else {
										jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD);
										jsonRequest.put(Keyword.TRANSFER_CLIPBOARD_TEXT, mSharedText);
									}
									break;
								case ACTION_ADD_DEVICES:
									TransferGroup existingGroup = new TransferGroup(mGroupId);
									getDatabase().reconstruct(existingGroup);

									jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER);
									jsonRequest.put(Keyword.TRANSFER_GROUP_ID, existingGroup.groupId);

									ArrayList<TransferObject> pendingTransfers = getDatabase()
											.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
													.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND "
																	+ AccessDatabase.FIELD_TRANSFER_TYPE + "=?",
															String.valueOf(existingGroup.groupId),
															TransferObject.Type.OUTGOING.toString()), TransferObject.class);

									if (pendingTransfers.size() == 0)
										throw new Exception("Empty share holder id: " + existingGroup.groupId);

									JSONArray filesArray = new JSONArray();

									getProgressDialog().setMax(pendingTransfers.size());

									for (TransferObject transferObject : pendingTransfers) {
										if (getDefaultInterrupter().interrupted())
											throw new InterruptedException("Interrupted by user");

										getProgressDialog().setSecondaryProgress(getProgressDialog().getSecondaryProgress() + 1);

										JSONObject thisJson = new JSONObject();

										try {
											thisJson.put(Keyword.INDEX_FILE_NAME, transferObject.friendlyName);
											thisJson.put(Keyword.INDEX_FILE_SIZE, transferObject.fileSize);
											thisJson.put(Keyword.TRANSFER_REQUEST_ID, transferObject.requestId);
											thisJson.put(Keyword.INDEX_FILE_MIME, transferObject.fileMimeType);

											if (transferObject.directory != null)
												thisJson.put(Keyword.INDEX_DIRECTORY, transferObject.directory);

											filesArray.put(thisJson);
										} catch (Exception e) {
											Log.e(TAG, "Sender error on fileUri: " + e.getClass().getName() + " : " + transferObject.friendlyName);
										}
									}

									assignee.groupId = existingGroup.groupId;

									// so that if the user rejects it won't be removed from the sender
									assignee.isClone = true;

									jsonRequest.put(Keyword.FILES_INDEX, filesArray);

									getDefaultInterrupter().addCloser(new Interrupter.Closer()
									{
										@Override
										public void onClose(boolean userAction)
										{
											getDatabase().remove(assignee);
										}
									});
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
								switch (mAction) {
									case ACTION_ADD_DEVICES:
										getDatabase().publish(assignee);

										setResult(RESULT_OK, new Intent()
												.putExtra(EXTRA_DEVICE_ID, assignee.deviceId)
												.putExtra(EXTRA_GROUP_ID, assignee.groupId));

										finish();
										break;
									default:
										if (pendingRegistry.size() > 0) {
											getDatabase().insert(groupInstance);
											getDatabase().insert(assignee);

											getDefaultInterrupter().addCloser(new Interrupter.Closer()
											{
												@Override
												public void onClose(boolean userAction)
												{
													getDatabase().remove(groupInstance);
												}
											});

											for (TransferObject transferObject : pendingRegistry) {
												if (getDefaultInterrupter().interrupted())
													throw new InterruptedException("Interrupted by user");

												getProgressDialog().setProgress(mProgressDialog.getProgress() + 1);
												getDatabase().insert(transferObject);
											}

											TransactionActivity.startInstance(getApplicationContext(), groupInstance.groupId);
										}
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

	@Override
	public Intent getIntent()
	{
		return super.getIntent();
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
			String action = mAction = getIntent().getAction();

			switch (action) {
				case ACTION_SEND:
				case Intent.ACTION_SEND:
					if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
						mSharedText = getIntent().getStringExtra(Intent.EXTRA_TEXT);

						setupUi(UiType.TEXT);
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

						setupUi(UiType.FILE);
						organizeFiles(fileUris, fileNames);
					}

					break;
				case ACTION_SEND_MULTIPLE:
				case Intent.ACTION_SEND_MULTIPLE:
					ArrayList<Uri> fileUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
					ArrayList<CharSequence> fileNames = getIntent().hasExtra(EXTRA_FILENAME_LIST) ? getIntent().getCharSequenceArrayListExtra(EXTRA_FILENAME_LIST) : null;

					setupUi(UiType.FILE);
					organizeFiles(fileUris, fileNames);
					break;
				case ACTION_ADD_DEVICES:
					mGroupId = getIntent().getLongExtra(EXTRA_GROUP_ID, -1);

					setupUi(UiType.DEVICE_ADDITION);
					mToolbar.setTitle(R.string.text_addDevicesToTransfer);
					break;
				default:
					mAction = null;

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
				publishStatusText(getString(R.string.mesg_organizingFiles));

				for (int position = 0; position < fileUris.size(); position++) {
					if (getDefaultInterrupter().interrupted())
						break;

					getProgressDialog().setProgress(getProgressDialog().getProgress() + 1);
					publishStatusText(String.format(Locale.getDefault(), "%s - %d", getString(R.string.mesg_organizingFiles), getProgressDialog().getProgress()));

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

							if (getSupportActionBar() != null) {
								String title;

								if (mFiles.size() == 0)
									title = getString(R.string.text_migrateNotice49);
								else
									title = mFiles.size() == 1
											? mFiles.get(0).getSelectableTitle()
											: getResources().getQuantityString(R.plurals.text_itemSelected, mFiles.size(), mFiles.size());

								getSupportActionBar().setTitle(title);
							}

							onRequestReady();
						}
					});
			}
		});
	}

	@Override
	public Bundle passPreLoadingArguments()
	{
		return mPreLoadingBundle;
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

	protected void setupUi(UiType type)
	{
		ArrayList<SmartFragmentPagerAdapter.StableItem> cdfArguments = new ArrayList<>();

		switch (type) {
			case FILE:
				cdfArguments.add(new SmartFragmentPagerAdapter.StableItem(2000, SelectionListFragment.class, new Bundle())
						.setIconOnly(true));
				break;
			case TEXT:
				cdfArguments.add(new SmartFragmentPagerAdapter.StableItem(2001, TextViewerFragment.class, new Bundle())
						.setIconOnly(true));
				break;
		}

		mPreLoadingBundle.putParcelableArrayList(ConnectDevicesFragment.EXTRA_CDF_FRAGMENT_NAMES_FRONT, cdfArguments);

		setContentView(R.layout.activity_share);

		mConnectDevicesFragment = (ConnectDevicesFragment) getSupportFragmentManager().findFragmentById(R.id.content_fragment);
		mToolbar = findViewById(R.id.toolbar);

		setSupportActionBar(mToolbar);

		mConnectDevicesFragment.showDevices();
		mToolbar.setTitle(R.string.text_shareWithTrebleshot);

		final UIConnectionUtils connectionUtils = new UIConnectionUtils(ConnectionUtils.getInstance(getApplicationContext()), this);

		mConnectDevicesFragment.setDeviceSelectedListener(new NetworkDeviceSelectedListener()
		{
			@Override
			public boolean onNetworkDeviceSelected(NetworkDevice networkDevice, @Nullable NetworkDevice.Connection connection)
			{
				if (networkDevice instanceof NetworkDeviceListAdapter.HotspotNetwork) {
					connectionUtils.makeAcquaintance(ShareActivity.this, getDatabase(), new UITask()
					{
						@Override
						public void updateTaskStarted(Interrupter interrupter)
						{
							createSnackbar(R.string.mesg_communicating)
									.show();
						}

						@Override
						public void updateTaskStopped()
						{

						}
					}, networkDevice, -1, new NetworkDeviceLoader.OnDeviceRegisteredListener()
					{
						@Override
						public void onDeviceRegistered(AccessDatabase database, NetworkDevice device, NetworkDevice.Connection connection)
						{
							doCommunicate(device, connection);
						}
					});
				} else if (connection == null)
					showChooserDialog(networkDevice);
				else
					doCommunicate(networkDevice, connection);

				return true;
			}

			@Override
			public boolean isListenerEffective()
			{
				return true;
			}
		});
	}

	protected void showChooserDialog(final NetworkDevice device)
	{
		device.isRestricted = false;
		getDatabase().publish(device);

		new ConnectionChooserDialog(ShareActivity.this, device, new ConnectionChooserDialog.OnDeviceSelectedListener()
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
		public String getSelectableTitle()
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
		public boolean setSelectableSelected(boolean selected)
		{
			mSelected = selected;
			return true;
		}
	}

	enum UiType
	{
		TEXT,
		FILE,
		DEVICE_ADDITION
	}
}

