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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.genonbeta.TrebleShot.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by: veli
 * Date: 5/29/17 4:29 PM
 */

abstract public class PathResolverRecyclerAdapter<T> extends RecyclerView.Adapter<PathResolverRecyclerAdapter.Holder<T>>
{
    private final List<Index<T>> mList = new ArrayList<>();
    private OnClickListener<T> mClickListener;
    private Context mContext;

    public PathResolverRecyclerAdapter(Context context)
    {
        mContext = context;
        initAdapter();
    }

    /*
     * To fix issues with the RecyclerView not appearing, the first item must be provided
     * when dealing with wrap_content height.
     */
    public abstract Index<T> onFirstItem();

    @NonNull
    @Override
    public Holder<T> onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return new Holder<T>(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_pathresolver, null));
    }

    @Override
    public void onBindViewHolder(@NonNull final Holder<T> holder, int position)
    {
        holder.index = getList().get(position);
        holder.text.setText(holder.index.title);
        holder.image.setImageResource(holder.index.imgRes);

        if (mClickListener != null)
            holder.text.setOnClickListener(view -> mClickListener.onClick(holder));
    }

    public void initAdapter()
    {
        synchronized (mList) {
            mList.clear();
            mList.add(onFirstItem());
        }
    }

    public Context getContext()
    {
        return mContext;
    }

    @Override
    public int getItemCount()
    {
        return mList.size();
    }

    public List<Index<T>> getList()
    {
        return mList;
    }

    public void setOnClickListener(OnClickListener<T> clickListener)
    {
        mClickListener = clickListener;
    }

    public interface OnClickListener<E>
    {
        void onClick(Holder<E> holder);
    }

    public static class Holder<E> extends RecyclerView.ViewHolder
    {
        public View container;
        public ImageView image;
        public Button text;
        public Index<E> index;

        private Holder(View view)
        {
            super(view);
            this.container = view;
            this.image = view.findViewById(R.id.list_pathresolver_image);
            this.text = view.findViewById(R.id.list_pathresolver_text);
        }
    }

    public static class Index<D>
    {
        public String title;
        public int imgRes;
        public D object;

        public Index(String title, int imgRes, D object)
        {
            this.title = title;
            this.imgRes = imgRes;
            this.object = object;
        }

        public Index(String title, D object)
        {
            this(title, R.drawable.ic_keyboard_arrow_right_white_24dp, object);
        }
    }
}
