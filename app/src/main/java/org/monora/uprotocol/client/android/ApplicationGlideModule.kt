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
package org.monora.uprotocol.client.android

import android.content.Context
import android.content.pm.ApplicationInfo
import android.graphics.drawable.Drawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoader.LoadData
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.module.AppGlideModule
import com.bumptech.glide.signature.ObjectKey

/**
 * created by: Veli
 * date: 28.03.2018 17:29
 */
@GlideModule
class ApplicationGlideModule : AppGlideModule() {
    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        super.registerComponents(context, glide, registry)
        registry.append(
            ApplicationInfo::class.java,
            Drawable::class.java,
            DrawableModelLoaderFactory(context)
        )
    }

    internal class DrawableDataFetcher(val context: Context, val model: ApplicationInfo) : DataFetcher<Drawable> {
        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Drawable>) {
            callback.onDataReady(context.packageManager.getApplicationIcon(model))
        }

        override fun cleanup() {
            // Empty Implementation
        }

        override fun cancel() {
            // Empty Implementation
        }

        override fun getDataClass(): Class<Drawable> {
            return Drawable::class.java
        }

        override fun getDataSource(): DataSource {
            return DataSource.LOCAL
        }
    }

    internal class DrawableModelLoader(private val context: Context) : ModelLoader<ApplicationInfo?, Drawable?> {
        override fun buildLoadData(
            applicationInfo: ApplicationInfo, width: Int, height: Int,
            options: Options,
        ): LoadData<Drawable?> {
            return LoadData(ObjectKey(applicationInfo), DrawableDataFetcher(context, applicationInfo))
        }

        override fun handles(applicationInfo: ApplicationInfo): Boolean {
            return true
        }
    }

    internal class DrawableModelLoaderFactory(private val context: Context) :
        ModelLoaderFactory<ApplicationInfo?, Drawable?> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ApplicationInfo?, Drawable?> {
            return DrawableModelLoader(context)
        }

        override fun teardown() {
            // Empty Implementation.
        }
    }
}