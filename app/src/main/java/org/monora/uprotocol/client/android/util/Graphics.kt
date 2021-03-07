package org.monora.uprotocol.client.android.util

import android.content.Context
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.drawable.TextDrawable
import org.monora.uprotocol.client.android.util.Resources.attrToRes
import org.monora.uprotocol.client.android.util.Resources.resToColor

object Graphics {
    fun createIconBuilder(context: Context) = TextDrawable.createBuilder().apply {
        textFirstLetters = true
        textMaxLength = 1
        textBold = true
        textColor = R.attr.colorControlNormal.attrToRes(context).resToColor(context)
        shapeColor = R.attr.colorPassive.attrToRes(context).resToColor(context)
    }
}