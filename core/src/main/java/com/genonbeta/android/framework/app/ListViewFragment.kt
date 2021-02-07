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
import android.os.Handler
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import android.view.View
import android.widget.ListView
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

/**
 * created by: veli
 * date: 26.03.2018 10:48
 */
abstract class ListViewFragment<T, E : ListViewAdapter<T?>?> : ListFragment<ListView?, T?, E?>() {
    private val mHandler: Handler? = Handler()
    private val mRequestFocus: Runnable? = Runnable {
        listViewInternal.focusableViewAvailable(
            listView
        )
    }
    private val mOnClickListener: OnItemClickListener? = object : OnItemClickListener {
        override fun onItemClick(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
            onListItemClick(parent as ListView?, v, position, id)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.setOnItemClickListener(mOnClickListener)
    }

    fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {}

    override fun ensureList() {
        listViewInternal.setEmptyView(emptyListContainerView)
        mHandler.post(mRequestFocus)
    }

    override fun generateDefaultView(inflater: LayoutInflater?, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.genfw_listfragment_default_lv, container, false)
    }
}