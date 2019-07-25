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
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.adapter.TransferPathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;

import java.io.File;

/**
 * created by: veli
 * date: 3/11/19 7:37 PM
 */
public class TransferFileExplorerFragment
        extends TransferListFragment
        implements TitleSupport
{
    private RecyclerView mPathView;
    private TransferPathResolverRecyclerAdapter mPathAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setDividerView(R.id.layout_transfer_explorer_separator);
    }

    @Override
    protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
    {
        View adaptedView = getLayoutInflater().inflate(R.layout.layout_transfer_explorer, null, false);
        listViewContainer.addView(adaptedView);

        mPathView = adaptedView.findViewById(R.id.layout_transfer_explorer_recycler);
        mPathAdapter = new TransferPathResolverRecyclerAdapter(getContext());

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), RecyclerView.HORIZONTAL, false);
        layoutManager.setStackFromEnd(true);

        mPathView.setHasFixedSize(true);
        mPathView.setLayoutManager(layoutManager);
        mPathView.setAdapter(mPathAdapter);

        mPathAdapter.setOnClickListener(new PathResolverRecyclerAdapter.OnClickListener<String>()
        {
            @Override
            public void onClick(PathResolverRecyclerAdapter.Holder<String> holder)
            {
                goPath(getAdapter().getGroupId(), holder.index.object);
            }
        });

        return super.onListView(mainContainer, (ViewGroup) adaptedView.findViewById(R.id.layout_transfer_explorer_fragment_content));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        setSnackbarContainer(view.findViewById(R.id.layout_transfer_explorer_fragment_content));
    }

    @Override
    protected void onListRefreshed()
    {
        super.onListRefreshed();

        String path = getAdapter().getPath();

        mPathAdapter.goTo(getAdapter().getDevice(), path == null ? null
                : path.split(File.separator));
        mPathAdapter.notifyDataSetChanged();

        if (mPathAdapter.getItemCount() > 0)
            mPathView.smoothScrollToPosition(mPathAdapter.getItemCount() - 1);
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_files);
    }
}