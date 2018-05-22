package com.genonbeta.TrebleShot.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.util.AppUtils;

/**
 * Created by: veli
 * Date: 1/19/17 5:05 PM
 */

public class TextEditorActivity extends Activity
{
	public static final String ACTION_EDIT_TEXT = "genonbeta.intent.action.EDIT_TEXT";
	public static final String EXTRA_SUPPORT_APPLY = "extraSupportApply";
	public static final String EXTRA_TEXT_INDEX = "extraText";
	public static final String EXTRA_CLIPBOARD_ID = "clipboardId";

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
				mTextStreamObject = new TextStreamObject(getIntent().getIntExtra(EXTRA_CLIPBOARD_ID, -1));

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
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.actions_text_editor, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public void onBackPressed()
	{
		if ((System.currentTimeMillis() - mBackPressTime) < 3000
				|| (mTextStreamObject == null && mEditTextEditor.getText().length() == 0))
			super.onBackPressed();
		else
			Snackbar.make(findViewById(android.R.id.content), mTextStreamObject != null ? R.string.mesg_clipboardUpdateNotice : R.string.mesg_textSaveNotice, Snackbar.LENGTH_LONG)
					.setAction(mTextStreamObject != null ? R.string.butn_update : R.string.butn_save, new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							if (mTextStreamObject == null)
								mTextStreamObject = new TextStreamObject(AppUtils.getUniqueNumber());

							mTextStreamObject.date = System.currentTimeMillis();
							mTextStreamObject.text = mEditTextEditor.getText().toString();

							getDatabase().publish(mTextStreamObject);

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

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.menu_action_done) {
			Intent intent = new Intent()
					.putExtra(EXTRA_TEXT_INDEX, mEditTextEditor.getText().toString());

			setResult(RESULT_OK, intent);
			finish();

			return true;
		} else if (id == R.id.menu_action_copy) {
			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
					.setPrimaryClip(ClipData.newPlainText("copiedText", mEditTextEditor.getText().toString()));

			Snackbar.make(findViewById(android.R.id.content), R.string.mesg_textCopiedToClipboard, Snackbar.LENGTH_LONG).show();

			return true;
		} else if (id == R.id.menu_action_share || id == R.id.menu_action_share_trebleshot) {
			Intent shareIntent = new Intent(item.getItemId() == R.id.menu_action_share
					? Intent.ACTION_SEND : ShareActivity.ACTION_SEND)
					.putExtra(Intent.EXTRA_TEXT, mEditTextEditor.getText().toString())
					.setType("text/*");

			startActivity((item.getItemId() == R.id.menu_action_share) ? Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)) : shareIntent);
			return true;
		} else if (id == android.R.id.home) {
			onBackPressed();
			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
