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
package org.monora.uprotocol.client.android.fragment.inner

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.fragment.inner.SelectionListFragment.MyAdapter
import com.genonbeta.android.framework.app.RecyclerViewFragment
import com.genonbeta.android.framework.util.actionperformer.SelectionModel
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder

/**
 * created by: veli
 * date: 9/3/18 10:17 PM
 */
class SelectionListFragment : RecyclerViewFragment<SelectionModel, ViewHolder, MyAdapter>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.actions_selection_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actions_selection_list_check_all -> updateSelection(true)
            R.id.actions_selection_list_undo_all -> updateSelection(false)
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MyAdapter(requireContext())
        emptyListImageView.setImageResource(R.drawable.ic_insert_drive_file_white_24dp)
        emptyListTextView.text = getString(R.string.text_listEmpty)
        useEmptyListActionButton(getString(R.string.butn_refresh)) { v: View? -> refreshList() }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    fun updateSelection(selectAll: Boolean) {
        synchronized(adapter.getList()) {
            for (selectionModel in adapter.getList()) selectionModel.select(selectAll)
        }
        adapter.notifyDataSetChanged()
    }

    class MyAdapter(context: Context) : RecyclerViewAdapter<SelectionModel, ViewHolder>(context) {
        private val list: ArrayList<SelectionModel> = ArrayList()

        private val pendingList: ArrayList<SelectionModel> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val holder = ViewHolder(layoutInflater.inflate(R.layout.list_selection, parent, false))
            val checkBox: AppCompatCheckBox = holder.itemView.findViewById(R.id.checkbox)
            holder.itemView.setOnClickListener { v: View? -> checkReversed(checkBox, list[holder.adapterPosition]) }
            checkBox.setOnClickListener { v: View -> checkReversed(checkBox, list[holder.adapterPosition]) }
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val selectionModel: SelectionModel = list[position]
            val checkBox: AppCompatCheckBox = holder.itemView.findViewById(R.id.checkbox)
            val text1: TextView = holder.itemView.findViewById<TextView>(R.id.text)

            text1.text = selectionModel.name()
            checkBox.isChecked = selectionModel.selected()
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onLoad(): MutableList<SelectionModel> {
            val selectionModelList: MutableList<SelectionModel> = ArrayList(pendingList)
            pendingList.clear()
            return selectionModelList
        }

        override fun onUpdate(passedItem: MutableList<SelectionModel>) {
            synchronized(list) {
                list.clear()
                list.addAll(passedItem)
            }
        }

        override fun getList(): ArrayList<SelectionModel> {
            return list
        }

        fun checkReversed(checkBox: AppCompatCheckBox, selectionModel: SelectionModel) {
            if (selectionModel.canSelect()) {
                selectionModel.select(!selectionModel.selected())
                checkBox.isChecked = selectionModel.selected()
            }
        }

        protected fun load(selectionModelList: ArrayList<out SelectionModel>?) {
            if (selectionModelList == null) return
            synchronized(pendingList) {
                pendingList.clear()
                pendingList.addAll(selectionModelList)
            }
        }
    }
}