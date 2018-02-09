package com.genonbeta.TrebleShot.activity;

import android.content.Intent;

import com.genonbeta.TrebleShot.app.Activity;

/**
 * Created by: veli
 * Date: 5/30/17 6:57 PM
 */

public class ChangeStoragePathActivity extends Activity
{
	public final static int REQUEST_CHOOSE_FOLDER = 1;

	@Override
	protected void onStart()
	{
		super.onStart();
		startActivityForResult(new Intent(this, FilePickerActivity.class)
				.setAction(FilePickerActivity.ACTION_CHOOSE_DIRECTORY), REQUEST_CHOOSE_FOLDER);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (data != null) {
			if (resultCode == Activity.RESULT_OK) {
				switch (requestCode) {
					case REQUEST_CHOOSE_FOLDER:
						if (data.hasExtra(FilePickerActivity.EXTRA_CHOSEN_PATH)) {
							getDefaultPreferences()
									.edit()
									.putString("storage_path", data.getStringExtra(FilePickerActivity.EXTRA_CHOSEN_PATH))
									.apply();
						}
						break;
				}
			}
		}

		finish();
	}
}
