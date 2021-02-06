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
import java.util.ArrayList

class PerformerEngine : IPerformerEngine {
    private val mConnectionList: MutableList<IBaseEngineConnection?>? = ArrayList()
    private val mPerformerCallbackList: MutableList<PerformerCallback?>? = ArrayList()
    private val mPerformerListenerList: MutableList<PerformerListener?>? = ArrayList()
    override fun <T : Selectable?> check(
        engineConnection: IEngineConnection<T?>?, selectable: T?, isSelected: Boolean,
        position: Int
    ): Boolean {
        synchronized(mPerformerCallbackList) {
            for (callback in mPerformerCallbackList) if (!callback.onSelection(
                    this,
                    engineConnection,
                    selectable,
                    isSelected,
                    position
                )
            ) return false
        }
        return true
    }

    override fun <T : Selectable?> check(
        engineConnection: IEngineConnection<T?>?, selectableList: MutableList<T?>?,
        isSelected: Boolean, positions: IntArray?
    ): Boolean {
        synchronized(mPerformerCallbackList) {
            for (callback in mPerformerCallbackList) if (!callback.onSelection(
                    this,
                    engineConnection,
                    selectableList,
                    isSelected,
                    positions
                )
            ) return false
        }
        return true
    }

    override fun getSelectionList(): MutableList<out Selectable?>? {
        val selectableList: MutableList<Selectable?> = ArrayList<Selectable?>()
        synchronized(mConnectionList) {
            for (baseEngineConnection in mConnectionList) selectableList.addAll(
                baseEngineConnection.getGenericSelectedItemList()
            )
        }
        return selectableList
    }

    override fun getConnectionList(): MutableList<IBaseEngineConnection?>? {
        return ArrayList(mConnectionList)
    }

    override fun hasActiveSlots(): Boolean {
        return mConnectionList.size > 0
    }

    override fun ensureSlot(provider: PerformerEngineProvider?, selectionConnection: IBaseEngineConnection?): Boolean {
        synchronized(mConnectionList) {
            if (mConnectionList.contains(selectionConnection) || mConnectionList.add(selectionConnection)) {
                if (selectionConnection.getEngineProvider() !== provider) selectionConnection.setEngineProvider(provider)
                return true
            }
        }
        return false
    }

    override fun <T : Selectable?> informListeners(
        engineConnection: IEngineConnection<T?>?, selectable: T?,
        isSelected: Boolean, position: Int
    ) {
        synchronized(mPerformerListenerList) {
            for (listener in mPerformerListenerList) listener.onSelected(
                this,
                engineConnection,
                selectable,
                isSelected,
                position
            )
        }
    }

    override fun <T : Selectable?> informListeners(
        engineConnection: IEngineConnection<T?>?, selectableList: MutableList<T?>?,
        isSelected: Boolean, positions: IntArray?
    ) {
        synchronized(mPerformerListenerList) {
            for (listener in mPerformerListenerList) listener.onSelected(
                this,
                engineConnection,
                selectableList,
                isSelected,
                positions
            )
        }
    }

    override fun removeSlot(selectionConnection: IBaseEngineConnection?): Boolean {
        synchronized(mConnectionList) { return mConnectionList.remove(selectionConnection) }
    }

    override fun removeSlots() {
        synchronized(mConnectionList) { mConnectionList.clear() }
    }

    override fun addPerformerCallback(callback: PerformerCallback?): Boolean {
        synchronized(mPerformerCallbackList) {
            return mPerformerCallbackList.contains(callback) || mPerformerCallbackList.add(
                callback
            )
        }
    }

    override fun addPerformerListener(listener: PerformerListener?): Boolean {
        synchronized(mPerformerListenerList) {
            return mPerformerListenerList.contains(listener) || mPerformerListenerList.add(
                listener
            )
        }
    }

    override fun removePerformerCallback(listener: PerformerCallback?): Boolean {
        synchronized(mPerformerCallbackList) { return mPerformerCallbackList.remove(listener) }
    }

    override fun removePerformerListener(listener: PerformerListener?): Boolean {
        synchronized(mPerformerListenerList) { return mPerformerListenerList.remove(listener) }
    }
}