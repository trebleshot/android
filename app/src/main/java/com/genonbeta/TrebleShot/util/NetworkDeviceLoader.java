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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.android.database.SQLQuery;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

public class NetworkDeviceLoader
{
    public static DeviceConnection processConnection(Kuick kuick, NetworkDevice device, String ipAddress)
    {
        DeviceConnection connection = new DeviceConnection(ipAddress);
        processConnection(kuick, device, connection);
        return connection;
    }

    public static void processConnection(Kuick kuick, NetworkDevice device, DeviceConnection connection)
    {
        try {
            kuick.reconstruct(connection);
        } catch (Exception e) {
            AppUtils.applyAdapterName(connection);
        }

        connection.lastCheckedDate = System.currentTimeMillis();
        connection.deviceId = device.id;

        kuick.remove(new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                .setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=? AND "
                                + Kuick.FIELD_DEVICECONNECTION_ADAPTERNAME + " =? AND "
                                + Kuick.FIELD_DEVICECONNECTION_IPADDRESS + " != ?",
                        connection.deviceId, connection.adapterName, connection.ipAddress));

        kuick.publish(connection);
    }

    public static void load(final Kuick kuick, final String ipAddress, OnDeviceRegisteredListener listener)
    {
        load(false, kuick, ipAddress, listener);
    }

    public static NetworkDevice load(boolean currentThread, final Kuick kuick, final String ipAddress,
                                     final OnDeviceRegisteredListener listener)
    {
        if (currentThread)
            return loadInternal(new CommunicationBridge.Client(kuick), kuick, ipAddress, listener);

        CommunicationBridge.connect(kuick, (client -> loadInternal(client, kuick, ipAddress, listener)));
        return null;
    }

    private static NetworkDevice loadInternal(CommunicationBridge.Client client, Kuick kuick, String ipAddress,
                                              OnDeviceRegisteredListener listener)
    {
        try {
            client.communicate(InetAddress.getByName(ipAddress), true);

            NetworkDevice device = client.getDevice();

            if (device.id != null) {
                NetworkDevice localDevice = AppUtils.getLocalDevice(kuick.getContext());
                DeviceConnection connection = processConnection(kuick, device, ipAddress);

                if (!localDevice.id.equals(device.id)) {
                    device.lastUsageTime = System.currentTimeMillis();

                    if (listener != null)
                        listener.onDeviceRegistered(kuick, device, connection);
                }
            }

            client.setReturn(device);
            return device;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static NetworkDevice loadFrom(Kuick kuick, JSONObject object) throws JSONException
    {
        JSONObject deviceInfo = object.getJSONObject(Keyword.DEVICE_INFO);
        JSONObject appInfo = object.getJSONObject(Keyword.APP_INFO);
        NetworkDevice device = new NetworkDevice(deviceInfo.getString(Keyword.DEVICE_INFO_SERIAL));

        try {
            kuick.reconstruct(device);
        } catch (Exception ignored) {
        }

        device.brand = deviceInfo.getString(Keyword.DEVICE_INFO_BRAND);
        device.model = deviceInfo.getString(Keyword.DEVICE_INFO_MODEL);
        device.nickname = deviceInfo.getString(Keyword.DEVICE_INFO_USER);
        device.secureKey = deviceInfo.has(Keyword.DEVICE_INFO_KEY) ? deviceInfo.getInt(Keyword.DEVICE_INFO_KEY) : -1;
        device.lastUsageTime = System.currentTimeMillis();
        device.versionCode = appInfo.getInt(Keyword.APP_INFO_VERSION_CODE);
        device.versionName = appInfo.getString(Keyword.APP_INFO_VERSION_NAME);
        device.clientVersion = appInfo.has(Keyword.APP_INFO_CLIENT_VERSION)
                ? appInfo.getInt(Keyword.APP_INFO_CLIENT_VERSION) : 0;

        if (device.nickname.length() > AppConfig.NICKNAME_LENGTH_MAX)
            device.nickname = device.nickname.substring(0, AppConfig.NICKNAME_LENGTH_MAX - 1);

        saveProfilePicture(kuick.getContext(), device, deviceInfo);
        return device;
    }

    public static byte[] loadProfilePictureFrom(JSONObject deviceInfo) throws Exception
    {
        if (deviceInfo.has(Keyword.DEVICE_INFO_PICTURE))
            return loadProfilePictureFrom(deviceInfo.getString(Keyword.DEVICE_INFO_PICTURE));

        throw new Exception(deviceInfo.toString());
    }

    public static byte[] loadProfilePictureFrom(String base64) throws IllegalArgumentException
    {
        return Base64.decode(base64, 0);
    }

    public static void saveProfilePicture(Context context, NetworkDevice device, JSONObject object)
    {
        try {
            saveProfilePicture(context, device, loadProfilePictureFrom(object));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveProfilePicture(Context context, NetworkDevice device, byte[] picture)
    {
        Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);

        if (bitmap != null)
            try (FileOutputStream outputStream = context.openFileOutput(device.generatePictureId(),
                    Context.MODE_PRIVATE)) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        else
            Log.d(NetworkDeviceLoader.class.getSimpleName(), "Bitmap was null");
    }

    public static void showPictureIntoView(NetworkDevice device, ImageView imageView,
                                           TextDrawable.IShapeBuilder iconBuilder)
    {
        Context context = imageView.getContext();

        if (context != null) {
            File file = context.getFileStreamPath(device.generatePictureId());

            if (file.isFile()) {
                GlideApp.with(imageView)
                        .asBitmap()
                        .load(file)
                        .circleCrop()
                        .into(imageView);

                return;
            }
        }

        imageView.setImageDrawable(iconBuilder.buildRound(device.nickname));
    }

    public interface OnDeviceRegisteredListener extends AttachedTaskListener
    {
        void onDeviceRegistered(Kuick kuick, NetworkDevice device, DeviceConnection connection);
    }
}
