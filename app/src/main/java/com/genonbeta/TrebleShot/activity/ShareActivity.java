package com.genonbeta.TrebleShot.activity;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceChooserDialog;
import com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.JsonResponseHandler;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.io.StreamInfo;
import com.genonbeta.TrebleShot.service.Keyword;
import com.genonbeta.TrebleShot.util.TransactionObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.util.ArrayList;

public class ShareActivity extends Activity
{
	public static final String TAG = "ShareActivity";

	public static final int REQUEST_CODE_EDIT_BOX = 1;

	public static final String ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND";
	public static final String ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE";

	public static final String EXTRA_FILENAME_LIST = "extraFileNames";
	public static final String EXTRA_DEVICE_ID = "extraDeviceId";

	private EditText mStatusText;
	private NetworkDeviceListFragment mDeviceListFragment;
	private ProgressDialog mProgressOrganizeFiles;
	private ProgressDialog mProgressConnect;
	private ConnectionHandler mConnectionHandler;
	private ArrayList<StreamInfo> mFiles = new ArrayList<>();
	private StatusUpdateReceiver mStatusReceiver = new StatusUpdateReceiver();
	private IntentFilter mStatusReceiverFilter = new IntentFilter();
	private AlertDialog mShownDeviceChooserDialog;
	private DeviceChooserDialog mDeviceChooserDialog;
	private AccessDatabase mDatabase;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_share);

		mDatabase = new AccessDatabase(getApplicationContext());
		mDeviceListFragment = (NetworkDeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_share_fragment);
		mStatusText = findViewById(R.id.activity_share_info_text);

		mProgressOrganizeFiles = new ProgressDialog(this);
		mProgressOrganizeFiles.setIndeterminate(true);
		mProgressOrganizeFiles.setCancelable(false);
		mProgressOrganizeFiles.setMessage(getString(R.string.mesg_organizingFiles));

		mProgressConnect = new ProgressDialog(this);
		mProgressConnect.setIndeterminate(true);
		mProgressConnect.setCancelable(false);
		mProgressConnect.setMessage(getString(R.string.mesg_communicating));

		// FIXME: 3.11.2017 define new constanst in table of Connection and use observer

		mDeviceListFragment.setOnListClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				showChooserDialog((NetworkDevice) mDeviceListFragment.getListAdapter().getItem(position));
			}
		});

		if (getIntent() != null && getIntent().getAction() != null) {
			String action = getIntent().getAction();

			switch (action) {
				case ACTION_SEND:
				case Intent.ACTION_SEND:
					if (getIntent().hasExtra(Intent.EXTRA_TEXT)) {
						appendStatusText(getIntent().getStringExtra(Intent.EXTRA_TEXT));

						ImageView editButton = (ImageView) findViewById(R.id.activity_share_edit_button);

						editButton.setVisibility(View.VISIBLE);
						editButton.setOnClickListener(new View.OnClickListener()
						{
							@Override
							public void onClick(View view)
							{
								startActivityForResult(new Intent(ShareActivity.this, TextEditorActivity.class)
										.setAction(TextEditorActivity.ACTION_EDIT_TEXT)
										.putExtra(TextEditorActivity.EXTRA_TEXT_INDEX, mStatusText.getText().toString()), REQUEST_CODE_EDIT_BOX);
							}
						});

						registerHandler(new ConnectionHandler()
						{
							@Override
							public void onHandle(CoolCommunication.Messenger.Process process, JSONObject json, NetworkDevice device, NetworkDevice.Connection connection) throws JSONException
							{
								json.put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD);
								json.put(Keyword.CLIPBOARD_TEXT, mStatusText.getText().toString());
							}

							@Override
							public void onError(CoolCommunication.Messenger.Process process, NetworkDevice device, NetworkDevice.Connection connection)
							{

							}
						});
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

						registerClickListenerFiles(fileUris, fileNames);
					}
					break;
				case ACTION_SEND_MULTIPLE:
				case Intent.ACTION_SEND_MULTIPLE:
					ArrayList<Uri> fileUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
					ArrayList<CharSequence> fileNames = getIntent().hasExtra(EXTRA_FILENAME_LIST) ? getIntent().getCharSequenceArrayListExtra(EXTRA_FILENAME_LIST) : null;

					registerClickListenerFiles(fileUris, fileNames);
					break;
				default:
					Toast.makeText(this, R.string.mesg_formatNotSupported, Toast.LENGTH_SHORT).show();
					finish();
			}

			if (mConnectionHandler != null && getIntent().hasExtra(EXTRA_DEVICE_ID)) {
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
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		registerReceiver(mStatusReceiver, mStatusReceiverFilter);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		unregisterReceiver(mStatusReceiver);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == RESULT_OK)
			if (requestCode == REQUEST_CODE_EDIT_BOX && data != null && data.hasExtra(TextEditorActivity.EXTRA_TEXT_INDEX))
				appendStatusText(data.getStringExtra(TextEditorActivity.EXTRA_TEXT_INDEX));
	}

	protected void appendStatusText(CharSequence charSequence)
	{
		mStatusText.getText().clear();
		mStatusText.getText().append(charSequence);
	}

	protected void organizeFiles(final ArrayList<Uri> fileUris, final ArrayList<CharSequence> fileNames)
	{
		mProgressOrganizeFiles.show();

		new Thread()
		{
			@Override
			public void run()
			{
				super.run();

				ContentResolver contentResolver = getApplicationContext().getContentResolver();

				for (int position = 0; position < fileUris.size(); position++) {
					Uri fileUri = fileUris.get(position);
					String fileName = fileNames != null ? String.valueOf(fileNames.get(position)) : null;

					try {
						StreamInfo streamInfo = StreamInfo.getStreamInfo(getApplicationContext(), fileUri);

						if (fileName != null)
							streamInfo.friendlyName = fileName;

						mFiles.add(streamInfo);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (StreamCorruptedException e) {
						e.printStackTrace();
					}
				}

				mProgressOrganizeFiles.cancel();

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if (mFiles.size() == 1)
							appendStatusText(mFiles.get(0).friendlyName);
						else if (mFiles.size() > 1)
							appendStatusText(getResources().getQuantityString(R.plurals.text_itemSelected, mFiles.size(), mFiles.size()));
					}
				});
			}
		}.start();
	}

	protected void registerHandler(ConnectionHandler handler)
	{
		mConnectionHandler = handler;
	}

	protected void registerClickListenerFiles(final ArrayList<Uri> fileUris,
											  final ArrayList<CharSequence> fileNames)
	{
		organizeFiles(fileUris, fileNames);

		registerHandler(new ConnectionHandler()
		{
			private int mGroupId;

			@Override
			public void onHandle(CoolCommunication.Messenger.Process process, JSONObject json, NetworkDevice device, NetworkDevice.Connection connection) throws JSONException
			{
				JSONArray filesArray = new JSONArray();

				mGroupId = AppUtils.getUniqueNumber();

				TransactionObject.Group groupInstance = new TransactionObject.Group(mGroupId, device.deviceId, connection.adapterName);
				mDatabase.publish(groupInstance);

				json.put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER);
				json.put(Keyword.GROUP_ID, mGroupId);

				for (StreamInfo fileState : mFiles) {
					int requestId = AppUtils.getUniqueNumber();
					TransactionObject transactionObject = new TransactionObject(requestId, groupInstance.groupId, fileState.friendlyName, fileState.uri.toString(), fileState.mimeType, 0, TransactionObject.Type.OUTGOING);
					JSONObject thisJson = new JSONObject();

					try {
						thisJson.put(Keyword.FILE_NAME, fileState.friendlyName);
						thisJson.put(Keyword.FILE_SIZE, fileState.size);
						thisJson.put(Keyword.REQUEST_ID, requestId);
						thisJson.put(Keyword.FILE_MIME, fileState.mimeType);

						filesArray.put(thisJson);

						mDatabase.publish(transactionObject);
					} catch (Exception e) {
						Log.e(TAG, "Sender error on fileUri: " + e.getClass().getName() + " : " + fileState.friendlyName);
					}
				}

				json.put(Keyword.FILES_INDEX, filesArray);
			}

			@Override
			public void onError(CoolCommunication.Messenger.Process process, NetworkDevice device, NetworkDevice.Connection connection)
			{
				// TODO: 3.11.2017 Remove group definer from the database
			}
		});
	}

	protected void showChooserDialog(final NetworkDevice device)
	{
		device.isRestricted = false;
		device.lastUsageTime = System.currentTimeMillis();

		mDatabase.publish(device);

		mDeviceChooserDialog = new DeviceChooserDialog(ShareActivity.this, mDatabase, device, new DeviceChooserDialog.OnDeviceSelectedListener()
		{
			@Override
			public void onDeviceSelected(final NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> availableInterfaces)
			{
				final String deviceIp = connection.ipAddress;

				mProgressConnect.show();

				CoolCommunication.Messenger.send(deviceIp, AppConfig.COMMUNATION_SERVER_PORT, null,
						new JsonResponseHandler()
						{
							@Override
							public void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json)
							{
								try {
									NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

									json.put(Keyword.SERIAL, localDevice.deviceId);

									mConnectionHandler.onHandle(process, json, device, connection);

									JSONObject jsonObject = new JSONObject(process.waitForResponse());

									if (!jsonObject.has(Keyword.RESULT) || !jsonObject.getBoolean(Keyword.RESULT))
										mConnectionHandler.onError(process, device, connection);

									if (jsonObject.has(Keyword.RESULT) && !jsonObject.getBoolean(Keyword.RESULT)) {
										Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), R.string.mesg_notAllowed, Snackbar.LENGTH_LONG);

										snackbar.setAction(R.string.ques_why, new View.OnClickListener()
										{
											@Override
											public void onClick(View v)
											{
												AlertDialog.Builder builder = new AlertDialog.Builder(ShareActivity.this);

												builder.setMessage(getString(R.string.text_notAllowedHelp,
														device.user,
														AppUtils.getLocalDeviceName(ShareActivity.this)));

												builder.setNegativeButton(R.string.butn_close, null);
												builder.show();
											}
										});

										snackbar.show();
									}
								} catch (Exception e) {
									mConnectionHandler.onError(process, device, connection);
									showToast(getString(R.string.mesg_fileSendError, getString(R.string.text_communicationProblem)));
								}

								mProgressConnect.cancel();
							}

							@Override
							public void onError(Exception e)
							{
								mProgressConnect.cancel();
								mConnectionHandler.onError(null, device, connection);
								showToast(getString(R.string.mesg_fileSendError, getString(R.string.text_connectionProblem)));
							}
						}
				);
			}
		});

		mShownDeviceChooserDialog = mDeviceChooserDialog.show();
	}

	protected void showToast(String msg)
	{
		Looper.prepare();
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		Looper.loop();
	}

	private interface ConnectionHandler
	{
		void onHandle(com.genonbeta.CoolSocket.CoolCommunication.Messenger.Process process, JSONObject json, NetworkDevice device, NetworkDevice.Connection connection) throws JSONException;

		void onError(com.genonbeta.CoolSocket.CoolCommunication.Messenger.Process process, NetworkDevice device, NetworkDevice.Connection connection);
	}

	private class StatusUpdateReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (mShownDeviceChooserDialog != null && mShownDeviceChooserDialog.isShowing()) {
				mShownDeviceChooserDialog.cancel();

				if (mDeviceChooserDialog != null)
					mShownDeviceChooserDialog = mDeviceChooserDialog.show();
			}
		}
	}
}
