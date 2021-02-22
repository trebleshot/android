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
package org.monora.uprotocol.client.android.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.ViewPager
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.adapter.MainFragmentPagerAdapter
import org.monora.uprotocol.client.android.app.Activity.OnBackPressedListener
import com.genonbeta.android.framework.app.Fragment
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment(), SnackbarPlacementProvider, OnBackPressedListener {
    private lateinit var viewPager: ViewPager

    private lateinit var pagerAdapter: MainFragmentPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.layout_home_fragment, container, false)
        val bottomNavigationView: BottomNavigationView = view.findViewById(R.id.layout_home_bottom_navigation_view)
        viewPager = view.findViewById(R.id.layout_home_view_pager)
        pagerAdapter = MainFragmentPagerAdapter(requireContext(), childFragmentManager)
        /*pagerAdapter.add(
            StableItem(
                0, TransferListFragment::class.qualifiedName!!,
                null
            )
        )
        pagerAdapter.add(
            StableItem(
                1, ActiveConnectionListFragment::class.qualifiedName!!,
                null
            )
        )
        pagerAdapter.add(StableItem(2, FileExplorerFragment::class.qualifiedName!!, null))
        pagerAdapter.add(StableItem(3, SharedTextListFragment::class.qualifiedName!!, null))
        pagerAdapter.createTabs(bottomNavigationView)
        viewPager.setAdapter(pagerAdapter)
        viewPager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(i: Int, v: Float, i1: Int) {}
            override fun onPageSelected(i: Int) {
                bottomNavigationView.setSelectedItemId(i)
            }

            override fun onPageScrollStateChanged(i: Int) {}
        })
        bottomNavigationView.setOnNavigationItemSelectedListener { menuItem: MenuItem ->
            viewPager.currentItem = menuItem.order
            true
        }*/
        return view
    }

    override fun onBackPressed(): Boolean {
        val activeItem: Any = pagerAdapter.getItem(viewPager.currentItem)
        if (activeItem is OnBackPressedListener && activeItem.onBackPressed()) return true
        if (viewPager.currentItem > 0) {
            viewPager.setCurrentItem(0, true)
            return true
        }
        return false
    }
}