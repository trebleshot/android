package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.util.TitleSupport;

/**
 * Created by: veli
 * Date: 4/9/17 12:06 PM
 */

public class TextShareFragment extends Fragment implements TitleSupport
{
	private EditText mEditTextEditor;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.layout_text_editor_activity, container, false);

		mEditTextEditor = view.findViewById(R.id.layout_text_editor_activity_text_text_box);

		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_share_text, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.file_actions_share || item.getItemId() == R.id.file_actions_share_trebleshot) {
			Intent shareIntent = new Intent(item.getItemId() == R.id.file_actions_share ? Intent.ACTION_SEND : ShareActivity.ACTION_SEND);

			shareIntent.putExtra(Intent.EXTRA_TEXT, mEditTextEditor.getText().toString());
			shareIntent.setType("text/*");
			startActivity((item.getItemId() == R.id.file_actions_share) ? Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)) : shareIntent);

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_shareText);
	}
}
