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
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.TrebleShot.view.HolderConsumer;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.List;

import static com.genonbeta.TrebleShot.fragment.TransferAssigneeListFragment.showPopupMenu;

/**
 * created by: veli
 * date: 06.04.2018 12:46
 */
public class TransferAssigneeListAdapter extends EditableListAdapter<ShowingAssignee, RecyclerViewAdapter.ViewHolder>
{
    private TransferGroup mGroup;
    private TextDrawable.IShapeBuilder mIconBuilder;

    public TransferAssigneeListAdapter(EditableListFragmentImpl<ShowingAssignee> fragment,
                                       HolderConsumer<ViewHolder> consumer, TransferGroup group)
    {
        super(fragment, consumer);
        mIconBuilder = AppUtils.getDefaultIconBuilder(fragment.getContext());
        mGroup = group;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        ViewHolder holder = new ViewHolder(getInflater().inflate(isHorizontalOrientation() || isGridLayoutRequested()
                ? R.layout.list_assignee_grid : R.layout.list_assignee, parent, false));

        getConsumer().registerLayoutViewClicks(holder);
        holder.itemView.findViewById(R.id.menu)
                .setOnClickListener(v -> showPopupMenu(getFragment(), this, mGroup, holder, v));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position)
    {
        ShowingAssignee assignee = getList().get(position);

        ImageView image = holder.itemView.findViewById(R.id.image);
        TextView text1 = holder.itemView.findViewById(R.id.text1);
        TextView text2 = holder.itemView.findViewById(R.id.text2);

        text1.setText(assignee.device.nickname);
        text2.setText(TextUtils.getAdapterName(getContext(), assignee.connection));
        NetworkDeviceLoader.showPictureIntoView(assignee.device, image, mIconBuilder);
    }

    @Override
    public List<ShowingAssignee> onLoad()
    {
        return TransferUtils.loadAssigneeList(getContext(), mGroup.id, null);
    }
}
