package com.genonbeta.TrebleShot.activity;

import android.content.ContentValues;
import android.content.Intent;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.android.database.CursorItem;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/30/17 6:57 PM
 */

public class StoragePathChangeActivity extends Activity
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
