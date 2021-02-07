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
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.*
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupLister.*
import com.genonbeta.android.database.DatabaseObject
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.MathUtils
import com.genonbeta.android.framework.util.listing.ComparableMerger
import com.genonbeta.android.framework.util.listing.Merger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.*

class FileListAdapter(fragment: IEditableListFragment<FileHolder, GroupViewHolder>) :
    GroupEditableListAdapter<FileHolder?, GroupViewHolder?>(fragment, MODE_GROUP_BY_DEFAULT),
    CustomGroupLister<FileHolder?> {
    private var mShowDirectories = true
    private var mShowFiles = true
    private var mShowThumbnails = true
    private var mSearchWord: String? = null
    private var mPath: DocumentFile? = null

    protected override fun onLoad(lister: GroupLister<FileHolder>) {
        mShowThumbnails = AppUtils.getDefaultPreferences(getContext()).getBoolean("load_thumbnails", true)
        val path = getPath()
        if (path != null) {
            val fileIndex = path.listFiles()
            if (fileIndex != null && fileIndex.size > 0) {
                for (file in fileIndex) {
                    if (mSearchWord != null && !file.name.matches(mSearchWord)) continue
                    lister.offerObliged(this, FileHolder(getContext(), file))
                }
            }
        } else {
            run {
                val saveDir = FileHolder(getContext(), Files.getApplicationDirectory(getContext()))
                saveDir.type = FileHolder.Type.SaveLocation
                lister.offerObliged(this, saveDir)
            }
            run {
                val rootDir = File(".")
                if (rootDir.canRead()) {
                    val rootHolder = FileHolder(getContext(), DocumentFile.fromFile(rootDir))
                    rootHolder.friendlyName = getContext().getString(R.string.text_fileRoot)
                    lister.offerObliged(this, rootHolder)
                }
            }
            val referencedDirectoryList: MutableList<File> = ArrayList()
            if (Build.VERSION.SDK_INT >= 21) referencedDirectoryList.addAll(Arrays.asList(*getContext().getExternalMediaDirs())) else if (Build.VERSION.SDK_INT >= 19) referencedDirectoryList.addAll(
                Arrays.asList(*getContext().getExternalFilesDirs(null))
            ) else referencedDirectoryList.add(Environment.getExternalStorageDirectory())
            for (mediaDir in referencedDirectoryList) {
                if (mediaDir == null || !mediaDir.canWrite()) continue
                val fileHolder = FileHolder(getContext(), DocumentFile.fromFile(mediaDir))
                fileHolder.type = FileHolder.Type.Storage
                val splitPath = mediaDir.absolutePath.split(File.separator.toRegex()).toTypedArray()
                if (splitPath.size >= 2 && splitPath[1] == "storage") {
                    if (splitPath.size >= 4 && splitPath[2] == "emulated") {
                        val file = File(buildPath(splitPath, 4))
                        if (file.canWrite()) {
                            fileHolder.file = DocumentFile.fromFile(file)
                            fileHolder.friendlyName =
                                if ("0" == splitPath[3]) getContext().getString(R.string.text_internalStorage) else getContext().getString(
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
                val savedDirList = AppUtils.getKuick(getContext())
                    .castQuery<Any, FileHolder>(
                        SQLQuery.Select(Kuick.Companion.TABLE_FILEBOOKMARK),
                        FileHolder::class.java
                    )
                for (dir in savedDirList) if (dir.file != null) lister.offerObliged(this, dir)
            }
            if (Build.VERSION.SDK_INT >= 21) {
                val mountButtonRep = FileHolder(
                    GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON,
                    getContext().getString(R.string.butn_mountDirectory)
                )
                mountButtonRep.requestCode = REQUEST_CODE_MOUNT_FOLDER
                mountButtonRep.type = FileHolder.Type.Storage
                lister.offerObliged(this, mountButtonRep)
            }
            run {
                val objects = AppUtils.getKuick(getContext())
                    .castQuery(
                        SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERITEM).setWhere(
                            String.format("%s = ?", Kuick.Companion.FIELD_TRANSFERITEM_FLAG),
                            TransferItem.Flag.DONE.toString()
                        ).setOrderBy(String.format("%s DESC", Kuick.Companion.FIELD_TRANSFERITEM_LASTCHANGETIME)),
                        TransferItem::class.java
                    )
                val pickedRecentFiles: MutableList<DocumentFile?> = ArrayList()
                val transferMap: MutableMap<Long, Transfer> = ArrayMap()
                for (transfer in AppUtils.getKuick(getContext()).castQuery(
                    SQLQuery.Select(Kuick.Companion.TABLE_TRANSFER), Transfer::class.java
                )) transferMap[transfer.id] = transfer
                var errorLimit = 3
                for (`object` in objects) {
                    val transfer = transferMap[`object`.transferId]
                    if (pickedRecentFiles.size >= 20 || errorLimit == 0 || transfer == null) break
                    try {
                        val documentFile = Files.getIncomingPseudoFile(
                            getContext(), `object`, transfer,
                            false
                        )
                        if (documentFile!!.exists() && !pickedRecentFiles.contains(documentFile)) pickedRecentFiles.add(
                            documentFile
                        ) else errorLimit--
                    } catch (e: IOException) {
                        errorLimit--
                    }
                }
                for (documentFile in pickedRecentFiles) {
                    val holder = FileHolder(getContext(), documentFile)
                    holder.type = FileHolder.Type.Recent
                    lister.offerObliged(this, holder)
                }
            }
        }
    }

    protected override fun onGenerateRepresentative(text: String, merger: Merger<FileHolder>?): FileHolder {
        return FileHolder(GroupEditableListAdapter.VIEW_TYPE_REPRESENTATIVE, text)
    }

    override fun onCustomGroupListing(lister: GroupLister<FileHolder>, mode: Int, `object`: FileHolder): Boolean {
        if (mode == MODE_GROUP_BY_DEFAULT
            || mode == MODE_GROUP_FOR_INBOX && `object`.file != null && `object`.file!!.isDirectory
        ) lister.offer(`object`, FileHolderMerger(`object`)) else return false
        return true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == EditableListAdapter.Companion.VIEW_TYPE_DEFAULT) GroupViewHolder(
            getInflater().inflate(
                R.layout.list_file, parent, false
            )
        ) else createDefaultViews(parent, viewType, false)
        if (viewType == GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON) getFragment().registerLayoutViewClicks(
            holder
        ) else if (!holder.isRepresentative()) {
            getFragment().registerLayoutViewClicks(holder)
            holder.itemView.findViewById<View>(R.id.layout_image)
                .setOnClickListener(View.OnClickListener { v: View? -> getFragment().setItemSelected(holder, true) })
            holder.itemView.findViewById<View>(R.id.menu).setOnClickListener(View.OnClickListener { v: View? ->
                val fileHolder: FileHolder = getList().get(holder.getAdapterPosition())
                val isFile =
                    FileHolder.Type.File == fileHolder.getType() || FileHolder.Type.Recent == fileHolder.getType() || FileHolder.Type.Pending == fileHolder.getType()
                var isMounted = FileHolder.Type.Mounted == fileHolder.getType()
                var isBookmarked = FileHolder.Type.Bookmarked == fileHolder.getType()
                val canWrite = fileHolder.file != null && fileHolder.file!!.canWrite()
                val canRead = fileHolder.file != null && fileHolder.file!!.canRead()
                if (!isMounted && !isBookmarked) try {
                    val dbTestObject = FileHolder(getContext(), fileHolder.file)
                    AppUtils.getKuick(getContext()).reconstruct<Any, FileHolder>(dbTestObject)
                    isMounted = FileHolder.Type.Mounted == dbTestObject.getType()
                    isBookmarked = FileHolder.Type.Bookmarked == dbTestObject.getType()
                } catch (ignored: ReconstructionFailedException) {
                }
                val popupMenu = PopupMenu(getContext(), v)
                val menuItself = popupMenu.menu
                popupMenu.menuInflater.inflate(R.menu.action_mode_file, menuItself)
                menuItself.findItem(R.id.action_mode_file_open).isVisible = canRead && isFile
                menuItself.findItem(R.id.action_mode_file_rename).isEnabled = ((canWrite || isMounted || isBookmarked)
                        && FileHolder.Type.Pending != fileHolder.getType())
                menuItself.findItem(R.id.action_mode_file_delete).isEnabled = canWrite && !isMounted
                menuItself.findItem(R.id.action_mode_file_show).isVisible = FileHolder.Type.Recent ==
                        fileHolder.getType()
                menuItself.findItem(R.id.action_mode_file_change_save_path).isVisible =
                    FileHolder.Type.SaveLocation == fileHolder.getType() || fileHolder.file != null && (Files.getApplicationDirectory(
                        getContext()
                    )
                            == fileHolder.file)
                menuItself.findItem(R.id.action_mode_file_eject_directory).isVisible = isMounted
                menuItself.findItem(R.id.action_mode_file_toggle_shortcut).setVisible(!isFile && !isMounted)
                    .setTitle(if (isBookmarked) R.string.butn_removeShortcut else R.string.butn_addShortcut)
                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    val id = item.itemId
                    val generateSelectionList = ArrayList<FileHolder>()
                    generateSelectionList.add(fileHolder)
                    if (id == R.id.action_mode_file_open) {
                        getFragment().performLayoutClickOpen(holder, fileHolder)
                    } else if (id == R.id.action_mode_file_show && fileHolder.file!!.parentFile != null) {
                        goPath(fileHolder.file!!.parentFile)
                        getFragment().refreshList()
                    } else if (id == R.id.action_mode_file_eject_directory) {
                        AppUtils.getKuick(getContext()).remove(fileHolder)
                        AppUtils.getKuick(getContext()).broadcast()
                    } else if (id == R.id.action_mode_file_toggle_shortcut) {
                        FileListFragment.shortcutItem<FileHolder>(getFragment(), fileHolder)
                    } else if (id == R.id.action_mode_file_change_save_path) {
                        getContext().startActivity(Intent(context, ChangeStoragePathActivity::class.java))
                    } else if (getFragment() is FileListFragment)
                        return@setOnMenuItemClickListener !FileListFragment.handleEditingAction(
                            item, getFragment() as FileListFragment?,
                            generateSelectionList
                        )
                    true
                }
                popupMenu.show()
            })
        }
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val objectHolder: FileHolder = getItem(position)
        if (!holder.tryBinding(objectHolder)) {
            val parentView: View = holder.itemView
            val lookAltered = !mShowFiles || !mShowDirectories
            val thumbnail = parentView.findViewById<ImageView>(R.id.thumbnail)
            val image = parentView.findViewById<ImageView>(R.id.image)
            val text1: TextView = parentView.findViewById(R.id.text)
            val text2: TextView = parentView.findViewById(R.id.text2)
            holder.setSelected(objectHolder.isSelectableSelected)
            text1.setText(objectHolder.friendlyName)
            text2.setText(objectHolder.getInfo(getContext()))
            if (lookAltered) {
                val enabled = (objectHolder.file == null || mShowFiles && objectHolder.file!!.isFile
                        || mShowDirectories && objectHolder.file!!.isDirectory)
                text1.setEnabled(enabled)
                text2.setEnabled(enabled)
                image.alpha = if (enabled) 1f else 0.5f
                thumbnail.alpha = if (enabled) 1f else 0.5f
            }
            if (!mShowThumbnails || !objectHolder.loadThumbnail(getContext(), thumbnail)) {
                image.setImageResource(objectHolder.getIconRes())
                thumbnail.setImageDrawable(null)
            } else image.setImageDrawable(null)
        } else if (holder.getItemViewType() == GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON) (holder.itemView.findViewById<View>(
            R.id.icon
        ) as ImageView).setImageResource(objectHolder.getIconRes())
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
        return super.createLister(loadedList, groupBy).setCustomLister(this)
    }

    override fun getGroupBy(): Int {
        return if (mPath != null && mPath == Files.getApplicationDirectory(getContext())) MODE_GROUP_FOR_INBOX else super.getGroupBy()
    }

    override fun getSortingCriteria(objectOne: FileHolder, objectTwo: FileHolder): Int {
        // Checking whether the path is null helps to increase the speed.
        return if (getPath() == null && FileHolder.Type.Recent == objectOne.getType()
            && FileHolder.Type.Recent == objectTwo.getType()
        )
            MODE_SORT_BY_DATE
        else
            super.getSortingCriteria(objectOne, objectTwo)
    }

    override fun getSortingOrder(objectOne: FileHolder, objectTwo: FileHolder): Int {
        // Checking whether the path is null helps to increase the speed.
        return if (getPath() == null && FileHolder.Type.Recent == objectOne.getType()
            && FileHolder.Type.Recent == objectTwo.getType()
        )
            MODE_SORT_ORDER_DESCENDING
        else
            super.getSortingOrder(objectOne, objectTwo)
    }

    fun getPath(): DocumentFile? {
        return mPath
    }

    fun goPath(path: File?) {
        goPath(DocumentFile.fromFile(path))
    }

    override fun getRepresentativeText(merger: Merger<out FileHolder>): String {
        return if (merger is FileHolderMerger) {
            when (merger.type) {
                FileHolderMerger.Type.Storage -> getContext().getString(R.string.text_storage)
                FileHolderMerger.Type.PublicFolder -> getContext().getString(R.string.text_shortcuts)
                FileHolderMerger.Type.Folder -> getContext().getString(R.string.text_folder)
                FileHolderMerger.Type.PartFile -> getContext().getString(R.string.text_pendingTransfers)
                FileHolderMerger.Type.RecentFile -> getContext().getString(R.string.text_recentFiles)
                FileHolderMerger.Type.File -> getContext().getString(R.string.text_file)
                FileHolderMerger.Type.Dummy -> getContext().getString(R.string.text_unknown)
                else -> getContext().getString(R.string.text_unknown)
            }
        } else super.getRepresentativeText(merger)
    }

    fun goPath(path: DocumentFile?) {
        mPath = path
    }

    fun setConfiguration(showDirectories: Boolean, showFiles: Boolean, fileMatch: String?) {
        mShowDirectories = showDirectories
        mShowFiles = showFiles
        mSearchWord = fileMatch
    }

    class FileHolder : GroupShareable, DatabaseObject<Any?> {
        var file: DocumentFile? = null
        var transferItem: TransferItem? = null
        var requestCode = 0
        var type: Type? = null

        constructor() : super() {}
        constructor(viewType: Int, representativeText: String?) : super(viewType, representativeText) {}
        constructor(context: Context?, file: DocumentFile?) {
            initialize(file)
            calculate(context)
        }

        protected fun calculate(context: Context?) {
            if (file != null && AppConfig.EXT_FILE_PART == Files.getFileFormat(file!!.name)) {
                type = Type.Pending
                try {
                    val kuick = AppUtils.getKuick(context)
                    val data: ContentValues? = kuick.getFirstFromTable(
                        SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERITEM)
                            .setWhere(Kuick.Companion.FIELD_TRANSFERITEM_FILE + "=?", file!!.name)
                    )
                    if (data != null) {
                        transferItem = TransferItem()
                        transferItem!!.reconstruct(kuick.writableDatabase, kuick, data)
                        mimeType = transferItem!!.mimeType
                        friendlyName = transferItem!!.name
                    }
                } catch (ignored: Exception) {
                }
            }
        }

        override fun comparisonSupported(): Boolean {
            return getViewType() != GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON && super.comparisonSupported()
        }

        @DrawableRes
        fun getIconRes(): Int {
            return if (file == null) 0 else if (file!!.isDirectory) {
                when (getType()) {
                    Type.Storage -> R.drawable.ic_save_white_24dp
                    Type.SaveLocation -> R.drawable.ic_uprotocol
                    Type.Bookmarked, Type.Mounted -> R.drawable.ic_bookmark_white_24dp
                    else -> R.drawable.ic_folder_white_24dp
                }
            } else {
                if (Type.Pending == getType() && transferItem == null)
                    R.drawable.ic_block_white_24dp
                else
                    MimeIconUtils.loadMimeIcon(mimeType)
            }
        }

        fun getInfo(context: Context): String {
            return when (getType()) {
                Type.Storage -> context.getString(R.string.text_storage)
                Type.Mounted -> context.getString(R.string.text_mountedDirectory)
                Type.Bookmarked, Type.Folder, Type.Public -> if (file != null && file!!.isDirectory) {
                    val itemSize = file!!.listFiles().size
                    context.resources.getQuantityString(R.plurals.text_items, itemSize, itemSize)
                } else context.getString(R.string.text_unknown)
                Type.SaveLocation -> context.getString(R.string.text_defaultFolder)
                Type.Pending -> if (transferItem == null) context.getString(R.string.mesg_notValidTransfer) else String.format(
                    "%s / %s", com.genonbeta.android.framework.util.Files.sizeExpression(comparableSize, false),
                    com.genonbeta.android.framework.util.Files.sizeExpression(transferItem!!.comparableSize, false)
                )
                Type.Recent, Type.File -> com.genonbeta.android.framework.util.Files.sizeExpression(
                    comparableSize,
                    false
                )
                else -> context.getString(R.string.text_unknown)
            }
        }

        fun getType(): Type {
            if (type == null && file == null) type = Type.Dummy
            return if (type == null) if (file!!.isDirectory) Type.Folder else Type.File else type!!
        }

        override fun getRequestCode(): Int {
            return requestCode
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

        protected fun initialize(file: DocumentFile?) {
            initialize(0, file!!.name, file.name, file.type, file.getLastModified(), file.getLength(), file.uri)
            this.file = file
        }

        fun loadThumbnail(context: Context?, imageView: ImageView?): Boolean {
            if (file == null || file!!.isDirectory || Type.Pending == getType() || !mimeType.startsWith("image/") && !mimeType.startsWith(
                    "video/"
                )
            ) return false
            GlideApp.with(context!!)
                .load(file!!.uri)
                .error(MimeIconUtils.loadMimeIcon(mimeType))
                .override(160)
                .circleCrop()
                .into(imageView)
            return true
        }

        override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
            uri = Uri.parse(item.getAsString(Kuick.Companion.FIELD_FILEBOOKMARK_PATH))
            type = if (uri.toString().startsWith("file")) Type.Bookmarked else Type.Mounted
            try {
                initialize(com.genonbeta.android.framework.util.Files.fromUri(kuick.getContext(), uri))
                calculate(kuick.getContext())
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            friendlyName = item.getAsString(Kuick.Companion.FIELD_FILEBOOKMARK_TITLE)
        }

        override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any, listener: Progress.Listener) {}
        override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any, listener: Progress.Listener) {}
        override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any, listener: Progress.Listener) {}
        override fun setSelectableSelected(selected: Boolean): Boolean {
            when (getType()) {
                Type.Dummy, Type.Public, Type.Storage, Type.Mounted, Type.Bookmarked -> return false
            }
            return super.setSelectableSelected(selected)
        }

        enum class Type {
            Storage, Bookmarked, Mounted, Public, SaveLocation, Recent, Pending, Folder, File, Dummy
        }
    }

    private class FileHolderMerger(holder: FileHolder) : ComparableMerger<FileHolder?>() {
        lateinit var type: Type

        override fun equals(other: Any?): Boolean {
            return other is FileHolderMerger && other.type == type
        }

        override operator fun compareTo(other: ComparableMerger<FileHolder?>): Int {
            return if (other is FileHolderMerger)
                MathUtils.compare(other.type.ordinal.toLong(), type.ordinal.toLong())
            else 0
        }

        enum class Type {
            Storage, Folder, PublicFolder, RecentFile, PartFile, File, Dummy
        }

        init {
            type = when (holder.getType()) {
                FileHolder.Type.Mounted, FileHolder.Type.Storage -> Type.Storage
                FileHolder.Type.Public, FileHolder.Type.Bookmarked -> Type.PublicFolder
                FileHolder.Type.Pending -> Type.PartFile
                FileHolder.Type.Recent -> Type.RecentFile
                FileHolder.Type.Folder, FileHolder.Type.SaveLocation -> Type.Folder
                FileHolder.Type.File -> Type.File
                FileHolder.Type.Dummy -> Type.Dummy
            }
        }
    }

    companion object {
        val MODE_GROUP_BY_DEFAULT: Int = GroupEditableListAdapter.MODE_GROUP_BY_NOTHING + 1
        val MODE_GROUP_FOR_INBOX: Int = GroupEditableListAdapter.MODE_GROUP_BY_DATE
        const val REQUEST_CODE_MOUNT_FOLDER = 1
    }
}