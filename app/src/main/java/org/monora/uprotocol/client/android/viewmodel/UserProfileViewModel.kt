package org.monora.uprotocol.client.android.viewmodel

import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.ViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.data.UserDataRepository
import org.monora.uprotocol.client.android.databinding.LayoutProfileEditorBinding
import javax.inject.Inject

@HiltViewModel
class UserProfileViewModel @Inject internal constructor(
    private val userDataRepository: UserDataRepository,
) : ViewModel() {
    val client = userDataRepository.client()

    val clientStatic
        get() = userDataRepository.clientStatic()

    val profileEditorListener = View.OnClickListener {
        val binding = LayoutProfileEditorBinding.inflate(
            LayoutInflater.from(it.context), null, false
        )
        val dialog = BottomSheetDialog(it.context)

        binding.viewModel = this
        binding.executePendingBindings()

        dialog.apply {
            setContentView(binding.root)
            show()
        }
    }
}