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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.android.codescanner.BarcodeEncoder
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.util.CommonErrors
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.SharedTextsViewModel
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.protocol.ClipboardType
import javax.inject.Inject

@AndroidEntryPoint
class TextEditorFragment : Fragment(R.layout.layout_text_editor), SnackbarPlacementProvider {
    private val viewModel: SharedTextsViewModel by viewModels()

    private val textEditorViewModel: TextEditorViewModel by viewModels()

    private val args: TextEditorFragmentArgs by navArgs()

    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    private var sharedText: SharedText? = null
        get() = field ?: args.sharedText

    private val text
        get() = requireView().findViewById<EditText>(R.id.editText).text.toString()

    private var shareButton: MenuItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val editText = view.findViewById<EditText>(R.id.editText)
        val text = args.text ?: args.sharedText?.text
        val backPressedDispatcher = requireActivity().onBackPressedDispatcher
        val snackbar = createSnackbar(R.string.sending)
        val backPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Capture back press events when there is unsaved changes that should be handled first.

                val hasObject = sharedText != null
                val eventListener = object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                        super.onDismissed(transientBottomBar, event)
                        isEnabled = true
                    }

                    override fun onShown(transientBottomBar: Snackbar?) {
                        super.onShown(transientBottomBar)
                        isEnabled = false
                    }
                }

                when {
                    checkDeletionNeeded() -> {
                        createSnackbar(R.string.delete_empty_text_question)
                            .addCallback(eventListener)
                            .setAction(R.string.delete) {
                                removeText()
                                backPressedDispatcher.onBackPressed()
                            }
                            .show()
                    }
                    checkSaveNeeded() -> {
                        createSnackbar(if (hasObject) R.string.update_clipboard_notice else R.string.save_clipboard_notice)
                            .addCallback(eventListener)
                            .setAction(if (hasObject) R.string.update else R.string.save) {
                                saveText()
                                backPressedDispatcher.onBackPressed()
                            }
                            .show()
                    }
                    else -> {
                        isEnabled = false
                        backPressedDispatcher.onBackPressed()
                    }
                }
            }
        }

        editText.addTextChangedListener {
            backPressedCallback.isEnabled = checkDeletionNeeded() || checkSaveNeeded()
            updateShareButton()
        }

        backPressedDispatcher.addCallback(viewLifecycleOwner, backPressedCallback)

        text?.let {
            editText.text.apply {
                clear()
                append(text)
            }
        }

        textEditorViewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is SendTextState.Sending -> snackbar.setText(R.string.sending).show()
                is SendTextState.Success -> snackbar.setText(R.string.send_success).show()
                is SendTextState.Error -> snackbar.setText(CommonErrors.messageOf(view.context, it.exception)).show()
            }
        }

        clientPickerViewModel.bridge.observe(viewLifecycleOwner) { bridge ->
            textEditorViewModel.consume(bridge, this@TextEditorFragment.text)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.text_editor, menu)
        shareButton = menu.findItem(R.id.menu_action_share_trebleshot)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.menu_action_save)
            .setVisible(!checkDeletionNeeded()).isEnabled = checkSaveNeeded()
        menu.findItem(R.id.menu_action_remove).isVisible = sharedText != null
        menu.findItem(R.id.menu_action_show_as_qr_code).isEnabled = (text.length in 1..1200)
        updateShareButton()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_action_save) {
            saveText()
            createSnackbar(R.string.save_text_success).show()
        } else if (id == R.id.menu_action_copy) {
            (requireContext().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("copiedText", text))
            createSnackbar(R.string.copy_text_to_clipboard_success).show()
        } else if (id == R.id.menu_action_share) {
            val shareIntent: Intent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, text)
                .setType("text/*")
            startActivity(Intent.createChooser(shareIntent, getString(R.string.choose_sharing_app)))
        } else if (id == R.id.menu_action_share_trebleshot) {
            findNavController().navigate(TextEditorFragmentDirections.pickClient())
        } else if (id == R.id.menu_action_show_as_qr_code) {
            if (text.length in 1..1200) {
                val formatWriter = MultiFormatWriter()
                try {
                    val bitMatrix: BitMatrix = formatWriter.encode(text, BarcodeFormat.QR_CODE, 800, 800)
                    val encoder = BarcodeEncoder()
                    val bitmap: Bitmap = encoder.createBitmap(bitMatrix)
                    val dialog = BottomSheetDialog(requireActivity())
                    val view = LayoutInflater.from(requireActivity()).inflate(
                        R.layout.layout_show_text_as_qr_code, null
                    )
                    val qrImage = view.findViewById<ImageView>(R.id.layout_show_text_as_qr_code_image)
                    GlideApp.with(this)
                        .load(bitmap)
                        .into(qrImage)
                    val params = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    dialog.setTitle(R.string.show_as_qr_code)
                    dialog.setContentView(view, params)
                    dialog.show()
                } catch (e: WriterException) {
                    Toast.makeText(requireContext(), R.string.unknown_failure, Toast.LENGTH_SHORT).show()
                }
            }
        } else if (id == R.id.menu_action_remove) {
            removeText()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    private fun removeText() {
        sharedText?.let {
            viewModel.remove(it)
            sharedText = null
        }
    }

    override fun createSnackbar(resId: Int, vararg objects: Any?): Snackbar {
        return Snackbar.make(requireView(), getString(resId, *objects), Snackbar.LENGTH_LONG)
    }

    private fun checkDeletionNeeded(): Boolean {
        val editorText: String = text
        return editorText.isEmpty() && !sharedText?.text.isNullOrEmpty()
    }

    private fun checkSaveNeeded(): Boolean {
        val editorText: String = text
        return editorText.isNotEmpty() && editorText != sharedText?.text
    }

    private fun updateShareButton() {
        shareButton?.isEnabled = this@TextEditorFragment.text.isNotEmpty()
    }

    private fun saveText() {
        val date = System.currentTimeMillis()
        var update = false
        val item = sharedText?.also {
            it.modified = date
            it.text = text
            update = true
        } ?: SharedText(0, null, text, date).also {
            this.sharedText = it
        }

        viewModel.save(item, update)
    }
}

@HiltViewModel
class TextEditorViewModel @Inject internal constructor() : ViewModel() {
    private val _state = MutableLiveData<SendTextState>()

    val state = liveData {
        emitSource(_state)
    }

    fun consume(bridge: CommunicationBridge, text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.postValue(SendTextState.Sending)

            try {
                bridge.use {
                    if (it.requestClipboard(text, ClipboardType.Text)) {
                        _state.postValue(SendTextState.Success)
                    }
                }
            } catch (e: Exception) {
                _state.postValue(SendTextState.Error(e))
            }
        }
    }
}

sealed class SendTextState {
    object Sending : SendTextState()

    object Success : SendTextState()

    class Error(val exception: Exception) : SendTextState()
}
