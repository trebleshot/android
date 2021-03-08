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
    suspend fun licenses(): List<LibraryLicense> = withContext(Dispatchers.IO) {
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