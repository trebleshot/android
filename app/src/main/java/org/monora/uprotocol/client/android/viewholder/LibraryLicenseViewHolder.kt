package org.monora.uprotocol.client.android.viewholder

import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.databinding.ListLibraryLicenseBinding
import org.monora.uprotocol.client.android.model.LibraryLicense
import org.monora.uprotocol.client.android.viewmodel.content.LibraryLicenseContentViewModel

class LibraryLicenseViewHolder(
    private val binding: ListLibraryLicenseBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(libraryLicense: LibraryLicense) {
        binding.viewModel = LibraryLicenseContentViewModel(libraryLicense)
        binding.executePendingBindings()
    }
}