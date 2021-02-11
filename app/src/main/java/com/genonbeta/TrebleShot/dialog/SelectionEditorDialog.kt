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

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.compileFrom
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import java.util.*

/**
 * created by: Veli
 * date: 5.01.2018 10:38
 */
class SelectionEditorDialog(activity: Activity?, provider: PerformerEngineProvider) : AlertDialog.Builder(
    activity!!
) {
    private val mLayoutInflater: LayoutInflater
    private val mAdapter: SelfAdapter
    private val mList: List<MappedSelectable<*>>
    private val mMappedConnectionList: MutableList<MappedConnection<*>> = ArrayList()
    fun checkReversed(textView: TextView, removeSign: View, selectable: Selectable) {
        selectable.setSelectableSelected(!selectable.isSelectableSelected())
        mark(textView, removeSign, selectable)
    }

    fun mark(textView: TextView, removeSign: View, selectable: Selectable) {
        val selected: Boolean = selectable.isSelectableSelected()
        textView.setEnabled(selected)
        removeSign.visibility = if (selected) View.GONE else View.VISIBLE
    }

    fun massCheck(check: Boolean) {
        synchronized(mList) { for (mappedConnection in mMappedConnectionList) massCheck(check, mappedConnection) }
        mAdapter.notifyDataSetChanged()
    }

    private fun <T : Selectable?> massCheck(check: Boolean, mappedConnection: MappedConnection<T>) {
        mappedConnection.connection.setSelected(mappedConnection.list, IntArray(mappedConnection.list.size), check)
    }

    override fun show(): AlertDialog {
        val dialog = super.show()
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener { v: View? -> massCheck(true) }
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener { v: View? -> massCheck(false) }
        return dialog
    }

    private inner class SelfAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return mList.size
        }

        override fun getItem(position: Int): Any {
            return mList[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) convertView = mLayoutInflater.inflate(R.layout.list_selection, parent, false)
            val selectable = getItem(position) as MappedSelectable<*>
            val textView1: TextView = convertView.findViewById<TextView>(R.id.text)
            val removalSignView = convertView.findViewById<View>(R.id.removalSign)
            textView1.setText(selectable.selectableTitle)
            mark(textView1, removalSignView, selectable)
            convertView.isClickable = true
            convertView.setOnClickListener { v: View? -> checkReversed(textView1, removalSignView, selectable) }
            return convertView
        }
    }

    private fun <T : Selectable?> addToMappedObjectList(connection: IEngineConnection<T>) {
        mMappedConnectionList.add(MappedConnection(connection, connection.getSelectedItemList()))
    }

    private class MappedConnection<T : Selectable?>(var connection: IEngineConnection<T>, list: List<T>?) {
        var list: List<T>

        init {
            this.list = ArrayList(list)
        }
    }

    companion object {
        val TAG = SelectionEditorDialog::class.java.simpleName
    }

    init {
        val engine = provider.performerEngine
        mLayoutInflater = LayoutInflater.from(activity)
        mAdapter = SelfAdapter()
        mList = compileFrom(engine)
        if (engine != null) for (baseEngineConnection in engine.connectionList) if (baseEngineConnection is IEngineConnection<*>) addToMappedObjectList(
            baseEngineConnection
        )
        val view = mLayoutInflater.inflate(R.layout.layout_selection_editor, null, false)
        val listView = view.findViewById<ListView>(R.id.listView)
        listView.setAdapter(mAdapter)
        listView.dividerHeight = 0
        if (mList.size > 0) setView(view) else setMessage(R.string.text_listEmpty)
        setTitle(R.string.text_previewAndEditList)
        setNeutralButton(R.string.butn_check, null)
        setNegativeButton(R.string.butn_uncheck, null)
        setPositiveButton(R.string.butn_close, null)
    }
}