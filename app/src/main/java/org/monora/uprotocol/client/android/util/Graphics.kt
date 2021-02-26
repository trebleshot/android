package org.monora.uprotocol.client.android.util

import android.content.Context
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.drawable.TextDrawable
import org.monora.uprotocol.client.android.util.Resources.attrToRes
import org.monora.uprotocol.client.android.util.Resources.resToColor

object Graphics {
    fun getDefaultIconBuilder(context: Context): TextDrawable.IShapeBuilder {
        val builder: TextDrawable.IShapeBuilder = TextDrawable.builder()
        builder.beginConfig()
            .firstLettersOnly(true)
            .textMaxLength(1)
            .bold()
            .textColor(R.attr.colorControlNormal.attrToRes(context).resToColor(context))
            .shapeColor(R.attr.colorPassive.attrToRes(context).resToColor(context))
        return builder
    }
}