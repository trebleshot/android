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

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.R
import com.genonbeta.android.framework.widget.RecyclerViewAdapter

/**
 * created by: veli
 * date: 26.03.2018 11:45
 */
abstract class RecyclerViewFragment<T, V : RecyclerViewAdapter.ViewHolder, E : RecyclerViewAdapter<T, V>> :
    ListFragment<RecyclerView, T, E>() {
    private val handler: Handler = Handler()
    private val requestFocus: Runnable = Runnable {
        listViewInternal.focusableViewAvailable(
            listViewInternal
        )
    }

    override fun onListRefreshed() {
        super.onListRefreshed()
        setListShown(adapter.getCount() > 0)
    }

    override fun ensureList() {
        handler.post(requestFocus)
    }

    open fun getLayoutManager(): RecyclerView.LayoutManager? {
        return LinearLayoutManager(context)
    }

    override fun generateDefaultView(inflater: LayoutInflater, container: ViewGroup?, savedState: Bundle?): View? {
        return inflater.inflate(R.layout.genfw_listfragment_default_rv, container, false)
    }

    override fun setListAdapter(adapter: E?, hadAdapter: Boolean) {
        listView.setAdapter(adapter)
    }

    override fun setListView(listView: RecyclerView?) {
        super.setListView(listView)
        listView.setLayoutManager(getLayoutManager())
    }
}