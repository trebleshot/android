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

package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;

import java.io.File;

public class ApplicationListAdapter extends GroupEditableListAdapter<ApplicationListAdapter.PackageHolder,
        GroupEditableListAdapter.GroupViewHolder>
{
    private SharedPreferences mPreferences;
    private PackageManager mManager;

    public ApplicationListAdapter(Context context, SharedPreferences preferences)
    {
        super(context, MODE_GROUP_BY_DATE);
        mPreferences = preferences;
        mManager = context.getPackageManager();
    }

    @Override
    protected void onLoad(GroupLister<PackageHolder> lister)
    {
        boolean showSystemApps = mPreferences.getBoolean("show_system_apps", false);

        for (PackageInfo packageInfo : getContext().getPackageManager().getInstalledPackages(
                PackageManager.GET_META_DATA)) {
            ApplicationInfo appInfo = packageInfo.applicationInfo;

            if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 1 || showSystemApps) {
                PackageHolder packageHolder = new PackageHolder(String.valueOf(appInfo.loadLabel(mManager)), appInfo,
                        packageInfo.versionName, packageInfo.packageName, new File(appInfo.sourceDir));

                lister.offerObliged(this, packageHolder);
            }
        }
    }

    @Override
    protected PackageHolder onGenerateRepresentative(String representativeText)
    {
        return new PackageHolder(VIEW_TYPE_REPRESENTATIVE, representativeText);
    }

    @NonNull
    @Override
    public GroupEditableListAdapter.GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return viewType == VIEW_TYPE_DEFAULT ? new GroupEditableListAdapter.GroupViewHolder(getInflater().inflate(
                isGridLayoutRequested() ? R.layout.list_application_grid : R.layout.list_application, parent,
                false)) : createDefaultViews(parent, viewType, false);
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupEditableListAdapter.GroupViewHolder holder, final int position)
    {
        try {
            final View parentView = holder.getView();
            final PackageHolder object = getItem(position);

            if (!holder.tryBinding(object)) {
                ImageView image = parentView.findViewById(R.id.image);
                TextView text1 = parentView.findViewById(R.id.text);
                TextView text2 = parentView.findViewById(R.id.text2);

                text1.setText(object.friendlyName);
                text2.setText(object.version);

                parentView.setSelected(object.isSelectableSelected());

                GlideApp.with(getContext())
                        .load(object.appInfo)
                        .override(160)
                        .centerCrop()
                        .into(image);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isGridSupported()
    {
        return true;
    }

    public static class PackageHolder extends GroupEditableListAdapter.GroupShareable
    {
        public static final String FORMAT = ".apk";
        public static final String MIME_TYPE = FileUtils.getFileContentType(FORMAT);

        public ApplicationInfo appInfo;
        public String version;
        public String packageName;

        public PackageHolder(int viewType, String representativeText)
        {
            super(viewType, representativeText);
        }

        public PackageHolder(String friendlyName, ApplicationInfo appInfo, String version, String packageName,
                             File executableFile)
        {
            super(appInfo.packageName.hashCode(),
                    friendlyName,
                    friendlyName + "_" + version + ".apk",
                    MIME_TYPE,
                    executableFile.lastModified(),
                    executableFile.length(),
                    Uri.fromFile(executableFile));

            this.appInfo = appInfo;
            this.version = version;
            this.packageName = packageName;
        }
    }
}
