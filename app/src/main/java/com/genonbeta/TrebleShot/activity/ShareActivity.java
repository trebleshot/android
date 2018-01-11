package com.genonbeta.TrebleShot.activity;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.util.Log;
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
import com.genonbeta.TrebleShot.dialog.SelectedEditorDialog;
import com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment;
import com.genonbeta.TrebleShot.io.StreamInfo;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.Selectable;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.util.AddressedInterface;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.Interrupter;
import com.genonbeta.TrebleShot.util.NetworkDeviceInfoLoader;
import com.genonbeta.TrebleShot.util.NetworkUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StreamCorruptedException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;

public class ShareActivity extends Activity
{
	public static final String TAG = "ShareActivity";

	public static final int REQUEST_CODE_EDIT_BOX = 1;

	public static final String ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND";
	public static final String ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE";

	public static final String EXTRA_FILENAME_LIST = "extraFileNames";
	public static final String EXTRA_DEVICE_ID = "extraDeviceId";

	private ArrayList<SelectableStream> mFiles = new ArrayList<>();
	private String mSharedText;
	private AccessDatabase mDatabase;
	private ProgressDialog mProgressDialog;
	private NetworkDeviceListFragment mDeviceListFragment;
	private Toolbar mToolbar;
	private FloatingActionButton mFAB;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_share);

		mToolbar = findViewById(R.id.toolbar);

		mFAB = findViewById(R.id.content_fab);
		mProgressDialog = new ProgressDialog(this);
		mDatabase = new AccessDatabase(getApplicationContext());
		mDeviceListFragment = (NetworkDeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_share_fragment);

		setSupportActionBar(mToolbar);

		mToolbar.setTitle(R.string.text_shareWithTrebleshot);
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

				if (device instanceof NetworkDeviceListAdapter.HotspotNetwork) {
					final NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork = (NetworkDeviceListAdapter.HotspotNetwork) device;
					final Interrupter interrupter = new Interrupter();

					mProgressDialog.setMessage(getString(R.string.mesg_connectingToSelfHotspot));
					mProgressDialog.setCancelable(false);
					mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
					mProgressDialog.setMax(20);
					mProgressDialog.setProgress(0);
					mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.butn_cancel), new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick(DialogInterface dialogInterface, int i)
						{
							interrupter.interrupt();
						}
					});

					mProgressDialog.show();

					new Thread()
					{
						private boolean mConnected = false;
						private long mStartTime = System.currentTimeMillis();
						private String mRemoteAddress;

						@Override
						public void run()
						{
							super.run();

							if (!mDeviceListFragment.isConnectedToNetwork(hotspotNetwork))
								mDeviceListFragment.toggleConnection(hotspotNetwork);

							while (mRemoteAddress == null) {
								int passedTime = (int) (System.currentTimeMillis() - mStartTime);

								for (AddressedInterface addressedInterface : NetworkUtils.getInterfaces(true, null)) {
									if (addressedInterface.getNetworkInterface().getDisplayName().startsWith(AppConfig.NETWORK_INTERFACE_WIFI)) {
										String remoteAddress = NetworkUtils.getAddressPrefix(addressedInterface.getAssociatedAddress()) + "1";

										if (NetworkUtils.ping(remoteAddress, 1000)) {
											mRemoteAddress = remoteAddress;
											break;
										}
									}
								}

								try {
									Thread.sleep(1000);
									mProgressDialog.setProgress(passedTime / 1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								} finally {
									if (passedTime > 20000 || interrupter.interrupted())
										break;
								}
							}

							if (mRemoteAddress != null) {
								try {
									NetworkDeviceInfoLoader.load(true, mDatabase, mRemoteAddress, new NetworkDeviceInfoLoader.OnDeviceRegisteredErrorListener()
									{
										@Override
										public void onError(Exception error)
										{
											mProgressDialog.cancel();
										}

										@Override
										public void onDeviceRegistered(AccessDatabase database, final NetworkDevice device, final NetworkDevice.Connection connection)
										{
											mConnected = true;

											runOnUiThread(new Runnable()
											{
												@Override
												public void run()
												{
													mProgressDialog.cancel();
													doCommunicate(device, connection);
												}
											});
										}
									});
								} catch (ConnectException e) {
									e.printStackTrace();
								}
							}

							if (!mConnected) {
								mProgressDialog.cancel();
								createSnackbar(R.string.mesg_connectionFailure)
										.show();
							}

							// We can't add dialog outside of the else statement as it may close other dialogs as well
						}
					}.start();
				} else
					showChooserDialog(device);
			}
		});

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

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK)
			if (requestCode == REQUEST_CODE_EDIT_BOX && data != null && data.hasExtra(TextEditorActivity.EXTRA_TEXT_INDEX))
				mSharedText = data.getStringExtra(TextEditorActivity.EXTRA_TEXT_INDEX);
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

	protected void createFolderStructure(Interrupter interrupter, ProgressDialog dialog, File file, String folderName)
	{
		File[] files = file.listFiles();

		if (files != null)
			for (File thisFile : files) {
				if (interrupter.interrupted())
					break;

				if (thisFile.isDirectory()) {
					createFolderStructure(interrupter, dialog, thisFile, (folderName != null ? folderName + File.separator : null) + thisFile.getName());
					continue;
				}

				dialog.setMax(dialog.getMax() + 1);
				dialog.setProgress(dialog.getProgress() + 1);

				try {
					mFiles.add(new SelectableStream(getApplicationContext(), Uri.fromFile(thisFile), false, folderName));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
	}

	protected Snackbar createSnackbar(int resId, String... objects)
	{
		return Snackbar.make(mDeviceListFragment.getListView(), getString(resId, objects), Snackbar.LENGTH_LONG);
	}

	protected void doCommunicate(final NetworkDevice device, final NetworkDevice.Connection connection)
	{
		final String deviceIp = connection.ipAddress;
		final Interrupter interrupter = new Interrupter();

		mProgressDialog.setMessage(getString(R.string.mesg_communicating));
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setMax(0);
		mProgressDialog.setCancelable(false);
		mProgressDialog.setProgress(0);
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.butn_cancel), new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialogInterface, int i)
			{
				interrupter.interrupt();
			}
		});

		mProgressDialog.show();

		CoolSocket.connect(new CoolSocket.Client.ConnectionHandler()
		{
			@Override
			public void onConnect(CoolSocket.Client client)
			{
				try {
					final CoolSocket.ActiveConnection activeConnection = client.connect(new InetSocketAddress(deviceIp, AppConfig.COMMUNICATION_SERVER_PORT), AppConfig.DEFAULT_SOCKET_LARGE_TIMEOUT);

					JSONObject clientResponse;
					JSONObject jsonRequest = new JSONObject()
							.put(Keyword.SERIAL, AppUtils.getLocalDevice(getApplicationContext()).deviceId);

					if (mSharedText == null) {
						JSONArray filesArray = new JSONArray();
						int groupId = AppUtils.getUniqueNumber();
						TransactionObject.Group groupInstance = new TransactionObject.Group(groupId, device.deviceId, connection.adapterName);

						jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER);
						jsonRequest.put(Keyword.GROUP_ID, groupId);

						ArrayList<TransactionObject> pendingRegistry = new ArrayList<>();

						mProgressDialog.setMax(mFiles.size());

						for (SelectableStream selectableStream : mFiles) {
							if (interrupter.interrupted())
								break;

							if (!selectableStream.isSelectableSelected())
								continue;

							mProgressDialog.setSecondaryProgress(mProgressDialog.getSecondaryProgress() + 1);

							int requestId = AppUtils.getUniqueNumber();
							JSONObject thisJson = new JSONObject();

							TransactionObject transactionObject = new TransactionObject(requestId, groupInstance.groupId, selectableStream.friendlyName, selectableStream.uri.toString(), selectableStream.mimeType, selectableStream.size, TransactionObject.Type.OUTGOING);

							if (selectableStream.directory != null)
								transactionObject.directory = selectableStream.directory;

							pendingRegistry.add(transactionObject);

							try {
								thisJson.put(Keyword.FILE_NAME, selectableStream.friendlyName);
								thisJson.put(Keyword.FILE_SIZE, selectableStream.size);
								thisJson.put(Keyword.REQUEST_ID, requestId);
								thisJson.put(Keyword.FILE_MIME, selectableStream.mimeType);

								if (selectableStream.directory != null)
									thisJson.put(Keyword.DIRECTORY, selectableStream.directory);

								filesArray.put(thisJson);
							} catch (Exception e) {
								Log.e(TAG, "Sender error on fileUri: " + e.getClass().getName() + " : " + selectableStream.friendlyName);
							}
						}

						jsonRequest.put(Keyword.FILES_INDEX, filesArray);

						activeConnection.reply(jsonRequest.toString());
						CoolSocket.ActiveConnection.Response response = activeConnection.receive();

						clientResponse = new JSONObject(response.response);

						if (clientResponse.has(Keyword.RESULT) && clientResponse.getBoolean(Keyword.RESULT)) {
							mDatabase.publish(groupInstance);

							for (TransactionObject transactionObject : pendingRegistry) {
								if (interrupter.interrupted())
									break;

								mProgressDialog.setProgress(mProgressDialog.getProgress() + 1);
								mDatabase.publish(transactionObject);
							}

							if (interrupter.interrupted())
								mDatabase.remove(groupInstance);
							else
								TransactionActivity.startInstance(getApplicationContext(), groupInstance.groupId);
						}
					} else {
						jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD);
						jsonRequest.put(Keyword.CLIPBOARD_TEXT, mSharedText);

						activeConnection.reply(jsonRequest.toString());
						CoolSocket.ActiveConnection.Response response = activeConnection.receive();

						clientResponse = new JSONObject(response.response);
					}

					if (clientResponse.has(Keyword.RESULT) && !clientResponse.getBoolean(Keyword.RESULT)) {
						if (clientResponse.has(Keyword.ERROR) && clientResponse.getString(Keyword.ERROR).equals(Keyword.NOT_ALLOWED))
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

					device.lastUsageTime = System.currentTimeMillis();
					mDatabase.publish(device);
				} catch (Exception e) {
					e.printStackTrace();
					createSnackbar(R.string.mesg_fileSendError, getString(R.string.text_connectionProblem))
							.show();
				}

				mProgressDialog.cancel();
			}
		});
	}

	protected void fabClicked()
	{
		if (mSharedText != null)
			mFAB.setOnClickListener(new View.OnClickListener()
			{
				@Override
				public void onClick(View view)
				{
					startActivityForResult(new Intent(ShareActivity.this, TextEditorActivity.class)
							.setAction(TextEditorActivity.ACTION_EDIT_TEXT)
							.putExtra(TextEditorActivity.EXTRA_TEXT_INDEX, mSharedText)
							.putExtra(TextEditorActivity.EXTRA_SUPPORT_APPLY, true), REQUEST_CODE_EDIT_BOX);
				}
			});
		else
			new SelectedEditorDialog<>(this, mFiles).show();
	}

	protected void organizeFiles(final ArrayList<Uri> fileUris, final ArrayList<CharSequence> fileNames)
	{
		final Interrupter interrupter = new Interrupter();

		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setMax(fileUris.size());
		mProgressDialog.setCancelable(false);
		mProgressDialog.setMessage(getString(R.string.mesg_organizingFiles));
		mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.butn_cancel), new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialogInterface, int i)
			{
				interrupter.interrupt();
			}
		});

		mProgressDialog.show();

		new Thread()
		{
			@Override
			public void run()
			{
				super.run();

				for (int position = 0; position < fileUris.size(); position++) {
					if (interrupter.interrupted())
						break;

					mProgressDialog.setProgress(mProgressDialog.getProgress() + 1);

					Uri fileUri = fileUris.get(position);
					String fileName = fileNames != null ? String.valueOf(fileNames.get(position)) : null;

					try {
						SelectableStream selectableStream = new SelectableStream(getApplicationContext(), fileUri, false, null);

						if (fileName != null)
							selectableStream.friendlyName = fileName;

						mFiles.add(selectableStream);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (StreamCorruptedException e) {
						e.printStackTrace();
					} catch (StreamInfo.FolderStateException e) {
						File parentFolder = new File(URI.create(fileUri.toString()));
						createFolderStructure(interrupter, mProgressDialog, parentFolder, parentFolder.getName());
					}
				}

				if (interrupter.interrupted()) {
					mFiles.clear();
					finish();
				}

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						mProgressDialog.cancel();

						if (mFiles.size() == 1)
							mToolbar.setTitle(mFiles.get(0).friendlyName);
						else if (mFiles.size() > 1)
							mToolbar.setTitle((getResources().getQuantityString(R.plurals.text_itemSelected, mFiles.size(), mFiles.size())));

						onRequestReady();
					}
				});
			}
		}.start();
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
		}).show();
	}

	private class SelectableStream
			extends StreamInfo
			implements Selectable
	{
		private boolean isSelected = true;

		public String directory;

		public SelectableStream(Context context, Uri uri, boolean openStreams, String directory) throws FileNotFoundException, StreamCorruptedException, FolderStateException
		{
			super(context, uri, openStreams);
			this.directory = directory;
		}

		@Override
		public String getSelectableFriendlyName()
		{
			return this.friendlyName;
		}

		@Override
		public boolean isSelectableSelected()
		{
			return this.isSelected;
		}

		@Override
		public void setSelectableSelected(boolean selected)
		{
			this.isSelected = selected;
		}
	}
}
