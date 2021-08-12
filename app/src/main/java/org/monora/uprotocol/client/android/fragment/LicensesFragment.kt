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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.ListLibraryLicenseBinding
import org.monora.uprotocol.client.android.model.LibraryLicense
import org.monora.uprotocol.client.android.viewholder.LibraryLicenseViewHolder
import org.monora.uprotocol.client.android.viewmodel.LicensesViewModel

/**
 * created by: veli
 * date: 7/20/18 8:56 PM
 */
@AndroidEntryPoint
class LicensesFragment : BottomSheetDialogFragment() {
    private val licensesViewModel: LicensesViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_licenses, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = LicensesAdapter()

        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter

        licensesViewModel.licenses.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
    }

    class LicensesAdapter : ListAdapter<LibraryLicense, LibraryLicenseViewHolder>(LibraryLicenseItemCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LibraryLicenseViewHolder {
            return LibraryLicenseViewHolder(
                ListLibraryLicenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: LibraryLicenseViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).hashCode().toLong()
        }
    }
}

class LibraryLicenseItemCallback : DiffUtil.ItemCallback<LibraryLicense>() {
    override fun areItemsTheSame(oldItem: LibraryLicense, newItem: LibraryLicense): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: LibraryLicense, newItem: LibraryLicense): Boolean {
        return oldItem == newItem
    }
}
