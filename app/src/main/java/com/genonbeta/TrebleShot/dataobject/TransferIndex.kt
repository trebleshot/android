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

import com.genonbeta.android.database.DatabaseObject
import com.genonbeta.android.database.SQLQuery
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import android.util.Log
import com.genonbeta.TrebleShot.util.Files
import java.lang.StringBuilder

/**
 * created by: veli
 * date: 7/24/19 6:08 PM
 */
class TransferIndex : GroupEditable, DatabaseObject<Device?> {
    var viewType = 0
    var representativeText: String? = null
    @JvmField
    var numberOfOutgoing = 0
    @JvmField
    var numberOfIncoming = 0
    @JvmField
    var numberOfOutgoingCompleted = 0
    @JvmField
    var numberOfIncomingCompleted = 0
    @JvmField
    var bytesOutgoing: Long = 0
    @JvmField
    var bytesIncoming: Long = 0
    @JvmField
    var bytesOutgoingCompleted: Long = 0
    @JvmField
    var bytesIncomingCompleted: Long = 0
    @JvmField
    var isRunning = false
    @JvmField
    var hasIssues = false
    @JvmField
    var transfer = Transfer()
    @JvmField
    var members = arrayOfNulls<LoadedMember>(0)
    var isSelectableSelected = false
        private set

    constructor() {
        transfer = Transfer()
    }

    constructor(transfer: Transfer) {
        this.transfer = transfer
    }

    constructor(representativeText: String?) {
        viewType = TransferListAdapter.VIEW_TYPE_REPRESENTATIVE
        this.representativeText = representativeText
    }

    override fun applyFilter(filteringKeywords: Array<String>): Boolean {
        val copyMembers = members
        for (keyword in filteringKeywords) for (member in copyMembers) if (member!!.device!!.username!!.toLowerCase()
                .contains(keyword.toLowerCase())
        ) return true
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

    override fun equals(obj: Any?): Boolean {
        return if (obj is TransferIndex) transfer == obj.transfer else super.equals(obj)
    }

    fun hasIncoming(): Boolean {
        return numberOfIncoming > 0
    }

    fun hasOutgoing(): Boolean {
        return numberOfOutgoing > 0
    }

    val memberAsTitle: String
        get() {
            val copyMembers = members
            val title = StringBuilder()
            for (member in copyMembers) {
                if (title.length > 0) title.append(", ")
                title.append(member!!.device!!.username)
            }
            return title.toString()
        }

    fun getMemberAsTitle(context: Context): String? {
        val copyMembers = members
        return if (copyMembers.size == 1) copyMembers[0]!!.device!!.username else context.resources.getQuantityString(
            R.plurals.text_devices,
            copyMembers.size, copyMembers.size
        )
    }

    val comparableName: String?
        get() = selectableTitle
    val comparableDate: Long
        get() = transfer.dateCreated
    val comparableSize: Long
        get() = bytesTotal()
    var id: Long
        get() = transfer.id
        set(id) {
            transfer.id = id
        }
    val selectableTitle: String
        get() {
            val title = memberAsTitle
            val size = Files.sizeExpression(bytesOutgoing + bytesOutgoing, false)
            return if (title.length > 0) String.format("%s (%s)", title, size) else size
        }
    val requestCode: Int
        get() = 0

    fun numberOfTotal(): Int {
        return numberOfOutgoing + numberOfIncoming
    }

    fun numberOfCompleted(): Int {
        return numberOfOutgoingCompleted + numberOfIncomingCompleted
    }

    fun percentage(): Double {
        val total = bytesTotal()
        val completed = bytesCompleted()
        return if (total == 0L) 1 else if (completed == 0L) 0 else completed.toDouble() / total
    }

    override fun getRepresentativeText(): String {
        return representativeText!!
    }

    override fun setRepresentativeText(text: CharSequence) {
        representativeText = text.toString()
    }

    val isGroupRepresentative: Boolean
        get() = representativeText != null

    override fun setDate(date: Long) {
        transfer.dateCreated = date
    }

    override fun setSelectableSelected(selected: Boolean): Boolean {
        if (isGroupRepresentative) return false
        isSelectableSelected = selected
        return true
    }

    override fun setSize(size: Long) {
        Log.e(TAG, "setSize: This is not implemented")
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, listener: Progress.Listener) {
        transfer.onCreateObject(db, kuick, parent, listener)
    }

    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, listener: Progress.Listener) {
        transfer.onUpdateObject(db, kuick, parent, listener)
    }

    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, listener: Progress.Listener) {
        transfer.onRemoveObject(db, kuick, parent, listener)
    }

    override fun getValues(): ContentValues {
        return transfer.values
    }

    override fun getWhere(): SQLQuery.Select {
        return transfer.where
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        transfer.reconstruct(db, kuick, item)
    }

    companion object {
        val TAG = TransferIndex::class.java.simpleName
    }
}