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

import android.content.*
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.coroutineScope
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.SharedTextModel
import org.monora.uprotocol.client.android.model.Device
import org.monora.uprotocol.client.android.model.DeviceAddress
import org.monora.uprotocol.client.android.taskimport.TextShareTask
import javax.inject.Inject

/**
 * Created by: veli
 * Date: 1/19/17 5:05 PM
 */
@AndroidEntryPoint
class TextEditorActivity : Activity(), SnackbarPlacementProvider {
    @Inject
    lateinit var appDatabase: AppDatabase

    private lateinit var editText: EditText

    private var textModel: SharedTextModel? = null

    private var backPressTime: Long = 0

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent == null || ACTION_EDIT_TEXT != intent.action) finish() else {
            setContentView(R.layout.layout_text_editor_activity)
            editText = findViewById(R.id.layout_text_editor_activity_text_text_box)

            if (intent.hasExtra(EXTRA_TEXT_MODEL)) {
                textModel = intent.getParcelableExtra(EXTRA_TEXT_MODEL)
                textModel?.let {
                    editText.text.clear()
                    editText.text.append(it.text)
                }
            } else if (intent.hasExtra(EXTRA_TEXT)) {
                editText.text.append(intent.getStringExtra(EXTRA_TEXT))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_DEVICE && data != null && data.hasExtra(AddDeviceActivity.EXTRA_DEVICE)
                && data.hasExtra(AddDeviceActivity.EXTRA_DEVICE_ADDRESS)
            ) {
                val device: Device? = data.getParcelableExtra(AddDeviceActivity.EXTRA_DEVICE)
                val address: DeviceAddress? = data.getParcelableExtra(AddDeviceActivity.EXTRA_DEVICE_ADDRESS)
                val text: String? = if (editText.text != null) editText.text.toString() else null

                if (device != null && address != null && text != null) {
                    runUiTask(TextShareTask(device, address, text))
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.actions_text_editor, menu)
        return true
    }

    override fun onBackPressed() {
        val hasObject = textModel != null
        val deletionNeeded = checkDeletionNeeded()
        val saveNeeded = checkSaveNeeded()
        if (!saveNeeded && !deletionNeeded || System.nanoTime() - backPressTime < 2e9) {
            super.onBackPressed()
        } else if (deletionNeeded) {
            createSnackbar(R.string.ques_deleteEmptiedText)
                .setAction(R.string.butn_delete) {
                    removeText()
                    finish()
                }
                .show()
        } else {
            createSnackbar(if (hasObject) R.string.mesg_clipboardUpdateNotice else R.string.mesg_textSaveNotice)
                .setAction(if (hasObject) R.string.butn_update else R.string.butn_save) {
                    saveText()
                    finish()
                }
                .show()
        }
        backPressTime = System.nanoTime()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.menu_action_save)
            .setVisible(!checkDeletionNeeded()).isEnabled = checkSaveNeeded()
        menu.findItem(R.id.menu_action_remove).isVisible = textModel != null
        menu.findItem(R.id.menu_action_show_as_qr_code).isEnabled = (editText.length() in 1..1200)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_action_save) {
            saveText()
            Snackbar.make(findViewById(android.R.id.content), R.string.mesg_textStreamSaved, Snackbar.LENGTH_LONG)
                .show()
        } else if (id == R.id.menu_action_copy) {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("copiedText", editText.getText().toString()))
            createSnackbar(R.string.mesg_textCopiedToClipboard).show()
        } else if (id == R.id.menu_action_share) {
            val shareIntent: Intent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, editText.getText().toString())
                .setType("text/*")
            startActivity(Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)))
        } else if (id == R.id.menu_action_share_trebleshot) {
            startActivityForResult(
                Intent(this@TextEditorActivity, AddDeviceActivity::class.java),
                REQUEST_CODE_CHOOSE_DEVICE
            )
        } else if (id == R.id.menu_action_show_as_qr_code) {
            if (editText.length() in 1..1200) {
                val formatWriter = MultiFormatWriter()
                try {
                    val bitMatrix: BitMatrix = formatWriter.encode(
                        editText.text.toString(),
                        BarcodeFormat.QR_CODE, 800, 800
                    )
                    val encoder = BarcodeEncoder()
                    val bitmap: Bitmap = encoder.createBitmap(bitMatrix)
                    val dialog = BottomSheetDialog(this)
                    val view = LayoutInflater.from(this).inflate(R.layout.layout_show_text_as_qr_code, null)
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
                    Toast.makeText(this, R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show()
                }
            }
        } else if (id == android.R.id.home) {
            onBackPressed()
        } else if (id == R.id.menu_action_remove) {
            removeText()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    protected fun checkDeletionNeeded(): Boolean {
        val editorText: String = editText.text.toString()
        return editorText.isEmpty() && !textModel?.text.isNullOrEmpty()
    }

    protected fun checkSaveNeeded(): Boolean {
        val editorText: String = editText.text.toString()
        return editorText.isNotEmpty() && editorText != textModel?.text
    }

    override fun createSnackbar(resId: Int, vararg objects: Any?): Snackbar {
        return Snackbar.make(findViewById(android.R.id.content), getString(resId, *objects), Snackbar.LENGTH_LONG)
    }

    private fun removeText() {
        textModel?.let {
            lifecycle.coroutineScope.launch {
                appDatabase.sharedTextDao().delete(it)
                textModel = null
            }
        }
    }

    private fun saveText() {
        val text = editText.text.toString()
        val date = System.currentTimeMillis()
        var update = false
        val item = textModel?.also {
            it.modified = date
            it.text = text
            update = true
        } ?: SharedTextModel(0, text, date).also {
            textModel = it
        }

        lifecycle.coroutineScope.launch {
            if (update) appDatabase.sharedTextDao().update(item) else appDatabase.sharedTextDao().insert(item)
        }
    }

    companion object {
        val TAG = TextEditorActivity::class.java.simpleName

        const val ACTION_EDIT_TEXT = "genonbeta.intent.action.EDIT_TEXT"

        const val EXTRA_TEXT = "extraText"

        const val EXTRA_TEXT_MODEL = "extraTextModel"

        const val REQUEST_CODE_CHOOSE_DEVICE = 0
    }
}