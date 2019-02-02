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

import java.io.IOException;

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

        registry.append(ApplicationInfo.class, ApplicationInfo.class,
                new ModelLoaderFactory<ApplicationInfo, ApplicationInfo>()
                {
                    @Override
                    public ModelLoader<ApplicationInfo, ApplicationInfo> build(
                            @NonNull MultiModelLoaderFactory multiFactory)
                    {
                        return new ApplicationIconModelLoader();
                    }

                    @Override
                    public void teardown()
                    {

                    }
                }).append(ApplicationInfo.class, Drawable.class, new ApplicationIconDecoder(context));
    }

    private class ApplicationIconModelLoader implements ModelLoader<ApplicationInfo, ApplicationInfo>
    {
        @Nullable
        @Override
        public LoadData<ApplicationInfo> buildLoadData(@NonNull final ApplicationInfo applicationInfo, int width, int height, @NonNull Options options)
        {
            return new LoadData<>(new ObjectKey(applicationInfo), new DataFetcher<ApplicationInfo>()
            {
                @Override
                public void loadData(@NonNull Priority priority, @NonNull DataCallback<? super ApplicationInfo> callback)
                {
                    callback.onDataReady(applicationInfo);
                }

                @Override
                public void cleanup()
                {

                }

                @Override
                public void cancel()
                {

                }

                @NonNull
                @Override
                public Class<ApplicationInfo> getDataClass()
                {
                    return ApplicationInfo.class;
                }

                @NonNull
                @Override
                public DataSource getDataSource()
                {
                    return DataSource.LOCAL;
                }
            });
        }

        @Override
        public boolean handles(ApplicationInfo applicationInfo)
        {
            return true;
        }
    }

    private class ApplicationIconDecoder implements ResourceDecoder<ApplicationInfo, Drawable>
    {
        private final Context context;

        public ApplicationIconDecoder(Context context)
        {
            this.context = context;
        }

        @Nullable
        @Override
        public Resource<Drawable> decode(@NonNull ApplicationInfo source, int width, int height, @NonNull Options options) throws IOException
        {
            Drawable icon = source.loadIcon(context.getPackageManager());
            return new DrawableResource<Drawable>(icon)
            {
                @NonNull
                @Override
                public Class<Drawable> getResourceClass()
                {
                    return Drawable.class;
                }

                @Override
                public int getSize()
                {
                    if (drawable instanceof BitmapDrawable)
                        return Util.getBitmapByteSize(((BitmapDrawable) drawable).getBitmap());

                    return 1;
                }

                @Override
                public void recycle()
                {
                }
            };
        }

        @Override
        public boolean handles(@NonNull ApplicationInfo source, @NonNull Options options) throws IOException
        {
            return true;
        }
    }
}
