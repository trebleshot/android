package com.genonbeta.TrebleShot.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.PreferenceUtils;

public class PreferencesActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
            onBackPressed();
        else if (id == R.id.actions_preference_main_reset_to_defaults) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.ques_resetToDefault)
                    .setMessage(R.string.text_resetPreferencesToDefaultSummary)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_proceed, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {

                            // TODO: 10/7/18 This will cause two seperate sync operations to start
                            AppUtils.getDefaultPreferences(getApplicationContext()).edit()
                                    .clear()
                                    .apply();

                            AppUtils.getDefaultLocalPreferences(getApplicationContext()).edit()
                                    .clear()
                                    .apply();

                            finish();
                        }
                    })
                    .show();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.actions_preferences_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        PreferenceUtils.syncPreferences(AppUtils.getDefaultLocalPreferences(this),
                AppUtils.getDefaultPreferences(this).getWeakManager());
    }
}
