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

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.adapter.MainFragmentStateAdapter
import org.monora.uprotocol.client.android.adapter.MainFragmentStateAdapter.PageItem
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.app.ListingFragmentBase
import org.monora.uprotocol.client.android.fragment.content.AudioBrowserFragment
import org.monora.uprotocol.client.android.fragment.content.FileFragment
import org.monora.uprotocol.client.android.fragment.content.ImageBrowserFragment
import org.monora.uprotocol.client.android.fragment.content.VideoBrowserFragment
import javax.inject.Inject

/**
 * created by: veli
 * date: 13/04/18 19:45
 */
@AndroidEntryPoint
class ContentSharingActivity : Activity() {
    private val selectionViewModel: SharingSelectionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_sharing)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val tabLayout = findViewById<TabLayout>(R.id.activity_content_sharing_tab_layout)
        val viewPager = findViewById<ViewPager2>(R.id.activity_content_sharing_view_pager)
        val pagerAdapter = MainFragmentStateAdapter(this, supportFragmentManager, lifecycle)

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.sharing, menu)

        val selections = menu.findItem(R.id.selections)
        val share = menu.findItem(R.id.share)

        selectionViewModel.selectionState.observe(this) {
            val enable = it.isNotEmpty()

            selections.title = it.size.toString()
            selections.isEnabled = enable
            share.isEnabled = enable
        }

        return true
    }
}

@HiltViewModel
class SharingSelectionViewModel @Inject internal constructor() : ViewModel() {
    private val selections = mutableListOf<Uri>()

    private val _selectionState = MutableLiveData<List<Uri>>(emptyList())

    val selectionState = liveData {
        emitSource(_selectionState)
    }

    fun contains(uri: Uri) = selections.contains(uri)

    fun setSelected(uri: Uri, selected: Boolean) {
        synchronized(selections) {
            val result = if (selected) selections.add(uri) else selections.remove(uri)

            if (result) {
                _selectionState.postValue(selections)
            }
        }
    }
}