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
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * created by: veli
 * date: 7/20/18 8:56 PM
 */
public class ThirdPartyLibraryListFragment extends DynamicRecyclerViewFragment<ThirdPartyLibraryListFragment.ModuleItem,
        RecyclerViewAdapter.ViewHolder, ThirdPartyLibraryListFragment.LicencesAdapter>
{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState)
    {
        return generateDefaultView(inflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        setListAdapter(new LicencesAdapter(getContext()));
    }

    public static class LicencesAdapter extends RecyclerViewAdapter<ModuleItem, RecyclerViewAdapter.ViewHolder>
    {
        private final List<ModuleItem> mList = new ArrayList<>();

        public LicencesAdapter(Context context)
        {
            super(context);
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            ViewHolder holder = new ViewHolder(getInflater().inflate(R.layout.list_third_party_library, parent,
                    false));

            holder.itemView.findViewById(R.id.menu).setOnClickListener(v -> {
                final ModuleItem moduleItem = getList().get(holder.getAdapterPosition());

                PopupMenu popupMenu = new PopupMenu(getContext(), v);
                popupMenu.getMenuInflater().inflate(R.menu.popup_third_party_library_item, popupMenu.getMenu());

                popupMenu.getMenu()
                        .findItem(R.id.popup_visitWebPage)
                        .setEnabled(moduleItem.moduleUrl != null);

                popupMenu.getMenu()
                        .findItem(R.id.popup_goToLicenceURL)
                        .setEnabled(moduleItem.licenceUrl != null);

                popupMenu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();

                    if (id == R.id.popup_goToLicenceURL)
                        getContext().startActivity(new Intent(Intent.ACTION_VIEW)
                                .setData(Uri.parse(moduleItem.licenceUrl)));
                    else if (id == R.id.popup_visitWebPage)
                        getContext().startActivity(new Intent(Intent.ACTION_VIEW)
                                .setData(Uri.parse(moduleItem.moduleUrl)));
                    else
                        return false;

                    return true;
                });

                popupMenu.show();
            });

            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            ModuleItem item = getList().get(position);
            TextView text1 = holder.itemView.findViewById(R.id.text);
            TextView text2 = holder.itemView.findViewById(R.id.text2);

            text1.setText(item.moduleName);

            StringBuilder stringBuilder = new StringBuilder();

            if (item.moduleVersion != null)
                stringBuilder.append(item.moduleVersion);

            if (item.licence != null) {
                if (stringBuilder.length() > 0)
                    stringBuilder.append(", ");

                stringBuilder.append(item.licence);
            }

            text2.setText(stringBuilder.toString());
        }

        @Override
        public int getItemCount()
        {
            return mList.size();
        }

        @Override
        public List<ModuleItem> onLoad()
        {
            InputStream inputStream = getContext().getResources().openRawResource(R.raw.libraries_index);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            try {
                int read;
                while ((read = inputStream.read()) != -1)
                    outputStream.write(read);

                JSONObject jsonObject = new JSONObject(outputStream.toString());
                JSONArray dependenciesArray = jsonObject.getJSONArray("dependencies");
                List<ModuleItem> returnedList = new ArrayList<>();

                for (int i = 0; i < dependenciesArray.length(); i++)
                    returnedList.add(new ModuleItem(dependenciesArray.getJSONObject(i)));

                return returnedList;
            } catch (Exception ignored) {
            }

            return new ArrayList<>();
        }

        @Override
        public void onUpdate(List<ModuleItem> passedItem)
        {
            synchronized (getList()) {
                getList().clear();
                getList().addAll(passedItem);
            }
        }

        @Override
        public List<ModuleItem> getList()
        {
            return mList;
        }
    }

    public static class ModuleItem
    {
        public String moduleName;
        public String moduleUrl;
        public String moduleVersion;
        public String licence;
        public String licenceUrl;

        public ModuleItem(JSONObject licenceObject) throws JSONException
        {
            if (licenceObject.has("moduleName"))
                moduleName = licenceObject.getString("moduleName");

            if (licenceObject.has("moduleUrl"))
                moduleUrl = licenceObject.getString("moduleUrl");

            if (licenceObject.has("moduleVersion"))
                moduleVersion = licenceObject.getString("moduleVersion");

            if (licenceObject.has("moduleLicense"))
                licence = licenceObject.getString("moduleLicense");

            if (licenceObject.has("moduleLicenseUrl"))
                licenceUrl = licenceObject.getString("moduleLicenseUrl");
        }
    }
}
