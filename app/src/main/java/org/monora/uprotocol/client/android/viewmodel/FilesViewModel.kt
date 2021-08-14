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

package org.monora.uprotocol.client.android.viewmodel

import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.genonbeta.android.framework.io.DocumentFile
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.FileRepository
import org.monora.uprotocol.client.android.database.model.SafFolder
import org.monora.uprotocol.client.android.model.ContentModel
import org.monora.uprotocol.client.android.model.FileModel
import org.monora.uprotocol.client.android.model.TitleSectionContentModel
import java.lang.ref.WeakReference
import java.text.Collator
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject internal constructor(
    @ApplicationContext context: Context,
    private val fileRepository: FileRepository,
) : ViewModel() {

    private val context = WeakReference(context)

    private val textFolder = context.getString(R.string.text_folder)

    private val textFile = context.getString(R.string.text_file)

    private val _files = MutableLiveData<List<ContentModel>>()

    val files = liveData {
        requestPath(fileRepository.appDirectory)
        emitSource(_files)
    }

    val isCustomStorageFolder: Boolean
        get() {
            val context = context.get() ?: return false
            return Uri.fromFile(fileRepository.defaultAppDirectory) != fileRepository.appDirectory.getUri()
        }

    private val _path = MutableLiveData<FileModel>()

    val path = liveData {
        emitSource(_path)
    }

    private val _pathTree = MutableLiveData<List<FileModel>>()

    val pathTree = liveData {
        emitSource(_pathTree)
    }

    val safFolders = fileRepository.getSafFolders()

    var appDirectory
        get() = fileRepository.appDirectory
        set(value) {
            fileRepository.appDirectory = value
        }

    fun clearStorageList() {
        viewModelScope.launch(Dispatchers.IO) {
            fileRepository.clearStorageList()
        }
    }

    fun createFolder(displayName: String): Boolean {
        val currentFolder = path.value ?: return false
        val context = context.get() ?: return false

        if (currentFolder.file.createDirectory(context, displayName) != null) {
            requestPath(currentFolder.file)
            return true
        }
        return false
    }

    private fun createOrderedFileList(file: DocumentFile): List<ContentModel> {
        val pathTree = mutableListOf<FileModel>()

        var pathChild = file
        do {
            pathTree.add(FileModel(pathChild))
        } while (pathChild.parent?.also { pathChild = it } != null)

        pathTree.reverse()
        _pathTree.postValue(pathTree)

        val list = fileRepository.getFileList(file)

        if (list.isEmpty()) return list

        val collator = Collator.getInstance()
        collator.strength = Collator.TERTIARY

        val sortedList = list.sortedWith(compareBy(collator) {
            it.name()
        })

        val contents = ArrayList<ContentModel>(0)
        val files = ArrayList<FileModel>(0)

        sortedList.forEach {
            if (it.file.isDirectory()) contents.add(it)
            else if (it.file.isFile()) files.add(it)
        }

        if (contents.isNotEmpty()) {
            contents.add(0, TitleSectionContentModel(textFolder))
        }

        if (files.isNotEmpty()) {
            contents.add(TitleSectionContentModel(textFile))
            contents.addAll(files)
        }

        return contents
    }

    fun goUp(): Boolean {
        val paths = pathTree.value ?: return false

        if (paths.size < 2) {
            return false
        }

        val iterator = paths.asReversed().listIterator()
        if (iterator.hasNext()) {
            iterator.next() // skip the first one that is already visible
            do {
                val next = iterator.next()
                if (next.file.canRead()) {
                    requestPath(next.file)
                    return true
                }
            } while (iterator.hasNext())
        }

        return false
    }

    @TargetApi(19)
    fun insertSafFolder(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = context.get() ?: return@launch

                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                val document = DocumentFile.fromUri(context, uri, true)
                val safFolder = SafFolder(uri, document.getName())

                fileRepository.insertFolder(safFolder)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun requestDefaultStorageFolder() {
        viewModelScope.launch(Dispatchers.IO) {
            context.get()?.let {
                requestPathInternal(DocumentFile.fromFile(fileRepository.defaultAppDirectory))
            }
        }
    }

    fun requestStorageFolder() {
        viewModelScope.launch(Dispatchers.IO) {
            context.get()?.let {
                requestPathInternal(fileRepository.appDirectory)
            }
        }
    }

    fun requestPath(file: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            requestPathInternal(file)
        }
    }

    fun requestPath(folder: SafFolder) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.get()?.let {
                    requestPathInternal(DocumentFile.fromUri(it, folder.uri, true))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun requestPathInternal(file: DocumentFile) {
        _path.postValue(FileModel(file))
        _files.postValue(createOrderedFileList(file))
    }
}
