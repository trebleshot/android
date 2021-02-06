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

import android.content.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Device
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.InetAddress

object DeviceLoader {
    @Throws(JSONException::class)
    fun loadAsClient(kuick: Kuick, `object`: JSONObject, device: Device) {
        device.isBlocked = false
        device.receiveKey = `object`.getInt(Keyword.DEVICE_KEY)
        loadFrom(kuick, `object`, device)
    }

    @Throws(JSONException::class, DeviceInsecureException::class)
    fun loadAsServer(kuick: Kuick, `object`: JSONObject, device: Device, hasPin: Boolean) {
        device.uid = `object`.getString(Keyword.DEVICE_UID)
        val receiveKey = `object`.getInt(Keyword.DEVICE_KEY)
        if (hasPin) device.isTrusted = true
        try {
            try {
                kuick.reconstruct(device)
                if (hasPin && receiveKey != device.receiveKey) throw ReconstructionFailedException("Generate new keys.")
            } catch (e: ReconstructionFailedException) {
                device.receiveKey = receiveKey
                device.sendKey = AppUtils.generateKey()
            }
            if (hasPin) {
                device.isBlocked = false
            } else if (device.isBlocked) throw DeviceBlockedException(
                "The device is blocked.",
                device
            ) else if (receiveKey != device.receiveKey) {
                device.isBlocked = true
                throw DeviceVerificationException("The device receive key is different.", device, receiveKey)
            }
        } finally {
            loadFrom(kuick, `object`, device)
        }
    }

    @Throws(JSONException::class)
    private fun loadFrom(kuick: Kuick, `object`: JSONObject, device: Device) {
        device.isLocal = AppUtils.getDeviceId(kuick.context) == device.uid
        device.brand = `object`.getString(Keyword.DEVICE_BRAND)
        device.model = `object`.getString(Keyword.DEVICE_MODEL)
        device.username = `object`.getString(Keyword.DEVICE_USERNAME)
        device.lastUsageTime = System.currentTimeMillis()
        device.versionCode = `object`.getInt(Keyword.DEVICE_VERSION_CODE)
        device.versionName = `object`.getString(Keyword.DEVICE_VERSION_NAME)
        device.protocolVersion = `object`.getInt(Keyword.DEVICE_PROTOCOL_VERSION)
        device.protocolVersionMin = `object`.getInt(Keyword.DEVICE_PROTOCOL_VERSION_MIN)
        if (device.username.length > AppConfig.NICKNAME_LENGTH_MAX) device.username =
            device.username.substring(0, AppConfig.NICKNAME_LENGTH_MAX)
        kuick.publish(device)
        saveProfilePicture(kuick.context, device, `object`)
    }

    fun load(kuick: Kuick?, address: InetAddress?, listener: OnDeviceResolvedListener?) {
        Thread {
            try {
                CommunicationBridge.Companion.connect(
                    kuick, DeviceAddress(address),
                    null, 0
                ).use { bridge -> listener?.onDeviceResolved(bridge.getDevice(), bridge.getDeviceAddress()) }
            } catch (ignored: Exception) {
            }
        }.start()
    }

    fun processConnection(kuick: Kuick?, device: Device, address: InetAddress?): DeviceAddress {
        val deviceAddress = DeviceAddress(device.uid, address, System.currentTimeMillis())
        processConnection(kuick, device, deviceAddress)
        return deviceAddress
    }

    fun processConnection(kuick: Kuick, device: Device, deviceAddress: DeviceAddress) {
        deviceAddress.lastCheckedDate = System.currentTimeMillis()
        deviceAddress.deviceId = device.uid
        kuick.remove(
            SQLQuery.Select(Kuick.Companion.TABLE_DEVICEADDRESS)
                .setWhere(Kuick.Companion.FIELD_DEVICEADDRESS_IPADDRESSTEXT + "=?", deviceAddress.hostAddress)
        )
        kuick.publish<Device, DeviceAddress>(deviceAddress)
    }

    fun saveProfilePicture(context: Context, device: Device, `object`: JSONObject) {
        if (!`object`.has(Keyword.DEVICE_AVATAR)) return
        try {
            saveProfilePicture(context, device, Base64.decode(`object`.getString(Keyword.DEVICE_AVATAR), 0))
        } catch (ignored: Exception) {
        }
    }

    @Throws(IOException::class)
    fun saveProfilePicture(context: Context, device: Device, picture: ByteArray) {
        val bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.size) ?: return
        context.openFileOutput(device.generatePictureId(), Context.MODE_PRIVATE)
            .use { outputStream -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream) }
    }

    fun showPictureIntoView(device: Device, imageView: ImageView, iconBuilder: IShapeBuilder) {
        val context = imageView.context
        if (context != null) {
            val file = context.getFileStreamPath(device.generatePictureId())
            if (file.isFile) {
                GlideApp.with(imageView)
                    .asBitmap()
                    .load(file)
                    .circleCrop()
                    .into(imageView)
                return
            }
        }
        imageView.setImageDrawable(iconBuilder.buildRound(device.username))
    }

    interface OnDeviceResolvedListener {
        fun onDeviceResolved(device: Device?, address: DeviceAddress?)
    }
}