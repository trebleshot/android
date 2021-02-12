package org.monora.uprotocol.client.view.model

interface ContentModel {
    fun canCopy(): Boolean

    fun canMove(): Boolean

    fun canShare(): Boolean

    fun canRemove(): Boolean

    fun canRename(): Boolean

    fun dateCreated(): Long

    fun dateModified(): Long

    fun length(): Long

    fun name(): String


}