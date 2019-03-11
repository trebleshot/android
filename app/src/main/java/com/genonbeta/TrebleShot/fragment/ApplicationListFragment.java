package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ApplicationListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;

public class ApplicationListFragment
        extends EditableListFragment<ApplicationListAdapter.PackageHolder, EditableListAdapter.EditableViewHolder, ApplicationListAdapter>
        implements TitleSupport
{
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        setFilteringSupported(true);
        setHasOptionsMenu(true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setEmptyImage(R.drawable.ic_android_head_white_24dp);
        setEmptyText(getString(R.string.text_listEmptyApp));
    }

    @Override
    public ApplicationListAdapter onAdapter()
    {
        final AppUtils.QuickActions<EditableListAdapter.EditableViewHolder> quickActions = new AppUtils.QuickActions<EditableListAdapter.EditableViewHolder>()
        {
            @Override
            public void onQuickActions(final EditableListAdapter.EditableViewHolder clazz)
            {
                registerLayoutViewClicks(clazz);

                clazz.getView().findViewById(R.id.visitView).setOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                performLayoutClickOpen(clazz);
                            }
                        });

                clazz.getView().findViewById(R.id.selector).setOnClickListener(
                        new View.OnClickListener()
                        {
                            @Override
                            public void onClick(View v)
                            {
                                if (getSelectionConnection() != null)
                                    getSelectionConnection().setSelected(clazz.getAdapterPosition());
                            }
                        });
            }
        };

        return new ApplicationListAdapter(getActivity(), AppUtils.getDefaultPreferences(getContext()))
        {
            @NonNull
            @Override
            public EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
            }
        };
    }

    @Override
    public boolean onDefaultClickAction(EditableListAdapter.EditableViewHolder holder)
    {
        return getSelectionConnection() != null
                ? getSelectionConnection().setSelected(holder)
                : performLayoutClickOpen(holder);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.actions_application, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (item.getItemId() == R.id.show_system_apps) {
            boolean isShowingSystem = !AppUtils.getDefaultPreferences(getContext()).getBoolean("show_system_apps", false);

            AppUtils.getDefaultPreferences(getContext()).edit()
                    .putBoolean("show_system_apps", isShowingSystem)
                    .apply();

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
        menuSystemApps.setChecked(AppUtils.getDefaultPreferences(getContext()).getBoolean("show_system_apps", false));
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_application);
    }

    @Override
    public boolean performLayoutClickOpen(EditableListAdapter.EditableViewHolder holder)
    {
        try {
            final ApplicationListAdapter.PackageHolder appInfo = getAdapter().getItem(holder);
            final Intent launchIntent = getActivity().getPackageManager().getLaunchIntentForPackage(appInfo.packageName);

            if (launchIntent != null) {
                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());

                dialogBuilder.setMessage(R.string.ques_launchApplication);
                dialogBuilder.setNegativeButton(R.string.butn_cancel, null);
                dialogBuilder.setPositiveButton(R.string.butn_appLaunch, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        startActivity(launchIntent);
                    }
                });

                dialogBuilder.show();
            } else
                Toast.makeText(getActivity(), R.string.mesg_launchApplicationError, Toast.LENGTH_SHORT).show();

            return true;
        } catch (Exception e) {
        }

        return false;
    }
}
