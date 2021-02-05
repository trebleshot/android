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
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import android.net.Uri
import android.os.*
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.util.FileUtils
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.MathUtils
import com.genonbeta.android.framework.util.listing.Merger
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.Exception
import java.lang.StringBuilder
import java.util.*

class FileListAdapter(fragment: IEditableListFragment<FileHolder?, GroupViewHolder?>?) :
    GroupEditableListAdapter<FileHolder?, GroupViewHolder?>(fragment, MODE_GROUP_BY_DEFAULT),
    CustomGroupLister<FileHolder?> {
    private var mShowDirectories = true
    private var mShowFiles = true
    private var mShowThumbnails = true
    private var mSearchWord: String? = null
    private var mPath: DocumentFile? = null
    protected override fun onLoad(lister: GroupLister<FileHolder>) {
        mShowThumbnails = AppUtils.getDefaultPreferences(getContext())!!.getBoolean("load_thumbnails", true)
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
                val saveDir = FileHolder(getContext(), FileUtils.getApplicationDirectory(getContext()))
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
                    GroupEditableListAdapter.Companion.VIEW_TYPE_ACTION_BUTTON,
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
                        val documentFile = FileUtils.getIncomingPseudoFile(
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
        return FileHolder(GroupEditableListAdapter.Companion.VIEW_TYPE_REPRESENTATIVE, text)
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
        if (viewType == GroupEditableListAdapter.Companion.VIEW_TYPE_ACTION_BUTTON) getFragment().registerLayoutViewClicks(
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
                    FileHolder.Type.SaveLocation == fileHolder.getType() || fileHolder.file != null && (FileUtils.getApplicationDirectory(
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
                        FileListFragment.Companion.shortcutItem<FileHolder>(getFragment(), fileHolder)
                    } else if (id == R.id.action_mode_file_change_save_path) {
                        getContext().startActivity(Intent(getContext(), ChangeStoragePathActivity::class.java))
                    } else if (getFragment() is FileListFragment) return@setOnMenuItemClickListener !FileListFragment.Companion.handleEditingAction(
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
        val `object`: FileHolder = getItem(position)
        if (!holder.tryBinding(`object`)) {
            val parentView: View = holder.itemView
            val lookAltered = !mShowFiles || !mShowDirectories
            val thumbnail = parentView.findViewById<ImageView>(R.id.thumbnail)
            val image = parentView.findViewById<ImageView>(R.id.image)
            val text1: TextView = parentView.findViewById<TextView>(R.id.text)
            val text2: TextView = parentView.findViewById<TextView>(R.id.text2)
            holder.setSelected(`object`.isSelectableSelected)
            text1.setText(`object`.friendlyName)
            text2.setText(`object`.getInfo(getContext()))
            if (lookAltered) {
                val enabled = (`object`.file == null || mShowFiles && `object`.file!!.isFile
                        || mShowDirectories && `object`.file!!.isDirectory)
                text1.setEnabled(enabled)
                text2.setEnabled(enabled)
                image.alpha = if (enabled) 1f else 0.5f
                thumbnail.alpha = if (enabled) 1f else 0.5f
            }
            if (!mShowThumbnails || !`object`.loadThumbnail(getContext(), thumbnail)) {
                image.setImageResource(`object`.getIconRes())
                thumbnail.setImageDrawable(null)
            } else image.setImageDrawable(null)
        } else if (holder.getItemViewType() == GroupEditableListAdapter.Companion.VIEW_TYPE_ACTION_BUTTON) (holder.itemView.findViewById<View>(
            R.id.icon
        ) as ImageView).setImageResource(`object`.getIconRes())
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
        return if (mPath != null && mPath == FileUtils.getApplicationDirectory(getContext())) MODE_GROUP_FOR_INBOX else super.getGroupBy()
    }

    override fun getSortingCriteria(objectOne: FileHolder, objectTwo: FileHolder): Int {
        // Checking whether the path is null helps to increase the speed.
        return if (getPath() == null && FileHolder.Type.Recent == objectOne.getType() && FileHolder.Type.Recent == objectTwo.getType()) EditableListAdapter.Companion.MODE_SORT_BY_DATE else super.getSortingCriteria(
            objectOne,
            objectTwo
        )
    }

    override fun getSortingOrder(objectOne: FileHolder, objectTwo: FileHolder): Int {
        // Checking whether the path is null helps to increase the speed.
        return if (getPath() == null && FileHolder.Type.Recent == objectOne.getType() && FileHolder.Type.Recent == objectTwo.getType()) EditableListAdapter.Companion.MODE_SORT_ORDER_DESCENDING else super.getSortingOrder(
            objectOne,
            objectTwo
        )
    }

    fun getPath(): DocumentFile? {
        return mPath
    }

    fun goPath(path: File?) {
        goPath(DocumentFile.fromFile(path))
    }

    override fun getRepresentativeText(merger: Merger<out FileHolder>): String {
        return if (merger is FileHolderMerger) {
            when ((merger as FileHolderMerger).type) {
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
            if (file != null && AppConfig.EXT_FILE_PART == FileUtils.getFileFormat(file!!.name)) {
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
            return getViewType() != GroupEditableListAdapter.Companion.VIEW_TYPE_ACTION_BUTTON && super.comparisonSupported()
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
                if (Type.Pending == getType() && transferItem == null) R.drawable.ic_block_white_24dp else MimeIconUtils.loadMimeIcon(
                    mimeType
                )
            }
        }

        override fun getId(): Long {
            if (super.id == 0L && file != null) setId(
                String.format("%s_%s", file!!.uri.toString(), getType()).hashCode().toLong()
            )
            return super.id
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
                    "%s / %s", com.genonbeta.android.framework.util.FileUtils.sizeExpression(comparableSize, false),
                    com.genonbeta.android.framework.util.FileUtils.sizeExpression(transferItem!!.comparableSize, false)
                )
                Type.Recent, Type.File -> com.genonbeta.android.framework.util.FileUtils.sizeExpression(
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
            contentValues.put(Kuick.Companion.FIELD_FILEBOOKMARK_TITLE, friendlyName)
            contentValues.put(Kuick.Companion.FIELD_FILEBOOKMARK_PATH, uri.toString())
            return contentValues
        }

        override fun getWhere(): SQLQuery.Select {
            return SQLQuery.Select(Kuick.Companion.TABLE_FILEBOOKMARK).setWhere(
                String.format(
                    "%s = ?",
                    Kuick.Companion.FIELD_FILEBOOKMARK_PATH
                ), uri.toString()
            )
        }

        protected fun initialize(file: DocumentFile?) {
            initialize(
                0, file!!.name, file.name, file.type, file.lastModified(), file.length(),
                file.uri
            )
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
                initialize(com.genonbeta.android.framework.util.FileUtils.fromUri(kuick.getContext(), uri))
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
        var type: Type? = null
        override fun equals(obj: Any?): Boolean {
            return obj is FileHolderMerger && obj.type == type
        }

        override operator fun compareTo(o: ComparableMerger<FileHolder?>): Int {
            return if (o is FileHolderMerger) MathUtils.compare(
                (o as FileHolderMerger).type!!.ordinal.toLong(),
                type!!.ordinal.toLong()
            ) else 0
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
                else -> Type.Dummy
            }
        }
    }

    companion object {
        val MODE_GROUP_BY_DEFAULT: Int = GroupEditableListAdapter.Companion.MODE_GROUP_BY_NOTHING + 1
        val MODE_GROUP_FOR_INBOX: Int = GroupEditableListAdapter.Companion.MODE_GROUP_BY_DATE
        const val REQUEST_CODE_MOUNT_FOLDER = 1
    }
}