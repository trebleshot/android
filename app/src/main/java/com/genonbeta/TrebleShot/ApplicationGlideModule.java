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

package com.genonbeta.TrebleShot;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.Priority;
import com.bumptech.glide.Registry;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.Options;
import com.bumptech.glide.load.ResourceDecoder;
import com.bumptech.glide.load.data.DataFetcher;
import com.bumptech.glide.load.engine.Resource;
import com.bumptech.glide.load.model.ModelLoader;
import com.bumptech.glide.load.model.ModelLoaderFactory;
import com.bumptech.glide.load.model.MultiModelLoaderFactory;
import com.bumptech.glide.load.resource.drawable.DrawableResource;
import com.bumptech.glide.module.AppGlideModule;
import com.bumptech.glide.signature.ObjectKey;
import com.bumptech.glide.util.Util;

/**
 * created by: Veli
 * date: 28.03.2018 17:29
 */
@GlideModule
public final class ApplicationGlideModule extends AppGlideModule
{
    @Override
    public void registerComponents(@NonNull Context context, @NonNull Glide glide, @NonNull Registry registry)
    {
        super.registerComponents(context, glide, registry);
        registry.append( ApplicationInfo.class, Drawable.class, new DrawableModelLoaderFactory(context));
    }

    static class DrawableDataFetcher implements DataFetcher<Drawable>
    {
        private final ApplicationInfo mModel;
        private final Context mContext;

        DrawableDataFetcher(Context context, ApplicationInfo model)
        {
            mModel = model;
            mContext = context;
        }

        @Override
        public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super Drawable> callback)
        {
            final Drawable icon = mContext.getPackageManager().getApplicationIcon(mModel);
            callback.onDataReady(icon);
        }

        @Override
        public void cleanup()
        {
            // Empty Implementation
        }

        @Override
        public void cancel()
        {
            // Empty Implementation
        }

        @NonNull
        @Override
        public Class<Drawable> getDataClass()
        {
            return Drawable.class;
        }

        @NonNull
        @Override
        public DataSource getDataSource()
        {
            return DataSource.LOCAL;
        }
    }

    static class DrawableModelLoader implements ModelLoader<ApplicationInfo, Drawable>
    {
        private final Context mContext;

        DrawableModelLoader(Context context)
        {
            mContext = context;
        }

        @Nullable
        @Override
        public LoadData<Drawable> buildLoadData(@NonNull ApplicationInfo applicationInfo, int width, int height,
                                                @NonNull Options options)
        {
            return new LoadData<>(new ObjectKey(applicationInfo), new DrawableDataFetcher(mContext, applicationInfo));
        }

        @Override
        public boolean handles(@NonNull ApplicationInfo applicationInfo)
        {
            return true;
        }
    }

    static class DrawableModelLoaderFactory implements ModelLoaderFactory<ApplicationInfo, Drawable>
    {
        private final Context mContext;

        DrawableModelLoaderFactory(Context context)
        {
            mContext = context;
        }

        @NonNull
        @Override
        public ModelLoader<ApplicationInfo, Drawable> build(@NonNull MultiModelLoaderFactory multiFactory)
        {
            return new DrawableModelLoader(mContext);
        }

        @Override
        public void teardown()
        {
            // Empty Implementation.
        }
    }
}
