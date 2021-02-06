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
package com.genonbeta.TrebleShot.activity

import android.content.*
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.util.AppUtils

/**
 * Created by: veli
 * Date: 1/19/17 5:05 PM
 */
class TextEditorActivity : Activity(), SnackbarPlacementProvider {
    private var mEditTextEditor: EditText? = null
    private var mDbObject: TextStreamObject? = null
    private var mBackPressTime: Long = 0
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent == null || ACTION_EDIT_TEXT != intent.action) finish() else {
            setContentView(R.layout.layout_text_editor_activity)
            if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            mEditTextEditor = findViewById<EditText>(R.id.layout_text_editor_activity_text_text_box)
            if (intent.hasExtra(EXTRA_CLIPBOARD_ID)) {
                mDbObject = TextStreamObject(intent.getLongExtra(EXTRA_CLIPBOARD_ID, -1))
                try {
                    database.reconstruct<Any, TextStreamObject>(mDbObject)
                    mEditTextEditor.getText()
                        .append(mDbObject.text)
                } catch (e: Exception) {
                    e.printStackTrace()
                    mDbObject = null
                }
            } else if (intent.hasExtra(EXTRA_TEXT_INDEX)) mEditTextEditor.getText()
                .append(intent.getStringExtra(EXTRA_TEXT_INDEX))
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_DEVICE && data != null && data.hasExtra(AddDeviceActivity.Companion.EXTRA_DEVICE)
                && data.hasExtra(AddDeviceActivity.Companion.EXTRA_DEVICE_ADDRESS)
            ) {
                val device: Device = data.getParcelableExtra(AddDeviceActivity.Companion.EXTRA_DEVICE)
                val address: DeviceAddress = data.getParcelableExtra(AddDeviceActivity.Companion.EXTRA_DEVICE_ADDRESS)
                val text: String? =
                    if (mEditTextEditor.getText() != null) mEditTextEditor.getText().toString() else null
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
        val hasObject = mDbObject != null
        val deletionNeeded = checkDeletionNeeded()
        val saveNeeded = checkSaveNeeded()
        if (!saveNeeded && !deletionNeeded || System.nanoTime() - mBackPressTime < 2e9) // 2secs to stay in interval
            super.onBackPressed() else if (deletionNeeded) createSnackbar(R.string.ques_deleteEmptiedText)
            .setAction(R.string.butn_delete, View.OnClickListener { v: View? ->
                removeText()
                finish()
            })
            .show() else createSnackbar(if (hasObject) R.string.mesg_clipboardUpdateNotice else R.string.mesg_textSaveNotice)
            .setAction(if (hasObject) R.string.butn_update else R.string.butn_save, View.OnClickListener { v: View? ->
                saveText()
                finish()
            })
            .show()
        mBackPressTime = System.nanoTime()
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val applySupported = (intent != null && intent.hasExtra(EXTRA_SUPPORT_APPLY)
                && intent.getBooleanExtra(EXTRA_SUPPORT_APPLY, false))
        menu.findItem(R.id.menu_action_save)
            .setVisible(!checkDeletionNeeded()).isEnabled = checkSaveNeeded()
        menu.findItem(R.id.menu_action_done).isVisible = applySupported
        menu.findItem(R.id.menu_action_share).isVisible = !applySupported
        menu.findItem(R.id.menu_action_share_trebleshot).isVisible = !applySupported
        menu.findItem(R.id.menu_action_remove).isVisible = mDbObject != null
        menu.findItem(R.id.menu_action_show_as_qr_code).isEnabled = (mEditTextEditor.length() > 0
                && mEditTextEditor.length() <= 1200)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.menu_action_save) {
            saveText()
            Snackbar.make(findViewById<View>(android.R.id.content), R.string.mesg_textStreamSaved, Snackbar.LENGTH_LONG)
                .show()
        } else if (id == R.id.menu_action_done) {
            val intent: Intent = Intent()
                .putExtra(EXTRA_TEXT_INDEX, mEditTextEditor.getText().toString())
            setResult(RESULT_OK, intent)
            finish()
        } else if (id == R.id.menu_action_copy) {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("copiedText", mEditTextEditor.getText().toString()))
            createSnackbar(R.string.mesg_textCopiedToClipboard).show()
        } else if (id == R.id.menu_action_share) {
            val shareIntent: Intent = Intent(Intent.ACTION_SEND)
                .putExtra(Intent.EXTRA_TEXT, mEditTextEditor.getText().toString())
                .setType("text/*")
            startActivity(Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)))
        } else if (id == R.id.menu_action_share_trebleshot) {
            startActivityForResult(
                Intent(this@TextEditorActivity, AddDeviceActivity::class.java),
                REQUEST_CODE_CHOOSE_DEVICE
            )
        } else if (id == R.id.menu_action_show_as_qr_code) {
            if (mEditTextEditor.length() > 0 && mEditTextEditor.length() <= 1200) {
                val formatWriter = MultiFormatWriter()
                try {
                    val bitMatrix: BitMatrix = formatWriter.encode(
                        mEditTextEditor.getText().toString(),
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
        val editorText: String = mEditTextEditor.getText().toString()
        return editorText.length <= 0 && mDbObject != null && editorText != mDbObject.text
    }

    protected fun checkSaveNeeded(): Boolean {
        val editorText: String = mEditTextEditor.getText().toString()
        return editorText.length > 0 && (mDbObject == null || editorText != mDbObject.text)
    }

    override fun createSnackbar(resId: Int, vararg objects: Any): Snackbar {
        return Snackbar.make(findViewById<View>(android.R.id.content), getString(resId, *objects), Snackbar.LENGTH_LONG)
    }

    fun removeText() {
        if (mDbObject != null) {
            database.remove(mDbObject)
            database.broadcast()
            mDbObject = null
        }
    }

    fun saveText() {
        if (mDbObject == null) mDbObject = TextStreamObject(AppUtils.getUniqueNumber())
        if (mDbObject.comparableDate === 0) mDbObject.comparableDate = System.currentTimeMillis()
        mDbObject.text = mEditTextEditor.getText().toString()
        database.publish<Any, TextStreamObject>(mDbObject)
        database.broadcast()
    }

    companion object {
        val TAG = TextEditorActivity::class.java.simpleName
        const val ACTION_EDIT_TEXT = "genonbeta.intent.action.EDIT_TEXT"
        const val EXTRA_SUPPORT_APPLY = "extraSupportApply"
        const val EXTRA_TEXT_INDEX = "extraText"
        const val EXTRA_CLIPBOARD_ID = "clipboardId"
        const val REQUEST_CODE_CHOOSE_DEVICE = 0
    }
}