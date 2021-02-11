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
package com.genonbeta.TrebleShot.fragment.external

/**
 * created by: veli
 * date: 7/20/18 8:56 PM
 */
class ThirdPartyLibraryListFragment :
    DynamicRecyclerViewFragment<ModuleItem?, RecyclerViewAdapter.ViewHolder?, LicencesAdapter?>() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return generateDefaultView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listAdapter = LicencesAdapter(context)
    }

    class LicencesAdapter(context: Context?) :
        RecyclerViewAdapter<ModuleItem, RecyclerViewAdapter.ViewHolder>(context) {
        private val mList: MutableList<ModuleItem> = ArrayList()
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val holder = ViewHolder(
                inflater.inflate(
                    R.layout.list_third_party_library, parent,
                    false
                )
            )
            holder.itemView.findViewById<View>(R.id.menu).setOnClickListener { v: View? ->
                val moduleItem = list[holder.adapterPosition]
                val popupMenu = PopupMenu(context, v)
                popupMenu.menuInflater.inflate(R.menu.popup_third_party_library_item, popupMenu.menu)
                popupMenu.menu
                    .findItem(R.id.popup_visitWebPage).isEnabled = moduleItem.moduleUrl != null
                popupMenu.menu
                    .findItem(R.id.popup_goToLicenceURL).isEnabled = moduleItem.licenceUrl != null
                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    val id = item.itemId
                    if (id == R.id.popup_goToLicenceURL) context.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(moduleItem.licenceUrl))
                    ) else if (id == R.id.popup_visitWebPage) context.startActivity(
                        Intent(Intent.ACTION_VIEW)
                            .setData(Uri.parse(moduleItem.moduleUrl))
                    ) else return@setOnMenuItemClickListener false
                    true
                }
                popupMenu.show()
            }
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = list[position]
            val text1: TextView = holder.itemView.findViewById<TextView>(R.id.text)
            val text2: TextView = holder.itemView.findViewById<TextView>(R.id.text2)
            text1.setText(item.moduleName)
            val stringBuilder = StringBuilder()
            if (item.moduleVersion != null) stringBuilder.append(item.moduleVersion)
            if (item.licence != null) {
                if (stringBuilder.length > 0) stringBuilder.append(", ")
                stringBuilder.append(item.licence)
            }
            text2.setText(stringBuilder.toString())
        }

        override fun getItemCount(): Int {
            return mList.size
        }

        override fun onLoad(): List<ModuleItem> {
            val inputStream = context.resources.openRawResource(R.raw.libraries_index)
            val outputStream = ByteArrayOutputStream()
            try {
                var read: Int
                while (inputStream.read().also { read = it } != -1) outputStream.write(read)
                val jsonObject = JSONObject(outputStream.toString())
                val dependenciesArray = jsonObject.getJSONArray("dependencies")
                val returnedList: MutableList<ModuleItem> = ArrayList()
                for (i in 0 until dependenciesArray.length()) returnedList.add(
                    ModuleItem(
                        dependenciesArray.getJSONObject(
                            i
                        )
                    )
                )
                return returnedList
            } catch (ignored: Exception) {
            }
            return ArrayList()
        }

        override fun onUpdate(passedItem: List<ModuleItem>) {
            synchronized(list) {
                list.clear()
                list.addAll(passedItem)
            }
        }

        override fun getList(): MutableList<ModuleItem> {
            return mList
        }
    }

    class ModuleItem(licenceObject: JSONObject) {
        var moduleName: String? = null
        var moduleUrl: String? = null
        var moduleVersion: String? = null
        var licence: String? = null
        var licenceUrl: String? = null

        init {
            if (licenceObject.has("moduleName")) moduleName = licenceObject.getString("moduleName")
            if (licenceObject.has("moduleUrl")) moduleUrl = licenceObject.getString("moduleUrl")
            if (licenceObject.has("moduleVersion")) moduleVersion = licenceObject.getString("moduleVersion")
            if (licenceObject.has("moduleLicense")) licence = licenceObject.getString("moduleLicense")
            if (licenceObject.has("moduleLicenseUrl")) licenceUrl = licenceObject.getString("moduleLicenseUrl")
        }
    }
}