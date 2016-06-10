package com.genonbeta.TrebleShot.fragment;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ApplicationListAdapter;

public class ApplicationListFragment extends AbstractMediaListFragment<ApplicationListAdapter> {
    private SharedPreferences mPreferences;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.setHasOptionsMenu(true);
        this.mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
    }

    @Override
    protected ApplicationListAdapter onAdapter() {
        return new ApplicationListAdapter(getActivity(), PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("show_system_apps", false));
    }

    @Override
    protected MediaChoiceListener onChoiceListener() {
        return new ChoiceListener();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.application_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.show_system_apps) {
            this.mPreferences.edit().putBoolean("show_system_apps", !this.mPreferences.getBoolean("show_system_apps", false)).commit();
            this.getAdapter().showSystemApps(this.mPreferences.getBoolean("show_system_apps", false));
            this.updateInBackground();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        MenuItem menuSystemApps = menu.findItem(R.id.show_system_apps);
        menuSystemApps.setChecked(this.mPreferences.getBoolean("show_system_apps", false));
    }

    private class ChoiceListener extends MediaChoiceListener {
        @Override
        public void onItemChecked(ActionMode mode, int pos, long id, boolean isChecked) {
            ApplicationListAdapter.AppInfo info = (ApplicationListAdapter.AppInfo) getAdapter().getItem(pos);

            if (isChecked)
                mCheckedList.add(Uri.parse("file://" + info.codePath));
            else
                mCheckedList.remove(Uri.parse("file://" + info.codePath));
        }
    }
}
