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
package org.monora.uprotocol.client.android.activity

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.adapter.MainFragmentStateAdapter
import org.monora.uprotocol.client.android.adapter.MainFragmentStateAdapter.PageItem
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.app.ListingFragmentBase
import org.monora.uprotocol.client.android.fragment.content.AudioBrowserFragment
import org.monora.uprotocol.client.android.fragment.content.FileFragment
import org.monora.uprotocol.client.android.fragment.content.ImageBrowserFragment
import org.monora.uprotocol.client.android.fragment.content.VideoBrowserFragment
import org.monora.uprotocol.client.android.util.Selections

/**
 * created by: veli
 * date: 13/04/18 19:45
 */
@AndroidEntryPoint
class ContentSharingActivity : Activity(), PerformerEngineProvider {
    private val performerEngine = PerformerEngine()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_sharing)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tabLayout: TabLayout = findViewById(R.id.activity_content_sharing_tab_layout)
        val viewPager: ViewPager2 = findViewById(R.id.activity_content_sharing_view_pager)
        val pagerAdapter: MainFragmentStateAdapter = object : MainFragmentStateAdapter(
            this, supportFragmentManager, lifecycle
        ) {
            override fun onItemInstantiated(item: PageItem) {
                val fragment: Fragment? = item.fragment
                if (fragment is ListingFragmentBase<*>) {
                    if (viewPager.currentItem == item.currentPosition) {

                    }
                }
            }
        }
        pagerAdapter.add(
            PageItem(
                0,
                R.drawable.ic_file_document_box_white_24dp,
                getString(R.string.text_files),
                FileFragment::class.java.name
            )
        )
        pagerAdapter.add(
            PageItem(
                1,
                R.drawable.ic_music_note_white_24dp,
                getString(R.string.text_music),
                AudioBrowserFragment::class.java.name
            )
        )
        pagerAdapter.add(
            PageItem(
                2,
                R.drawable.ic_photo_white_24dp,
                getString(R.string.text_image),
                ImageBrowserFragment::class.java.name
            )
        )
        pagerAdapter.add(
            PageItem(
                3,
                R.drawable.ic_video_library_white_24dp,
                getString(R.string.text_video),
                VideoBrowserFragment::class.java.name
            )
        )

        pagerAdapter.createTabs(tabLayout, withIcon = false, withText = true)
        viewPager.adapter = pagerAdapter

        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        viewPager.setCurrentItem(tab.position, true)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabReselected(tab: TabLayout.Tab?) {

                }
            }
        )

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    tabLayout.getTabAt(position)?.select()
                    val fragment = pagerAdapter.getItem(position).fragment
                    if (fragment is ListingFragmentBase<*>) {
                        val editableListFragment = fragment as ListingFragmentBase<*>
                        Handler(Looper.getMainLooper()).postDelayed(
                            { editableListFragment.adapterImpl.syncAllAndNotify() }, 200
                        )
                    }
                }
            }
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (canExit()) finish()
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun canExit(): Boolean {
        if (Selections.getTotalSize(performerEngine) > 0) {
            AlertDialog.Builder(this)
                .setMessage(R.string.ques_cancelSelection)
                .setNegativeButton(R.string.butn_no, null)
                .setPositiveButton(R.string.butn_yes) { dialog: DialogInterface?, which: Int -> finish() }
                .show()
            return false
        }
        return true
    }

    override fun getPerformerEngine(): IPerformerEngine {
        return performerEngine
    }
}