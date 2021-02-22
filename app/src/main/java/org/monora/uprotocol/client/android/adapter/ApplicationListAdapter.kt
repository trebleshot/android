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
package org.monora.uprotocol.client.android.adapter

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.IListingFragment
import org.monora.uprotocol.client.android.model.AppPackageModel
import org.monora.uprotocol.client.android.util.AppUtils
import org.monora.uprotocol.client.android.widget.ListingAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter

class ApplicationListAdapter(
    fragment: IListingFragment<AppPackageModel, ViewHolder>,
) : ListingAdapter<AppPackageModel, RecyclerViewAdapter.ViewHolder>(fragment) {
    private val preferences = AppUtils.getDefaultPreferences(context)

    private val manager = context.packageManager

    override fun onLoad(): MutableList<AppPackageModel> {
        val list = ArrayList<AppPackageModel>()
        val showSystemApps = preferences.getBoolean("show_system_apps", false)

        for (packageInfo in manager.getInstalledPackages(PackageManager.GET_META_DATA)) {
            try {
                val info = packageInfo.applicationInfo
                if (info.flags and ApplicationInfo.FLAG_SYSTEM == 1 && !showSystemApps) continue

                list.add(AppPackageModel(packageInfo, info, info.loadLabel(manager).toString()))
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        return list
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(layoutInflater.inflate(R.layout.list_application, parent, false)).also {
            fragment.registerLayoutViewClicks(it)
            it.itemView.findViewById<View>(R.id.visitView).setOnClickListener { v: View? ->
                fragment.performLayoutClickOpen(it)
            }
            it.itemView.findViewById<View>(R.id.selector).setOnClickListener { v: View? ->
                fragment.setItemSelected(it, true)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val parentView: View = holder.itemView
        val item = getItem(position)
        val image: ImageView = parentView.findViewById(R.id.image)
        val text1: TextView = parentView.findViewById(R.id.text)
        val text2: TextView = parentView.findViewById(R.id.text2)
        val layoutSplitApk = parentView.findViewById<ViewGroup>(R.id.layout_split_apk)
        val isSplitApk = Build.VERSION.SDK_INT >= 21 && item.applicationInfo.splitSourceDirs != null
        text1.text = item.name()
        text2.text = item.packageInfo.versionName
        layoutSplitApk.visibility = if (isSplitApk) View.VISIBLE else View.GONE
        parentView.isSelected = item.selected()
        GlideApp.with(context)
            .load(item.applicationInfo)
            .transition(DrawableTransitionOptions.withCrossFade())
            .override(160)
            .into(image)
    }
}

