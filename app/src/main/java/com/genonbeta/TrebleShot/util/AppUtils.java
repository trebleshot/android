/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.util;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import androidx.annotation.AnyRes;
import androidx.annotation.AttrRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.BuildConfig;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.EditableListFragmentBase;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.object.Identity;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class AppUtils
{
    public static final String TAG = AppUtils.class.getSimpleName();

    private static int mUniqueNumber = 0;
    private static Kuick mKuick;
    private static SharedPreferences mDefaultPreferences;
    private static SharedPreferences mViewingPreferences;

    public static void applyAdapterName(DeviceAddress connection)
    {
        if (connection.ipAddress == null) {
            Log.e(TAG, "Connection should be provided with IP address");
            return;
        }

        try {
            NetworkInterface networkInterface = NetworkUtils.findNetworkInterface(connection.toInet4Address());

            if (networkInterface != null)
                connection.adapterName = networkInterface.getDisplayName();
            else
                Log.d(TAG, "applyAdapterName(): No network interface found");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        if (connection.adapterName == null)
            connection.adapterName = Keyword.Local.NETWORK_INTERFACE_UNKNOWN;
    }

    public static boolean checkRunningConditions(Context context)
    {
        for (RationalePermissionRequest.PermissionRequest request : getRequiredPermissions(context))
            if (ActivityCompat.checkSelfPermission(context, request.permission) != PackageManager.PERMISSION_GRANTED)
                return false;

        return true;
    }

    public static DocumentFile createLog(Context context)
    {
        DocumentFile saveDirectory = FileUtils.getApplicationDirectory(context);
        String fileName = FileUtils.getUniqueFileName(saveDirectory, "trebleshot_log.txt", true);
        DocumentFile logFile = saveDirectory.createFile(null, fileName);
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Service.ACTIVITY_SERVICE);

        if (activityManager == null)
            return null;

        try {

            List<ActivityManager.RunningAppProcessInfo> processList = activityManager.getRunningAppProcesses();

            String command = "logcat -d -v threadtime *:*";
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            OutputStream outputStream = context.getContentResolver()
                    .openOutputStream(logFile.getUri(), "w");

            if (outputStream == null)
                throw new IOException(String.format("Could not open %s", fileName));

            String readLine;

            while ((readLine = reader.readLine()) != null)
                for (ActivityManager.RunningAppProcessInfo processInfo : processList)
                    if (readLine.contains(String.valueOf(processInfo.pid))) {
                        outputStream.write((readLine + "\n").getBytes());
                        outputStream.flush();

                        break;
                    }

            outputStream.close();
            reader.close();

            return logFile;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static int generateKey()
    {
        return (int) (Integer.MAX_VALUE * Math.random());
    }

    public static int generateNetworkPin(Context context)
    {
        int networkPin = AppUtils.generateKey();

        getDefaultPreferences(context).edit()
                .putInt(Keyword.NETWORK_PIN, networkPin)
                .apply();

        return networkPin;
    }


    @NonNull
    public static BackgroundService getBgService(Activity activity) throws IllegalStateException
    {
        BackgroundService service = getBgServiceRef(activity).get();
        if (service == null)
            throw new IllegalStateException("The service is not ready or removed from the scope. Do not call this " +
                    "method after the application process exits.");
        return service;
    }

    /**
     * A reference object for the service. If you don't want to deal with 'null' state, use
     * {@link #getBgService(Activity)} instead
     *
     * @param activity That will be used to extract the right {@link Application} instance which should be
     *                 {@link App}
     * @return the service instance that was bound to the {@link Application} class
     */
    public static WeakReference<BackgroundService> getBgServiceRef(Activity activity)
    {
        Application application = activity.getApplication();
        if (application instanceof App)
            return ((App) application).getBackgroundService();

        return new WeakReference<>(null);
    }

    public static Keyword.Flavor getBuildFlavor()
    {
        try {
            return Keyword.Flavor.valueOf(BuildConfig.FLAVOR);
        } catch (Exception e) {
            Log.e(TAG, "Current build flavor " + BuildConfig.FLAVOR + " is not specified in " +
                    "the vocab. Is this a custom build?");
            return Keyword.Flavor.unknown;
        }
    }

    public static TextDrawable.IShapeBuilder getDefaultIconBuilder(Context context)
    {
        TextDrawable.IShapeBuilder builder = TextDrawable.builder();

        builder.beginConfig()
                .firstLettersOnly(true)
                .textMaxLength(1)
                .textColor(ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorControlNormal)))
                .shapeColor(ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorPassive)));

        return builder;
    }

    public static SharedPreferences getDefaultPreferences(final Context context)
    {
        if (mDefaultPreferences == null)
            mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        return mDefaultPreferences;
    }


    public static String getDeviceId(Context context)
    {
        // The ANDROID_ID is unique to the user it is representing. That's why we don't store this. Because the
        // app will not have the data and settings that corresponds to that user anyway.
        /*
        SharedPreferences preferences = getDefaultPreferences(context);
        String uuid = preferences.getString("uuid", null);

        if (uuid == null) {
            preferences.edit()
                    .putString("uuid", Settings.Secure.ANDROID_ID)
                    .apply();
        }

        return uid;
         */
        return Settings.Secure.ANDROID_ID;
    }

    public static Kuick getKuick(Context context)
    {
        if (mKuick == null)
            mKuick = new Kuick(context);

        return mKuick;
    }

    public static JSONObject getLocalDeviceAsJson(Context context, Device targetDevice, int pin) throws JSONException
    {
        Device device = getLocalDevice(context);
        JSONObject object = new JSONObject()
                .put(Keyword.DEVICE_UID, device.uid)
                .put(Keyword.DEVICE_BRAND, device.brand)
                .put(Keyword.DEVICE_MODEL, device.model)
                .put(Keyword.DEVICE_USERNAME, device.username)
                .put(Keyword.DEVICE_VERSION_CODE, device.versionCode)
                .put(Keyword.DEVICE_VERSION_NAME, device.versionName)
                .put(Keyword.DEVICE_PROTOCOL_VERSION, device.protocolVersion)
                .put(Keyword.DEVICE_PROTOCOL_VERSION_MIN, device.protocolVersionMin)
                .put(Keyword.DEVICE_KEY, targetDevice.sendKey);

        if (pin != 0)
            object.put(Keyword.DEVICE_PIN, pin);

        try {
            ByteArrayOutputStream imageBytes = new ByteArrayOutputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(context.openFileInput("profilePicture"));

            bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageBytes);
            object.put(Keyword.DEVICE_AVATAR, Base64.encodeToString(imageBytes.toByteArray(), 0));
        } catch (Exception ignored) {
        }

        return object;
    }

    public static <T extends Editable> void showFolderSelectionHelp(EditableListFragmentBase<T> fragment)
    {
        IEngineConnection<T> connection = fragment.getEngineConnection();
        SharedPreferences preferences = AppUtils.getDefaultPreferences(fragment.getContext());

        if (connection.getSelectedItemList().size() > 0 && !preferences.getBoolean("helpFolderSelection",
                false))
            fragment.createSnackbar(R.string.mesg_helpFolderSelection)
                    .setAction(R.string.butn_gotIt, v -> preferences
                            .edit()
                            .putBoolean("helpFolderSelection", true)
                            .apply())
                    .show();
    }

    public static void startApplicationDetails(Context context)
    {
        context.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.getPackageName(), null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    public static void startFeedbackActivity(Context context)
    {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_EMAIL, new String[]{AppConfig.EMAIL_DEVELOPER})
                .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.text_appName))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        DocumentFile logFile = AppUtils.createLog(context);

        if (logFile != null) {
            try {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        .putExtra(Intent.EXTRA_STREAM, (FileUtils.getSecureUri(context, logFile)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.butn_feedbackContact)));
    }

    public static void interruptTasksBy(Activity activity, Identity identity, boolean userAction)
    {
        try {
            BackgroundService service = AppUtils.getBgService(activity);
            service.interruptTasksBy(identity, userAction);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public static String getFriendlySSID(String ssid)
    {
        ssid = ssid.replace("\"", "");

        if (ssid.startsWith(AppConfig.PREFIX_ACCESS_POINT))
            ssid = ssid.substring(AppConfig.PREFIX_ACCESS_POINT.length());

        return ssid.replace("_", " ");
    }

    @NonNull
    public static String getHotspotName(Context context)
    {
        return AppConfig.PREFIX_ACCESS_POINT + AppUtils.getLocalDeviceName(context)
                .replaceAll(" ", "_");
    }

    public static String getLocalDeviceName(Context context)
    {
        String deviceName = getDefaultPreferences(context).getString("device_name", null);
        return deviceName == null || deviceName.length() == 0 ? Build.MODEL.toUpperCase() : deviceName;
    }

    public static Device getLocalDevice(Context context)
    {
        Device device = new Device(getDeviceId(context));

        device.brand = Build.BRAND;
        device.model = Build.MODEL;
        device.username = AppUtils.getLocalDeviceName(context);
        device.protocolVersion = BuildConfig.PROTOCOL_VERSION;
        device.protocolVersionMin = BuildConfig.PROTOCOL_VERSION_MIN;
        device.versionCode = BuildConfig.VERSION_CODE;
        device.versionName = BuildConfig.VERSION_NAME;
        device.isLocal = true;
        device.sendKey = AppUtils.generateKey();
        device.receiveKey = AppUtils.generateKey();

        return device;
    }

    @AnyRes
    public static int getReference(Context context, @AttrRes int refId)
    {
        TypedValue typedValue = new TypedValue();

        if (!context.getTheme().resolveAttribute(refId, typedValue, true)) {
            TypedArray values = context.getTheme().obtainStyledAttributes(context.getApplicationInfo().theme,
                    new int[]{refId});

            return values.length() > 0 ? values.getResourceId(0, 0) : 0;
        }

        return typedValue.resourceId;
    }

    public static List<RationalePermissionRequest.PermissionRequest> getRequiredPermissions(Context context)
    {
        List<RationalePermissionRequest.PermissionRequest> permissionRequests = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= 16) {
            permissionRequests.add(new RationalePermissionRequest.PermissionRequest(context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.text_requestPermissionStorage,
                    R.string.text_requestPermissionStorageSummary));
        }

        return permissionRequests;
    }

    /**
     * This method returns a number unique to the application session. One of the reasons it is not deprecated is that
     * it is heavily used for the {@link android.app.PendingIntent} who asks for unique operation or unique request code
     * to function. In order to get rid of this, first, the notification should be shown in a merged manner meaning each
     * notification should not create an individual notification so that notification actions don't create a collision.
     *
     * @return A unique integer number that does not mix with the current session.
     */
    public static int getUniqueNumber()
    {
        return (int) (System.currentTimeMillis() / 1000) + (++mUniqueNumber);
    }

    public static SharedPreferences getViewingPreferences(Context context)
    {
        if (mViewingPreferences == null)
            mViewingPreferences = context.getSharedPreferences(Keyword.Local.SETTINGS_VIEWING, Context.MODE_PRIVATE);

        return mViewingPreferences;
    }

    public static boolean isLatestChangeLogSeen(Context context)
    {
        SharedPreferences preferences = getDefaultPreferences(context);
        Device device = getLocalDevice(context);
        int lastSeenChangelog = preferences.getInt("changelog_seen_version", -1);
        boolean dialogAllowed = preferences.getBoolean("show_changelog_dialog", true);

        return !preferences.contains("previously_migrated_version") || device.versionCode == lastSeenChangelog
                || !dialogAllowed;
    }

    public static void publishLatestChangelogSeen(Context context)
    {
        Device device = getLocalDevice(context);

        getDefaultPreferences(context).edit()
                .putInt("changelog_seen_version", device.versionCode)
                .apply();
    }

    public static boolean toggleDeviceScanning(DeviceScannerService service)
    {
        if (!service.getDeviceScanner().isBusy()) {
            service.startService(new Intent(service, DeviceScannerService.class)
                    .setAction(DeviceScannerService.ACTION_SCAN_DEVICES));
            return true;
        }

        service.getDeviceScanner().interrupt();
        return false;
    }
}