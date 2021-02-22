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

import android.content.Context
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.*

/**
 * created by: veli
 * date: 11/04/18 21:53
 */
open class MainFragmentPagerAdapter(
    val context: Context, fm: FragmentManager,
) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {
    private val fragments: MutableList<PageItem> = ArrayList()

    private val fragmentFactory: FragmentFactory = fm.fragmentFactory

    open fun onItemInstantiated(item: PageItem) {}

    fun add(fragment: PageItem) {
        fragments.add(fragment)
    }

    fun add(position: Int, fragment: PageItem) {
        fragments.add(position, fragment)
    }

    @JvmOverloads
    fun createTabs(tabLayout: TabLayout, withIcon: Boolean = true, withText: Boolean = true) {
        if (count > 0) for (iterator in 0 until count) {
            val item = getPagerItem(iterator)
            val tab: TabLayout.Tab = tabLayout.newTab()
            if (withIcon) tab.setIcon(item.iconRes)
            if (withText || !item.iconOnly) tab.text = item.title

            tabLayout.addTab(tab)
        }
    }

    fun createTabs(bottomNavigationView: BottomNavigationView) {
        if (count > 0) for (iterator in 0 until count) {
            val item = getPagerItem(iterator)
            bottomNavigationView.menu.add(0, iterator, iterator, item.title).setIcon(item.iconRes)
        }
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val fragment = super.instantiateItem(container, position) as Fragment
        Log.d(MainFragmentPagerAdapter::class.java.simpleName, "instantiateItem: " + fragment.javaClass.name)
        val item = getPagerItem(position)
        item.initiatedItem = fragment
        item.currentPosition = position
        onItemInstantiated(item)
        return fragment
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getItemId(position: Int): Long {
        return getPagerItem(position).id.toLong()
    }

    override fun getItem(position: Int): Fragment {
        val item = getPagerItem(position)
        val instantiatedItem = item.initiatedItem ?: fragmentFactory.instantiate(context.classLoader, item.clazz)
        instantiatedItem.arguments = item.arguments
        return instantiatedItem
    }

    override fun getPageTitle(position: Int): CharSequence = getPagerItem(position).title

    private fun getPagerItem(position: Int): PageItem = synchronized(fragments) { fragments[position] }

    @Parcelize
    class PageItem(
        var id: Int,
        var iconRes: Int,
        var title: String,
        var clazz: String,
        var arguments: Bundle? = null,
        var iconOnly: Boolean = false,
    ) : Parcelable {
        @IgnoredOnParcel
        var initiatedItem: Fragment? = null

        @IgnoredOnParcel
        var currentPosition = -1
    }
}