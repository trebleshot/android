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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.util.Size
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
import org.monora.uprotocol.client.android.content.removeId
import java.io.FileInputStream
import java.io.FileNotFoundException

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
            AppIconModelLoaderFactory(context)
        )
        registry.append(
            Uri::class.java,
            Bitmap::class.java,
            AlbumArtModelLoaderFactory(context)
        )
    }

    internal class AlbumArtDataFetcher(val context: Context, val model: Uri) : DataFetcher<Bitmap> {
        override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
            try {
                if (Build.VERSION.SDK_INT >= 29) {
                    callback.onDataReady(
                        context.contentResolver.loadThumbnail(model, Size(500, 500), null)
                    )
                } else {
                    val cursor = context.contentResolver.query(
                        model, arrayOf(MediaStore.Audio.Albums.ALBUM_ART), null, null, null,
                    ) ?: throw FileNotFoundException("Could not query the uri: $model")

                    cursor.use {
                        if (it.moveToFirst()) {
                            val albumArtIndex = it.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)
                            val albumArt = it.getString(albumArtIndex) ?: throw FileNotFoundException(
                                "The file path was empty"
                            )
                            FileInputStream(albumArt).use { inputStream ->
                                val artData = inputStream.readBytes()
                                callback.onDataReady(BitmapFactory.decodeByteArray(artData, 0, artData.size))
                            }
                        } else {
                            throw FileNotFoundException("No row returned after query")
                        }
                    }
                }
            } catch (e: Exception) {
                callback.onLoadFailed(e)
            } catch (ignored: Throwable) {
                callback.onLoadFailed(Exception())
            }
        }

        override fun cleanup() {
            // Empty Implementation
        }

        override fun cancel() {
            // Empty Implementation
        }

        override fun getDataClass(): Class<Bitmap> {
            return Bitmap::class.java
        }

        override fun getDataSource(): DataSource {
            return DataSource.LOCAL
        }
    }

    internal class AlbumArtModelLoader(private val context: Context) : ModelLoader<Uri, Bitmap> {
        override fun buildLoadData(uri: Uri, width: Int, height: Int, options: Options): LoadData<Bitmap> {
            return LoadData(ObjectKey(uri), AlbumArtDataFetcher(context, uri))
        }

        override fun handles(uri: Uri): Boolean {
            try {
                return MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI == uri.removeId()
            } catch (ignored: Throwable) { }

            return false
        }
    }

    internal class AlbumArtModelLoaderFactory(
        private val context: Context,
    ) : ModelLoaderFactory<Uri, Bitmap> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<Uri, Bitmap> {
            return AlbumArtModelLoader(context)
        }

        override fun teardown() {
            // Empty Implementation.
        }
    }

    internal class AppIconDataFetcher(val context: Context, val model: ApplicationInfo) : DataFetcher<Drawable> {
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

    internal class AppIconModelLoader(private val context: Context) : ModelLoader<ApplicationInfo, Drawable> {
        override fun buildLoadData(
            applicationInfo: ApplicationInfo, width: Int, height: Int, options: Options,
        ): LoadData<Drawable> {
            return LoadData(ObjectKey(applicationInfo), AppIconDataFetcher(context, applicationInfo))
        }

        override fun handles(applicationInfo: ApplicationInfo): Boolean {
            return true
        }
    }

    internal class AppIconModelLoaderFactory(
        private val context: Context,
    ) : ModelLoaderFactory<ApplicationInfo, Drawable> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<ApplicationInfo, Drawable> {
            return AppIconModelLoader(context)
        }

        override fun teardown() {
            // Empty Implementation.
        }
    }
}
