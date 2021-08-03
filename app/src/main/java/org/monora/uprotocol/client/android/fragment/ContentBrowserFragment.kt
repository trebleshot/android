/*
 * Copyright (C) 2021 Veli Tasalı
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
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.adapter.MainFragmentStateAdapter
import org.monora.uprotocol.client.android.databinding.LayoutContentBrowserBinding
import org.monora.uprotocol.client.android.fragment.content.AudioBrowserFragment
import org.monora.uprotocol.client.android.fragment.content.FileBrowserFragment
import org.monora.uprotocol.client.android.fragment.content.ImageBrowserFragment
import org.monora.uprotocol.client.android.fragment.content.VideoBrowserFragment
import org.monora.uprotocol.client.android.fragment.content.transfer.PrepareIndexFragment
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.SharingSelectionViewModel

@AndroidEntryPoint
class ContentBrowserFragment : Fragment(R.layout.layout_content_browser) {
    private val selectionViewModel: SharingSelectionViewModel by activityViewModels()

    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutContentBrowserBinding.bind(view)
        val pagerAdapter = MainFragmentStateAdapter(requireContext(), childFragmentManager, lifecycle)

        pagerAdapter.add(
            MainFragmentStateAdapter.PageItem(
                0,
                R.drawable.ic_file_document_box_white_24dp,
                getString(R.string.text_files),
                FileBrowserFragment::class.java.name
            )
        )
        pagerAdapter.add(
            MainFragmentStateAdapter.PageItem(
                1,
                R.drawable.ic_music_note_white_24dp,
                getString(R.string.text_music),
                AudioBrowserFragment::class.java.name
            )
        )
        pagerAdapter.add(
            MainFragmentStateAdapter.PageItem(
                2,
                R.drawable.ic_photo_white_24dp,
                getString(R.string.text_image),
                ImageBrowserFragment::class.java.name
            )
        )
        pagerAdapter.add(
            MainFragmentStateAdapter.PageItem(
                3,
                R.drawable.ic_video_library_white_24dp,
                getString(R.string.text_video),
                VideoBrowserFragment::class.java.name
            )
        )

        pagerAdapter.createTabs(binding.tabLayout, withIcon = false, withText = true)
        binding.viewPager.adapter = pagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = pagerAdapter.getItem(position).title
        }.attach()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.sharing, menu)

        val selections = menu.findItem(R.id.selections)
        val share = menu.findItem(R.id.share)

        selectionViewModel.selectionState.observe(this) {
            val enable = it.isNotEmpty()

            selections.title = it.size.toString()
            selections.isEnabled = enable
            share.isEnabled = enable
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.share) {
            findNavController().navigate(
                ContentBrowserFragmentDirections.actionContentBrowserFragmentToPrepareIndexFragment()
            )
        }

        return super.onOptionsItemSelected(item)
    }
}
