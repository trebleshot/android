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
package com.genonbeta.TrebleShot.adapter

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.ApplicationListAdapter.PackageHolder
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.dataobject.Container
import com.genonbeta.TrebleShot.io.Containable
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.listing.Merger
import java.io.File
import java.util.*

class ApplicationListAdapter(fragment: IEditableListFragment<PackageHolder, GroupViewHolder>) :
    GroupEditableListAdapter<PackageHolder, GroupViewHolder>(fragment, MODE_GROUP_BY_DATE) {
    private val preferences = AppUtils.getDefaultPreferences(context)

    private val manager = context.packageManager

    override fun onLoad(lister: GroupLister<PackageHolder>) {
        val showSystemApps = preferences.getBoolean("show_system_apps", false)
        for (packageInfo in context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)) {
            try {
                val appInfo: ApplicationInfo = packageInfo.applicationInfo
                if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1 && showSystemApps) continue
                val packageHolder = PackageHolder(
                    appInfo.loadLabel(manager).toString(), appInfo,
                    packageInfo.versionName, packageInfo.packageName, File(appInfo.sourceDir)
                )
                lister.offerObliged(this, packageHolder)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun onGenerateRepresentative(text: String, merger: Merger<PackageHolder>?): PackageHolder {
        return PackageHolder(VIEW_TYPE_REPRESENTATIVE, text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == VIEW_TYPE_DEFAULT) GroupViewHolder(
            layoutInflater.inflate(
                R.layout.list_application,
                parent,
                false
            )
        ) else createDefaultViews(parent, viewType, false)
        if (!holder.isRepresentative()) {
            fragment.registerLayoutViewClicks(holder)
            holder.itemView.findViewById<View>(R.id.visitView).setOnClickListener { v: View? ->
                fragment.performLayoutClickOpen(holder)
            }
            holder.itemView.findViewById<View>(R.id.selector).setOnClickListener { v: View? ->
                fragment.setItemSelected(holder, true)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val parentView: View = holder.itemView
        val item: PackageHolder = getItem(position)
        if (!holder.tryBinding(item)) {
            val image = parentView.findViewById<ImageView>(R.id.image)
            val text1: TextView = parentView.findViewById(R.id.text)
            val text2: TextView = parentView.findViewById(R.id.text2)
            val layoutSplitApk = parentView.findViewById<ViewGroup>(R.id.layout_split_apk)
            val isSplitApk = Build.VERSION.SDK_INT >= 21 && item.appInfo.splitSourceDirs != null
            text1.text = item.friendlyName
            text2.text = item.version
            layoutSplitApk.visibility = if (isSplitApk) View.VISIBLE else View.GONE
            parentView.isSelected = item.isSelectableSelected()
            GlideApp.with(context)
                .load(item.appInfo)
                .transition(DrawableTransitionOptions.withCrossFade())
                .override(160)
                .into(image)
        }
    }

    class PackageHolder : GroupShareable, Container {
        lateinit var appInfo: ApplicationInfo

        var version: String? = null

        lateinit var packageName: String

        constructor(viewType: Int, representativeText: String) : super(viewType, representativeText)

        constructor(
            friendlyName: String, appInfo: ApplicationInfo, version: String?, packageName: String,
            executableFile: File,
        ) {
            initialize(
                appInfo.packageName.hashCode().toLong(), friendlyName, friendlyName + "_" + version + FORMAT,
                MIME_TYPE, executableFile.lastModified(), executableFile.length(), Uri.fromFile(executableFile)
            )
            this.appInfo = appInfo
            this.version = version
            this.packageName = packageName
        }

        override fun expand(): Containable? {
            if (Build.VERSION.SDK_INT < 21)
                return null

            val splitSourceDirs = appInfo.splitSourceDirs ?: return null
            val fileList: MutableList<Uri> = ArrayList()
            for (location in splitSourceDirs) fileList.add(Uri.fromFile(File(location)))

            return Containable(uri, fileList)
        }

        companion object {
            const val FORMAT = ".apk"
            val MIME_TYPE = Files.getFileContentType(FORMAT)
        }
    }

}