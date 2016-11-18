package velitasali.updatewithgithub;

import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.support.v7.app.AlertDialog;
import android.view.WindowManager;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.net.URI;

/**
 * Created by: veli
 * Date: 11/13/16 8:25 AM
 */

public class GithubUpdater
{
	private Context mContext;
	private String mRepo;
	private int mThemeRes;

	public GithubUpdater(Context context, String repo, int themeRes)
	{
		this.mContext = context;
		this.mRepo = repo;
		this.mThemeRes = themeRes;
	}

	public void checkForUpdates()
	{
		new Thread()
		{
			@Override
			public void run()
			{
				super.run();

				Looper.prepare();

				try
				{
					Toast.makeText(mContext, R.string.uwg_check_for_updates_ongoing, Toast.LENGTH_LONG).show();

					mContext.setTheme(mThemeRes);

					RemoteServer server = new RemoteServer(mRepo);
					String result = server.connect(null, null);

					PackageInfo packInfo = mContext.getPackageManager().getPackageInfo(mContext.getApplicationInfo().packageName, 0);

					String appVersionName = packInfo.versionName;
					int appVersionCode = packInfo.versionCode;
					final String applicationName = getAppLabel(mContext);

					JSONArray releases = new JSONArray(result);

					if (releases.length() > 0)
					{
						JSONObject lastRelease = releases.getJSONObject(0);

						final String availableVersion = lastRelease.getString("tag_name");

						ComparableVersion comLast = new ComparableVersion(availableVersion);
						ComparableVersion comCurr = new ComparableVersion(appVersionName);

						final File updateFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + applicationName + " v" + availableVersion + ".apk");

						if (comLast.compareTo(comCurr) > 0)
						{
							if (lastRelease.has("assets"))
							{
								JSONArray releaseAssets = lastRelease.getJSONArray("assets");

								if (releaseAssets.length() > 0)
								{
									final JSONObject firstAsset = releaseAssets.getJSONObject(0);
									final String downloadURL = firstAsset.getString("browser_download_url");

									int negativeButtonLabel;
									int positiveButtonLabel;
									final String updateBody;
									DialogInterface.OnClickListener positiveButtonListener;

									DialogInterface.OnClickListener downloadButtonListener = new DialogInterface.OnClickListener()
									{
										@Override
										public void onClick(DialogInterface dialog, int which)
										{
											if (updateFile.isFile())
												updateFile.delete();

											DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
											DownloadManager.Request downloadRequest = new DownloadManager.Request(Uri.parse(downloadURL));

											downloadRequest.setTitle(mContext.getString(R.string.uwg_downloading_update_title, applicationName, availableVersion));
											downloadRequest.setDescription(mContext.getString(R.string.uwg_downloading_update_description, applicationName));
											downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, applicationName + " v" + availableVersion + ".apk");
											downloadRequest.setMimeType("application/vnd.android.package-archive");
											if (Build.VERSION.SDK_INT >= 11)
												downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

											manager.enqueue(downloadRequest);
										}
									};

									DialogInterface.OnClickListener updateButtonListener = new DialogInterface.OnClickListener()
									{
										@Override
										public void onClick(DialogInterface dialog, int which)
										{
											mContext.startActivity(new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.fromFile(updateFile), "application/vnd.android.package-archive"));
										}
									};

									if (updateFile.isFile())
									{
										if (firstAsset.getLong("size") == updateFile.length())
										{
											negativeButtonLabel = R.string.uwg_later;
											positiveButtonLabel = R.string.uwg_update_now;
											updateBody = mContext.getString(R.string.uwg_update_now_info);
											positiveButtonListener = updateButtonListener;
										}
										else
										{
											negativeButtonLabel = R.string.uwg_cancel;
											positiveButtonLabel = R.string.uwg_download_anyway;
											updateBody = mContext.getString(R.string.uwg_update_file_different_info);
											positiveButtonListener = downloadButtonListener;
										}
									}
									else
									{
										negativeButtonLabel = R.string.uwg_later;
										positiveButtonLabel = R.string.uwg_download_now;
										updateBody = String.format(mContext.getString(R.string.uwg_update_body), appVersionName, availableVersion, lastRelease.getString("published_at"), lastRelease.getString("body"));

										positiveButtonListener = downloadButtonListener;
									}

									AlertDialog.Builder builder = new AlertDialog.Builder(mContext);

									builder.setTitle(R.string.uwg_update_available);
									builder.setMessage(updateBody);

									builder.setNegativeButton(negativeButtonLabel, null);
									builder.setPositiveButton(positiveButtonLabel, positiveButtonListener);

									AlertDialog dialog = builder.create();

									dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
									dialog.show();
								}
							}
							else
								Toast.makeText(mContext, R.string.uwg_no_update_available, Toast.LENGTH_LONG).show();
						}
						else
							Toast.makeText(mContext, R.string.uwg_currently_latest_version_info, Toast.LENGTH_LONG).show();
					}
				} catch (Exception e) {
					Toast.makeText(mContext, R.string.uwg_version_check_error, Toast.LENGTH_LONG).show();
				}
				finally
				{
					Looper.loop();
				}
			}
		}.start();
	}


	public String getAppLabel(Context context) {
		PackageManager packageManager = context.getPackageManager();
		ApplicationInfo applicationInfo = null;
		try {
			applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
		} catch (final PackageManager.NameNotFoundException e) {
		}
		return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "Unknown");
	}
}
