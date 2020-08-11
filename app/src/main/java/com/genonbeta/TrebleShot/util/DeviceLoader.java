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
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.protocol.DeviceBlockedException;
import com.genonbeta.TrebleShot.protocol.DeviceInsecureException;
import com.genonbeta.TrebleShot.protocol.DeviceVerificationException;
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener;
import com.genonbeta.TrebleShot.util.communicationbridge.DifferentClientException;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;

public class DeviceLoader
{
    public static void loadFrom(Kuick kuick, JSONObject object, Device device, boolean hasPin, boolean asClient)
            throws JSONException, DeviceInsecureException
    {
        device.uid = object.getString(Keyword.DEVICE_UID);
        int receiveKey = object.getInt(Keyword.DEVICE_KEY);

        try {
            kuick.reconstruct(device);
            if (device.isBlocked)
                throw new DeviceBlockedException("The device is blocked.", device);
            else if (receiveKey != device.receiveKey)
                throw new DeviceVerificationException("The device receive key is different.", device, receiveKey);
        } catch (ReconstructionFailedException e) {
            device.isLocal = AppUtils.getDeviceId(kuick.getContext()).equals(device.uid);

            if (hasPin || asClient) {
                device.isBlocked = false;
                device.isTrusted = hasPin;
                device.receiveKey = receiveKey;
                device.sendKey = AppUtils.generateKey();
            } else if (e instanceof DeviceBlockedException) {
                throw (DeviceBlockedException) e;
            } else {
                device.isBlocked = true;

                if (e instanceof DeviceVerificationException) {
                    throw (DeviceInsecureException) e;
                } else {
                    throw new DeviceInsecureException("The device is not known.", device);
                }
            }
        } finally {
            device.brand = object.getString(Keyword.DEVICE_BRAND);
            device.model = object.getString(Keyword.DEVICE_MODEL);
            device.username = object.getString(Keyword.DEVICE_USERNAME);
            device.lastUsageTime = System.currentTimeMillis();
            device.versionCode = object.getInt(Keyword.DEVICE_VERSION_CODE);
            device.versionName = object.getString(Keyword.DEVICE_VERSION_NAME);
            device.protocolVersion = object.getInt(Keyword.DEVICE_PROTOCOL_VERSION);
            device.protocolVersionMin = object.getInt(Keyword.DEVICE_PROTOCOL_VERSION_MIN);

            if (device.username.length() > AppConfig.NICKNAME_LENGTH_MAX)
                device.username = device.username.substring(0, AppConfig.NICKNAME_LENGTH_MAX);

            kuick.publish(device);

            saveProfilePicture(kuick.getContext(), device, object);
        }
    }

    public static void load(Kuick kuick, InetAddress address, @Nullable OnDeviceRegisteredListener listener)
    {
        new Thread(() -> {
            try (CommunicationBridge communicationBridge = CommunicationBridge.connect(kuick,
                    new DeviceAddress(address), null, 0)) {
                if (listener != null)
                    listener.onDeviceRegistered(kuick, communicationBridge.getDevice(),
                            communicationBridge.getDeviceAddress());
            } catch (IOException e) {
                e.printStackTrace();
            } catch (DifferentClientException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public static byte[] loadProfilePictureFrom(JSONObject deviceInfo) throws Exception
    {
        if (deviceInfo.has(Keyword.DEVICE_AVATAR))
            return loadProfilePictureFrom(deviceInfo.getString(Keyword.DEVICE_AVATAR));

        throw new Exception(deviceInfo.toString());
    }

    public static byte[] loadProfilePictureFrom(String base64) throws IllegalArgumentException
    {
        return Base64.decode(base64, 0);
    }

    public static DeviceAddress processConnection(Kuick kuick, Device device, InetAddress address)
    {
        DeviceAddress deviceAddress = new DeviceAddress(device.uid, address, System.currentTimeMillis());
        processConnection(kuick, device, deviceAddress);
        return deviceAddress;
    }

    public static void processConnection(Kuick kuick, Device device, DeviceAddress deviceAddress)
    {
        deviceAddress.lastCheckedDate = System.currentTimeMillis();
        deviceAddress.deviceId = device.uid;

        kuick.remove(new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                .setWhere(Kuick.FIELD_DEVICECONNECTION_IPADDRESSTEXT + "=?", deviceAddress.getHostAddress()));
        kuick.publish(deviceAddress);
    }

    public static void saveProfilePicture(Context context, Device device, JSONObject object)
    {
        try {
            saveProfilePicture(context, device, loadProfilePictureFrom(object));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveProfilePicture(Context context, Device device, byte[] picture)
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
            Log.d(DeviceLoader.class.getSimpleName(), "Bitmap was null");
    }

    public static void showPictureIntoView(Device device, ImageView imageView, TextDrawable.IShapeBuilder iconBuilder)
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

        imageView.setImageDrawable(iconBuilder.buildRound(device.username));
    }

    public interface OnDeviceRegisteredListener extends AttachedTaskListener
    {
        void onDeviceRegistered(Kuick kuick, Device device, DeviceAddress connection);
    }
}
