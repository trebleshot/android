/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ApplicationListAdapter;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;

import java.util.Map;

public class ApplicationListFragment extends GroupEditableListFragment<ApplicationListAdapter.PackageHolder,
        GroupEditableListAdapter.GroupViewHolder, ApplicationListAdapter>
{
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setFilteringSupported(true);
        setHasOptionsMenu(true);
        setDefaultOrderingCriteria(ApplicationListAdapter.MODE_SORT_ORDER_DESCENDING);
        setDefaultSortingCriteria(ApplicationListAdapter.MODE_SORT_BY_DATE);
        setDefaultGroupingCriteria(ApplicationListAdapter.MODE_GROUP_BY_DATE);
        setUseDefaultPaddingDecoration(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setEmptyListImage(R.drawable.ic_android_head_white_24dp);
        setEmptyListText(getString(R.string.text_listEmptyApp));
    }

    @Override
    public void onGroupingOptions(Map<String, Integer> options)
    {
        super.onGroupingOptions(options);
        options.put(getString(R.string.text_groupByNothing), ApplicationListAdapter.MODE_GROUP_BY_NOTHING);
        options.put(getString(R.string.text_groupByDate), ApplicationListAdapter.MODE_GROUP_BY_DATE);
    }

    @Override
    public boolean onDefaultClickAction(GroupEditableListAdapter.GroupViewHolder holder)
    {
        return performLayoutClickOpen(holder);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actions_application, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.show_system_apps) {
            boolean isShowingSystem = !AppUtils.getDefaultPreferences(getContext()).getBoolean("show_system_apps",
                    false);

            AppUtils.getDefaultPreferences(getContext()).edit()
                    .putBoolean("show_system_apps", isShowingSystem)
                    .apply();

            refreshList();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu)
    {
        super.onPrepareOptionsMenu(menu);

        MenuItem menuSystemApps = menu.findItem(R.id.show_system_apps);
        menuSystemApps.setChecked(AppUtils.getDefaultPreferences(getContext()).getBoolean("show_system_apps",
                false));
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_application);
    }

    @Override
    public boolean performLayoutClickOpen(GroupEditableListAdapter.GroupViewHolder holder)
    {
        try {
            getListView().smoothScrollBy(0, 30);

            final ApplicationListAdapter.PackageHolder appInfo = getAdapter().getItem(holder);
            final Intent launchIntent = getActivity().getPackageManager().getLaunchIntentForPackage(appInfo.packageName);

            if (launchIntent != null) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

                dialogBuilder.setMessage(R.string.ques_launchApplication);
                dialogBuilder.setNegativeButton(R.string.butn_cancel, null);
                dialogBuilder.setPositiveButton(R.string.butn_appLaunch, (dialog, which) -> startActivity(launchIntent));

                dialogBuilder.show();
            } else
                Toast.makeText(getActivity(), R.string.mesg_launchApplicationError, Toast.LENGTH_SHORT).show();

            return true;
        } catch (Exception ignore) {
        }

        return false;
    }
}
