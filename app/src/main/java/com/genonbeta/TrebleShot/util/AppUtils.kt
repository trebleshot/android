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
package com.genonbeta.TrebleShot.util

import android.Manifest
import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import androidx.annotation.AnyRes
import androidx.annotation.AttrRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.genonbeta.TrebleShot.BuildConfig
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable
import com.genonbeta.android.database.exception.ReconstructionFailedException
import com.genonbeta.android.framework.io.DocumentFile
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

object AppUtils {
    val TAG = AppUtils::class.java.simpleName
    private var mUniqueNumber = 0
    private var mKuick: Kuick? = null
    private var mDefaultPreferences: SharedPreferences? = null
    private var mViewingPreferences: SharedPreferences? = null

    fun checkRunningConditions(context: Context): Boolean {
        for (request in getRequiredPermissions(context))
            if (ActivityCompat.checkSelfPermission(context, request.permission) != PackageManager.PERMISSION_GRANTED)
                return false
        return true
    }

    fun createLog(context: Context): DocumentFile? {
        val saveDirectory = FileUtils.getApplicationDirectory(context)
        val logFile = saveDirectory.createFile("text/plain", "trebleshot_log")
        val activityManager = context.getSystemService(Service.ACTIVITY_SERVICE) as ActivityManager

        if (logFile.exists())
            logFile.delete()

        try {
            val processList = activityManager.runningAppProcesses
            val command = "logcat -d -v threadtime *:*"
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val outputStream = context.contentResolver
                .openOutputStream(logFile.uri, "w") ?: throw IOException("Could not open " + logFile.name)
            var readLine: String
            while (reader.readLine().also { readLine = it } != null)
                for (processInfo in processList) if (readLine.contains(processInfo.pid.toString())) {
                    outputStream.write(readLine.toByteArray())
                    outputStream.flush()
                    break
                }
            outputStream.close()
            reader.close()
            return logFile
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun generateKey(): Int {
        return (Int.MAX_VALUE * Math.random()).toInt()
    }

    fun generateNetworkPin(context: Context?): Int {
        val networkPin = generateKey()
        getDefaultPreferences(context).edit()
            .putInt(Keyword.NETWORK_PIN, networkPin)
            .apply()
        return networkPin
    }

    val buildFlavor: Keyword.Flavor
        get() = try {
            Keyword.Flavor.valueOf(BuildConfig.FLAVOR)
        } catch (e: Exception) {
            Log.e(
                TAG, "Current build flavor " + BuildConfig.FLAVOR + " is not specified in " +
                        "the vocab. Is this a custom build?"
            )
            Keyword.Flavor.unknown
        }

    fun getDefaultIconBuilder(context: Context): TextDrawable.IShapeBuilder {
        val builder: TextDrawable.IShapeBuilder = TextDrawable.Companion.builder()
        builder.beginConfig()
            .firstLettersOnly(true)
            .textMaxLength(1)
            .bold()
            .textColor(ContextCompat.getColor(context, getReference(context, R.attr.colorControlNormal)))
            .shapeColor(ContextCompat.getColor(context, getReference(context, R.attr.colorPassive)))
        return builder
    }

    fun getDefaultPreferences(context: Context?): SharedPreferences {
        if (mDefaultPreferences == null)
            mDefaultPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        return mDefaultPreferences as SharedPreferences
    }

    fun getDeviceId(context: Context): String {
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
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getKuick(context: Context?): Kuick {
        if (mKuick == null)
            mKuick = Kuick(context)
        return mKuick as Kuick
    }

    @Throws(JSONException::class)
    fun getLocalDeviceAsJson(context: Context, sendKey: Int, pin: Int): JSONObject {
        val device = getLocalDevice(context)
        val `object` = JSONObject()
            .put(Keyword.DEVICE_UID, device.uid)
            .put(Keyword.DEVICE_BRAND, device.brand)
            .put(Keyword.DEVICE_MODEL, device.model)
            .put(Keyword.DEVICE_USERNAME, device.username)
            .put(Keyword.DEVICE_VERSION_CODE, device.versionCode)
            .put(Keyword.DEVICE_VERSION_NAME, device.versionName)
            .put(Keyword.DEVICE_PROTOCOL_VERSION, device.protocolVersion)
            .put(Keyword.DEVICE_PROTOCOL_VERSION_MIN, device.protocolVersionMin)
            .put(Keyword.DEVICE_KEY, sendKey)
        if (pin != 0) `object`.put(Keyword.DEVICE_PIN, pin)
        try {
            val imageBytes = ByteArrayOutputStream()
            val bitmap = BitmapFactory.decodeStream(context.openFileInput("profilePicture"))
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageBytes)
            `object`.put(Keyword.DEVICE_AVATAR, Base64.encodeToString(imageBytes.toByteArray(), 0))
        } catch (ignored: Exception) {
        }
        return `object`
    }

    fun <T : Editable> showFolderSelectionHelp(fragment: EditableListFragmentBase<T>) {
        val connection = fragment.engineConnection
        val preferences = getDefaultPreferences(fragment.context)
        if (connection.selectedItemList.size > 0 && !preferences.getBoolean("helpFolderSelection", false))
            fragment.createSnackbar(R.string.mesg_helpFolderSelection)
                .setAction(R.string.butn_gotIt) {
                    preferences
                        .edit()
                        .putBoolean("helpFolderSelection", true)
                        .apply()
                }
                .show()
    }

    fun startApplicationDetails(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun startFeedbackActivity(context: Context) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConfig.EMAIL_DEVELOPER))
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.text_appName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val logFile = createLog(context)
        if (logFile != null) {
            try {
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .putExtra(
                        Intent.EXTRA_STREAM,
                        com.genonbeta.android.framework.util.FileUtils.getSecureUri(context, logFile)
                    )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        context.startActivity(Intent.createChooser(intent, context.getString(R.string.butn_feedbackContact)))
    }

    fun getFriendlySSID(ssid: String): String {
        var ssid = ssid
        ssid = ssid.replace("\"", "")
        if (ssid.startsWith(AppConfig.PREFIX_ACCESS_POINT)) ssid = ssid.substring(AppConfig.PREFIX_ACCESS_POINT.length)
        return ssid.replace("_", " ")
    }

    fun getHotspotName(context: Context?): String {
        return AppConfig.PREFIX_ACCESS_POINT + getLocalDeviceName(context)
            .replace(" ".toRegex(), "_")
    }

    fun getLocalDeviceName(context: Context?): String {
        val deviceName = getDefaultPreferences(context).getString("device_name", null)
        return if (deviceName == null || deviceName.length == 0) Build.MODEL.toUpperCase() else deviceName
    }

    fun getLocalDevice(context: Context): Device {
        val device = Device(getDeviceId(context))
        var publishNeeded = false
        try {
            getKuick(context).reconstruct(device)
            if (device.sendKey != device.receiveKey || device.sendKey == 0)
                throw ReconstructionFailedException("The keys need a reset.")
        } catch (e: ReconstructionFailedException) {
            device.sendKey = generateKey()
            device.receiveKey = device.sendKey
            publishNeeded = true
        }
        device.brand = Build.BRAND
        device.model = Build.MODEL
        device.username = getLocalDeviceName(context)
        device.protocolVersion = BuildConfig.PROTOCOL_VERSION
        device.protocolVersionMin = BuildConfig.PROTOCOL_VERSION_MIN
        device.versionCode = BuildConfig.VERSION_CODE
        device.versionName = BuildConfig.VERSION_NAME
        device.isLocal = true
        if (publishNeeded) getKuick(context).publish(device)
        return device
    }

    @AnyRes
    fun getReference(context: Context, @AttrRes refId: Int): Int {
        val typedValue = TypedValue()
        if (!context.theme.resolveAttribute(refId, typedValue, true)) {
            val values = context.theme.obtainStyledAttributes(context.applicationInfo.theme, intArrayOf(refId))
            return if (values.length() > 0) values.getResourceId(0, 0) else 0
        }
        return typedValue.resourceId
    }

    fun getRequiredPermissions(context: Context): List<RationalePermissionRequest.PermissionRequest> {
        val permissionRequests: MutableList<RationalePermissionRequest.PermissionRequest> = ArrayList()
        if (Build.VERSION.SDK_INT >= 16) {
            permissionRequests.add(
                RationalePermissionRequest.PermissionRequest(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, R.string.text_requestPermissionStorage,
                    R.string.text_requestPermissionStorageSummary
                )
            )
        }
        return permissionRequests
    }

    /**
     * This method returns a number unique to the application session. One of the reasons it is not deprecated is that
     * it is heavily used for the [android.app.PendingIntent] who asks for unique operation or unique request code
     * to function. In order to get rid of this, first, the notification should be shown in a merged manner meaning each
     * notification should not create an individual notification so that notification actions don't create a collision.
     *
     * @return A unique integer number that does not mix with the current session.
     */
    val uniqueNumber: Int
        get() = (System.currentTimeMillis() / 1000).toInt() + ++mUniqueNumber

    fun getViewingPreferences(context: Context): SharedPreferences {
        if (mViewingPreferences == null) mViewingPreferences =
            context.getSharedPreferences(Keyword.Local.SETTINGS_VIEWING, Context.MODE_PRIVATE)
        return mViewingPreferences as SharedPreferences
    }

    fun isLatestChangeLogSeen(context: Context): Boolean {
        val preferences = getDefaultPreferences(context)
        val device = getLocalDevice(context)
        val lastSeenChangelog = preferences.getInt("changelog_seen_version", -1)
        val dialogAllowed = preferences.getBoolean("show_changelog_dialog", true)
        return !preferences.contains("previously_migrated_version") || device.versionCode == lastSeenChangelog
                || !dialogAllowed
    }

    fun publishLatestChangelogSeen(context: Context) {
        val device = getLocalDevice(context)
        getDefaultPreferences(context).edit()
            .putInt("changelog_seen_version", device.versionCode)
            .apply()
    }
}