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
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * created by: veli
 * date: 4/7/19 10:35 PM
 */
public class ActiveConnectionListAdapter extends EditableListAdapter<ActiveConnectionListAdapter.EditableNetworkInterface, EditableListAdapter.EditableViewHolder>
{
    public ActiveConnectionListAdapter(Context context)
    {
        super(context);
    }

    @Override
    public List<EditableNetworkInterface> onLoad()
    {
        List<EditableNetworkInterface> resultList = new ArrayList<>();
        List<NetworkInterface> interfaceList = NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES);

        for (NetworkInterface addressedInterface : interfaceList) {
            EditableNetworkInterface editableInterface = new EditableNetworkInterface(addressedInterface,
                    TextUtils.getAdapterName(getContext(), addressedInterface));

            if (filterItem(editableInterface))
                resultList.add(editableInterface);
        }

        return resultList;
    }

    @NonNull
    @Override
    public EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return new EditableViewHolder(getInflater().inflate(R.layout.list_active_connection,
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EditableViewHolder holder, int position)
    {
        try {
            EditableNetworkInterface object = getItem(position);
            View parentView = holder.getView();

            TextView text1 = parentView.findViewById(R.id.text);
            TextView text2 = parentView.findViewById(R.id.text2);

            text1.setText(object.getSelectableTitle());
            text2.setText(TextUtils.makeWebShareLink(getContext(), NetworkUtils.getFirstInet4Address(object)
                    .getHostAddress()));
        } catch (NotReadyException e) {
            e.printStackTrace();
        }
    }

    public static class EditableNetworkInterface implements Editable
    {
        private NetworkInterface mInterface;
        private String mName;
        private boolean mSelected = false;

        public EditableNetworkInterface(NetworkInterface addressedInterface, String name)
        {
            mInterface = addressedInterface;
            mName = name;
        }

        @Override
        public boolean applyFilter(String[] filteringKeywords)
        {
            for (String word : filteringKeywords) {
                String wordLC = word.toLowerCase();

                if (mInterface.getDisplayName().toLowerCase().contains(wordLC)
                        || mName.toLowerCase().contains(wordLC))
                    return true;
            }

            return false;
        }

        @Override
        public long getId()
        {
            return mInterface.hashCode();
        }

        @Override
        public void setId(long id)
        {
            // not required
        }

        @Override
        public boolean comparisonSupported()
        {
            return false;
        }

        @Override
        public String getComparableName()
        {
            return mName;
        }

        @Override
        public long getComparableDate()
        {
            return 0;
        }

        @Override
        public long getComparableSize()
        {
            return 0;
        }

        public NetworkInterface getInterface()
        {
            return mInterface;
        }

        public String getName()
        {
            return mName;
        }

        @Override
        public String getSelectableTitle()
        {
            return mName;
        }

        @Override
        public boolean isSelectableSelected()
        {
            return mSelected;
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            mSelected = selected;
            return true;
        }
    }
}
