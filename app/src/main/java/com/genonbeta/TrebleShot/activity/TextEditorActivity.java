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

package com.genonbeta.TrebleShot.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.task.TextShareTask;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

/**
 * Created by: veli
 * Date: 1/19/17 5:05 PM
 */

public class TextEditorActivity extends Activity implements SnackbarPlacementProvider
{
    public static final String TAG = TextEditorActivity.class.getSimpleName(),
            ACTION_EDIT_TEXT = "genonbeta.intent.action.EDIT_TEXT",
            EXTRA_SUPPORT_APPLY = "extraSupportApply",
            EXTRA_TEXT_INDEX = "extraText",
            EXTRA_CLIPBOARD_ID = "clipboardId";

    public static final int REQUEST_CODE_CHOOSE_DEVICE = 0;

    private EditText mEditTextEditor;
    private TextStreamObject mDbObject;
    private long mBackPressTime = 0;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (getIntent() == null || !ACTION_EDIT_TEXT.equals(getIntent().getAction()))
            finish();
        else {
            setContentView(R.layout.layout_text_editor_activity);

            if (getSupportActionBar() != null)
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);

            mEditTextEditor = findViewById(R.id.layout_text_editor_activity_text_text_box);

            if (getIntent().hasExtra(EXTRA_CLIPBOARD_ID)) {
                mDbObject = new TextStreamObject(getIntent().getLongExtra(EXTRA_CLIPBOARD_ID, -1));

                try {
                    getDatabase().reconstruct(mDbObject);

                    mEditTextEditor.getText()
                            .append(mDbObject.text);
                } catch (Exception e) {
                    e.printStackTrace();
                    mDbObject = null;
                }
            } else if (getIntent().hasExtra(EXTRA_TEXT_INDEX))
                mEditTextEditor.getText()
                        .append(getIntent().getStringExtra(EXTRA_TEXT_INDEX));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == android.app.Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_DEVICE && data != null
                    && data.hasExtra(AddDeviceActivity.EXTRA_DEVICE)
                    && data.hasExtra(AddDeviceActivity.EXTRA_CONNECTION)) {
                Device device = data.getParcelableExtra(AddDeviceActivity.EXTRA_DEVICE);
                DeviceAddress connection = data.getParcelableExtra(AddDeviceActivity.EXTRA_CONNECTION);
                String text = mEditTextEditor.getText() != null ? mEditTextEditor.getText().toString() : null;

                if (device != null && connection != null && text != null) {
                    runUiTask(new TextShareTask(device, connection, text));
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.actions_text_editor, menu);

        return true;
    }

    @Override
    public void onBackPressed()
    {
        boolean hasObject = mDbObject != null;
        boolean deletionNeeded = checkDeletionNeeded();
        boolean saveNeeded = checkSaveNeeded();

        if ((!saveNeeded && !deletionNeeded) || System.nanoTime() - mBackPressTime < 2e9) // 2secs to stay in interval
            super.onBackPressed();
        else if (deletionNeeded)
            createSnackbar(R.string.ques_deleteEmptiedText)
                    .setAction(R.string.butn_delete, v -> {
                        removeText();
                        finish();
                    })
                    .show();
        else
            createSnackbar(hasObject ? R.string.mesg_clipboardUpdateNotice : R.string.mesg_textSaveNotice)
                    .setAction(hasObject ? R.string.butn_update : R.string.butn_save, v -> {
                        saveText();
                        finish();
                    })
                    .show();

        mBackPressTime = System.nanoTime();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean applySupported = getIntent() != null && getIntent().hasExtra(EXTRA_SUPPORT_APPLY)
                && getIntent().getBooleanExtra(EXTRA_SUPPORT_APPLY, false);

        menu.findItem(R.id.menu_action_save)
                .setVisible(!checkDeletionNeeded())
                .setEnabled(checkSaveNeeded());

        menu.findItem(R.id.menu_action_done)
                .setVisible(applySupported);

        menu.findItem(R.id.menu_action_share)
                .setVisible(!applySupported);

        menu.findItem(R.id.menu_action_share_trebleshot)
                .setVisible(!applySupported);

        menu.findItem(R.id.menu_action_remove).setVisible(mDbObject != null);

        menu.findItem(R.id.menu_action_show_as_qr_code).setEnabled(mEditTextEditor.length() > 0
                && mEditTextEditor.length() <= 1200);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == R.id.menu_action_save) {
            saveText();
            Snackbar.make(findViewById(android.R.id.content), R.string.mesg_textStreamSaved, Snackbar.LENGTH_LONG).show();
        } else if (id == R.id.menu_action_done) {
            Intent intent = new Intent()
                    .putExtra(EXTRA_TEXT_INDEX, mEditTextEditor.getText().toString());

            setResult(RESULT_OK, intent);
            finish();
        } else if (id == R.id.menu_action_copy) {
            ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
                    .setPrimaryClip(ClipData.newPlainText("copiedText", mEditTextEditor.getText().toString()));

            createSnackbar(R.string.mesg_textCopiedToClipboard).show();
        } else if (id == R.id.menu_action_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_TEXT, mEditTextEditor.getText().toString())
                    .setType("text/*");

            startActivity(Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)));
        } else if (id == R.id.menu_action_share_trebleshot) {
            startActivityForResult(new Intent(TextEditorActivity.this, AddDeviceActivity.class),
                    REQUEST_CODE_CHOOSE_DEVICE);
        } else if (id == R.id.menu_action_show_as_qr_code) {
            if (mEditTextEditor.length() > 0 && mEditTextEditor.length() <= 1200) {
                MultiFormatWriter formatWriter = new MultiFormatWriter();

                try {
                    BitMatrix bitMatrix = formatWriter.encode(mEditTextEditor.getText().toString(),
                            BarcodeFormat.QR_CODE, 800, 800);

                    BarcodeEncoder encoder = new BarcodeEncoder();
                    Bitmap bitmap = encoder.createBitmap(bitMatrix);
                    BottomSheetDialog dialog = new BottomSheetDialog(this);

                    View view = LayoutInflater.from(this).inflate(R.layout.layout_show_text_as_qr_code, null);
                    ImageView qrImage = view.findViewById(R.id.layout_show_text_as_qr_code_image);

                    GlideApp.with(this)
                            .load(bitmap)
                            .into(qrImage);

                    ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);

                    dialog.setTitle(R.string.butn_showAsQrCode);
                    dialog.setContentView(view, params);
                    dialog.show();
                } catch (WriterException e) {
                    Toast.makeText(this, R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (id == android.R.id.home) {
            onBackPressed();
        } else if (id == R.id.menu_action_remove) {
            removeText();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    protected boolean checkDeletionNeeded()
    {
        String editorText = mEditTextEditor.getText().toString();
        return editorText.length() <= 0 && mDbObject != null && !editorText.equals(mDbObject.text);
    }

    protected boolean checkSaveNeeded()
    {
        String editorText = mEditTextEditor.getText().toString();
        return editorText.length() > 0 && (mDbObject == null || !editorText.equals(mDbObject.text));
    }

    @Override
    public Snackbar createSnackbar(int resId, Object... objects)
    {
        return Snackbar.make(findViewById(android.R.id.content), getString(resId, objects), Snackbar.LENGTH_LONG);
    }

    public void removeText()
    {
        if (mDbObject != null) {
            getDatabase().remove(mDbObject);
            getDatabase().broadcast();
            mDbObject = null;
        }
    }

    public void saveText()
    {
        if (mDbObject == null)
            mDbObject = new TextStreamObject(AppUtils.getUniqueNumber());

        if (mDbObject.date == 0)
            mDbObject.date = System.currentTimeMillis();

        mDbObject.text = mEditTextEditor.getText().toString();
        getDatabase().publish(mDbObject);
        getDatabase().broadcast();
    }
}
