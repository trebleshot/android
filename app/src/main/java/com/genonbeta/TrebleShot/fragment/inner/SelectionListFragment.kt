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

/**
 * created by: veli
 * date: 9/3/18 10:17 PM
 */
class SelectionListFragment : DynamicRecyclerViewFragment<Selectable?, RecyclerViewAdapter.ViewHolder?, MyAdapter?>(),
    IconProvider, TitleProvider {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.actions_selection_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.actions_selection_list_check_all) updateSelection(true) else if (id == R.id.actions_selection_list_undo_all) updateSelection(
            false
        ) else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listAdapter = MyAdapter(context)
        setEmptyListImage(R.drawable.ic_insert_drive_file_white_24dp)
        setEmptyListText(getString(R.string.text_listEmpty))
        useEmptyListActionButton(getString(R.string.butn_refresh)) { v: View? -> refreshList() }
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    @DrawableRes
    override fun getIconRes(): Int {
        return R.drawable.ic_insert_drive_file_white_24dp
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_files)
    }

    fun updateSelection(selectAll: Boolean) {
        if (adapter != null) {
            synchronized(adapter.getList()) {
                for (selectable in adapter.getList()) selectable.setSelectableSelected(
                    selectAll
                )
            }
            adapter.notifyDataSetChanged()
        }
    }

    class MyAdapter(context: Context?) : RecyclerViewAdapter<Selectable, RecyclerViewAdapter.ViewHolder>(context) {
        private val mList: ArrayList<Selectable> = ArrayList<Selectable>()
        private val mPendingList: ArrayList<Selectable> = ArrayList<Selectable>()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val holder = ViewHolder(inflater.inflate(R.layout.list_selection, parent, false))
            val checkBox: AppCompatCheckBox = holder.itemView.findViewById(R.id.checkbox)
            holder.itemView.setOnClickListener { v: View? -> checkReversed(checkBox, list[holder.adapterPosition]) }
            checkBox.setOnClickListener(View.OnClickListener { v: View? ->
                checkReversed(
                    checkBox,
                    list[holder.adapterPosition]
                )
            })
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val selectable: Selectable = list[position]
            val checkBox: AppCompatCheckBox = holder.itemView.findViewById(R.id.checkbox)
            val text1: TextView = holder.itemView.findViewById<TextView>(R.id.text)
            text1.setText(selectable.getSelectableTitle())
            checkBox.setChecked(selectable.isSelectableSelected())
        }

        override fun getItemCount(): Int {
            return mList.size
        }

        override fun onLoad(): List<Selectable> {
            val selectableList: List<Selectable> = ArrayList<Selectable>(mPendingList)
            mPendingList.clear()
            return selectableList
        }

        override fun onUpdate(passedItem: List<Selectable>) {
            synchronized(list) {
                mList.clear()
                mList.addAll(passedItem)
            }
        }

        override fun getList(): ArrayList<Selectable> {
            return mList
        }

        fun checkReversed(checkBox: AppCompatCheckBox, selectable: Selectable) {
            if (selectable.setSelectableSelected(!selectable.isSelectableSelected())) checkBox.setChecked(selectable.isSelectableSelected())
        }

        protected fun load(selectableList: ArrayList<out Selectable>?) {
            if (selectableList == null) return
            synchronized(mPendingList) {
                mPendingList.clear()
                mPendingList.addAll(selectableList)
            }
        }
    }
}