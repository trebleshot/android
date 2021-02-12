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
package com.genonbeta.TrebleShot.dataobject

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.Companion.VIEW_TYPE_REPRESENTATIVE
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupEditable
import com.genonbeta.android.database.DatabaseObject
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.util.Files.sizeExpression
import java.util.*

/**
 * created by: veli
 * date: 7/24/19 6:08 PM
 */
class TransferIndex : GroupEditable, DatabaseObject<Device> {
    private var viewType = 0

    private var representativeText: String? = null

    var numberOfOutgoing = 0

    var numberOfIncoming = 0

    var numberOfOutgoingCompleted = 0

    var numberOfIncomingCompleted = 0

    var bytesOutgoing: Long = 0

    var bytesIncoming: Long = 0

    var bytesOutgoingCompleted: Long = 0

    var bytesIncomingCompleted: Long = 0

    var isRunning = false

    var hasIssues = false

    var transfer = Transfer()

    var members = emptyArray<LoadedMember>()

    private var isSelected = false

    override var id: Long
        get() = transfer.id
        set(id) {
            transfer.id = id
        }

    override var requestCode: Int = 0

    constructor() {
        transfer = Transfer()
    }

    constructor(transfer: Transfer) {
        this.transfer = transfer
    }

    constructor(representativeText: String?) {
        viewType = VIEW_TYPE_REPRESENTATIVE
        this.representativeText = representativeText
    }

    override fun applyFilter(filteringKeywords: Array<String>): Boolean {
        val copyMembers = members
        for (keyword in filteringKeywords) for (member in copyMembers) {
            if (member.device.username.toLowerCase(Locale.getDefault())
                    .contains(keyword.toLowerCase(Locale.getDefault()))
            ) return true
        }
        return false
    }

    fun bytesTotal(): Long {
        return bytesOutgoing + bytesIncoming
    }

    fun bytesCompleted(): Long {
        return bytesOutgoingCompleted + bytesIncomingCompleted
    }

    fun bytesPending(): Long {
        return bytesTotal() - bytesCompleted()
    }

    override fun comparisonSupported(): Boolean {
        return true
    }

    override fun equals(other: Any?): Boolean {
        return if (other is TransferIndex) transfer == other.transfer else super.equals(other)
    }

    fun hasIncoming(): Boolean {
        return numberOfIncoming > 0
    }

    fun hasOutgoing(): Boolean {
        return numberOfOutgoing > 0
    }

    private fun getMemberAsTitle(): String {
        val copyMembers = members
        val title = StringBuilder()
        for (member in copyMembers) {
            if (title.isNotEmpty()) title.append(", ")
            title.append(member.device.username)
        }
        return title.toString()
    }

    fun getMemberAsTitle(context: Context): String {
        val copyMembers = members
        return if (copyMembers.size == 1) {
            copyMembers[0].device.username
        } else {
            context.resources.getQuantityString(R.plurals.text_devices, copyMembers.size, copyMembers.size)
        }
    }

    override fun getComparableName(): String = getSelectableTitle()

    override fun getComparableDate(): Long = transfer.dateCreated

    override fun getComparableSize(): Long = bytesTotal()

    override fun getSelectableTitle(): String {
        val title = getMemberAsTitle()
        val size = sizeExpression(bytesOutgoing + bytesOutgoing, false)
        return if (title.isNotEmpty()) String.format("%s (%s)", title, size) else size
    }

    override fun getViewType(): Int = viewType

    override fun isSelectableSelected(): Boolean {
        TODO("Not yet implemented")
    }

    fun numberOfTotal(): Int {
        return numberOfOutgoing + numberOfIncoming
    }

    fun numberOfCompleted(): Int {
        return numberOfOutgoingCompleted + numberOfIncomingCompleted
    }

    fun percentage(): Double {
        val total = bytesTotal()
        val completed = bytesCompleted()
        return if (total == 0L) 1.0 else if (completed == 0L) 0.0 else completed.toDouble() / total
    }

    override fun getRepresentativeText(): String {
        return representativeText!!
    }

    override fun setRepresentativeText(text: CharSequence) {
        representativeText = text.toString()
    }

    override fun isGroupRepresentative(): Boolean = representativeText != null

    override fun setDate(date: Long) {
        transfer.dateCreated = date
    }

    override fun setSelectableSelected(selected: Boolean): Boolean {
        if (isGroupRepresentative()) return false
        isSelected = selected
        return true
    }

    override fun setSize(size: Long) {
        Log.e(TAG, "setSize: This is not implemented")
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, progress: Progress.Context?) {
        transfer.onCreateObject(db, kuick, parent, progress)
    }

    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, progress: Progress.Context?) {
        transfer.onUpdateObject(db, kuick, parent, progress)
    }

    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, progress: Progress.Context?) {
        transfer.onRemoveObject(db, kuick, parent, progress)
    }

    override fun getValues(): ContentValues {
        return transfer.getValues()
    }

    override fun getWhere(): SQLQuery.Select {
        return transfer.getWhere()
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, values: ContentValues) {
        transfer.reconstruct(db, kuick, values)
    }

    companion object {
        val TAG = TransferIndex::class.java.simpleName
    }
}