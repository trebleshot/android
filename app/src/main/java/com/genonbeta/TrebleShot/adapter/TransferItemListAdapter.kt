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

import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.system.Os
import android.system.StructStatVfs
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.collection.ArrayMap
import androidx.core.content.ContextCompat.getColor
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.ImageViewCompat
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.TransferItemListAdapter.GenericItem
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.LoadedMember
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.AppUtils.getReference
import com.genonbeta.TrebleShot.util.MimeIcons
import com.genonbeta.TrebleShot.util.TextUtils
import com.genonbeta.TrebleShot.util.Transfers
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupLister.CustomGroupLister
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.database.exception.ReconstructionFailedException
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.TreeDocumentFile
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.MathUtils
import com.genonbeta.android.framework.util.listing.ComparableMerger
import com.genonbeta.android.framework.util.listing.Merger
import java.io.File
import java.text.NumberFormat
import java.util.*

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */
class TransferItemListAdapter(
    fragment: IEditableListFragment<GenericItem, GroupViewHolder>,
) : GroupEditableListAdapter<GenericItem, GroupViewHolder>(fragment, MODE_GROUP_BY_DEFAULT),
    CustomGroupLister<GenericItem> {
    var select: SQLQuery.Select = SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)

    var path: String? = null
        set(value) {
            field = value
            pathChangedListener?.onPathChange(value)
        }

    var member: LoadedMember? = null
        set(value) {
            if (value != null) {
                try {
                    AppUtils.getKuick(context).reconstruct(value)
                    field = value
                } catch (ignored: ReconstructionFailedException) {
                }
            } else
                field = null
        }

    var pathChangedListener: PathChangedListener? = null

    private val percentFormat = NumberFormat.getPercentInstance()

    private val mTransfer = Transfer()

    @ColorInt
    private val colorPending: Int = getColor(context, getReference(context, R.attr.colorControlNormal))
    private val colorDone: Int = getColor(context, getReference(context, R.attr.colorAccent))
    private val colorError: Int = getColor(context, getReference(context, R.attr.colorError))

    protected override fun onLoad(lister: GroupLister<GenericItem>) {
        val loadThumbnails = AppUtils.getDefaultPreferences(context)
            .getBoolean("load_thumbnails", true)
        try {
            AppUtils.getKuick(context).reconstruct(mTransfer)
        } catch (e: ReconstructionFailedException) {
            e.printStackTrace()
            return
        }
        var hasIncoming = false
        val currentPath = path?.let { if (it.isEmpty()) null else it }
        val folders: MutableMap<String, TransferFolder> = ArrayMap()
        val member = member
        val members = Transfers.loadMemberList(context, getGroupId(), null)
        val memberArray = members.toTypedArray()
        val transferSelect = SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
        val transferWhere = StringBuilder(Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=?")
        val transferArgs: MutableList<String> = ArrayList()
        transferArgs.add(mTransfer.id.toString())
        if (currentPath != null) {
            transferWhere.append(
                " AND (" + Kuick.FIELD_TRANSFERITEM_DIRECTORY + "=? OR "
                        + Kuick.FIELD_TRANSFERITEM_DIRECTORY + " LIKE ?)"
            )
            transferArgs.add(currentPath)
            transferArgs.add(currentPath + File.separator + "%")
        }
        if (member != null) {
            transferWhere.append(" AND " + Kuick.FIELD_TRANSFERITEM_TYPE + "=?")
            transferArgs.add(member.type.toString())
        }
        if (sortingCriteria == MODE_GROUP_BY_DATE) {
            transferSelect.setOrderBy(
                Kuick.FIELD_TRANSFERITEM_LASTCHANGETIME + " "
                        + if (sortingOrder == MODE_SORT_ORDER_ASCENDING) "ASC" else "DESC"
            )
        }
        transferSelect.where = transferWhere.toString()
        transferSelect.whereArgs = transferArgs.toTypedArray()
        val statusItem = DetailsTransferFolder(
            mTransfer.id,
            if (currentPath == null) {
                member?.device?.username ?: context.getString(R.string.text_home)
            } else if (currentPath.contains(File.separator)) {
                currentPath.substring(currentPath.lastIndexOf(File.separator) + 1)
            } else currentPath, currentPath ?: "Empty"
        )
        lister.offerObliged(this, statusItem)
        val derivedList = AppUtils.getKuick(context).castQuery(
            transferSelect, GenericTransferItem::class.java
        )

        // we first get the default files
        for (item in derivedList) {
            item.members = memberArray
            item.directory =
                if (item.directory == null || item.directory!!.length == 0) null else item.directory
            if (currentPath != null && item.directory == null) continue
            var transferFolder: TransferFolder? = null
            val isIncoming = TransferItem.Type.INCOMING == item.type
            val isOutgoing = TransferItem.Type.OUTGOING == item.type
            if (currentPath == null && item.directory == null || item.directory == currentPath) {
                try {
                    if (!loadThumbnails) item.supportThumbnail = false else {
                        val format = item.mimeType.split(File.separator.toRegex()).toTypedArray()
                        if (format.size > 0 && ("image" == format[0] || "video" == format[0])) {
                            var documentFile: DocumentFile? = null
                            if (isOutgoing) documentFile = Files.fromUri(
                                context,
                                Uri.parse(item.file)
                            ) else if (TransferItem.Flag.DONE == item.flag) documentFile =
                                com.genonbeta.TrebleShot.util.Files.getIncomingPseudoFile(
                                    context, item, mTransfer,
                                    false
                                )
                            if (documentFile != null && documentFile.exists()) {
                                item.documentFile = documentFile
                                item.supportThumbnail = true
                            }
                        } else item.supportThumbnail = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                lister.offerObliged(this, item)
            } else if (currentPath == null || item.directory!!.startsWith(currentPath)) {
                val pathToErase = if (currentPath == null) 0 else currentPath.length + File.separator.length
                var cleanedPath = item.directory!!.substring(pathToErase)
                val slashPos = cleanedPath.indexOf(File.separator)
                if (slashPos != -1) cleanedPath = cleanedPath.substring(0, slashPos)
                transferFolder = folders[cleanedPath]
                if (transferFolder == null) {
                    transferFolder = TransferFolder(
                        mTransfer.id,
                        cleanedPath,
                        if (currentPath != null) currentPath + File.separator + cleanedPath else cleanedPath
                    )
                    folders[cleanedPath] = transferFolder
                    lister.offerObliged(this, transferFolder)
                }
            }
            if (!hasIncoming && isIncoming) hasIncoming = true
            mergeTransferInfo(statusItem, item, isIncoming, transferFolder)
        }
        if (currentPath == null && hasIncoming) try {
            val transfer = Transfer(mTransfer.id)
            AppUtils.getKuick(context).reconstruct(transfer)
            val savePath = com.genonbeta.TrebleShot.util.Files.getSavePath(context, transfer)
            val storageItem = StorageStatusItem()
            storageItem.directory = savePath.getUri().toString()
            storageItem.name = savePath.getName()
            storageItem.bytesRequired = statusItem.bytesTotal - statusItem.bytesReceived
            if (savePath is LocalDocumentFile) {
                val saveFile: File = savePath.file
                storageItem.bytesTotal = saveFile.totalSpace
                storageItem.bytesFree = saveFile.freeSpace // return used space
            } else if (Build.VERSION.SDK_INT >= 21 && savePath is TreeDocumentFile) {
                try {
                    val descriptor = context.contentResolver.openFileDescriptor(savePath.originalUri, "r")
                    if (descriptor != null) {
                        val stats: StructStatVfs = Os.fstatvfs(descriptor.getFileDescriptor())
                        storageItem.bytesTotal = stats.f_blocks * stats.f_bsize
                        storageItem.bytesFree = stats.f_bavail * stats.f_bsize
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                storageItem.bytesTotal = -1
                storageItem.bytesFree = -1
            }
            lister.offerObliged(this, storageItem)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    protected override fun onGenerateRepresentative(text: String, merger: Merger<GenericItem>?): GenericTransferItem {
        return GenericTransferItem(text)
    }

    override fun onCustomGroupListing(lister: GroupLister<GenericItem>, mode: Int, holder: GenericItem): Boolean {
        if (mode == MODE_GROUP_BY_DEFAULT) lister.offer(
            holder,
            GroupEditableTransferObjectMerger(holder, this)
        ) else return false
        return true
    }

    override fun createLister(loadedList: MutableList<GenericItem>, groupBy: Int): GroupLister<GenericItem> {
        return super.createLister(loadedList, groupBy).also { it.customLister = this }
    }

    fun getDeviceId(): String? {
        return member?.deviceId
    }

    fun mergeTransferInfo(
        details: DetailsTransferFolder, item: GenericTransferItem, isIncoming: Boolean,
        folder: TransferFolder?,
    ) {
        if (isIncoming) {
            mergeTransferInfo(details, item, item.flag, true, folder)
        } else {
            if (member != null) mergeTransferInfo(
                details,
                item,
                item.getFlag(getDeviceId()),
                false,
                folder
            ) else if (item.members.isEmpty()) mergeTransferInfo(
                details,
                item,
                TransferItem.Flag.PENDING,
                false,
                folder
            ) else for (loadedMember in item.members) {
                if (TransferItem.Type.OUTGOING != loadedMember.type) continue
                mergeTransferInfo(
                    details, item, item.getFlag(loadedMember.deviceId),
                    false, folder
                )
            }
        }
    }

    fun mergeTransferInfo(
        details: DetailsTransferFolder, item: TransferItem, flag: TransferItem.Flag,
        isIncoming: Boolean, folder: TransferFolder?,
    ) {
        details.bytesTotal += item.getComparableSize()
        details.numberOfTotal++
        if (folder != null) {
            folder.bytesTotal += item.getComparableSize()
            folder.numberOfTotal++
        }
        if (TransferItem.Flag.DONE == flag) {
            details.numberOfCompleted++
            details.bytesCompleted += item.getComparableSize()
            if (folder != null) {
                folder.numberOfCompleted++
                folder.bytesCompleted += item.getComparableSize()
            }
        } else if (Transfers.isError(flag)) {
            details.hasIssues = true
            if (folder != null) folder.hasIssues = true
        } else if (TransferItem.Flag.IN_PROGRESS == flag) {
            val completed = flag.bytesValue
            details.bytesCompleted += completed
            details.hasOngoing = true
            if (folder != null) {
                folder.bytesCompleted += completed
                folder.hasOngoing = true
            }
            if (isIncoming) {
                details.bytesReceived += completed
                if (folder != null) folder.bytesReceived += completed
            }
        }
    }

    fun getGroupId(): Long {
        return mTransfer.id
    }

    fun setTransferId(transferId: Long) {
        mTransfer.id = transferId
    }

    private fun getPercentFormat(): NumberFormat {
        return percentFormat
    }

    override fun getRepresentativeText(merger: Merger<out GenericItem>): String {
        return if (merger is GroupEditableTransferObjectMerger) {
            when (merger.type) {
                GroupEditableTransferObjectMerger.Type.STATUS -> context.getString(R.string.text_transactionDetails)
                GroupEditableTransferObjectMerger.Type.FOLDER -> context.getString(R.string.text_folder)
                GroupEditableTransferObjectMerger.Type.FILE_ERROR -> context.getString(R.string.text_flagInterrupted)
                GroupEditableTransferObjectMerger.Type.FOLDER_ONGOING, GroupEditableTransferObjectMerger.Type.FILE_ONGOING -> context.getString(
                    R.string.text_taskOngoing
                )
                else -> context.getString(R.string.text_file)
            }
        } else super.getRepresentativeText(merger)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == VIEW_TYPE_DEFAULT) GroupViewHolder(
            layoutInflater.inflate(R.layout.list_transfer_item, parent, false)
        ) else createDefaultViews(
            parent, viewType,
            false
        )
        if (!holder.isRepresentative()) {
            fragment.registerLayoutViewClicks(holder)
            holder.itemView.findViewById<View>(R.id.layout_image).setOnClickListener { v: View? ->
                fragment.setItemSelected(holder, true)
            }
        }
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        try {
            val item: GenericItem = getItem(position)
            if (!holder.tryBinding(item)) {
                val parentView: View = holder.itemView
                @ColorInt val appliedColor: Int
                val percentage = (item.getPercentage(this) * 100).toInt()
                val progressBar = parentView.findViewById<ProgressBar>(R.id.progressBar)
                val thumbnail = parentView.findViewById<ImageView>(R.id.thumbnail)
                val image = parentView.findViewById<ImageView>(R.id.image)
                val indicator = parentView.findViewById<ImageView>(R.id.indicator)
                val sIcon = parentView.findViewById<ImageView>(R.id.statusIcon)
                val titleText: TextView = parentView.findViewById(R.id.text)
                val firstText: TextView = parentView.findViewById(R.id.text2)
                val secondText: TextView = parentView.findViewById(R.id.text3)
                val thirdText: TextView = parentView.findViewById(R.id.text4)
                parentView.isSelected = item.isSelectableSelected()
                appliedColor =
                    if (item.hasIssues(this)) colorError else if (item.isComplete(this)) colorDone else colorPending
                titleText.setText(item.name)
                firstText.setText(item.getFirstText(this))
                secondText.setText(item.getSecondText(this))
                thirdText.setText(item.getThirdText(this))
                item.handleStatusIcon(sIcon, mTransfer)
                item.handleStatusIndicator(indicator)
                ImageViewCompat.setImageTintList(sIcon, ColorStateList.valueOf(appliedColor))
                progressBar.max = 100
                if (Build.VERSION.SDK_INT >= 24) progressBar.setProgress(
                    if (percentage <= 0) 1 else percentage,
                    true
                ) else progressBar.progress = if (percentage <= 0) 1 else percentage
                thirdText.setTextColor(appliedColor)
                ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(appliedColor))
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    val wrapDrawable: Drawable = DrawableCompat.wrap(progressBar.progressDrawable)
                    DrawableCompat.setTint(wrapDrawable, appliedColor)
                    progressBar.progressDrawable = DrawableCompat.unwrap(wrapDrawable)
                } else progressBar.progressTintList = ColorStateList.valueOf(appliedColor)
                val supportThumbnail = item.loadThumbnail(thumbnail)
                progressBar.visibility =
                    if (!supportThumbnail || !item.isComplete(this)) View.VISIBLE else View.GONE
                if (supportThumbnail) image.setImageDrawable(null) else {
                    image.setImageResource(item.getIconRes())
                    thumbnail.setImageDrawable(null)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    interface PathChangedListener {
        fun onPathChange(path: String?)
    }

    internal interface StatusItem

    abstract class GenericItem : TransferItem, GroupEditable {
        private var viewType = 0

        private lateinit var representativeText: String

        override var requestCode: Int = 0

        override var size: Long
            get() = length
            set(value) {
                length = size
            }

        constructor()

        constructor(representativeText: String) {
            viewType = VIEW_TYPE_REPRESENTATIVE
            setRepresentativeText(representativeText)
        }

        override fun applyFilter(filteringKeywords: Array<String>): Boolean {
            for (keyword in filteringKeywords) if (name != null && name.toLowerCase()
                    .contains(keyword.toLowerCase())
            ) return true
            return false
        }

        @DrawableRes
        abstract fun getIconRes(): Int

        abstract fun loadThumbnail(imageView: ImageView): Boolean

        abstract fun getPercentage(adapter: TransferItemListAdapter): Double

        abstract fun hasIssues(adapter: TransferItemListAdapter): Boolean

        abstract fun isComplete(adapter: TransferItemListAdapter): Boolean

        abstract fun isOngoing(adapter: TransferItemListAdapter): Boolean

        abstract fun handleStatusIcon(imageView: ImageView, transfer: Transfer)

        abstract fun handleStatusIndicator(imageView: ImageView)

        abstract fun getFirstText(adapter: TransferItemListAdapter): String?

        abstract fun getSecondText(adapter: TransferItemListAdapter): String

        abstract fun getThirdText(adapter: TransferItemListAdapter): String?

        override fun getViewType(): Int {
            return viewType
        }

        override fun getRepresentativeText(): String {
            return representativeText
        }

        override fun setRepresentativeText(text: CharSequence) {
            representativeText = text.toString()
        }

        override fun isGroupRepresentative(): Boolean {
            return viewType == VIEW_TYPE_REPRESENTATIVE
        }

        override fun setSelectableSelected(selected: Boolean): Boolean {
            return !isGroupRepresentative() && super.setSelectableSelected(selected)
        }
    }

    class GenericTransferItem : GenericItem {
        var documentFile: DocumentFile? = null

        lateinit var members: Array<LoadedMember>

        var supportThumbnail = false

        constructor()

        internal constructor(representativeText: String) : super(representativeText)

        override fun applyFilter(filteringKeywords: Array<String>): Boolean {
            if (super.applyFilter(filteringKeywords)) return true
            for (keyword in filteringKeywords) if (mimeType.toLowerCase().contains(keyword.toLowerCase())) return true
            return false
        }

        override fun getIconRes(): Int {
            return MimeIcons.loadMimeIcon(mimeType)
        }

        override fun handleStatusIcon(imageView: ImageView, transfer: Transfer) {
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(if (Type.INCOMING == type) R.drawable.ic_arrow_down_white_24dp else R.drawable.ic_arrow_up_white_24dp)
        }

        override fun handleStatusIndicator(imageView: ImageView) {
            imageView.visibility = View.GONE
        }

        override fun getFirstText(adapter: TransferItemListAdapter): String {
            return Files.sizeExpression(getComparableSize(), false)
        }

        override fun getSecondText(adapter: TransferItemListAdapter): String {
            val member = adapter.member
            if (member != null) return member.device.username
            var totalDevices = 1
            if (Type.OUTGOING == type) synchronized(senderFlagListInternal) { totalDevices = senderFlagList.size }
            return adapter.context.resources.getQuantityString(
                R.plurals.text_devices,
                totalDevices, totalDevices
            )
        }

        override fun getThirdText(adapter: TransferItemListAdapter): String {
            return TextUtils.getTransactionFlagString(
                adapter.context, this,
                adapter.getPercentFormat(), adapter.getDeviceId()
            )
        }

        override fun loadThumbnail(imageView: ImageView): Boolean {
            val localDocumentFile = documentFile

            if (localDocumentFile != null && supportThumbnail && localDocumentFile.exists()) {
                GlideApp.with(imageView.context)
                    .load(localDocumentFile.getUri())
                    .error(getIconRes())
                    .override(160)
                    .circleCrop()
                    .into(imageView)
                return true
            }
            return false
        }

        override fun getPercentage(adapter: TransferItemListAdapter): Double {
            return getPercentage(members, adapter.getDeviceId())
        }

        override fun hasIssues(adapter: TransferItemListAdapter): Boolean {
            if (members.isEmpty()) return false
            if (Type.INCOMING == type) return Transfers.isError(flag) else if (adapter.getDeviceId() != null) {
                return Transfers.isError(getFlag(adapter.getDeviceId()))
            } else synchronized(senderFlagListInternal) { for (member in members) if (Transfers.isError(getFlag(member.deviceId))) return true }
            return false
        }

        override fun isComplete(adapter: TransferItemListAdapter): Boolean {
            if (members.size == 0) return false
            if (Type.INCOMING == type) return Flag.DONE == flag else if (adapter.getDeviceId() != null) {
                return Flag.DONE == getFlag(adapter.getDeviceId())
            } else synchronized(senderFlagListInternal) { for (member in members) if (Flag.DONE != getFlag(member.deviceId)) return false }
            return true
        }

        override fun isOngoing(adapter: TransferItemListAdapter): Boolean {
            if (members.size == 0) return false
            if (Type.INCOMING == type) return Flag.IN_PROGRESS == flag else if (adapter.getDeviceId() != null) {
                return Flag.IN_PROGRESS == getFlag(adapter.getDeviceId())
            } else synchronized(senderFlagListInternal) { for (member in members) if (Flag.IN_PROGRESS == getFlag(member.deviceId)) return true }
            return false
        }
    }

    open class TransferFolder internal constructor(
        transferId: Long, friendlyName: String, var directoryLocal: String,
    ) : GenericItem() {
        override var directory: String? = directoryLocal

        var hasIssues = false

        var hasOngoing = false

        var numberOfTotal = 0

        var numberOfCompleted = 0

        var bytesTotal: Long = 0

        var bytesCompleted: Long = 0

        var bytesReceived: Long = 0

        override var id: Long
            get() = directory.hashCode().toLong()
            set(value) {}

        override fun equals(other: Any?): Boolean {
            return other is TransferFolder && directory != null && directory == other.directory
        }

        override fun getComparableSize(): Long {
            return bytesTotal
        }

        override fun getIconRes(): Int {
            return R.drawable.ic_folder_white_24dp
        }

        override fun getFirstText(adapter: TransferItemListAdapter): String? {
            return Files.sizeExpression(bytesTotal, false)
        }

        override fun getSecondText(adapter: TransferItemListAdapter): String {
            return adapter.context
                .getString(R.string.text_transferStatusFiles, numberOfCompleted, numberOfTotal)
        }

        override fun getThirdText(adapter: TransferItemListAdapter): String? {
            return adapter.getPercentFormat().format(getPercentage(adapter))
        }

        override fun getWhere(): SQLQuery.Select {
            return SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                .setWhere(
                    Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=? AND ("
                            + Kuick.FIELD_TRANSFERITEM_DIRECTORY + " LIKE ? OR "
                            + Kuick.FIELD_TRANSFERITEM_DIRECTORY + " = ?)",
                    transferId.toString(),
                    directory + File.separator + "%",
                    directoryLocal
                )
        }

        override fun handleStatusIcon(imageView: ImageView, transfer: Transfer) {
            imageView.visibility = View.GONE
        }

        override fun handleStatusIndicator(imageView: ImageView) {
            imageView.visibility = View.GONE
        }

        override fun hasIssues(adapter: TransferItemListAdapter): Boolean {
            return hasIssues
        }

        override fun loadThumbnail(imageView: ImageView): Boolean {
            return false
        }

        override fun getPercentage(adapter: TransferItemListAdapter): Double {
            return if (bytesTotal == 0L || bytesCompleted == 0L) 0.0 else (bytesCompleted.toFloat() / bytesTotal).toDouble()
        }

        override fun isComplete(adapter: TransferItemListAdapter): Boolean {
            return numberOfTotal == numberOfCompleted && numberOfTotal != 0
        }

        override fun isOngoing(adapter: TransferItemListAdapter): Boolean {
            return hasOngoing
        }

        init {
            this.transferId = transferId
            name = friendlyName
        }
    }

    class DetailsTransferFolder internal constructor(transferId: Long, friendlyName: String, directory: String) :
        TransferFolder(transferId, friendlyName, directory), StatusItem {
        override var id: Long = directoryLocal.hashCode().toLong()

        override fun handleStatusIcon(imageView: ImageView, transfer: Transfer) {
            if (transfer.isServedOnWeb) {
                imageView.visibility = View.VISIBLE
                imageView.setImageResource(R.drawable.ic_web_white_24dp)
            } else super.handleStatusIcon(imageView, transfer)
        }

        override fun handleStatusIndicator(imageView: ImageView) {
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(R.drawable.ic_arrow_right_white_24dp)
        }

        override fun getIconRes(): Int {
            return R.drawable.ic_device_hub_white_24dp
        }

        override fun isSelectableSelected(): Boolean {
            return false
        }

        override fun setSelectableSelected(selected: Boolean): Boolean {
            return false
        }
    }

    class StorageStatusItem : GenericItem(), StatusItem {
        override var id: Long = (if (directory != null) directory else name).hashCode().toLong()

        var bytesTotal: Long = 0

        var bytesFree: Long = 0

        var bytesRequired: Long = 0

        override fun hasIssues(adapter: TransferItemListAdapter): Boolean {
            return bytesFree < bytesRequired && bytesFree != -1L
        }

        override fun isComplete(adapter: TransferItemListAdapter): Boolean {
            return bytesFree == -1L || !hasIssues(adapter)
        }

        override fun isOngoing(adapter: TransferItemListAdapter): Boolean {
            return false
        }

        override fun isSelectableSelected(): Boolean {
            return false
        }

        override fun getIconRes(): Int {
            return R.drawable.ic_save_white_24dp
        }

        override fun getPercentage(adapter: TransferItemListAdapter): Double {
            return if (bytesTotal <= 0 || bytesFree <= 0) 0.0 else java.lang.Long.valueOf(bytesTotal - bytesFree)
                .toDouble() / java.lang.Long.valueOf(bytesTotal).toDouble()
        }

        override fun handleStatusIcon(imageView: ImageView, transfer: Transfer) {
            imageView.visibility = View.GONE
        }

        override fun handleStatusIndicator(imageView: ImageView) {
            imageView.visibility = View.VISIBLE
            imageView.setImageResource(R.drawable.ic_arrow_right_white_24dp)
        }

        override fun getFirstText(adapter: TransferItemListAdapter): String {
            return if (bytesFree == -1L) adapter.context
                .getString(R.string.text_unknown) else Files.sizeExpression(bytesFree, false)
        }

        override fun getSecondText(adapter: TransferItemListAdapter): String {
            return adapter.context.getString(R.string.text_savePath)
        }

        override fun getThirdText(adapter: TransferItemListAdapter): String? {
            return adapter.getPercentFormat().format(getPercentage(adapter))
        }

        override fun loadThumbnail(imageView: ImageView): Boolean {
            return false
        }

        override fun setSelectableSelected(selected: Boolean): Boolean {
            return false
        }
    }

    class GroupEditableTransferObjectMerger internal constructor(
        holder: GenericItem,
        adapter: TransferItemListAdapter,
    ) : ComparableMerger<GenericItem>() {
        val type = when {
            holder is StatusItem -> Type.STATUS
            holder is TransferFolder -> Type.FOLDER
            holder.hasIssues(adapter) -> Type.FILE_ERROR
            holder.isOngoing(adapter) -> Type.FILE_ONGOING
            else -> Type.FILE
        }

        override fun equals(other: Any?): Boolean {
            return (other is GroupEditableTransferObjectMerger && other.type == type)
        }

        override fun hashCode(): Int = type.hashCode()

        override operator fun compareTo(other: ComparableMerger<GenericItem>): Int {
            return if (other is GroupEditableTransferObjectMerger) {
                MathUtils.compare(other.type.ordinal.toLong(), type.ordinal.toLong())
            } else 1
        }

        enum class Type {
            STATUS, FOLDER_ONGOING, FOLDER, FILE_ONGOING, FILE_ERROR, FILE
        }
    }

    companion object {
        //public static final int MODE_SORT_BY_DEFAULT = MODE_SORT_BY_NAME - 1;
        val MODE_GROUP_BY_DEFAULT: Int = GroupEditableListAdapter.MODE_GROUP_BY_NOTHING + 1
    }
}