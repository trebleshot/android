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
package com.genonbeta.android.framework.util.actionperformer

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
 * If you want to be informed when [IEngineConnection.setSelected] is invoked you can do so with this listener.
 * Unlike [PerformerCallback], this doesn't have the ability to manipulate the process. This is informed after
 * the process is done.
 */
interface PerformerListener {
    /**
     * This is called when the state of a [Selectable] has changed.
     *
     * @param engine     that is holding an instance of this class
     * @param owner      is the connection that is making the call
     * @param selectable is the [Selectable] whose state has been changed
     * @param isSelected is the new state that has been set
     * @param position   is where the [Selectable] is positioned in
     * [SelectableProvider.getSelectableList]
     */
    open fun onSelected(
        engine: IPerformerEngine?, owner: IBaseEngineConnection?, selectable: Selectable?, isSelected: Boolean,
        position: Int
    )

    /**
     * This method is called after a list of [Selectable]s has changed.
     *
     * @param engine         that is holding an instance of this class
     * @param owner          is the connection that is making the call
     * @param selectableList is the list of [Selectable]s whose states have been changed
     * @param isSelected     is the new state that has been set
     * @param positions      are where the [Selectable]s are positioned in
     * [SelectableProvider.getSelectableList]
     */
    open fun onSelected(
        engine: IPerformerEngine?, owner: IBaseEngineConnection?, selectableList: MutableList<out Selectable?>?,
        isSelected: Boolean, positions: IntArray?
    )
}