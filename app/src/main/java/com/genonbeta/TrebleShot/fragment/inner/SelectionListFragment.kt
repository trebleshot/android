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
package com.genonbeta.TrebleShot.fragment.inner

import android.content.Context
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.widget.AppCompatCheckBox
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.fragment.inner.SelectionListFragment.MyAdapter
import com.genonbeta.TrebleShot.ui.callback.IconProvider
import com.genonbeta.TrebleShot.ui.callback.TitleProvider
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.android.framework.util.actionperformer.Selectable
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder

/**
 * created by: veli
 * date: 9/3/18 10:17 PM
 */
class SelectionListFragment : DynamicRecyclerViewFragment<Selectable, ViewHolder, MyAdapter>(), IconProvider,
    TitleProvider {
    override val iconRes: Int = R.drawable.ic_insert_drive_file_white_24dp

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

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_files)
    }

    fun updateSelection(selectAll: Boolean) {
        synchronized(adapter.getList()) {
            for (selectable in adapter.getList()) selectable.setSelectableSelected(selectAll)
        }
        adapter.notifyDataSetChanged()
    }

    class MyAdapter(context: Context) : RecyclerViewAdapter<Selectable, ViewHolder>(context) {
        private val list: ArrayList<Selectable> = ArrayList()

        private val pendingList: ArrayList<Selectable> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val holder = ViewHolder(layoutInflater.inflate(R.layout.list_selection, parent, false))
            val checkBox: AppCompatCheckBox = holder.itemView.findViewById(R.id.checkbox)
            holder.itemView.setOnClickListener { v: View? -> checkReversed(checkBox, list[holder.adapterPosition]) }
            checkBox.setOnClickListener { v: View -> checkReversed(checkBox, list[holder.adapterPosition]) }
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val selectable: Selectable = list[position]
            val checkBox: AppCompatCheckBox = holder.itemView.findViewById(R.id.checkbox)
            val text1: TextView = holder.itemView.findViewById<TextView>(R.id.text)

            text1.text = selectable.getSelectableTitle()
            checkBox.isChecked = selectable.isSelectableSelected()
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onLoad(): MutableList<Selectable> {
            val selectableList: MutableList<Selectable> = ArrayList(pendingList)
            pendingList.clear()
            return selectableList
        }

        override fun onUpdate(passedItem: MutableList<Selectable>) {
            synchronized(list) {
                list.clear()
                list.addAll(passedItem)
            }
        }

        override fun getList(): ArrayList<Selectable> {
            return list
        }

        fun checkReversed(checkBox: AppCompatCheckBox, selectable: Selectable) {
            if (selectable.setSelectableSelected(!selectable.isSelectableSelected())) {
                checkBox.isChecked = selectable.isSelectableSelected()
            }
        }

        protected fun load(selectableList: ArrayList<out Selectable>?) {
            if (selectableList == null) return
            synchronized(pendingList) {
                pendingList.clear()
                pendingList.addAll(selectableList)
            }
        }
    }
}