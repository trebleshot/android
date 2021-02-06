/*
 * Copyright (C) 2020 Veli Tasalı
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
package com.genonbeta.android.framework.app

import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import kotlin.Throws
import com.genonbeta.android.framework.io.StreamInfo.FolderStateException
import android.provider.OpenableColumns
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.StreamDocumentFile
import androidx.annotation.RequiresApi
import android.provider.DocumentsContract
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import android.view.View
import androidx.fragment.app.Fragment
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

/**
 * created by: veli
 * date: 7/31/18 11:54 AM
 */
open class Fragment : Fragment(), FragmentBase {
    private var mSnackbarContainer: View? = null
    private var mSnackbarLength = Snackbar.LENGTH_LONG
    override fun createSnackbar(resId: Int, vararg objects: Any?): Snackbar? {
        val drawOverView = if (mSnackbarContainer == null) view else mSnackbarContainer
        return if (drawOverView != null) Snackbar.make(
            drawOverView,
            getString(resId, *objects),
            mSnackbarLength
        ) else null
    }

    fun isScreenLandscape(): Boolean {
        return resources.getBoolean(R.bool.genfw_screen_isLandscape)
    }

    fun isScreenSmall(): Boolean {
        return resources.getBoolean(R.bool.genfw_screen_isSmall)
    }

    fun isScreenNormal(): Boolean {
        return resources.getBoolean(R.bool.genfw_screen_isNormal)
    }

    fun isScreenLarge(): Boolean {
        return resources.getBoolean(R.bool.genfw_screen_isLarge)
    }

    fun isXScreenLarge(): Boolean {
        return resources.getBoolean(R.bool.genfw_screen_isXLarge)
    }

    fun setSnackbarLength(length: Int) {
        mSnackbarLength = length
    }

    fun setSnackbarContainer(view: View?) {
        mSnackbarContainer = view
    }
}