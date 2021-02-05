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
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import android.os.Parcelable
import android.os.Parcel
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.os.Bundle
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.os.PowerManager
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import android.view.LayoutInflater
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import android.util.Base64
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import android.widget.ImageView
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
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