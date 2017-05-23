package com.genonbeta.TrebleShot.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.dialog.DeviceChooserDialog;
import com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;
import com.genonbeta.TrebleShot.helper.FileUtils;
import com.genonbeta.TrebleShot.helper.JsonResponseHandler;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.service.Keyword;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
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
	private Transaction mTransaction;
	private NetworkDeviceListFragment mDeviceListFragment;
	private DeviceRegistry mDeviceRegistry;
	private ProgressDialog mProgressOrganizeFiles;
	private ProgressDialog mProgressConnect;
	private ConnectionHandler mConnectionHandler;
	private ArrayList<FileState> mFiles = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_share);

		mTransaction = new Transaction(getApplicationContext());
		mDeviceRegistry = new DeviceRegistry(getApplicationContext());
		mDeviceListFragment = (NetworkDeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_share_fragment);
		mStatusText = (EditText) findViewById(R.id.activity_share_info_text);

		mProgressOrganizeFiles = new ProgressDialog(this);
		mProgressOrganizeFiles.setIndeterminate(true);
		mProgressOrganizeFiles.setCancelable(false);
		mProgressOrganizeFiles.setMessage(getString(R.string.progress_organize_files));

		mProgressConnect = new ProgressDialog(this);
		mProgressConnect.setIndeterminate(true);
		mProgressConnect.setCancelable(false);
		mProgressConnect.setMessage(getString(R.string.progress_communicate));

		mDeviceListFragment.setOnListClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				showChooserDialog((NetworkDevice) mDeviceListFragment.getListAdapter().getItem(position));
			}
		});

		if (getIntent() != null && getIntent().getAction() != null)
		{
			String action = getIntent().getAction();

			switch (action)
			{
				case ACTION_SEND:
				case Intent.ACTION_SEND:
					if (getIntent().hasExtra(Intent.EXTRA_TEXT))
					{
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
							public void onHandle(CoolCommunication.Messenger.Process process, JSONObject json, NetworkDevice device, String chosenIp)
							{
								try
								{
									json.put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD);
									json.put(Keyword.CLIPBOARD_TEXT, mStatusText.getText().toString());

									JSONObject response = new JSONObject(process.waitForResponse());

									if (response.getBoolean(Keyword.RESULT))
									{

									}
									else
										showToast(getString(R.string.file_sending_error_msg, getString(R.string.not_allowed_error)));
								} catch (Exception e)
								{
									showToast(getString(R.string.file_sending_error_msg, getString(R.string.communication_problem)));
								}
							}
						});
					}
					else
					{
						ArrayList<Uri> fileUris = new ArrayList<>();
						ArrayList<CharSequence> fileNames = null;
						Uri fileUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);

						fileUris.add(fileUri);

						if (getIntent().hasExtra(EXTRA_FILENAME_LIST))
						{
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
					Toast.makeText(this, R.string.type_not_supported_msg, Toast.LENGTH_SHORT).show();
					finish();
			}

			if (mConnectionHandler != null && getIntent().hasExtra(EXTRA_DEVICE_ID))
			{
				String deviceId = getIntent().getStringExtra(EXTRA_DEVICE_ID);
				NetworkDevice chosenDevice = mDeviceRegistry.getNetworkDeviceById(deviceId);

				if (chosenDevice != null)
					showChooserDialog(chosenDevice);
			}
		}
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

	protected void organizeFiles(File file, String fileName)
	{
		if (!file.canRead())
			return;

		if (file.isDirectory())
		{
			for (File fileInstance : file.listFiles())
				organizeFiles(fileInstance, null);
		}
		else
		{
			FileState fileState = new FileState();

			fileState.file = file;
			fileState.fileName = fileName != null ? fileName : file.getName();
			fileState.fileMime = FileUtils.getFileContentType(file.getName());

			mFiles.add(fileState);
		}
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

				for (int position = 0; position < fileUris.size(); position++)
				{
					Uri fileUri = fileUris.get(position);
					String fileName = fileNames != null ? String.valueOf(fileNames.get(position)) : null;

					organizeFiles(ApplicationHelper.getFileFromUri(getApplicationContext(), fileUri), fileName);
				}

				mProgressOrganizeFiles.cancel();

				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						if (mFiles.size() == 1)
							appendStatusText(mFiles.get(0).fileName);
						else if (mFiles.size() > 1)
							appendStatusText(getString(R.string.item_selected, String.valueOf(mFiles.size())));
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
			@Override
			public void onHandle(CoolCommunication.Messenger.Process process, JSONObject json, NetworkDevice device, String chosenIp)
			{
				device = mDeviceRegistry.getNetworkDevice(chosenIp);
				JSONArray filesArray = new JSONArray();

				try
				{
					int groupId = ApplicationHelper.getUniqueNumber();
					Transaction.EditingSession editingSession = mTransaction.edit();

					json.put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER);
					json.put(Keyword.GROUP_ID, groupId);

					for (FileState fileState : mFiles)
					{
						int requestId = ApplicationHelper.getUniqueNumber();
						AwaitedFileSender sender = new AwaitedFileSender(device, requestId, groupId, fileState.fileName, 0, fileState.file);
						JSONObject thisJson = new JSONObject();

						try
						{
							thisJson.put(Keyword.FILE_NAME, fileState.fileName);
							thisJson.put(Keyword.FILE_SIZE, fileState.file.length());
							thisJson.put(Keyword.REQUEST_ID, requestId);
							thisJson.put(Keyword.FILE_MIME, fileState.fileMime);

							filesArray.put(thisJson);

							editingSession.registerTransaction(sender);
						} catch (Exception e)
						{
							Log.e(TAG, "Sender error on file: " + e.getClass().getName() + " : " + fileState.file.getName());
						}
					}

					json.put(Keyword.FILES_INDEX, filesArray);

					JSONObject response = new JSONObject(process.waitForResponse());

					if (!response.getBoolean(Keyword.RESULT))
					{
						Log.d(TAG, "Keyword did not accept the request remove pre-added senders");
						editingSession.removeTransactionGroup(groupId);

						showToast(getString(R.string.file_sending_error_msg, getString(R.string.not_allowed_error)));
					}
				} catch (Exception e)
				{
					showToast(getString(R.string.file_sending_error_msg, getString(R.string.communication_problem)));
				}
			}
		});
	}

	protected void showChooserDialog(final NetworkDevice device)
	{
		mDeviceRegistry.updateRestrictionByDeviceId(device, false);
		mDeviceRegistry.updateLastUsageTime(device, System.currentTimeMillis());

		new DeviceChooserDialog(ShareActivity.this, device, new DeviceChooserDialog.OnDeviceSelectedListener()
		{
			@Override
			public void onDeviceSelected(DeviceChooserDialog.AddressHolder addressHolder, ArrayList<DeviceChooserDialog.AddressHolder> availableInterfaces)
			{
				final String deviceIp = addressHolder.address;

				mProgressConnect.show();

				CoolCommunication.Messenger.send(deviceIp, AppConfig.COMMUNATION_SERVER_PORT, null,
						new JsonResponseHandler()
						{
							@Override
							public void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json)
							{
								mConnectionHandler.onHandle(process, json, device, deviceIp);
								mProgressConnect.cancel();
							}

							@Override
							public void onError(Exception e)
							{
								mProgressConnect.cancel();
								showToast(getString(R.string.file_sending_error_msg, getString(R.string.connection_problem)));
							}
						}
				);
			}
		}).show();
	}

	protected void showToast(String msg)
	{
		Looper.prepare();
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		Looper.loop();
	}

	protected void updateStats(NetworkDevice device)
	{
		mProgressConnect.show();
		mDeviceRegistry.updateRestrictionByDeviceId(device, false);
	}

	private interface ConnectionHandler
	{
		public void onHandle(com.genonbeta.CoolSocket.CoolCommunication.Messenger.Process process, JSONObject json, NetworkDevice device, String chosenIp);
	}

	private class FileState
	{
		public File file;
		public String fileName;
		public String fileMime;
	}
}
