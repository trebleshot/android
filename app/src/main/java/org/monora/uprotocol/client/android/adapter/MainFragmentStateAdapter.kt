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
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

/**
 * created by: veli
 * date: 11/04/18 21:53
 */
class MainFragmentStateAdapter(
    val context: Context, fm: FragmentManager, lifecycle: Lifecycle,
) : FragmentStateAdapter(fm, lifecycle) {
    private val fragments: MutableList<PageItem> = ArrayList()

    private val fragmentFactory: FragmentFactory = fm.fragmentFactory

    fun add(fragment: PageItem) {
        fragments.add(fragment)
    }

    fun add(position: Int, fragment: PageItem) {
        fragments.add(position, fragment)
    }

    fun createTabs(tabLayout: TabLayout, withIcon: Boolean = true, withText: Boolean = true) {
        if (itemCount > 0) for (iterator in 0 until itemCount) {
            val item = getItem(iterator)
            val tab: TabLayout.Tab = tabLayout.newTab()
            if (withIcon) tab.setIcon(item.iconRes)
            if (withText || !item.iconOnly) tab.text = item.title

            tabLayout.addTab(tab)
        }
    }

    fun createTabs(bottomNavigationView: BottomNavigationView) {
        if (itemCount > 0) for (iterator in 0 until itemCount) {
            val item = getItem(iterator)
            bottomNavigationView.menu.add(0, iterator, iterator, item.title).setIcon(item.iconRes)
        }
    }

    override fun createFragment(position: Int): Fragment {
        val item = getItem(position).apply {
            currentPosition = position
        }
        val fragment = item.fragment ?: fragmentFactory.instantiate(context.classLoader, item.clazz).apply {
            arguments = item.arguments
        }

        return fragment
    }

    override fun getItemCount(): Int = fragments.size

    override fun getItemId(position: Int): Long {
        return getItem(position).id.toLong()
    }

    fun getItem(position: Int): PageItem = synchronized(fragments) { fragments[position] }

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
        var fragment: Fragment? = null

        @IgnoredOnParcel
        var currentPosition = -1
    }
}