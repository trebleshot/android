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
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.ShowingAssignee;

import java.io.File;

/**
 * created by: veli
 * date: 3/11/19 7:39 PM
 */
public class TransferPathResolverRecyclerAdapter extends PathResolverRecyclerAdapter<String>
{
    private ShowingAssignee mAssignee;
    private String mHomeName;

    public TransferPathResolverRecyclerAdapter(Context context)
    {
        super(context);
        mHomeName = context.getString(R.string.text_home);
    }

    @Override
    public Index<String> onFirstItem()
    {
        if (mAssignee != null)
            return new Index<>(mAssignee.device.username, R.drawable.ic_device_hub_white_24dp, null);

        return new Index<>(mHomeName, R.drawable.ic_home_white_24dp, null);
    }

    public void goTo(ShowingAssignee assignee, String[] paths)
    {
        mAssignee = assignee;

        StringBuilder mergedPath = new StringBuilder();
        initAdapter();

        synchronized (getList()) {
            if (paths != null)
                for (String path : paths) {
                    if (path.length() == 0)
                        continue;

                    if (mergedPath.length() > 0)
                        mergedPath.append(File.separator);

                    mergedPath.append(path);

                    getList().add(new Index<>(path, mergedPath.toString()));
                }
        }
    }
}
