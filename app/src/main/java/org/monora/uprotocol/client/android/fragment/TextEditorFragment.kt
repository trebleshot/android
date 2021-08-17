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
import androidx.lifecycle.coroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.android.codescanner.BarcodeEncoder
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.TextEditorActivity
import org.monora.uprotocol.client.android.data.SharedTextRepository
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.core.protocol.ClipboardType
import javax.inject.Inject

@AndroidEntryPoint
class TextEditorFragment : Fragment(R.layout.layout_text_editor), SnackbarPlacementProvider {
    @Inject
    lateinit var sharedTextRepository: SharedTextRepository

    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    private var sharedText: SharedText? = null

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
        val text = requireActivity().intent?.let {
            if (it.hasExtra(TextEditorActivity.EXTRA_TEXT_MODEL)) {
                sharedText = it.getParcelableExtra(TextEditorActivity.EXTRA_TEXT_MODEL)
                return@let sharedText?.text
            } else if (it.hasExtra(TextEditorActivity.EXTRA_TEXT)) {
                return@let it.getStringExtra(TextEditorActivity.EXTRA_TEXT)
            }

            null
        }

        val backPressedDispatcher = requireActivity().onBackPressedDispatcher
        val backPressedCallback = object : OnBackPressedCallback(false) {
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
                        createSnackbar(R.string.ques_deleteEmptiedText)
                            .addCallback(eventListener)
                            .setAction(R.string.butn_delete) {
                                removeText()
                                backPressedDispatcher.onBackPressed()
                            }
                            .show()
                    }
                    checkSaveNeeded() -> {
                        createSnackbar(if (hasObject) R.string.mesg_clipboardUpdateNotice else R.string.mesg_textSaveNotice)
                            .addCallback(eventListener)
                            .setAction(if (hasObject) R.string.butn_update else R.string.butn_save) {
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

        clientPickerViewModel.bridge.observe(viewLifecycleOwner) { bridge ->
            createSnackbar(R.string.text_sending).show()
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    bridge.use {
                        if (it.requestClipboard(this@TextEditorFragment.text, ClipboardType.Text)) {
                            lifecycleScope.launch {
                                createSnackbar(R.string.mesg_sent).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.actions_text_editor, menu)
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
            createSnackbar(R.string.mesg_textStreamSaved).show()
        } else if (id == R.id.menu_action_copy) {
            (requireContext().getSystemService(AppCompatActivity.CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("copiedText", text))
            createSnackbar(R.string.mesg_textCopiedToClipboard).show()
        } else if (id == R.id.menu_action_share) {
            val shareIntent: Intent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, text)
                .setType("text/*")
            startActivity(Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)))
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
                    dialog.setTitle(R.string.butn_showAsQrCode)
                    dialog.setContentView(view, params)
                    dialog.show()
                } catch (e: WriterException) {
                    Toast.makeText(requireContext(), R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show()
                }
            }
        } else if (id == R.id.menu_action_remove) {
            removeText()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    private fun removeText() {
        sharedText?.let {
            lifecycle.coroutineScope.launch {
                sharedTextRepository.delete(it)
                sharedText = null
            }
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
        val item = this.sharedText?.also {
            it.modified = date
            it.text = text
            update = true
        } ?: SharedText(0, text, date).also {
            this.sharedText = it
        }

        lifecycleScope.launch {
            if (update) sharedTextRepository.update(item) else sharedTextRepository.insert(item)
        }
    }
}
