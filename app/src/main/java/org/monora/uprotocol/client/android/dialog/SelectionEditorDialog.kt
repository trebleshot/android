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
package org.monora.uprotocol.client.android.dialog

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.model.MappedSelectionModel
import org.monora.uprotocol.client.android.model.MappedSelectionModel.Companion.compileFrom
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.android.framework.util.actionperformer.SelectionModel

/**
 * created by: Veli
 * date: 5.01.2018 10:38
 */
class SelectionEditorDialog(activity: Activity, provider: PerformerEngineProvider) : AlertDialog.Builder(activity) {
    private val inflater: LayoutInflater = LayoutInflater.from(activity)

    private val adapter = SelfAdapter()

    private val engine = provider.getPerformerEngine()

    private val mappedList: List<MappedSelectionModel<*>> = compileFrom(engine)

    private val mappedConnectionList: MutableList<MappedConnection<*>> = ArrayList()

    fun checkReversed(textView: TextView, removeSign: View, selectionModel: SelectionModel) {
        selectionModel.select(!selectionModel.selected())
        mark(textView, removeSign, selectionModel)
    }

    fun mark(textView: TextView, removeSign: View, selectionModel: SelectionModel) {
        val selected: Boolean = selectionModel.selected()
        textView.setEnabled(selected)
        removeSign.visibility = if (selected) View.GONE else View.VISIBLE
    }

    fun massCheck(check: Boolean) {
        synchronized(mappedList) { for (mappedConnection in mappedConnectionList) massCheck(check, mappedConnection) }
        adapter.notifyDataSetChanged()
    }

    private fun <T : SelectionModel> massCheck(check: Boolean, mappedConnection: MappedConnection<T>) {
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
            return mappedList.size
        }

        override fun getItem(position: Int): Any {
            return mappedList[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.list_selection, parent, false)
            val selectionModel = getItem(position) as MappedSelectionModel<*>
            val textView1: TextView = view.findViewById(R.id.text)
            val removalSignView = view.findViewById<View>(R.id.removalSign)
            textView1.text = selectionModel.name()
            mark(textView1, removalSignView, selectionModel)
            view.isClickable = true
            view.setOnClickListener { v: View? -> checkReversed(textView1, removalSignView, selectionModel) }
            return view
        }
    }

    private fun <T : SelectionModel> addToMappedObjectList(connection: IEngineConnection<T>) {
        mappedConnectionList.add(MappedConnection(connection, connection.getSelectionList()))
    }

    private class MappedConnection<T : SelectionModel>(var connection: IEngineConnection<T>, list: List<T>?) {
        var list: MutableList<T> = if (list == null) ArrayList() else ArrayList(list)
    }

    companion object {
        val TAG = SelectionEditorDialog::class.java.simpleName
    }

    init {
        if (engine != null) {
            for (baseEngineConnection in engine.getConnectionList()) {
                if (baseEngineConnection is IEngineConnection<*>) {
                    addToMappedObjectList(baseEngineConnection)
                }
            }
        }
        val view = inflater.inflate(R.layout.layout_selection_editor, null, false)
        val listView = view.findViewById<ListView>(R.id.listView)
        listView.adapter = adapter
        listView.dividerHeight = 0
        if (mappedList.isNotEmpty()) setView(view) else setMessage(R.string.text_listEmpty)
        setTitle(R.string.text_previewAndEditList)
        setNeutralButton(R.string.butn_check, null)
        setNegativeButton(R.string.butn_uncheck, null)
        setPositiveButton(R.string.butn_close, null)
    }
}