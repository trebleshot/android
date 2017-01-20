package com.genonbeta.TrebleShot.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.GActivity;

/**
 * Created by: veli
 * Date: 1/19/17 5:05 PM
 */

public class TextEditorActivity extends GActivity
{
	public static final String ACTION_EDIT_TEXT = "genonbeta.intent.action.EDIT_TEXT";
	public static final String EXTRA_TEXT_INDEX = "extraText";

	private EditText mEditTextEditor;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (getIntent() == null || !ACTION_EDIT_TEXT.equals(getIntent().getAction()) || !getIntent().hasExtra(EXTRA_TEXT_INDEX))
			finish();
		else
		{
			setContentView(R.layout.layout_text_editor_activity);
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

			setContentView(R.layout.layout_text_editor_activity);

			mEditTextEditor = (EditText) findViewById(R.id.layout_text_editor_activity_text_text_box);
			mEditTextEditor.getText().append(getIntent().getStringExtra(EXTRA_TEXT_INDEX));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.action_text_editor, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.menu_action_done)
		{
			Intent intent = new Intent();
			intent.putExtra(EXTRA_TEXT_INDEX, mEditTextEditor.getText().toString());

			setResult(RESULT_OK, intent);
			finish();

			return true;
		}
		else if (id == android.R.id.home)
		{
			setResult(RESULT_CANCELED);
			finish();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}
}
