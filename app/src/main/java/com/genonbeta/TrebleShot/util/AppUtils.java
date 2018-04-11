package com.genonbeta.TrebleShot.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.text.format.DateUtils;
import android.util.Log;

import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.preference.DbSharablePreferences;
import com.genonbeta.TrebleShot.preference.SuperPreferences;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AppUtils
{
	public static final String TAG = AppUtils.class.getSimpleName();

	private static int mUniqueNumber = 0;
	private static AccessDatabase mDatabase;
	private static SuperPreferences mDefaultPreferences;
	private static SuperPreferences mDefaultLocalPreferences;
	private static SuperPreferences mViewingPreferences;

	public static void applyAdapterName(NetworkDevice.Connection connection)
	{
		if (connection.ipAddress == null) {
			Log.e(AppUtils.class.getSimpleName(), "Connection should be provided with IP address");
			return;
		}

		ArrayList<AddressedInterface> interfaceList = NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES);

		for (AddressedInterface addressedInterface : interfaceList) {
			if (NetworkUtils.getAddressPrefix(addressedInterface.getAssociatedAddress())
					.equals(NetworkUtils.getAddressPrefix(connection.ipAddress))) {
				connection.adapterName = addressedInterface.getNetworkInterface().getDisplayName();
				return;
			}
		}

		connection.adapterName = Keyword.Local.NETWORK_INTERFACE_UNKNOWN;
	}

	public static void applyDeviceToJSON(NetworkDevice device, JSONObject object) throws JSONException
	{
		JSONObject deviceInformation = new JSONObject();
		JSONObject appInfo = new JSONObject();

		deviceInformation.put(Keyword.DEVICE_INFO_SERIAL, device.deviceId);
		deviceInformation.put(Keyword.DEVICE_INFO_BRAND, device.brand);
		deviceInformation.put(Keyword.DEVICE_INFO_MODEL, device.model);
		deviceInformation.put(Keyword.DEVICE_INFO_USER, device.nickname);

		appInfo.put(Keyword.APP_INFO_VERSION_CODE, device.versionNumber);
		appInfo.put(Keyword.APP_INFO_VERSION_NAME, device.versionName);

		object.put(Keyword.APP_INFO, appInfo);
		object.put(Keyword.DEVICE_INFO, deviceInformation);
	}

	public static boolean checkRunningConditions(Context context)
	{
		for (RationalePermissionRequest.PermissionRequest request : getRequiredPermissions(context))
			if (ActivityCompat.checkSelfPermission(context, request.permission) != PackageManager.PERMISSION_GRANTED)
				return false;

		return true;
	}

	public static CharSequence formatDateTime(Context context, long millis)
	{
		return DateUtils.formatDateTime(context, millis, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE);
	}

	public static AccessDatabase getAccessDatabase(Context context)
	{
		if (mDatabase == null)
			mDatabase = new AccessDatabase(context);

		return mDatabase;
	}

	public static SuperPreferences getDefaultPreferences(final Context context)
	{
		if (mDefaultPreferences == null) {
			DbSharablePreferences databasePreferences = new DbSharablePreferences(context, "__default", true)
					.setUpdateListener(new DbSharablePreferences.AsynchronousUpdateListener()
					{
						@Override
						public void onCommitComplete()
						{
							context.sendBroadcast(new Intent(App.ACTION_REQUEST_PREFERENCES_SYNC));
						}
					});

			mDefaultPreferences = new SuperPreferences(databasePreferences);
			mDefaultPreferences.setOnPreferenceUpdateListener(new SuperPreferences.OnPreferenceUpdateListener()
			{
				@Override
				public void onPreferenceUpdate(SuperPreferences superPreferences, boolean commit)
				{
					PreferenceUtils.syncPreferences(superPreferences, getDefaultLocalPreferences(context).getWeakManager());
				}
			});
		}

		return mDefaultPreferences;
	}

	public static SuperPreferences getDefaultLocalPreferences(final Context context)
	{
		if (mDefaultLocalPreferences == null) {
			mDefaultLocalPreferences = new SuperPreferences(PreferenceManager.getDefaultSharedPreferences(context));

			mDefaultLocalPreferences.setOnPreferenceUpdateListener(new SuperPreferences.OnPreferenceUpdateListener()
			{
				@Override
				public void onPreferenceUpdate(SuperPreferences superPreferences, boolean commit)
				{
					PreferenceUtils.syncPreferences(superPreferences, getDefaultPreferences(context).getWeakManager());
				}
			});
		}

		return mDefaultLocalPreferences;
	}

	public static String getDeviceSerial(Context context)
	{
		return Build.VERSION.SDK_INT < 26
				? Build.SERIAL
				: (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ? Build.getSerial() : null);
	}

	@NonNull
	public static String getHotspotName(Context context)
	{
		return AppConfig.PREFIX_ACCESS_POINT + AppUtils.getLocalDeviceName(context)
				.replace(" ", "_");
	}

	public static String getLocalDeviceName(Context context)
	{
		String deviceName = getDefaultPreferences(context)
				.getString("device_name", null);

		return deviceName == null || deviceName.length() == 0
				? Build.MODEL.toUpperCase()
				: deviceName;
	}

	public static ArrayList<RationalePermissionRequest.PermissionRequest> getRequiredPermissions(Context context)
	{
		ArrayList<RationalePermissionRequest.PermissionRequest> permissionRequests = new ArrayList<>();

		if (Build.VERSION.SDK_INT >= 16) {
			permissionRequests.add(new RationalePermissionRequest.PermissionRequest(context,
					Manifest.permission.WRITE_EXTERNAL_STORAGE,
					R.string.text_requestPermissionStorage,
					R.string.text_requestPermissionStorageSummary));
		}

		if (Build.VERSION.SDK_INT >= 26) {
			permissionRequests.add(new RationalePermissionRequest.PermissionRequest(context,
					Manifest.permission.READ_PHONE_STATE,
					R.string.text_requestPermissionReadPhoneState,
					R.string.text_requestPermissionReadPhoneStateSummary));
		}

		return permissionRequests;
	}

	public static int getUniqueNumber()
	{
		return (int) System.currentTimeMillis() + (++mUniqueNumber);
	}

	public static NetworkDevice getLocalDevice(Context context)
	{
		NetworkDevice device = new NetworkDevice(getDeviceSerial(context));

		device.brand = Build.BRAND;
		device.model = Build.MODEL;
		device.nickname = AppUtils.getLocalDeviceName(context);
		device.isRestricted = false;
		device.isLocalAddress = true;

		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getApplicationInfo().packageName, 0);

			device.versionNumber = packageInfo.versionCode;
			device.versionName = packageInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		return device;
	}

	public static SuperPreferences getViewingPreferences(Context context)
	{
		if (mViewingPreferences == null)
			mViewingPreferences = new SuperPreferences(context.getSharedPreferences(Keyword.Local.SETTINGS_VIEWING, Context.MODE_PRIVATE));

		return mViewingPreferences;
	}

	public static <T> T quickAction(T clazz, QuickActions<T> quickActions)
	{
		quickActions.onQuickActions(clazz);
		return clazz;
	}

	public static void startForegroundService(Context context, Intent intent)
	{
		if (Build.VERSION.SDK_INT >= 26)
			context.startForegroundService(intent);
		else
			context.startService(intent);
	}

	public interface QuickActions<T>
	{
		void onQuickActions(T clazz);
	}
}