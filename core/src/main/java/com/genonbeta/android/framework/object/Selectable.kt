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
package com.genonbeta.android.framework.`object`

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
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

/**
 * created by: Veli
 * date: 5.01.2018 10:58
 */
/**
 * @see IPerformerEngine
 *
 * @see IEngineConnection
 */
interface Selectable {
    /**
     * This is used to help humans identify this selectable.
     *
     * @return a human readable string usually distinguishing to be used on a given UI element
     */
    open fun getSelectableTitle(): String?

    /**
     * The current state of this selectable.
     *
     * @return true when it is marked as selected
     */
    open fun isSelectableSelected(): Boolean

    /**
     * This is called when this state of this selectable needs to be altered by an [IEngineConnection] instance.
     *
     * @param selected true when it needs to be marked as selected
     * @return true when it is possible to alter the state
     * @see IPerformerEngine.check
     */
    open fun setSelectableSelected(selected: Boolean): Boolean
}