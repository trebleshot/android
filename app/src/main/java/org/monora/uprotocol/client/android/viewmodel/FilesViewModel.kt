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

import android.content.Context
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
import org.monora.uprotocol.client.android.model.ContentModel
import org.monora.uprotocol.client.android.model.FileModel
import org.monora.uprotocol.client.android.model.TitleSectionContentModel
import org.monora.uprotocol.client.android.util.Files
import java.text.Collator
import javax.inject.Inject

@HiltViewModel
class FilesViewModel @Inject internal constructor(
    @ApplicationContext context: Context,
    private val fileRepository: FileRepository
) : ViewModel() {
    private val appDirectory = Files.getAppDirectory(context)

    private val textFolder = context.getString(R.string.text_folder)

    private val textFile = context.getString(R.string.text_file)

    private val _files = MutableLiveData<List<ContentModel>>()

    val files = liveData {
        requestPath(appDirectory)
        emitSource(_files)
    }

    private val _path = MutableLiveData<List<FileModel>>()

    val path = liveData {
        emitSource(_path)
    }

    private fun createOrderedFileList(file: DocumentFile): List<ContentModel> {
        val pathTree = mutableListOf<FileModel>()

        var pathChild = file
        do {
            pathTree.add(FileModel(pathChild))
        } while (pathChild.parent?.also { pathChild = it } != null)

        pathTree.reverse()
        _path.postValue(pathTree)

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

    fun requestPath(file: DocumentFile) {
        viewModelScope.launch(Dispatchers.IO) {
            _files.postValue(createOrderedFileList(file))
        }
    }
}