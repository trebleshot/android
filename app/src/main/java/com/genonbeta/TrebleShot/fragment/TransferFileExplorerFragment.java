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
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.adapter.TransferPathResolverRecyclerAdapter;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.io.File;

/**
 * created by: veli
 * date: 3/11/19 7:37 PM
 */
public class TransferFileExplorerFragment extends TransferListFragment
{
    private RecyclerView mPathView;
    private TransferPathResolverRecyclerAdapter mPathAdapter;
    private ExtendedFloatingActionButton mToggleButton;

    @Override
    public void onAttach(@NonNull Context context)
    {
        super.onAttach(context);
        setHasBottomSpace(true);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setLayoutResId(R.layout.layout_transfer_explorer);
        setDividerView(R.id.layout_transfer_explorer_separator);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        mToggleButton = view.findViewById(R.id.layout_transfer_explorer_efab);
        mPathView = view.findViewById(R.id.layout_transfer_explorer_recycler);
        mPathAdapter = new TransferPathResolverRecyclerAdapter(getContext());
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL,
                false);
        layoutManager.setStackFromEnd(true);

        mPathView.setHasFixedSize(true);
        mPathView.setLayoutManager(layoutManager);
        mPathView.setAdapter(mPathAdapter);

        mPathAdapter.setOnClickListener(holder -> goPath(holder.index.object));

        if (getActivity() instanceof ViewTransferActivity)
            ((ViewTransferActivity) getActivity()).showMenus();
    }

    @Override
    protected void onListRefreshed()
    {
        super.onListRefreshed();

        String path = getAdapter().getPath();

        mPathAdapter.goTo(getAdapter().getAssignee(), path == null ? null : path.split(File.separator));
        mPathAdapter.notifyDataSetChanged();

        if (mPathAdapter.getItemCount() > 0)
            mPathView.smoothScrollToPosition(mPathAdapter.getItemCount() - 1);
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_files);
    }

    @Nullable
    public ExtendedFloatingActionButton getToggleButton()
    {
        return mToggleButton;
    }
}