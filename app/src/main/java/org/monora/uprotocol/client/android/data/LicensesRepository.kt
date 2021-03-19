/*
 * Copyright (C) 2021 Veli Tasalı
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

package org.monora.uprotocol.client.android.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.monora.uprotocol.client.android.fragment.external.LicensesFragment
import org.monora.uprotocol.client.android.model.LibraryLicense
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicensesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun getLicenses(): List<LibraryLicense> = withContext(Dispatchers.IO) {
        val list = mutableListOf<LibraryLicense>()

        context.assets.open("licenses.json").use { inputStream ->
            val jsonReader = JsonReader(InputStreamReader(inputStream))
            val gson = Gson()

            try {
                // skip to the "libraries" index
                jsonReader.beginObject()

                if (!jsonReader.hasNext()) {
                    return@use
                } else if (jsonReader.nextName() != "libraries") {
                    Log.d(LicensesFragment::class.simpleName, "onViewCreated: 'libraries' does not exist")
                    return@use
                }

                jsonReader.beginArray()

                while (jsonReader.hasNext()) {
                    list.add(gson.fromJson(jsonReader, LibraryLicense::class.java))
                }

                list.sortBy { it.artifactId.group }

                jsonReader.endArray()
                jsonReader.endObject()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        list
    }
}