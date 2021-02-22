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
package org.monora.uprotocol.client.android.adapter

import android.view.ViewGroup
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.adapter.FileListAdapter.FileHolder
import org.monora.uprotocol.client.android.app.IListingFragment
import org.monora.uprotocol.client.android.widget.ListingAdapter
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import org.monora.uprotocol.client.android.model.ContentModel

class FileListAdapter(
    fragment: IListingFragment<FileHolder, ViewHolder>,
) : ListingAdapter<FileHolder, RecyclerViewAdapter.ViewHolder>(fragment) {
    var path: DocumentFile? = null

    var showDirectories = true

    var showFiles = true

    private var showThumbnails = true

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(layoutInflater.inflate(R.layout.list_selection, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun onLoad(): MutableList<FileHolder> = ArrayList()

    class FileHolder : ContentModel {
        override fun canCopy(): Boolean = false

        override fun canMove(): Boolean = false

        override fun canSelect(): Boolean = false

        override fun canShare(): Boolean = false

        override fun canRemove(): Boolean = false

        override fun canRename(): Boolean = false

        override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
            TODO("Not yet implemented")
        }

        override fun dateCreated(): Long = 0

        override fun dateModified(): Long = 0

        override fun dateSupported(): Boolean = false

        override fun filter(charSequence: CharSequence): Boolean = false

        override fun id(): Long = 0

        override fun length(): Long = 0

        override fun lengthSupported(): Boolean = false

        override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
            TODO("Not yet implemented")
        }

        override fun name(): String = "FileHolder"

        override fun remove(operationBackend: OperationBackend): Boolean {
            TODO("Not yet implemented")
        }

        override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
            TODO("Not yet implemented")
        }

        override fun selected(): Boolean = false

        override fun select(selected: Boolean) {

        }
    }
    /*
       protected override fun onLoad(lister: GroupLister<FileHolder>) {
           showThumbnails = AppUtils.getDefaultPreferences(context).getBoolean("load_thumbnails", true)
           val path = path
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
               val saveDir = FileHolder(context, Files.getApplicationDirectory(context))
               saveDir.type = FileHolder.Type.SaveLocation
               lister.offerObliged(this, saveDir)

               val rootDir = File(".")
               if (rootDir.canRead()) {
                   val rootHolder = FileHolder(context, DocumentFile.fromFile(rootDir))
                   rootHolder.friendlyName = context.getString(R.string.text_fileRoot)
                   lister.offerObliged(this, rootHolder)
               }
               val referencedDirectoryList: MutableList<File> = ArrayList()
               when {
                   Build.VERSION.SDK_INT >= 21 -> {
                       referencedDirectoryList.addAll(listOf(*context.externalMediaDirs))
                   }
                   Build.VERSION.SDK_INT >= 19 -> {
                       referencedDirectoryList.addAll(listOf(*context.getExternalFilesDirs(null)))
                   }
                   else -> {
                       referencedDirectoryList.add(Environment.getExternalStorageDirectory())
                   }
               }

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

       override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
           val holder = ViewHolder(layoutInflater.inflate(R.layout.list_file, parent, false))

           fragment.registerLayoutViewClicks(holder)
           holder.itemView.findViewById<View>(R.id.layout_image).setOnClickListener { v: View? ->
               fragment.setItemSelected(holder, true)
           }
           holder.itemView.findViewById<View>(R.id.menu).setOnClickListener { v: View? ->
               val fileHolder: FileHolder = getList()[holder.adapterPosition]
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
           return holder
       }

       override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
           val itemHolder: FileHolder = getItem(position)
           val parentView: View = holder.itemView
           val lookAltered = !showFiles || !showDirectories
           val thumbnail = parentView.findViewById<ImageView>(R.id.thumbnail)
           val image = parentView.findViewById<ImageView>(R.id.image)
           val text1: TextView = parentView.findViewById(R.id.text)
           val text2: TextView = parentView.findViewById(R.id.text2)
           holder.setSelected(itemHolder.selected())
           text1.setText(itemHolder.name())
           text2.text = itemHolder.getInfo(context)
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

   /*
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
   }*/
       class FileHolder : ContentModel, DatabaseObject<Any?> {
           lateinit var file: DocumentFile

           var transferItem: TransferItem? = null

           var type: Type = Type.Dummy
               get() {
                   val file = file
                   return if (field == Type.Dummy && file != null) {
                       if (file.isDirectory()) Type.Folder else Type.File
                   } else field
               }
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
                       MimeIcons.loadMimeIcon(mimeType)
               }
           }

           fun getInfo(context: Context): String {
               val fileLocal = file
               return when (type) {
                   Type.Storage -> context.getString(R.string.text_storage)
                   Type.Mounted -> context.getString(R.string.text_mountedDirectory)
                   Type.Bookmarked, Type.Folder, Type.Public -> if (fileLocal != null && fileLocal.isDirectory()) {
                       val itemSize = fileLocal.listFiles().size
                       context.resources.getQuantityString(R.plurals.text_items, itemSize, itemSize)
                   } else context.getString(R.string.text_unknown)
                   Type.SaveLocation -> context.getString(R.string.text_defaultFolder)
                   Type.Pending -> if (transferItem == null) context.getString(R.string.mesg_notValidTransfer) else String.format(
                       "%s / %s", com.genonbeta.android.framework.util.Files.formatLength(getComparableSize(), false),
                       com.genonbeta.android.framework.util.Files.formatLength(transferItem!!.getComparableSize(), false)
                   )
                   Type.Recent, Type.File -> com.genonbeta.android.framework.util.Files.formatLength(
                       getComparableSize(),
                       false
                   )
                   else -> context.getString(R.string.text_unknown)
               }
           }

           override fun getValues(): ContentValues {
               val contentValues = ContentValues()
               contentValues.put(Kuick.FIELD_FILEBOOKMARK_TITLE, name())
               contentValues.put(Kuick.FIELD_FILEBOOKMARK_PATH, uri.toString())
               return contentValues
           }

           override fun getWhere(): SQLQuery.Select {
               return SQLQuery.Select(Kuick.TABLE_FILEBOOKMARK)
                   .setWhere(String.format("%s = ?", Kuick.FIELD_FILEBOOKMARK_PATH), uri.toString())
           }

           fun loadThumbnail(context: Context, imageView: ImageView): Boolean {
               val file = file*/
    //val mimeType = mimeType ?: "*/*"
/*
            if (file == null || file.isDirectory() || Type.Pending == type || !mimeType.startsWith("image/")
                && !mimeType.startsWith("video/")
            ) return false
            GlideApp.with(context)
                .load(file.getUri())
                .error(MimeIcons.loadMimeIcon(mimeType))
                .override(160)
                .circleCrop()
                .into(imageView)
            return true
        }

        override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, values: ContentValues) {
            uri = Uri.parse(values.getAsString(Kuick.FIELD_FILEBOOKMARK_PATH))
            type = if (uri.toString().startsWith("file")) Type.Bookmarked else Type.Mounted
            try {
                initialize(com.genonbeta.android.framework.util.Files.fromUri(kuick.context, uri))
                calculate(kuick.context)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
            friendlyName = values.getAsString(Kuick.FIELD_FILEBOOKMARK_TITLE)
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
        const val REQUEST_CODE_MOUNT_FOLDER = 1
    }*/
}