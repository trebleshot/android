package com.genonbeta.TrebleShot.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;

/**
 * Created by: veli
 * Date: 1/19/17 5:05 PM
 */

public class TextEditorActivity extends Activity
{
	public static final String ACTION_EDIT_TEXT = "genonbeta.intent.action.EDIT_TEXT";
	public static final String EXTRA_SUPPORT_APPLY = "extraSupportApply";
	public static final String EXTRA_TEXT_INDEX = "extraText";

	private EditText mEditTextEditor;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (getIntent() == null || !ACTION_EDIT_TEXT.equals(getIntent().getAction()) || !getIntent().hasExtra(EXTRA_TEXT_INDEX))
			finish();
		else {
			setContentView(R.layout.layout_text_editor_activity);

			if (getSupportActionBar() != null)
				getSupportActionBar().setDisplayHomeAsUpEnabled(true);

			mEditTextEditor = findViewById(R.id.layout_text_editor_activity_text_text_box);
			mEditTextEditor.getText().append(getIntent().getStringExtra(EXTRA_TEXT_INDEX));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.actions_text_editor, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu)
	{
		if (getIntent() == null
				|| !getIntent().hasExtra(EXTRA_SUPPORT_APPLY)
				|| !getIntent().getBooleanExtra(EXTRA_SUPPORT_APPLY, false))
			menu.findItem(R.id.menu_action_done).setVisible(false);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.menu_action_done) {
			Intent intent = new Intent();
			intent.putExtra(EXTRA_TEXT_INDEX, mEditTextEditor.getText().toString());

			setResult(RESULT_OK, intent);
			finish();

			return true;
		} else if (id == R.id.menu_action_copy) {
			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE))
					.setPrimaryClip(ClipData.newPlainText("copiedText", mEditTextEditor.getText().toString()));

			Snackbar.make(findViewById(android.R.id.content), R.string.mesg_textCopiedToClipboard, Snackbar.LENGTH_LONG).show();

			return true;
		} else if (id == android.R.id.home) {
			setResult(RESULT_CANCELED);
			finish();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
