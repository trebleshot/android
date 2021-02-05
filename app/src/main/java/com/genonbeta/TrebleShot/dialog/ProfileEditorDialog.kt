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
package com.genonbeta.TrebleShot.dialog

import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.util.AppUtils

class ProfileEditorDialog(activity: Activity) : AlertDialog.Builder(activity) {
    private var mDialog: AlertDialog? = null
    protected fun closeIfPossible() {
        if (mDialog != null) {
            if (mDialog!!.isShowing) mDialog!!.dismiss() else mDialog = null
        }
    }

    override fun show(): AlertDialog {
        return super.show().also { mDialog = it }
    }

    fun saveNickname(activity: Activity, editText: EditText) {
        AppUtils.getDefaultPreferences(context).edit()
            .putString("device_name", editText.text.toString())
            .apply()
        activity.notifyUserProfileChanged()
    }

    init {
        val view = LayoutInflater.from(activity).inflate(R.layout.layout_profile_editor, null, false)
        val image = view.findViewById<ImageView>(R.id.layout_profile_picture_image_default)
        val editImage = view.findViewById<ImageView>(R.id.layout_profile_picture_image_preferred)
        val editText: EditText = view.findViewById(R.id.editText)
        val deviceName = AppUtils.getLocalDeviceName(context)
        editText.text.clear()
        editText.text.append(deviceName)
        activity.loadProfilePictureInto(deviceName, image)
        editText.requestFocus()
        editImage.setOnClickListener {
            activity.requestProfilePictureChange()
            saveNickname(activity, editText)
            closeIfPossible()
        }
        setView(view)
        setNegativeButton(R.string.butn_remove) { _: DialogInterface?, _: Int ->
            activity.deleteFile("profilePicture")
            activity.notifyUserProfileChanged()
        }
        setPositiveButton(R.string.butn_save) { _: DialogInterface?, _: Int ->
            saveNickname(activity, editText)
        }
        setNeutralButton(R.string.butn_close, null)
    }
}