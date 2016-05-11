package com.genonbeta.TrebleShot.service;

import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.*;
import android.os.*;
import android.preference.*;
import android.util.*;
import android.widget.*;
import com.genonbeta.CoolSocket.*;
import com.genonbeta.TrebleShot.*;
import com.genonbeta.TrebleShot.activity.*;
import com.genonbeta.TrebleShot.config.*;
import com.genonbeta.TrebleShot.helper.*;
import com.genonbeta.TrebleShot.receiver.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.json.*;

public class CommunicationService extends Service
{
	public static final String TAG = "CommunationService";

	public static final String FILE_TRANSFER_ACCEPT = "com.genonbeta.TrebleShot.FILE_TRANSFER_ACCEPT";
	public static final String FILE_TRANSFER_REJECT = "com.genonbeta.TrebleShot.FILE_TRANSFER_REJECT";
	public static final String STOP_SERVICE = "com.genonbeta.TrebleShot.STOP_SERVICE";
	public static final String ALLOW_IP = "com.genonbeta.TrebleShot.ALLOW_IP";
	public static final String REJECT_IP = "com.genonbeta.TrebleShot.REJECT_IP";

	public static final String EXTRA_DEVICE_IP = "extraDeviceIp";
	public static final String EXTRA_FILE_PATH = "extraFilePath";
	public static final String EXTRA_FILE_NAME = "extraFileName";
	public static final String EXTRA_FILE_SIZE = "extraFileSize";
	public static final String EXTRA_FILE_MIME = "extraFileMime";
	public static final String EXTRA_REQUEST_ID = "extraRequestId";
	public static final String EXTRA_ACCEPT_ID = "extraAcceptId";
	public static final String EXTRA_SERVICE_LOCK_REQUEST = "extraServiceStartLock";

	private CommunicationServer mCommunationServer = new CommunicationServer();
	private NotificationPublisher mPublisher;
	private SharedPreferences mPreferences;

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		if (!mCommunationServer.start())
			stopSelf();

		mPublisher = new NotificationPublisher(this);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

		if (mPreferences.getBoolean("notify_com_server_started", false))
			startForeground(NotificationPublisher.NOTIFICATION_SERVICE_STARTED, mPublisher.notifyServiceStarted());
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
			Log.d(TAG, "onStart() : action = " + intent.getAction());

		if (intent != null)
		{
			if ((Intent.ACTION_SEND.equals(intent.getAction()) || ShareActivity.ACTION_SEND.equals(intent.getAction())) && intent.hasExtra(EXTRA_DEVICE_IP) && intent.hasExtra(Intent.EXTRA_STREAM))
			{
				Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
				final File file = ApplicationHelper.getFileFromUri(this, fileUri);

				if (file != null && file.isFile())
				{
					final int requestId = ApplicationHelper.getUniqueNumber();
					final String deviceIp = intent.getStringExtra(EXTRA_DEVICE_IP);
					final String fileMime = (intent.getType() == null) ? FileUtils.getFileContentType(file.getAbsolutePath()) : intent.getType();

					CoolCommunication.Messenger.send(deviceIp, AppConfig.COMMUNATION_SERVER_PORT, null,
						new JsonResponseHandler()
						{
							@Override
							public void onJsonMessage(Socket socket, com.genonbeta.CoolSocket.CoolCommunication.Messenger.Process process, JSONObject json)
							{
								try
								{
									json.put("request", "file_transfer_request");
									json.put("fileName", file.getName());
									json.put("fileSize", file.length());
									json.put("fileMime", fileMime);
									json.put("requestId", requestId);

									JSONObject response = new JSONObject(process.waitForResponse());

									if (response.getBoolean("result"))
									{
										ApplicationHelper.getSenders().put(requestId, new AwaitedFileSender(deviceIp, file, requestId));
									}
									else
										showToast(getString(R.string.file_sending_error_msg, getString(R.string.not_allowed_error)));
								}
								catch (JSONException e)
								{
									showToast(getString(R.string.file_sending_error_msg, getString(R.string.communication_problem)));
								}
							}

							@Override
							public void onError(Exception e)
							{
								showToast(getString(R.string.file_sending_error_msg, getString(R.string.connection_problem)));
							}
						}
					);

					return START_STICKY;
				}

				mPublisher.makeToast(R.string.file_type_not_supported_msg);
			}
			else if ((Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction()) || ShareActivity.ACTION_SEND_MULTIPLE.equals(intent.getAction())) && intent.hasExtra(Intent.EXTRA_STREAM))
			{
				final ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
				final String deviceIp = intent.getStringExtra(EXTRA_DEVICE_IP);

				CoolCommunication.Messenger.send(deviceIp, AppConfig.COMMUNATION_SERVER_PORT, null, 
					new JsonResponseHandler()
					{
						@Override
						public void onConfigure(CoolCommunication.Messenger.Process process)
						{
							process.setSocketTimeout(AppConfig.DEFAULT_SOCKET_LARGE_TIMEOUT);
						}

						@Override
						public void onJsonMessage(Socket socket, com.genonbeta.CoolSocket.CoolCommunication.Messenger.Process process, JSONObject json)
						{
							JSONArray filesArray = new JSONArray();

							try
							{
								json.put("request", "multifile_transfer_request");

								for (Uri fileUri : uris)
								{
									File file = ApplicationHelper.getFileFromUri(getApplicationContext(), fileUri);

									if (file == null)
										continue;

									if (file.isFile())
									{
										int requestId = ApplicationHelper.getUniqueNumber();
										AwaitedFileSender sender = new AwaitedFileSender(deviceIp, file, requestId);
										JSONObject thisJson = new JSONObject();

										try
										{
											String fileMime = FileUtils.getFileContentType(file.getAbsolutePath());

											thisJson.put("fileName", file.getName());
											thisJson.put("fileSize", file.length());
											thisJson.put("requestId", requestId);
											thisJson.put("fileMime", fileMime);

											filesArray.put(thisJson);

											ApplicationHelper.getSenders().put(sender.requestId, sender);
										}
										catch (Exception e)
										{
											Log.e(TAG, "Sender error on file: " + e.getClass().getName() + " : " + file.getName());
										}
									}
								}

								json.put("filesJson", filesArray);

								JSONObject response = new JSONObject(process.waitForResponse());

								if (!response.getBoolean("result"))
								{
									Log.d(TAG, "Server did not accept the request remove pre-added senders");
									
									for (int i = 0; i < filesArray.length(); i++)
									{
										int requestId = filesArray.getJSONObject(i).getInt("requestId");
										ApplicationHelper.getSenders().remove(requestId);
									}
									
									showToast(getString(R.string.file_sending_error_msg, getString(R.string.not_allowed_error)));
								}
							}
							catch (JSONException e)
							{
								showToast(getString(R.string.file_sending_error_msg, getString(R.string.communication_problem)));
							}
						}

						@Override
						public void onError(Exception e)
						{
							showToast(getString(R.string.file_sending_error_msg, getString(R.string.connection_problem)));
						}
					}
				);
			}
			else if (STOP_SERVICE.equals(intent.getAction()))
			{
				if (intent.getBooleanExtra(EXTRA_SERVICE_LOCK_REQUEST, false))
				{
					mPreferences.edit().putBoolean("serviceLock", true).commit();
					mPublisher.makeToast(R.string.service_lock_notice, Toast.LENGTH_LONG);
				}

				stopSelf();
			}
			else if (FILE_TRANSFER_ACCEPT.equals(intent.getAction()))
			{
				final String oppositeIp = intent.getStringExtra(EXTRA_DEVICE_IP);
				final int acceptId = intent.getIntExtra(EXTRA_ACCEPT_ID, -1);
				final int notificationId = intent.getIntExtra(NotificationPublisher.EXTRA_NOTIFICATION_ID, -1);

				mPublisher.cancelNotification(notificationId);

				Log.d(TAG, "fileTransferAccepted ; ip = " + oppositeIp + " ; acceptId = " + acceptId + "; notificationId = " + notificationId);

				if (ApplicationHelper.getDeviceList().containsKey(oppositeIp))
					ApplicationHelper.getDeviceList().get(oppositeIp).isRestricted = false;

				if (ApplicationHelper.acceptPendingReceivers(acceptId) < 1)
				{
					mPublisher.makeToast(R.string.something_went_wrong);

					return START_NOT_STICKY;
				}

				startService(new Intent(this, ServerService.class).setAction(ServerService.ACTION_CHECK_AVAILABLES));

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
							}
							catch (JSONException e)
							{
								e.printStackTrace();
							}
						}
					}
				);
			}
			else if (FILE_TRANSFER_REJECT.equals(intent.getAction()) && intent.hasExtra(NotificationPublisher.EXTRA_NOTIFICATION_ID))
			{
				final String oppositeIp = intent.getStringExtra(EXTRA_DEVICE_IP);
				final int acceptId = intent.getIntExtra(EXTRA_ACCEPT_ID, -1);
				final int notificationId = intent.getIntExtra(NotificationPublisher.EXTRA_NOTIFICATION_ID, -1);

				mPublisher.cancelNotification(notificationId);

				if (ApplicationHelper.getDeviceList().containsKey(oppositeIp))
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

								for (AwaitedFileReceiver receiver : ApplicationHelper.getPendingReceiversByAcceptId(acceptId))
								{
									idList.put(receiver.requestId);
								}

								json.put("requestIds", idList);
							}
							catch (JSONException e)
							{
								e.printStackTrace();
							}
							finally
							{
								ApplicationHelper.removePendingReceivers(acceptId);
							}
						}
					}
				);
			}
			else if (STOP_SERVICE.equals(intent.getAction()))
			{
				if (intent.getBooleanExtra(EXTRA_SERVICE_LOCK_REQUEST, false))
				{
					mPreferences.edit().putBoolean("serviceLock", true).commit();
					mPublisher.makeToast(R.string.service_lock_notice, Toast.LENGTH_LONG);
				}

				stopSelf();
			}
			else if (ALLOW_IP.equals(intent.getAction()))
			{
				String oppositeIp = intent.getStringExtra(EXTRA_DEVICE_IP);
				int notificationId = intent.getIntExtra(NotificationPublisher.EXTRA_NOTIFICATION_ID, -1);

				mPublisher.cancelNotification(notificationId);

				if (!ApplicationHelper.getDeviceList().containsKey(oppositeIp))
					return START_NOT_STICKY;

				ApplicationHelper.getDeviceList().get(oppositeIp).isRestricted = false;
			}
			else if (REJECT_IP.equals(intent.getAction()))
			{
				String oppositeIp = intent.getStringExtra(EXTRA_DEVICE_IP);
				int notificationId = intent.getIntExtra(NotificationPublisher.EXTRA_NOTIFICATION_ID, -1);

				mPublisher.cancelNotification(notificationId);

				if (!ApplicationHelper.getDeviceList().containsKey(oppositeIp))
					return START_NOT_STICKY;

				ApplicationHelper.getDeviceList().get(oppositeIp).isRestricted = true;
			}
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mCommunationServer.stop();
		stopForeground(true);

		System.gc();
	}

	protected void showToast(String msg)
	{
		Looper.prepare();

		mPublisher.makeToast(msg);

		Looper.loop();
	}

	public class CommunicationServer extends CoolJsonCommunication
	{
		public CommunicationServer()
		{
			super(AppConfig.COMMUNATION_SERVER_PORT);
			this.setSocketTimeout(AppConfig.DEFAULT_SOCKET_LARGE_TIMEOUT);
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
				boolean result = false;
				boolean shouldContinue = true;

				PackageInfo packageInfo = getPackageManager().getPackageInfo(getApplicationInfo().packageName, 0);
				JSONObject appInfo = new JSONObject();

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

				if (receivedMessage.has("request") && !receivedMessage.getString("request").equals(null))
					if (!ApplicationHelper.getDeviceList().containsKey(clientIp))
					{
						device = new NetworkDevice(clientIp, null, null, null);
						device.isRestricted = true;

						ApplicationHelper.getDeviceList().put(clientIp, device);
						sendBroadcast(new Intent(DeviceScannerProvider.ACTION_ADD_IP).putExtra(DeviceScannerProvider.EXTRA_DEVICE_IP, clientIp));
						mPublisher.notifyConnectionRequest(clientIp);
						
						shouldContinue = false;
					}
					else
					{
						device = ApplicationHelper.getDeviceList().get(clientIp);

						if (device.isRestricted == true)
							shouldContinue = false;
					}

				if (shouldContinue && receivedMessage.has("request"))
				{
					switch (receivedMessage.getString("request"))
					{
						case("file_transfer_request"):
							if (receivedMessage.has("fileSize") && receivedMessage.has("fileMime") && receivedMessage.has("fileName") && receivedMessage.has("requestId"))
							{
								long fileSize = receivedMessage.getLong("fileSize");
								String fileName = receivedMessage.getString("fileName");
								String fileMime = receivedMessage.getString("fileMime");
								int requestId = receivedMessage.getInt("requestId");

								int acceptId = ApplicationHelper.getUniqueNumber();

								AwaitedFileReceiver receiver = new AwaitedFileReceiver(device.ip, requestId, acceptId, fileName, fileSize, fileMime);
								ApplicationHelper.getPendingReceivers().offer(receiver);

								device.isRestricted = true;

								mPublisher.notifyTransferRequest(acceptId, device, receiver);

								result = true;
							}
							break;
						case("multifile_transfer_request"):
							if (receivedMessage.has("filesJson"))
							{
								String jsonIndex = receivedMessage.getString("filesJson");

								Log.d(TAG, jsonIndex);

								JSONArray jsonArray = new JSONArray(jsonIndex);

								int count = 0;
								int acceptId = ApplicationHelper.getUniqueNumber();

								Log.d(TAG, "First PendingReceiver count " + ApplicationHelper.getPendingReceivers().size());

								for (int i = 0; i < jsonArray.length(); i++)
								{
									if (!(jsonArray.get(i) instanceof JSONObject))
										continue;

									JSONObject requestIndex = jsonArray.getJSONObject(i);

									if (requestIndex != null && requestIndex.has("fileName") && requestIndex.has("fileSize") && requestIndex.has("fileMime") && requestIndex.has("requestId"))
									{
										count++;
										AwaitedFileReceiver receiver = new AwaitedFileReceiver(clientIp, requestIndex.getInt("requestId"), acceptId, requestIndex.getString("fileName"), requestIndex.getLong("fileSize"), requestIndex.getString("fileMime"));

										receiver.acceptId = acceptId;

										Log.d(TAG, "Received acceptId test they must be the same = " + receiver.acceptId);

										ApplicationHelper.getPendingReceivers().offer(receiver);
									}
								}

								Log.d(TAG, "Last PendingReceiver count " + ApplicationHelper.getPendingReceivers().size());

								if (count > 0)
								{
									mPublisher.notifyMultiTransferRequest(count, acceptId, device);
									result = true;
									device.isRestricted = true;
								}
							}
							break;
						case ("file_transfer_request_accepted"):
							if (receivedMessage.has("requestId"))
							{
								int requestId = receivedMessage.getInt("requestId");

								if (ApplicationHelper.getSenders().containsKey(requestId))
								{
									AwaitedFileSender sender = ApplicationHelper.getSenders().get(requestId);

									if (sender.ip.equals(clientIp))
									{
										result = true;									
									}
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

									if (ApplicationHelper.getSenders().containsKey(requestId))
									{
										AwaitedFileSender sender = ApplicationHelper.getSenders().get(requestId);

										if (sender.ip.equals(clientIp))
										{
											ApplicationHelper.getSenders().remove(requestId);
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

								if (ApplicationHelper.getSenders().containsKey(requestId))
								{
									AwaitedFileSender sender = ApplicationHelper.getSenders().get(requestId);

									sender.setPort(socketPort);
									startService(new Intent(getApplicationContext(), ClientService.class).setAction(ClientService.ACTION_SEND).putExtra(EXTRA_REQUEST_ID, requestId));

									result = true;
								}
							}
							break;
						case ("poke_the_device"):
							if (mPreferences.getBoolean("allow_poke", true))
							{
								mPublisher.notifyOppositeDevicePing(device);
								result = true;
							}
					}
				}

				response.put("result", result);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		protected void onError(Exception exception)
		{}
	}
}
