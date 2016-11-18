package com.genonbeta.TrebleShot.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.GActivity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;
import com.genonbeta.TrebleShot.helper.FileUtils;
import com.genonbeta.TrebleShot.helper.JsonResponseHandler;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.Socket;
import java.util.ArrayList;

public class ShareActivity extends GActivity
{
	public static final String TAG = "ShareActivity";

	public static final String ACTION_SEND = "genonbeta.intent.action.TREBLESHOT_SEND";
	public static final String ACTION_SEND_TEXT = "genonbeta.intent.action.TREBLESHOT_SEND_TEXT";
	public static final String ACTION_SEND_MULTIPLE = "genonbeta.intent.action.TREBLESHOT_SEND_MULTIPLE";

	public enum ResultType
	{
		NOT_CONTINUE,
		SINGLE_FILE_SHARE,
		TEXT_SHARE,
		MULTI_FILE_SHARE
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		ResultType resultType = ResultType.NOT_CONTINUE;
		String info = new String();

		if (getIntent() != null)
			if (ACTION_SEND_TEXT.equals(getIntent().getAction()))
			{
				resultType = ResultType.TEXT_SHARE;
			}
			else if (getIntent().hasExtra(Intent.EXTRA_STREAM) || getIntent().hasExtra(Intent.EXTRA_TEXT))
			{
				if (Intent.ACTION_SEND.equals(getIntent().getAction()) || ShareActivity.ACTION_SEND.equals(getIntent().getAction()))
				{
					if (getIntent().hasExtra(Intent.EXTRA_STREAM))
					{
						Uri fileUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
						File file = ApplicationHelper.getFileFromUri(this, fileUri);

						if (file != null)
						{
							info = file.getName();

							resultType = ResultType.SINGLE_FILE_SHARE;
						}
					}
					else if (getIntent().hasExtra(Intent.EXTRA_TEXT))
					{
						info = getIntent().getStringExtra(Intent.EXTRA_TEXT);
						resultType = ResultType.TEXT_SHARE;
					}
				}
				else if (Intent.ACTION_SEND_MULTIPLE.equals(getIntent().getAction()) || ShareActivity.ACTION_SEND_MULTIPLE.equals(getIntent().getAction()))
				{
					ArrayList<Uri> fileUris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);
					info = getString(R.string.item_selected, String.valueOf(fileUris.size()));

					resultType = ResultType.MULTI_FILE_SHARE;
				}
			}

		if (resultType == ResultType.NOT_CONTINUE)
		{
			Toast.makeText(this, R.string.type_not_supported_msg, Toast.LENGTH_SHORT).show();
			finish();
		}
		else
		{
			setContentView(R.layout.activity_share);

			final NetworkDeviceListFragment deviceListFragment = (NetworkDeviceListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_share_fragment);
			final EditText editText = (EditText) findViewById(R.id.activity_share_info_edit_text);
			final TextView infoText = (TextView) findViewById(R.id.activity_share_info_text);

			if (resultType == ResultType.TEXT_SHARE)
			{
				editText.setVisibility(View.VISIBLE);
				editText.getText().append(info);
			}
			else
			{
				infoText.setVisibility(View.VISIBLE);
				infoText.setText(info);
			}

			final ResultType finalResultType = resultType;

			deviceListFragment.setOnListClickListener(new AdapterView.OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> parent, View view, int position, long id)
				{
					final NetworkDevice device = (NetworkDevice) deviceListFragment.getListAdapter().getItem(position);
					final String deviceIp = device.ip;

					if (finalResultType == ResultType.SINGLE_FILE_SHARE)
					{
						Uri fileUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
						final File file = ApplicationHelper.getFileFromUri(getApplicationContext(), fileUri);

						if (file != null && file.isFile())
						{
							final int requestId = ApplicationHelper.getUniqueNumber();
							final String fileMime = (getIntent().getType() == null) ? FileUtils.getFileContentType(file.getAbsolutePath()) : getIntent().getType();

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
											} catch (JSONException e)
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
					}
					else if (finalResultType == ResultType.MULTI_FILE_SHARE)
					{
						final ArrayList<Uri> uris = getIntent().getParcelableArrayListExtra(Intent.EXTRA_STREAM);

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
													} catch (Exception e)
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
										} catch (JSONException e)
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
					else if (finalResultType == ResultType.TEXT_SHARE)
					{
						CoolCommunication.Messenger.send(deviceIp, AppConfig.COMMUNATION_SERVER_PORT, null,
								new JsonResponseHandler()
								{
									@Override
									public void onJsonMessage(Socket socket, com.genonbeta.CoolSocket.CoolCommunication.Messenger.Process process, JSONObject json)
									{
										try
										{
											json.put("request", "request_clipboard");
											json.put("clipboardText", editText.getText().toString());

											JSONObject response = new JSONObject(process.waitForResponse());

											if (response.getBoolean("result"))
											{

											}
											else
												showToast(getString(R.string.file_sending_error_msg, getString(R.string.not_allowed_error)));
										} catch (JSONException e)
										{
											showToast(getString(R.string.file_sending_error_msg, getString(R.string.communication_problem)));
										}
									}

									@Override
									public void onError(Exception e)
									{
										e.printStackTrace();
										showToast(getString(R.string.file_sending_error_msg, getString(R.string.connection_problem)));
									}
								}
						);
					}
				}
			});
		}
	}

	protected void showToast(String msg)
	{
		Looper.prepare();
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		Looper.loop();
	}
}
