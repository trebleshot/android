package com.genonbeta.TrebleShot.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.GActivity;
import com.genonbeta.TrebleShot.fragment.dialog.AboutDialog;
import com.genonbeta.TrebleShot.helper.FileUtils;

import java.io.File;

public class TrebleShotActivity extends GActivity
{
	public static final String OPEN_RECEIVED_FILES_ACTION = "genonbeta.intent.action.OPEN_RECEIVED_FILES";
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.trebleshot_options, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.trebleshot_options_about:
				new AboutDialog().show(getSupportFragmentManager(), "aboutDialog");
				break;
			case (R.id.trebleshot_options_send_application):
				sendTheApplication();
				return true;
			case R.id.trebleshot_options_preferences:
				startActivity(new Intent(this, PreferencesActivity.class));
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	private void sendTheApplication()
	{
		File apkFile = new File(getPackageCodePath());

		Intent sendIntent = new Intent(Intent.ACTION_SEND);

		sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(apkFile));
		sendIntent.setType(FileUtils.getFileContentType(apkFile.getAbsolutePath()));

		startActivity(Intent.createChooser(sendIntent, getString(R.string.file_share_app_chooser_msg)));
	}
}
