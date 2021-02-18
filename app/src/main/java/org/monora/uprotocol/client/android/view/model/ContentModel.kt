package org.monora.uprotocol.client.android.view.model

interface ContentModel {
    fun canCopy(): Boolean

    fun canMove(): Boolean

    fun canShare(): Boolean

    fun canRemove(): Boolean

    fun canRename(): Boolean

    fun dateCreated(): Long

    fun dateModified(): Long

    fun filter(charSequence: CharSequence): Boolean

    fun length(): Long

    fun name(): String
}