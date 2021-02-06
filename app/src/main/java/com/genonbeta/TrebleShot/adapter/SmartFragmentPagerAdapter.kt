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
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.genonbeta.TrebleShot.ui.callback.IconProvider
import com.genonbeta.TrebleShot.ui.callback.TitleProvider
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import java.util.*

/**
 * created by: veli
 * date: 11/04/18 21:53
 */
open class SmartFragmentPagerAdapter(private val context: Context, fm: FragmentManager) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    val fragments: MutableList<StableItem> = ArrayList()

    private val fragmentFactory: FragmentFactory = fm.fragmentFactory

    open fun onItemInstantiated(item: StableItem) {}

    fun add(fragment: StableItem) {
        fragments.add(fragment)
    }

    fun add(position: Int, fragment: StableItem) {
        fragments.add(position, fragment)
    }

    @JvmOverloads
    fun createTabs(tabLayout: TabLayout, icons: Boolean = true, text: Boolean = true) {
        if (getCount() > 0) for (iterator in 0 until count) {
            val stableItem = getStableItem(iterator)
            val fragment = getItem(iterator)
            val tab: TabLayout.Tab = tabLayout.newTab()
            if (fragment is IconProvider && icons)
                tab.setIcon(fragment.iconRes)
            if (!stableItem.iconOnly)
                stableItem.title?.let {
                    if (it.isEmpty())
                        tab.text = stableItem.title
                } ?: run {
                    if (fragment is TitleProvider)
                        tab.text = fragment.getDistinctiveTitle(getContext())
                }

            tabLayout.addTab(tab)
        }
    }

    fun createTabs(bottomNavigationView: BottomNavigationView) {
        if (count > 0) for (iterator in 0 until count) {
            val stableItem = getStableItem(iterator)
            val fragment = getItem(iterator)
            val menuTitle: CharSequence = stableItem.title?.let {
                return
            } ?: run {
                if (fragment is TitleProvider)
                    fragment.getDistinctiveTitle(getContext())
                else
                    iterator.toString()
            }

            val menuItem: MenuItem = bottomNavigationView.menu
                .add(0, iterator, iterator, menuTitle)
            if (fragment is IconProvider)
                menuItem.setIcon(fragment.iconRes)
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        Log.d(SmartFragmentPagerAdapter::class.java.simpleName, "instantiateItem: " + fragment.javaClass.name)
        val stableItem = getStableItem(position)
        stableItem.initiatedItem = fragment
        stableItem.currentPosition = position
        onItemInstantiated(stableItem)
        return fragment
    }

    fun getContext(): Context {
        return context
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getItemId(position: Int): Long {
        return getStableItem(position).itemId
    }

    override fun getItem(position: Int): Fragment {
        val stableItem = getStableItem(position)
        var instantiatedItem = stableItem.initiatedItem
        if (instantiatedItem == null)
            instantiatedItem = fragmentFactory.instantiate(getContext().classLoader, stableItem.clazzName)
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
        return fragments[position]
    }

    class StableItem(var itemId: Long, var clazzName: String, var arguments: Bundle?) : Parcelable {
        var title: String? = null
        var iconOnly = false
        var initiatedItem: Fragment? = null
        var currentPosition = -1

        constructor(itemId: Long, clazz: Class<out Fragment?>, arguments: Bundle?) : this(
            itemId,
            clazz.name,
            arguments
        )

        constructor(source: Parcel) : this(source.readLong(), source.readString(), source.readBundle()) {
            setTitle(source.readString())
            setIconOnly(source.readInt() == 1)
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

        companion object CREATOR : Creator<StableItem> {
            override fun createFromParcel(parcel: Parcel): StableItem {
                return StableItem(parcel)
            }

            override fun newArray(size: Int): Array<StableItem?> {
                return arrayOfNulls(size)
            }
        }
    }
}