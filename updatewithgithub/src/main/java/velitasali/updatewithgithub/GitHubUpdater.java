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
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by: veli
 * Date: 11/13/16 8:25 AM
 */

public class GitHubUpdater
{
	public static final String TAG = GitHubUpdater.class.getSimpleName();

	private Context mContext;
	private String mRepo;
	private int mThemeRes;

	public GitHubUpdater(Context context, String repo, int themeRes)
	{
		mContext = context;
		mRepo = repo;
		mThemeRes = themeRes;
	}

	public void checkForUpdates(final boolean popupDialog, final OnInfoAvailableListener listener)
	{
		if (popupDialog)
			Toast.makeText(mContext, R.string.uwg_check_for_updates_ongoing, Toast.LENGTH_LONG).show();

		new Thread()
		{
			@Override
			public void run()
			{
				super.run();

				Looper.prepare();

				try
				{
					Log.d(TAG, "Checking updates");

					mContext.setTheme(mThemeRes);

					RemoteServer server = new RemoteServer(mRepo);
					String result = server.connect(null, null);

					Log.d(TAG, "Server connected");

					final PackageInfo packInfo = mContext.getPackageManager().getPackageInfo(mContext.getApplicationInfo().packageName, 0);
					final String appVersionName = packInfo.versionName;
					final String applicationName = getAppLabel(mContext);

					JSONArray releases = new JSONArray(result);

					if (releases.length() > 0)
					{
						Log.d(TAG, "Reading releases: (total) " + releases.length());

						JSONObject lastRelease = releases.getJSONObject(0);

						final String lastVersionName = lastRelease.getString("tag_name");
						final String lastVersionTitle = lastRelease.getString("name");
						final String lastVersionDate = lastRelease.getString("published_at");
						final String lastVersionBody = lastRelease.getString("body");

						ComparableVersion comLast = new ComparableVersion(lastVersionName);
						ComparableVersion comCurr = new ComparableVersion(appVersionName);

						if (listener != null)
							listener.onInfoAvailable(comLast.compareTo(comCurr) > 0, lastVersionName, lastVersionTitle, lastVersionBody, lastVersionDate);

						final File updateFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + applicationName + " v" + lastVersionName + ".apk");

						if (popupDialog && comLast.compareTo(comCurr) > 0)
						{
							Log.d(TAG, "New version found: " + lastVersionName);

							if (lastRelease.has("assets"))
							{
								Log.d(TAG, "Reading assets");

								JSONArray releaseAssets = lastRelease.getJSONArray("assets");

								if (releaseAssets.length() > 0)
								{
									Log.d(TAG, "Assets is cached: (total) " + releaseAssets.length());

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

											downloadRequest.setTitle(mContext.getString(R.string.uwg_downloading_update_title, applicationName, lastVersionName));
											downloadRequest.setDescription(mContext.getString(R.string.uwg_downloading_update_description, applicationName));
											downloadRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, applicationName + " v" + lastVersionName + ".apk");
											downloadRequest.setMimeType("application/vnd.android.package-archive");

											if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
												downloadRequest.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

											manager.enqueue(downloadRequest);
										}
									};

									DialogInterface.OnClickListener updateButtonListener = new DialogInterface.OnClickListener()
									{
										@Override
										public void onClick(DialogInterface dialog, int which)
										{
											Intent installerIntent = new Intent(Intent.ACTION_VIEW);

											if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
												installerIntent.setDataAndType(FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", updateFile), "application/vnd.android.package-archive")
														.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
											else
												installerIntent.setDataAndType(Uri.fromFile(updateFile), "application/vnd.android.package-archive");

											mContext.startActivity(installerIntent);
										}
									};

									if (updateFile.isFile())
									{
										Log.d(TAG, "File is already exists: " + updateFile.getName());

										if (firstAsset.getLong("size") == updateFile.length())
										{
											Log.d(TAG, "Existing file is ok");

											negativeButtonLabel = R.string.uwg_later;
											positiveButtonLabel = R.string.uwg_update_now;
											updateBody = mContext.getString(R.string.uwg_update_now_info);
											positiveButtonListener = updateButtonListener;
										}
										else
										{
											Log.d(TAG, "Existing file is corrupted");

											negativeButtonLabel = R.string.uwg_cancel;
											positiveButtonLabel = R.string.uwg_download_anyway;
											updateBody = mContext.getString(R.string.uwg_update_file_different_info);
											positiveButtonListener = downloadButtonListener;
										}
									}
									else
									{
										Log.d(TAG, "Update is downloadable");

										negativeButtonLabel = R.string.uwg_later;
										positiveButtonLabel = R.string.uwg_download_now;
										updateBody = String.format(mContext.getString(R.string.uwg_update_body), appVersionName, lastVersionName, lastVersionDate, lastVersionBody);

										positiveButtonListener = downloadButtonListener;
									}

									new AlertDialog.Builder(mContext)
											.setTitle(R.string.uwg_update_available)
											.setMessage(updateBody)
											.setNegativeButton(negativeButtonLabel, null)
											.setPositiveButton(positiveButtonLabel, positiveButtonListener)
											.show();
								}
								else
									Log.d(TAG, "No downloadable file is provided");
							}
							else
								Toast.makeText(mContext, R.string.uwg_no_update_available, Toast.LENGTH_LONG).show();
						}
						else if (popupDialog)
							Toast.makeText(mContext, R.string.uwg_currently_latest_version_info, Toast.LENGTH_LONG).show();
					}
				} catch (Exception e)
				{
					e.printStackTrace();
					Log.d(TAG, "Error occurred");

					if (popupDialog)
						Toast.makeText(mContext, R.string.uwg_version_check_error, Toast.LENGTH_LONG).show();
				}
				finally
				{
					Looper.loop();
				}
			}
		}.start();
	}

	public String getAppLabel(Context context)
	{
		PackageManager packageManager = context.getPackageManager();
		ApplicationInfo applicationInfo = null;
		try
		{
			applicationInfo = packageManager.getApplicationInfo(context.getApplicationInfo().packageName, 0);
		} catch (final PackageManager.NameNotFoundException e)
		{
		}
		return (String) (applicationInfo != null ? packageManager.getApplicationLabel(applicationInfo) : "Unknown");
	}

	public boolean isNewVersion(String comparedVersionName)
	{
		try
		{
			PackageInfo packageInfo = mContext.getPackageManager().getPackageInfo(mContext.getApplicationInfo().packageName, 0);

			ComparableVersion comparableGiven = new ComparableVersion(comparedVersionName);
			ComparableVersion comparableCurrent = new ComparableVersion(packageInfo.versionName);

			return comparableGiven.compareTo(comparableCurrent) > 0;
		} catch (PackageManager.NameNotFoundException e)
		{
			e.printStackTrace();
		}

		return false;
	}

	public interface OnInfoAvailableListener
	{
		public void onInfoAvailable(boolean newVersion, String versionName, String title, String description, String releaseDate);
	}
}
