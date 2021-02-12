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
package com.genonbeta.TrebleShot.adapter

import android.content.*
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.*
import android.view.*
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.ChangeStoragePathActivity
import com.genonbeta.TrebleShot.adapter.FileListAdapter.*
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.fragment.FileListFragment
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Files
import com.genonbeta.TrebleShot.util.MimeIconUtils
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.*
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupLister.*
import com.genonbeta.android.database.DatabaseObject
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.database.exception.ReconstructionFailedException
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.MathUtils
import com.genonbeta.android.framework.util.listing.ComparableMerger
import com.genonbeta.android.framework.util.listing.Merger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

class FileListAdapter(fragment: IEditableListFragment<FileHolder, GroupViewHolder>) :
    GroupEditableListAdapter<FileHolder, GroupViewHolder>(fragment, MODE_GROUP_BY_DEFAULT),
    CustomGroupLister<FileHolder> {
    private var showDirectories = true
    private var showFiles = true
    private var showThumbnails = true
    private var searchWord: String? = null
    private var path: DocumentFile? = null

    protected override fun onLoad(lister: GroupLister<FileHolder>) {
        showThumbnails = AppUtils.getDefaultPreferences(context).getBoolean("load_thumbnails", true)
        val path = getPath()
        if (path != null) {
            val fileIndex = path.listFiles()
            if (fileIndex.isNotEmpty()) {
                for (file in fileIndex) {
                    val searchWord = searchWord

                    if (searchWord != null && !file.getName().matches(Regex(searchWord))) continue
                    lister.offerObliged(this, FileHolder(context, file))
                }
            }
        } else {
            run {
                val saveDir = FileHolder(context, Files.getApplicationDirectory(context))
                saveDir.type = FileHolder.Type.SaveLocation
                lister.offerObliged(this, saveDir)
            }
            run {
                val rootDir = File(".")
                if (rootDir.canRead()) {
                    val rootHolder = FileHolder(context, DocumentFile.fromFile(rootDir))
                    rootHolder.friendlyName = context.getString(R.string.text_fileRoot)
                    lister.offerObliged(this, rootHolder)
                }
            }
            val referencedDirectoryList: MutableList<File> = ArrayList()
            if (Build.VERSION.SDK_INT >= 21) referencedDirectoryList.addAll(Arrays.asList(*context.getExternalMediaDirs())) else if (Build.VERSION.SDK_INT >= 19) referencedDirectoryList.addAll(
                Arrays.asList(*context.getExternalFilesDirs(null))
            ) else referencedDirectoryList.add(Environment.getExternalStorageDirectory())
            for (mediaDir in referencedDirectoryList) {
                if (!mediaDir.canWrite()) continue
                val fileHolder = FileHolder(context, DocumentFile.fromFile(mediaDir))
                fileHolder.type = FileHolder.Type.Storage
                val splitPath = mediaDir.absolutePath.split(File.separator.toRegex()).toTypedArray()
                if (splitPath.size >= 2 && splitPath[1] == "storage") {
                    if (splitPath.size >= 4 && splitPath[2] == "emulated") {
                        val file = File(buildPath(splitPath, 4))
                        if (file.canWrite()) {
                            fileHolder.file = DocumentFile.fromFile(file)
                            fileHolder.friendlyName =
                                if ("0" == splitPath[3]) context.getString(R.string.text_internalStorage) else context.getString(
                                    R.string.text_emulatedMediaDirectory,
                                    splitPath[3]
                                )
                        }
                    } else if (splitPath.size >= 3) {
                        val file = File(buildPath(splitPath, 3))
                        if (!file.canWrite()) continue
                        fileHolder.friendlyName = splitPath[2]
                        fileHolder.file = DocumentFile.fromFile(file)
                    }
                }
                lister.offerObliged(this, fileHolder)
            }
            run {
                val savedDirList = AppUtils.getKuick(context).castQuery(
                    SQLQuery.Select(Kuick.TABLE_FILEBOOKMARK),
                    FileHolder::class.java
                )
                for (dir in savedDirList) if (dir.file != null) lister.offerObliged(this, dir)
            }
            if (Build.VERSION.SDK_INT >= 21) {
                val mountButtonRep = FileHolder(
                    VIEW_TYPE_ACTION_BUTTON,
                    context.getString(R.string.butn_mountDirectory)
                )
                mountButtonRep.requestCode = REQUEST_CODE_MOUNT_FOLDER
                mountButtonRep.type = FileHolder.Type.Storage
                lister.offerObliged(this, mountButtonRep)
            }
            run {
                val objects = AppUtils.getKuick(context)
                    .castQuery(
                        SQLQuery.Select(Kuick.TABLE_TRANSFERITEM).setWhere(
                            String.format("%s = ?", Kuick.FIELD_TRANSFERITEM_FLAG),
                            TransferItem.Flag.DONE.toString()
                        ).setOrderBy(String.format("%s DESC", Kuick.FIELD_TRANSFERITEM_LASTCHANGETIME)),
                        TransferItem::class.java
                    )
                val pickedRecentFiles: MutableList<DocumentFile> = ArrayList()
                val transferMap: MutableMap<Long, Transfer> = ArrayMap()
                for (transfer in AppUtils.getKuick(context).castQuery(
                    SQLQuery.Select(Kuick.TABLE_TRANSFER), Transfer::class.java
                )) transferMap[transfer.id] = transfer
                var errorLimit = 3
                for (item in objects) {
                    val transfer = transferMap[item.transferId]
                    if (pickedRecentFiles.size >= 20 || errorLimit == 0 || transfer == null) break
                    try {
                        val documentFile = Files.getIncomingPseudoFile(
                            context, item, transfer,
                            false
                        )
                        if (documentFile.exists() && !pickedRecentFiles.contains(documentFile)) pickedRecentFiles.add(
                            documentFile
                        ) else errorLimit--
                    } catch (e: IOException) {
                        errorLimit--
                    }
                }
                for (documentFile in pickedRecentFiles) {
                    val holder = FileHolder(context, documentFile)
                    holder.type = FileHolder.Type.Recent
                    lister.offerObliged(this, holder)
                }
            }
        }
    }

    override fun onGenerateRepresentative(text: String, merger: Merger<FileHolder>?): FileHolder {
        return FileHolder(VIEW_TYPE_REPRESENTATIVE, text)
    }

    override fun onCustomGroupListing(lister: GroupLister<FileHolder>, mode: Int, holder: FileHolder): Boolean {
        val file = holder.file
        if (mode == MODE_GROUP_BY_DEFAULT || mode == MODE_GROUP_FOR_INBOX && file != null && file.isDirectory()) {
            lister.offer(holder, FileHolderMerger(holder))
        } else return false
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == VIEW_TYPE_DEFAULT) GroupViewHolder(
            layoutInflater.inflate(R.layout.list_file, parent, false)
        ) else createDefaultViews(parent, viewType, false)

        if (viewType == VIEW_TYPE_ACTION_BUTTON) fragment.registerLayoutViewClicks(
            holder
        ) else if (!holder.isRepresentative()) {
            fragment.registerLayoutViewClicks(holder)
            holder.itemView.findViewById<View>(R.id.layout_image)
                .setOnClickListener { v: View? -> fragment.setItemSelected(holder, true) }
            holder.itemView.findViewById<View>(R.id.menu).setOnClickListener { v: View? ->
                val fileHolder: FileHolder = getList().get(holder.adapterPosition)
                val file = fileHolder.file
                val isFile =
                    FileHolder.Type.File == fileHolder.type || FileHolder.Type.Recent == fileHolder.type
                            || FileHolder.Type.Pending == fileHolder.type
                var isMounted = FileHolder.Type.Mounted == fileHolder.type
                var isBookmarked = FileHolder.Type.Bookmarked == fileHolder.type
                val canWrite = file != null && file.canWrite()
                val canRead = file != null && file.canRead()
                if (!isMounted && !isBookmarked && file != null) try {
                    val dbTestObject = FileHolder(context, file)
                    AppUtils.getKuick(context).reconstruct(dbTestObject)
                    isMounted = FileHolder.Type.Mounted == dbTestObject.type
                    isBookmarked = FileHolder.Type.Bookmarked == dbTestObject.type
                } catch (ignored: ReconstructionFailedException) {
                }
                val popupMenu = PopupMenu(context, v)
                val menuItself = popupMenu.menu
                popupMenu.menuInflater.inflate(R.menu.action_mode_file, menuItself)
                menuItself.findItem(R.id.action_mode_file_open).isVisible = canRead && isFile
                menuItself.findItem(R.id.action_mode_file_rename).isEnabled = ((canWrite || isMounted || isBookmarked)
                        && FileHolder.Type.Pending != fileHolder.type)
                menuItself.findItem(R.id.action_mode_file_delete).isEnabled = canWrite && !isMounted
                menuItself.findItem(R.id.action_mode_file_show).isVisible = FileHolder.Type.Recent ==
                        fileHolder.type
                menuItself.findItem(R.id.action_mode_file_change_save_path).isVisible =
                    FileHolder.Type.SaveLocation == fileHolder.type || fileHolder.file != null
                            && Files.getApplicationDirectory(context) == fileHolder.file
                menuItself.findItem(R.id.action_mode_file_eject_directory).isVisible = isMounted
                menuItself.findItem(R.id.action_mode_file_toggle_shortcut).setVisible(!isFile && !isMounted)
                    .setTitle(if (isBookmarked) R.string.butn_removeShortcut else R.string.butn_addShortcut)
                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    val id = item.itemId
                    val generateSelectionList = ArrayList<FileHolder>()
                    val parentFile = fileHolder.file?.getParentFile()
                    val fragment = fragment

                    generateSelectionList.add(fileHolder)

                    if (id == R.id.action_mode_file_open) {
                        fragment.performLayoutClickOpen(holder, fileHolder)
                    } else if (id == R.id.action_mode_file_show && parentFile != null) {
                        goPath(parentFile)
                        fragment.refreshList()
                    } else if (id == R.id.action_mode_file_eject_directory) {
                        AppUtils.getKuick(context).remove(fileHolder)
                        AppUtils.getKuick(context).broadcast()
                    } else if (id == R.id.action_mode_file_toggle_shortcut) {
                        FileListFragment.shortcutItem(fragment, fileHolder)
                    } else if (id == R.id.action_mode_file_change_save_path) {
                        context.startActivity(Intent(context, ChangeStoragePathActivity::class.java))
                    } else if (fragment is FileListFragment)
                        return@setOnMenuItemClickListener !FileListFragment.handleEditingAction(
                            item, fragment,
                            generateSelectionList
                        )
                    true
                }
                popupMenu.show()
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val itemHolder: FileHolder = getItem(position)
        if (!holder.tryBinding(itemHolder)) {
            val parentView: View = holder.itemView
            val lookAltered = !showFiles || !showDirectories
            val thumbnail = parentView.findViewById<ImageView>(R.id.thumbnail)
            val image = parentView.findViewById<ImageView>(R.id.image)
            val text1: TextView = parentView.findViewById(R.id.text)
            val text2: TextView = parentView.findViewById(R.id.text2)
            holder.setSelected(itemHolder.isSelectableSelected())
            text1.setText(itemHolder.friendlyName)
            text2.setText(itemHolder.getInfo(context))
            if (lookAltered) {
                val file = itemHolder.file
                val enabled = (file == null || showFiles && file.isFile() || showDirectories && file.isDirectory())
                text1.isEnabled = enabled
                text2.isEnabled = enabled
                image.alpha = if (enabled) 1f else 0.5f
                thumbnail.alpha = if (enabled) 1f else 0.5f
            }
            if (!showThumbnails || !itemHolder.loadThumbnail(context, thumbnail)) {
                image.setImageResource(itemHolder.getIconRes())
                thumbnail.setImageDrawable(null)
            } else image.setImageDrawable(null)
        } else if (holder.itemViewType == VIEW_TYPE_ACTION_BUTTON) (holder.itemView.findViewById<View>(
            R.id.icon
        ) as ImageView).setImageResource(itemHolder.getIconRes())
    }

    fun buildPath(splitPath: Array<String>, count: Int): String {
        val stringBuilder = StringBuilder()
        var i = 0
        while (i < count && i < splitPath.size) {
            stringBuilder.append(File.separator)
            stringBuilder.append(splitPath[i])
            i++
        }
        return stringBuilder.toString()
    }

    override fun createLister(loadedList: MutableList<FileHolder>, groupBy: Int): GroupLister<FileHolder> {
        return super.createLister(loadedList, groupBy).also { it.customLister = this }
    }

    override fun getGroupBy(): Int {
        return if (path != null && path == Files.getApplicationDirectory(context)) MODE_GROUP_FOR_INBOX else super.getGroupBy()
    }

    override fun getSortingCriteria(objectOne: FileHolder, objectTwo: FileHolder): Int {
        // Checking whether the path is null helps to increase the speed.
        return if (getPath() == null && FileHolder.Type.Recent == objectOne.type
            && FileHolder.Type.Recent == objectTwo.type
        )
            MODE_SORT_BY_DATE
        else
            super.getSortingCriteria(objectOne, objectTwo)
    }

    override fun getSortingOrder(objectOne: FileHolder, objectTwo: FileHolder): Int {
        // Checking whether the path is null helps to increase the speed.
        return if (getPath() == null && FileHolder.Type.Recent == objectOne.type
            && FileHolder.Type.Recent == objectTwo.type
        )
            MODE_SORT_ORDER_DESCENDING
        else
            super.getSortingOrder(objectOne, objectTwo)
    }

    fun getPath(): DocumentFile? {
        return path
    }

    fun goPath(path: File?) {
        goPath(path?.let { DocumentFile.fromFile(path) })
    }

    override fun getRepresentativeText(merger: Merger<out FileHolder>): String {
        return if (merger is FileHolderMerger) {
            when (merger.type) {
                FileHolderMerger.Type.Storage -> context.getString(R.string.text_storage)
                FileHolderMerger.Type.PublicFolder -> context.getString(R.string.text_shortcuts)
                FileHolderMerger.Type.Folder -> context.getString(R.string.text_folder)
                FileHolderMerger.Type.PartFile -> context.getString(R.string.text_pendingTransfers)
                FileHolderMerger.Type.RecentFile -> context.getString(R.string.text_recentFiles)
                FileHolderMerger.Type.File -> context.getString(R.string.text_file)
                FileHolderMerger.Type.Dummy -> context.getString(R.string.text_unknown)
            }
        } else super.getRepresentativeText(merger)
    }

    fun goPath(path: DocumentFile?) {
        this.path = path
    }

    fun setConfiguration(showDirectories: Boolean, showFiles: Boolean, fileMatch: String?) {
        this.showDirectories = showDirectories
        this.showFiles = showFiles
        this.searchWord = fileMatch
    }

    class FileHolder : GroupShareable, DatabaseObject<Any?> {
        lateinit var file: DocumentFile

        var transferItem: TransferItem? = null

        var type: Type = Type.Dummy
            get() {
                val file = file
                return if (field == Type.Dummy && file != null) {
                    if (file.isDirectory()) Type.Folder else Type.File
                } else field
            }

        constructor() : super()

        constructor(viewType: Int, representativeText: String) : super(viewType, representativeText)

        constructor(context: Context, file: DocumentFile) {
            initialize(file)
            calculate(context)
        }

        protected fun calculate(context: Context) {
            val fileLocal = file

            if (fileLocal != null && AppConfig.EXT_FILE_PART == Files.getFileFormat(fileLocal.getName())) {
                type = Type.Pending
                try {
                    val kuick = AppUtils.getKuick(context)
                    val data: ContentValues? = kuick.getFirstFromTable(
                        SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                            .setWhere(Kuick.FIELD_TRANSFERITEM_FILE + "=?", fileLocal.getName())
                    )
                    data?.let { contentValues: ContentValues ->
                        transferItem = TransferItem().also {
                            it.reconstruct(kuick.writableDatabase, kuick, contentValues)
                            mimeType = it.mimeType
                            friendlyName = it.name
                        }
                    }
                } catch (ignored: Exception) {
                }
            }
        }

        override fun comparisonSupported(): Boolean {
            return getViewType() != VIEW_TYPE_ACTION_BUTTON && super.comparisonSupported()
        }

        @DrawableRes
        fun getIconRes(): Int {
            val fileLocal = file

            return if (fileLocal == null) 0 else if (fileLocal.isDirectory()) {
                when (type) {
                    Type.Storage -> R.drawable.ic_save_white_24dp
                    Type.SaveLocation -> R.drawable.ic_uprotocol
                    Type.Bookmarked, Type.Mounted -> R.drawable.ic_bookmark_white_24dp
                    else -> R.drawable.ic_folder_white_24dp
                }
            } else {
                if (Type.Pending == type && transferItem == null)
                    R.drawable.ic_block_white_24dp
                else
                    MimeIconUtils.loadMimeIcon(mimeType)
            }
        }

        fun getInfo(context: Context): String {
            val fileLocal = file
            return when (type) {
                Type.Storage -> context.getString(R.string.text_storage)
                Type.Mounted -> context.getString(R.string.text_mountedDirectory)
                Type.Bookmarked, Type.Folder, Type.Public -> if (fileLocal != null && fileLocal.isDirectory()) {
                    val itemSize = file!!.listFiles().size
                    context.resources.getQuantityString(R.plurals.text_items, itemSize, itemSize)
                } else context.getString(R.string.text_unknown)
                Type.SaveLocation -> context.getString(R.string.text_defaultFolder)
                Type.Pending -> if (transferItem == null) context.getString(R.string.mesg_notValidTransfer) else String.format(
                    "%s / %s", com.genonbeta.android.framework.util.Files.sizeExpression(getComparableSize(), false),
                    com.genonbeta.android.framework.util.Files.sizeExpression(transferItem!!.getComparableSize(), false)
                )
                Type.Recent, Type.File -> com.genonbeta.android.framework.util.Files.sizeExpression(
                    getComparableSize(),
                    false
                )
                else -> context.getString(R.string.text_unknown)
            }
        }

        override fun getValues(): ContentValues {
            val contentValues = ContentValues()
            contentValues.put(Kuick.FIELD_FILEBOOKMARK_TITLE, friendlyName)
            contentValues.put(Kuick.FIELD_FILEBOOKMARK_PATH, uri.toString())
            return contentValues
        }

        override fun getWhere(): SQLQuery.Select {
            return SQLQuery.Select(Kuick.TABLE_FILEBOOKMARK)
                .setWhere(String.format("%s = ?", Kuick.FIELD_FILEBOOKMARK_PATH), uri.toString())
        }

        protected fun initialize(file: DocumentFile) {
            initialize(
                0, file.getName(), file.getName(), file.getType(), file.getLastModified(), file.getLength(),
                file.getUri()
            )
            this.file = file
        }

        fun loadThumbnail(context: Context, imageView: ImageView): Boolean {
            val file = file
            val mimeType = mimeType ?: "*/*"

            if (file == null || file.isDirectory() || Type.Pending == type || !mimeType.startsWith("image/")
                && !mimeType.startsWith("video/")
            ) return false
            GlideApp.with(context)
                .load(file.getUri())
                .error(MimeIconUtils.loadMimeIcon(mimeType))
                .override(160)
                .circleCrop()
                .into(imageView)
            return true
        }

        override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, values: ContentValues) {
            uri = Uri.parse(item.getAsString(Kuick.FIELD_FILEBOOKMARK_PATH))
            type = if (uri.toString().startsWith("file")) Type.Bookmarked else Type.Mounted
            try {
                initialize(com.genonbeta.android.framework.util.Files.fromUri(kuick.context, uri))
                calculate(kuick.context)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            friendlyName = item.getAsString(Kuick.FIELD_FILEBOOKMARK_TITLE)
        }

        override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any?, progress: Progress.Context?) {}

        override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any?, progress: Progress.Context?) {}

        override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any?, progress: Progress.Context?) {}

        override fun setSelectableSelected(selected: Boolean): Boolean {
            return when (type) {
                Type.Dummy, Type.Public, Type.Storage, Type.Mounted, Type.Bookmarked -> false
                else -> super.setSelectableSelected(selected)
            }
        }

        enum class Type {
            Storage, Bookmarked, Mounted, Public, SaveLocation, Recent, Pending, Folder, File, Dummy
        }
    }

    private class FileHolderMerger(holder: FileHolder) : ComparableMerger<FileHolder>() {
        var type: Type = when (holder.type) {
            FileHolder.Type.Mounted, FileHolder.Type.Storage -> Type.Storage
            FileHolder.Type.Public, FileHolder.Type.Bookmarked -> Type.PublicFolder
            FileHolder.Type.Pending -> Type.PartFile
            FileHolder.Type.Recent -> Type.RecentFile
            FileHolder.Type.Folder, FileHolder.Type.SaveLocation -> Type.Folder
            FileHolder.Type.File -> Type.File
            FileHolder.Type.Dummy -> Type.Dummy
        }

        override fun equals(other: Any?): Boolean {
            return other is FileHolderMerger && other.type == type
        }

        override fun hashCode(): Int {
            return type.hashCode()
        }

        override operator fun compareTo(other: ComparableMerger<FileHolder>): Int {
            return if (other is FileHolderMerger)
                MathUtils.compare(other.type.ordinal.toLong(), type.ordinal.toLong())
            else 0
        }

        enum class Type {
            Storage, Folder, PublicFolder, RecentFile, PartFile, File, Dummy
        }
    }

    companion object {
        const val MODE_GROUP_BY_DEFAULT: Int = MODE_GROUP_BY_NOTHING + 1
        const val MODE_GROUP_FOR_INBOX: Int = MODE_GROUP_BY_DATE
        const val REQUEST_CODE_MOUNT_FOLDER = 1
    }
}