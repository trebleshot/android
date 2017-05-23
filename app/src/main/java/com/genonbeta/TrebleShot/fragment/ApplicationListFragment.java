package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ApplicationListAdapter;
import com.genonbeta.TrebleShot.support.FragmentTitle;

public class ApplicationListFragment extends AbstractEditableListFragment<ApplicationListAdapter.AppInfo, ApplicationListAdapter> implements FragmentTitle
{
	private SharedPreferences mPreferences;

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
	}

	@Override
	public ApplicationListAdapter onAdapter()
	{
		return new ApplicationListAdapter(getActivity(), PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("show_system_apps", false));
	}

	@Override
	protected ActionModeListener onActionModeListener()
	{
		return new ChoiceListener();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.application_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.show_system_apps)
		{
			mPreferences.edit().putBoolean("show_system_apps", !mPreferences.getBoolean("show_system_apps", false)).apply();
			getAdapter().showSystemApps(mPreferences.getBoolean("show_system_apps", false));
			refreshList();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu)
	{
		super.onPrepareOptionsMenu(menu);

		MenuItem menuSystemApps = menu.findItem(R.id.show_system_apps);
		menuSystemApps.setChecked(mPreferences.getBoolean("show_system_apps", false));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		final ApplicationListAdapter.AppInfo appInfo = (ApplicationListAdapter.AppInfo) getAdapter().getItem(position);
		final Intent launchIntent = getActivity().getPackageManager().getLaunchIntentForPackage(appInfo.packageName);

		if (launchIntent != null)
		{
			AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

			dialogBuilder.setMessage(R.string.launch_application_question);
			dialogBuilder.setNegativeButton(R.string.cancel, null);
			dialogBuilder.setPositiveButton(R.string.launch, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					startActivity(launchIntent);
				}
			});

			dialogBuilder.show();
		}
		else
			Toast.makeText(getActivity(), R.string.launch_application_error, Toast.LENGTH_SHORT).show();
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.application);
	}

	private class ChoiceListener extends ActionModeListener
	{
		public ApplicationListAdapter.AppInfo getItem(int position)
		{
			return (ApplicationListAdapter.AppInfo) getAdapter().getItem(position);
		}

		public Uri onItemChecked(ActionMode mode, int pos, long id, boolean isChecked)
		{
			return Uri.parse("file://" + getItem(pos).codePath);
		}

		@Override
		public String onProvideName(ActionMode mode, int position)
		{
			return getItem(position).label + "_" + getItem(position).version + ".apk";
		}
	}
}
