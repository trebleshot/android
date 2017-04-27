package com.genonbeta.TrebleShot.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.CoolSocket.CoolJsonCommunication;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;
import com.genonbeta.TrebleShot.helper.JsonResponseHandler;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.helper.NotificationUtils;
import com.genonbeta.TrebleShot.receiver.DeviceScannerProvider;
import com.genonbeta.android.database.CursorItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;

public class CommunicationService extends Service
{
	public static final String TAG = "CommunicationService";

	public static final String ACTION_FILE_TRANSFER_ACCEPT = "com.genonbeta.TrebleShot.FILE_TRANSFER_ACCEPT";
	public static final String ACTION_FILE_TRANSFER_REJECT = "com.genonbeta.TrebleShot.FILE_TRANSFER_REJECT";
	public static final String ACTION_STOP_SERVICE = "com.genonbeta.TrebleShot.STOP_SERVICE";
	public static final String ACTION_ALLOW_IP = "com.genonbeta.TrebleShot.ALLOW_IP";
	public static final String ACTION_REJECT_IP = "com.genonbeta.TrebleShot.REJECT_IP";
	public static final String ACTION_CLIPBOARD = "com.genonbeta.TrebleShot.CLIPBOARD";

	public static final String EXTRA_DEVICE_IP = "extraDeviceIp";
	public static final String EXTRA_REQUEST_ID = "extraRequestId";
	public static final String EXTRA_ACCEPT_ID = "extraAcceptId";
	public static final String EXTRA_SERVICE_LOCK_REQUEST = "extraServiceStartLock";
	public static final String EXTRA_HALF_RESTRICT = "extraHalfRestrict";
	public static final String EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted";

	private CommunicationServer mCommunicationServer = new CommunicationServer();
	private NotificationUtils mNotification;
	private SharedPreferences mPreferences;
	private String mReceivedClipboardIndex;
	private Transaction mTransaction;

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		if (!mCommunicationServer.start())
			stopSelf();

		mNotification = new NotificationUtils(this);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mTransaction = new Transaction(this);

		if (mPreferences.getBoolean("notify_com_server_started", false))
			startForeground(NotificationUtils.NOTIFICATION_ID_SERVICE, mNotification.notifyService().build());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
			Log.d(TAG, "onStart() : action = " + intent.getAction());

		if (intent != null)
		{
			if (ACTION_STOP_SERVICE.equals(intent.getAction()))
			{
				if (intent.getBooleanExtra(EXTRA_SERVICE_LOCK_REQUEST, false))
				{
					mPreferences.edit().putBoolean("serviceLock", true).apply();
					mNotification.showToast(R.string.service_lock_notice, Toast.LENGTH_LONG);
				}

				stopSelf();
			}
			else if (ACTION_FILE_TRANSFER_ACCEPT.equals(intent.getAction()))
			{
				final String oppositeIp = intent.getStringExtra(EXTRA_DEVICE_IP);
				final int acceptId = intent.getIntExtra(EXTRA_ACCEPT_ID, -1);
				final int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				mNotification.cancel(notificationId);

				Log.d(TAG, "fileTransferAccepted ; ip = " + oppositeIp + " ; acceptId = " + acceptId + "; notificationId = " + notificationId);

				if (ApplicationHelper.getDeviceList().containsKey(oppositeIp))
					ApplicationHelper.getDeviceList().get(oppositeIp).isRestricted = false;

				if (mTransaction.acceptPendingReceivers(acceptId) < 1)
				{
					mNotification.showToast(R.string.something_went_wrong);

					return START_NOT_STICKY;
				}

				startService(new Intent(this, ServerService.class).setAction(ServerService.ACTION_CHECK_AVAILABLE));

				CoolCommunication.Messenger.send(oppositeIp, AppConfig.COMMUNATION_SERVER_PORT, null,
						new JsonResponseHandler()
						{
							@Override
							public void onJsonMessage(Socket socket, com.genonbeta.CoolSocket.CoolCommunication.Messenger.Process process, JSONObject json)
							{
								try
								{
									json.put("request", "file_transfer_request_accepted");
									json.put("requestId", acceptId);
								} catch (JSONException e)
								{
									e.printStackTrace();
								}
							}
						}
				);
			}
			else if (ACTION_FILE_TRANSFER_REJECT.equals(intent.getAction()) && intent.hasExtra(NotificationUtils.EXTRA_NOTIFICATION_ID))
			{
				final String oppositeIp = intent.getStringExtra(EXTRA_DEVICE_IP);
				final int acceptId = intent.getIntExtra(EXTRA_ACCEPT_ID, -1);
				final int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				mNotification.cancel(notificationId);

				if (ApplicationHelper.getDeviceList().containsKey(oppositeIp) && !intent.hasExtra(EXTRA_HALF_RESTRICT))
					ApplicationHelper.getDeviceList().get(oppositeIp).isRestricted = false;

				CoolCommunication.Messenger.send(oppositeIp, AppConfig.COMMUNATION_SERVER_PORT, null,
						new JsonResponseHandler()
						{
							@Override
							public void onJsonMessage(Socket socket, com.genonbeta.CoolSocket.CoolCommunication.Messenger.Process process, JSONObject json)
							{
								try
								{
									JSONArray idList = new JSONArray();

									json.put("request", "file_transfer_request_rejected");

									for (AwaitedFileReceiver receiver : mTransaction.getPendingReceiversByAcceptId(acceptId))
										idList.put(receiver.requestId);

									json.put("requestIds", idList);
								} catch (JSONException e)
								{
									e.printStackTrace();
								}
								finally
								{
									mTransaction.removePendingReceivers(acceptId);
								}
							}
						}
				);
			}
			else if (ACTION_ALLOW_IP.equals(intent.getAction()))
			{
				String oppositeIp = intent.getStringExtra(EXTRA_DEVICE_IP);
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				mNotification.cancel(notificationId);

				if (!ApplicationHelper.getDeviceList().containsKey(oppositeIp))
					return START_NOT_STICKY;

				ApplicationHelper.getDeviceList().get(oppositeIp).isRestricted = false;
			}
			else if (ACTION_REJECT_IP.equals(intent.getAction()))
			{
				String oppositeIp = intent.getStringExtra(EXTRA_DEVICE_IP);
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				mNotification.cancel(notificationId);

				if (!ApplicationHelper.getDeviceList().containsKey(oppositeIp))
					return START_NOT_STICKY;

				ApplicationHelper.getDeviceList().get(oppositeIp).isRestricted = true;
			}
			else if (ACTION_CLIPBOARD.equals(intent.getAction()) && intent.hasExtra(EXTRA_CLIPBOARD_ACCEPTED))
			{
				String oppositeIp = intent.getStringExtra(EXTRA_DEVICE_IP);
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				mNotification.cancel(notificationId);

				if (!ApplicationHelper.getDeviceList().containsKey(oppositeIp))
					return START_NOT_STICKY;

				ApplicationHelper.getDeviceList().get(oppositeIp).isRestricted = false;

				if (intent.getBooleanExtra(EXTRA_CLIPBOARD_ACCEPTED, false))
				{
					((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("receivedText", mReceivedClipboardIndex));
					Toast.makeText(this, R.string.clipboard_text_copied, Toast.LENGTH_SHORT).show();
				}
			}

		}

		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mCommunicationServer.stop();
		stopForeground(true);
	}

	public class CommunicationServer extends CoolJsonCommunication
	{
		public CommunicationServer()
		{
			super(AppConfig.COMMUNATION_SERVER_PORT);
			this.setSocketTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT);
		}

		@Override
		public void onJsonMessage(Socket socket, JSONObject receivedMessage, JSONObject response, String clientIp)
		{
			NetworkDevice device = null;

			try
			{
				if (receivedMessage != null)
					Log.d(TAG, "receivedMessage = " + receivedMessage.toString());

				JSONObject deviceInformation = new JSONObject();
				JSONObject appInfo = new JSONObject();
				boolean result = false;
				boolean shouldContinue = true;
				boolean halfRestriction = false;

				PackageInfo packageInfo = getPackageManager().getPackageInfo(getApplicationInfo().packageName, 0);

				appInfo.put("versionCode", packageInfo.versionCode);
				appInfo.put("versionName", packageInfo.versionName);

				deviceInformation.put("device", Build.DEVICE);
				deviceInformation.put("brand", Build.BRAND);
				deviceInformation.put("display", Build.DISPLAY);
				deviceInformation.put("model", Build.MODEL);
				deviceInformation.put("manufacturer", Build.MANUFACTURER);
				deviceInformation.put("deviceName", mPreferences.getString("device_name", Build.BOARD));

				response.put("appInfo", appInfo);
				response.put("deviceInfo", deviceInformation);

				if (receivedMessage.has("request") && !receivedMessage.getString("request").equals(""))
					if (!ApplicationHelper.getDeviceList().containsKey(clientIp))
					{
						device = new NetworkDevice(clientIp, null, null, null);
						device.isRestricted = true;

						ApplicationHelper.getDeviceList().put(clientIp, device);
						sendBroadcast(new Intent(DeviceScannerProvider.ACTION_ADD_IP).putExtra(DeviceScannerProvider.EXTRA_DEVICE_IP, clientIp));

						if (receivedMessage.getString("request").equals("file_transfer_request") || receivedMessage.getString("request").equals("multifile_transfer_request"))
						{
							shouldContinue = true;
							halfRestriction = true;
						}
						else
							mNotification.notifyConnectionRequest(clientIp);
					}
					else
					{
						device = ApplicationHelper.getDeviceList().get(clientIp);

						if (device.isRestricted)
							shouldContinue = false;
					}

				if (shouldContinue && receivedMessage.has("request"))
				{
					switch (receivedMessage.getString("request"))
					{
						case ("file_transfer_request"):
							if (receivedMessage.has("fileSize") && receivedMessage.has("fileMime") && receivedMessage.has("fileName") && receivedMessage.has("requestId"))
							{
								int requestId = receivedMessage.getInt("requestId");
								long fileSize = receivedMessage.getLong("fileSize");
								String fileName = receivedMessage.getString("fileName");
								String fileMime = receivedMessage.getString("fileMime");

								int acceptId = ApplicationHelper.getUniqueNumber();

								AwaitedFileReceiver receiver = new AwaitedFileReceiver(device.ip, requestId, acceptId, fileName, fileSize, fileMime);
								mTransaction.getPendingReceivers().offer(receiver);

								mNotification.notifyTransferRequest(acceptId, device, receiver, halfRestriction);

								device.isRestricted = true;

								result = true;
							}
							break;
						case ("multifile_transfer_request"):
							if (receivedMessage.has("filesJson"))
							{
								String jsonIndex = receivedMessage.getString("filesJson");

								Log.d(TAG, jsonIndex);

								JSONArray jsonArray = new JSONArray(jsonIndex);

								int count = 0;
								int acceptId = ApplicationHelper.getUniqueNumber();

								Log.d(TAG, "First PendingReceiver count " + mTransaction.getPendingReceivers().size());

								for (int i = 0; i < jsonArray.length(); i++)
								{
									if (!(jsonArray.get(i) instanceof JSONObject))
										continue;

									JSONObject requestIndex = jsonArray.getJSONObject(i);

									if (requestIndex != null && requestIndex.has("fileName") && requestIndex.has("fileSize") && requestIndex.has("fileMime") && requestIndex.has("requestId"))
									{
										count++;
										AwaitedFileReceiver receiver = new AwaitedFileReceiver(clientIp, requestIndex.getInt("requestId"), acceptId, requestIndex.getString("fileName"), requestIndex.getLong("fileSize"), requestIndex.getString("fileMime"));
										Log.d(TAG, "Received acceptId test they must be the same = " + receiver.acceptId);

										mTransaction.getPendingReceivers().offer(receiver);
									}
								}

								Log.d(TAG, "Last PendingReceiver count " + mTransaction.getPendingReceivers().size());

								if (count > 0)
								{
									mNotification.notifyTransferRequest(count, acceptId, device, halfRestriction);
									result = true;
									device.isRestricted = true;
								}
							}
							break;
						case ("file_transfer_request_accepted"):
							if (receivedMessage.has("requestId"))
							{
								int requestId = receivedMessage.getInt("requestId");

								if (mTransaction.transactionExists(requestId))
								{
									AwaitedFileSender sender = new AwaitedFileSender(mTransaction.getTransaction(requestId));
									result = sender.ip.equals(clientIp);
								}
							}
							break;
						case ("file_transfer_request_rejected"):
							if (receivedMessage.has("requestIds"))
							{
								JSONArray requestIds = receivedMessage.getJSONArray("requestIds");

								for (int i = 0; i < requestIds.length(); i++)
								{
									int requestId = requestIds.getInt(i);

									CursorItem item = mTransaction.getTransaction(requestId);

									if (item != null)
									{
										AwaitedFileSender sender = new AwaitedFileSender(mTransaction.getTransaction(requestId));

										if (sender.ip.equals(clientIp))
										{
											mTransaction.removeTransaction(requestId);
											result = true;
										}
									}
								}
							}
							break;
						case ("file_transfer_notify_server_ready"):
							if (receivedMessage.has("requestId") && receivedMessage.has("socketPort"))
							{
								int requestId = receivedMessage.getInt("requestId");
								int socketPort = receivedMessage.getInt("socketPort");

								if (mTransaction.applyAccessPort(requestId, socketPort))
								{
									startService(new Intent(getApplicationContext(), ClientService.class).setAction(ClientService.ACTION_SEND).putExtra(EXTRA_REQUEST_ID, requestId));
									result = true;
								}
							}
							break;
						case ("request_clipboard"):
							if (receivedMessage.has("clipboardText"))
							{
								mReceivedClipboardIndex = receivedMessage.getString("clipboardText");
								mNotification.notifyClipboardRequest(clientIp, mReceivedClipboardIndex);

								device.isRestricted = true;

								result = true;
							}
							break;
						case ("poke_the_device"):
							if (mPreferences.getBoolean("allow_poke", true))
							{
								mNotification.notifyPing(device);
								result = true;
							}
					}
				}

				response.put("result", result);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		protected void onError(Exception exception)
		{
		}
	}
}
