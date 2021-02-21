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
package com.genonbeta.TrebleShot.model

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.genonbeta.TrebleShot.R
import com.genonbeta.android.database.DatabaseObject
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import com.genonbeta.android.database.SQLQuery
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import org.monora.uprotocol.client.android.model.ContentModel

/**
 * created by: veli
 * date: 7/24/19 6:08 PM
 */
class TransferIndex : ContentModel, DatabaseObject<Device> {
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

    private var selected = false

    constructor() {
        transfer = Transfer()
    }

    constructor(transfer: Transfer) {
        this.transfer = transfer
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

    override fun canCopy(): Boolean = false

    override fun canMove(): Boolean = false

    override fun canShare(): Boolean = false

    override fun canRemove(): Boolean = false

    override fun canRename(): Boolean = false

    override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
        TODO("Not yet implemented")
    }

    override fun dateCreated(): Long = transfer.dateCreated

    override fun dateModified(): Long = transfer.dateCreated

    override fun dateSupported(): Boolean = true

    override fun filter(charSequence: CharSequence): Boolean = false

    override fun id(): Long = transfer.id

    override fun length(): Long {
        TODO("Not yet implemented")
    }

    override fun lengthSupported(): Boolean {
        TODO("Not yet implemented")
    }

    override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(operationBackend: OperationBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun canSelect(): Boolean = true

    override fun selected(): Boolean = selected

    override fun select(selected: Boolean) {
        this.selected = selected
    }

    override fun name(): String = getMemberAsTitle()

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