package com.genonbeta.TrebleShot.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.snackbar.Snackbar;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

/**
 * Created by: veli
 * Date: 1/19/17 5:05 PM
 */

public class TextEditorActivity extends Activity implements SnackbarSupport
{
    public static final String TAG = TextEditorActivity.class.getSimpleName();
    public static final String ACTION_EDIT_TEXT = "genonbeta.intent.action.EDIT_TEXT";
    public static final String EXTRA_SUPPORT_APPLY = "extraSupportApply";
    public static final String EXTRA_TEXT_INDEX = "extraText";
    public static final String EXTRA_CLIPBOARD_ID = "clipboardId";

    public static final int REQUEST_CODE_CHOOSE_DEVICE = 0;

    private EditText mEditTextEditor;

    private TextStreamObject mTextStreamObject;
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
                mTextStreamObject = new TextStreamObject(getIntent().getLongExtra(EXTRA_CLIPBOARD_ID, -1));

                try {
                    getDatabase().reconstruct(mTextStreamObject);

                    mEditTextEditor
                            .getText()
                            .append(mTextStreamObject.text);
                } catch (Exception e) {
                    e.printStackTrace();
                    mTextStreamObject = null;
                }
            } else if (getIntent().hasExtra(EXTRA_TEXT_INDEX))
                mEditTextEditor
                        .getText()
                        .append(getIntent().getStringExtra(EXTRA_TEXT_INDEX));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == android.app.Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_CHOOSE_DEVICE
                    && data != null
                    && data.hasExtra(ConnectionManagerActivity.EXTRA_DEVICE_ID)
                    && data.hasExtra(ConnectionManagerActivity.EXTRA_CONNECTION_ADAPTER)) {
                String deviceId = data.getStringExtra(ConnectionManagerActivity.EXTRA_DEVICE_ID);
                String connectionAdapter = data.getStringExtra(ConnectionManagerActivity.EXTRA_CONNECTION_ADAPTER);

                try {
                    NetworkDevice networkDevice = new NetworkDevice(deviceId);
                    NetworkDevice.Connection connection = new NetworkDevice.Connection(deviceId, connectionAdapter);

                    getDatabase().reconstruct(networkDevice);
                    getDatabase().reconstruct(connection);

                    doCommunicate(networkDevice, connection);
                } catch (Exception e) {
                    Toast.makeText(TextEditorActivity.this, R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
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
        boolean hasObject = mTextStreamObject != null;
        boolean hasEntry = mEditTextEditor.getText().length() > 0;
        boolean hasSavedText = hasObject && mTextStreamObject.text != null && mTextStreamObject.text.length() > 0;

        if (!hasEntry || (hasSavedText && mTextStreamObject.text.equals(mEditTextEditor.getText().toString()))
                || (System.currentTimeMillis() - mBackPressTime) < 3000)
            super.onBackPressed();
        else
            createSnackbar(hasObject ? R.string.mesg_clipboardUpdateNotice : R.string.mesg_textSaveNotice)
                    .setAction(hasObject ? R.string.butn_update : R.string.butn_save, new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            saveText();
                            finish();
                        }
                    })
                    .show();

        mBackPressTime = System.currentTimeMillis();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        boolean applySupported = getIntent() != null
                && getIntent().hasExtra(EXTRA_SUPPORT_APPLY)
                && getIntent().getBooleanExtra(EXTRA_SUPPORT_APPLY, false);

        menu.findItem(R.id.menu_action_done)
                .setVisible(applySupported);

        menu.findItem(R.id.menu_action_share)
                .setVisible(!applySupported);

        menu.findItem(R.id.menu_action_share_trebleshot)
                .setVisible(!applySupported);

        menu.findItem(R.id.menu_action_remove).setVisible(mTextStreamObject != null);

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
            startActivityForResult(
                    new Intent(TextEditorActivity.this, ConnectionManagerActivity.class)
                            .putExtra(ConnectionManagerActivity.EXTRA_REQUEST_TYPE, ConnectionManagerActivity.RequestType.RETURN_RESULT.toString())
                            .putExtra(ConnectionManagerActivity.EXTRA_ACTIVITY_SUBTITLE, getString(R.string.text_sendText)),
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

                    ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    );

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
            if (mTextStreamObject != null)
                getDatabase().remove(mTextStreamObject);

            mTextStreamObject = null;
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public Snackbar createSnackbar(int resId, Object... objects)
    {
        return Snackbar.make(findViewById(android.R.id.content), getString(resId, objects), Snackbar.LENGTH_LONG);
    }

    protected void doCommunicate(final NetworkDevice device, final NetworkDevice.Connection connection)
    {
        createSnackbar(R.string.mesg_communicating).show();

        new WorkerService.RunningTask()
        {
            @Override
            public void onRun()
            {
                final DialogInterface.OnClickListener retryButtonListener = new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        doCommunicate(device, connection);
                    }
                };

                CommunicationBridge.connect(getDatabase(), true, new CommunicationBridge.Client.ConnectionHandler()
                {
                    @Override
                    public void onConnect(CommunicationBridge.Client client)
                    {
                        client.setDevice(device);

                        try {
                            final JSONObject jsonRequest = new JSONObject();
                            final CoolSocket.ActiveConnection activeConnection = client.communicate(device, connection);

                            jsonRequest.put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD);
                            jsonRequest.put(Keyword.TRANSFER_CLIPBOARD_TEXT, mEditTextEditor.getText().toString());

                            activeConnection.reply(jsonRequest.toString());

                            CoolSocket.ActiveConnection.Response response = activeConnection.receive();
                            activeConnection.getSocket().close();

                            JSONObject clientResponse = new JSONObject(response.response);

                            if (clientResponse.has(Keyword.RESULT) && clientResponse.getBoolean(Keyword.RESULT))
                                createSnackbar(R.string.mesg_sent).show();
                            else
                                UIConnectionUtils.showConnectionRejectionInformation(
                                        TextEditorActivity.this,
                                        device, clientResponse, retryButtonListener);
                        } catch (Exception e) {
                            e.printStackTrace();
                            UIConnectionUtils.showUnknownError(TextEditorActivity.this, retryButtonListener);
                        }
                    }
                });
            }
        }.setTitle(getString(R.string.mesg_communicating))
                .setIconRes(R.drawable.ic_compare_arrows_white_24dp_static)
                .run(this);
    }

    public void saveText()
    {
        if (mTextStreamObject == null)
            mTextStreamObject = new TextStreamObject(AppUtils.getUniqueNumber());

        if (mTextStreamObject.date == 0)
            mTextStreamObject.date = System.currentTimeMillis();

        mTextStreamObject.text = mEditTextEditor.getText().toString();
        getDatabase().publish(mTextStreamObject);
    }
}
