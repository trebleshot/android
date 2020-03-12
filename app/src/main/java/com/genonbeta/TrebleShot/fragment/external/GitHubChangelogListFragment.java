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

package com.genonbeta.TrebleShot.fragment.external;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;
import com.genonbeta.android.updatewithgithub.RemoteServer;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: veli
 * date: 9/12/18 5:51 PM
 */
public class GitHubChangelogListFragment extends DynamicRecyclerViewFragment<GitHubChangelogListFragment.VersionObject,
        RecyclerViewAdapter.ViewHolder, GitHubChangelogListFragment.VersionListAdapter>
{
    @Override
    public void onResume()
    {
        super.onResume();
        AppUtils.publishLatestChangelogSeen(getActivity());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new VersionListAdapter(getContext()));
        setEmptyListImage(R.drawable.ic_github_circle_white_24dp);
        setEmptyListText(getString(R.string.mesg_noInternetConnection));
        useEmptyActionButton(getString(R.string.butn_refresh), v -> refreshList());
    }

    public static class VersionObject
    {
        public String tag;
        public String name;
        public String changes;

        public VersionObject(String tag, String name, String changes)
        {
            this.tag = tag;
            this.name = name;
            this.changes = changes;
        }
    }

    public static class VersionListAdapter extends RecyclerViewAdapter<VersionObject, RecyclerViewAdapter.ViewHolder>
    {
        private List<VersionObject> mList = new ArrayList<>();
        private String mCurrentVersion;

        public VersionListAdapter(Context context)
        {
            super(context);
            mCurrentVersion = AppUtils.getLocalDevice(context).versionName;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            return new ViewHolder(getInflater().inflate(R.layout.list_changelog, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            final VersionObject versionObject = getList().get(position);
            ImageView imageCheck = holder.itemView.findViewById(R.id.image_check);
            TextView text1 = holder.itemView.findViewById(R.id.text1);
            TextView text2 = holder.itemView.findViewById(R.id.text2);

            text1.setText(versionObject.name);
            text2.setText(versionObject.changes);

            imageCheck.setVisibility(mCurrentVersion.equals(versionObject.tag)
                    ? View.VISIBLE
                    : View.GONE);
        }

        @Override
        public List<VersionObject> onLoad()
        {
            List<VersionObject> versionObjects = new ArrayList<>();
            RemoteServer server = new RemoteServer(AppConfig.URI_REPO_APP_UPDATE);

            try {
                String result = server.connect(null, null);

                JSONArray releases = new JSONArray(result);

                if (releases.length() > 0) {
                    for (int iterator = 0; iterator < releases.length(); iterator++) {
                        JSONObject currentObject = releases.getJSONObject(iterator);

                        versionObjects.add(new VersionObject(
                                currentObject.getString("tag_name"),
                                currentObject.getString("name"),
                                currentObject.getString("body")
                        ));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return versionObjects;
        }

        @Override
        public void onUpdate(List<VersionObject> passedItem)
        {
            synchronized (getList()) {
                getList().clear();
                getList().addAll(passedItem);
            }
        }

        @Override
        public long getItemId(int i)
        {
            return 0;
        }

        @Override
        public int getItemCount()
        {
            return getList().size();
        }

        @Override
        public List<VersionObject> getList()
        {
            return mList;
        }
    }
}

