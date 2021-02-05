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

import android.content.*
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import android.os.Parcelable
import android.os.Parcel
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.os.Bundle
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.os.PowerManager
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import android.view.LayoutInflater
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.util.ArrayList

/**
 * created by: veli
 * date: 11/04/18 21:53
 */
open class SmartFragmentPagerAdapter(private val mContext: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm, FragmentPagerAdapter.BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val mItems: MutableList<StableItem> = ArrayList()
    private val mFragmentFactory: FragmentFactory
    open fun onItemInstantiated(item: StableItem) {}
    fun add(fragment: StableItem) {
        mItems.add(fragment)
    }

    fun add(position: Int, fragment: StableItem) {
        mItems.add(position, fragment)
    }

    @JvmOverloads
    fun createTabs(tabLayout: TabLayout, icons: Boolean = true, text: Boolean = true) {
        if (getCount() > 0) for (iterator in 0 until getCount()) {
            val stableItem = getStableItem(iterator)
            val fragment = getItem(iterator)
            val tab: TabLayout.Tab = tabLayout.newTab()
            if (fragment is IconProvider && icons) tab.setIcon((fragment as IconProvider).getIconRes())
            if (!stableItem.iconOnly && text) if (stableItem.title != null && stableItem.title!!.length > 0) tab.setText(
                stableItem.title
            ) else if (fragment is TitleProvider) tab.setText((fragment as TitleProvider).getDistinctiveTitle(getContext()))
            tabLayout.addTab(tab)
        }
    }

    fun createTabs(bottomNavigationView: BottomNavigationView) {
        if (getCount() > 0) for (iterator in 0 until getCount()) {
            val stableItem = getStableItem(iterator)
            val fragment = getItem(iterator)
            var menuTitle: CharSequence?
            menuTitle =
                if (stableItem.title != null && stableItem.title!!.length > 0) stableItem.title else if (fragment is TitleProvider) (fragment as TitleProvider).getDistinctiveTitle(
                    getContext()
                ) else iterator.toString()
            val menuItem: MenuItem = bottomNavigationView.getMenu()
                .add(0, iterator, iterator, menuTitle)
            if (fragment is IconProvider) menuItem.setIcon((fragment as IconProvider).getIconRes())
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        Log.d(SmartFragmentPagerAdapter::class.java.simpleName, "instantiateItem: " + fragment.javaClass.name)
        val stableItem = getStableItem(position)
        stableItem.mInitiatedItem = fragment
        stableItem.mCurrentPosition = position
        onItemInstantiated(stableItem)
        return fragment
    }

    fun getContext(): Context {
        return mContext
    }

    override fun getCount(): Int {
        return mItems.size
    }

    fun getFragments(): List<StableItem> {
        return mItems
    }

    override fun getItemId(position: Int): Long {
        return getStableItem(position).itemId
    }

    override fun getItem(position: Int): Fragment {
        val stableItem = getStableItem(position)
        var instantiatedItem = stableItem.getInitiatedItem()!!
        if (instantiatedItem == null) instantiatedItem =
            mFragmentFactory.instantiate(getContext().classLoader, stableItem.clazzName)
        instantiatedItem.arguments = stableItem.arguments
        return instantiatedItem
    }

    override fun getPageTitle(position: Int): CharSequence? {
        val fragment = getItem(position)
        return if (fragment is TitleProvider) (fragment as TitleProvider).getDistinctiveTitle(getContext()) else super.getPageTitle(
            position
        )
    }

    fun getStableItem(position: Int): StableItem {
        return mItems[position]
    }

    class StableItem(var itemId: Long, var clazzName: String?, var arguments: Bundle?) : Parcelable {
        var title: String? = null
        var iconOnly = false
        var mInitiatedItem: Fragment? = null
        var mCurrentPosition = -1

        constructor(itemId: Long, clazz: Class<out Fragment?>, arguments: Bundle?) : this(
            itemId,
            clazz.name,
            arguments
        ) {
        }

        constructor(source: Parcel) : this(source.readLong(), source.readString(), source.readBundle()) {
            setTitle(source.readString())
            setIconOnly(source.readInt() == 1)
        }

        fun getCurrentPosition(): Int {
            return mCurrentPosition
        }

        fun getInitiatedItem(): Fragment? {
            return mInitiatedItem
        }

        fun setIconOnly(iconOnly: Boolean): StableItem {
            this.iconOnly = iconOnly
            return this
        }

        fun setTitle(title: String?): StableItem {
            this.title = title
            return this
        }

        override fun describeContents(): Int {
            return 0
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeLong(itemId)
            dest.writeString(clazzName)
            dest.writeBundle(arguments)
            dest.writeString(title)
            dest.writeInt(if (iconOnly) 1 else 0)
        }

        companion object {
            val CREATOR: Creator<StableItem> = object : Creator<StableItem?> {
                override fun createFromParcel(source: Parcel): StableItem? {
                    return StableItem(source)
                }

                override fun newArray(size: Int): Array<StableItem?> {
                    return arrayOfNulls(size)
                }
            }
        }
    }

    init {
        mFragmentFactory = fm.fragmentFactory
    }
}