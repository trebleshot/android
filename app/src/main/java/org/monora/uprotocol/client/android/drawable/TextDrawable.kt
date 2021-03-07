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
package org.monora.uprotocol.client.android.drawable

import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.RoundRectShape
import org.monora.uprotocol.client.android.util.TextManipulators
import java.util.*
import kotlin.math.min

class TextDrawable private constructor(
    builder: Builder,
    private val text: String,
    private val textPaint: Paint,
    private val borderPaint: Paint,
) : ShapeDrawable(builder.shape) {
    private val height = builder.height
    
    private val width = builder.width

    private val radius = builder.radius

    private val shapeBorderThickness = builder.shapeBorderThickness

    private val textSize = builder.textSize
    
    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val r = bounds

        // draw border
        if (shapeBorderThickness > 0) {
            drawBorder(canvas)
        }

        val count = canvas.save()
        canvas.translate(r.left.toFloat(), r.top.toFloat())

        // draw text
        val width = if (width < 0) r.width() else width
        val height = if (height < 0) r.height() else height
        val fontSize = if (textSize < 0) min(width, height) / 2 else textSize
        textPaint.textSize = fontSize.toFloat()

        canvas.drawText(
            text, (width / 2).toFloat(), height / 2 - (textPaint.descent() + textPaint.ascent()) / 2, textPaint
        )
        canvas.restoreToCount(count)
    }

    private fun drawBorder(canvas: Canvas) {
        val rect = RectF(bounds).also {
            it.inset((shapeBorderThickness / 2).toFloat(), (shapeBorderThickness / 2).toFloat())
        }

        when (shape) {
            is OvalShape -> canvas.drawOval(rect, borderPaint)
            is RoundRectShape -> canvas.drawRoundRect(rect, radius, radius, borderPaint)
            else -> canvas.drawRect(rect, borderPaint)
        }
    }

    override fun getIntrinsicWidth(): Int {
        return width
    }

    override fun getIntrinsicHeight(): Int {
        return height
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun setAlpha(alpha: Int) {
        textPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        textPaint.colorFilter = colorFilter
    }

    interface Builder {
        var radius: Float

        var shape: RectShape

        var shapeBorderThickness: Int

        var shapeColor: Int

        var textAllCaps: Boolean

        var textBold: Boolean

        var textColor: Int

        var textTypeface: Typeface

        var textFirstLetters: Boolean

        var textMaxLength: Int

        var textSingleCharacter: Boolean

        var textSize: Int

        var height: Int

        var width: Int

        fun build(text: String): TextDrawable

        fun buildRect(text: String): TextDrawable

        fun buildRound(text: String): TextDrawable

        fun buildRoundRect(text: String, radius: Int): TextDrawable

        fun rect()

        fun round()

        fun roundRect(radius: Int)
    }

    private class BuilderImpl : Builder {
        override var radius: Float = 0f

        override var shape: RectShape = RectShape()

        override var shapeBorderThickness: Int = 0

        override var shapeColor: Int = Color.GRAY

        override var textAllCaps: Boolean = false

        override var textBold: Boolean = false

        override var textColor: Int = Color.WHITE

        override var textFirstLetters: Boolean = false

        override var textMaxLength: Int = -1

        override var textSingleCharacter: Boolean = false

        override var textSize: Int = -1

        override var textTypeface: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)

        override var height: Int = -1

        override var width: Int = -1

        override fun rect() {
            shape = RectShape()
        }

        override fun round() {
            shape = OvalShape()
        }

        override fun roundRect(radius: Int) {
            val radiusF = radius.toFloat()
            val radii = floatArrayOf(radiusF, radiusF, radiusF, radiusF, radiusF, radiusF, radiusF, radiusF)
            shape = RoundRectShape(radii, null, null)
            this.radius = radiusF
        }

        override fun buildRect(text: String): TextDrawable {
            rect()
            return build(text)
        }

        override fun buildRoundRect(text: String, radius: Int): TextDrawable {
            roundRect(radius)
            return build(text)
        }

        override fun buildRound(text: String): TextDrawable {
            round()
            return build(text)
        }

        override fun build(text: String): TextDrawable {
            var modifiedText = text

            if (textAllCaps) {
                modifiedText = modifiedText.toUpperCase(Locale.getDefault())
            }

            if (textMaxLength > 0) {
                modifiedText = when {
                    textFirstLetters -> TextManipulators.getLetters(modifiedText, textMaxLength)
                    modifiedText.length > textMaxLength -> modifiedText.substring(0, textMaxLength)
                    else -> modifiedText
                }
            }

            val textPaint = Paint().apply {
                color = textColor
                isAntiAlias = true
                isFakeBoldText = textBold
                style = Paint.Style.FILL
                typeface = textTypeface
                textAlign = Paint.Align.CENTER
                strokeWidth = shapeBorderThickness.toFloat()
            }

            // border paint settings
            val borderPaint = Paint().apply {
                color = getDarkerShade(shapeColor)
                style = Paint.Style.STROKE
                strokeWidth = shapeBorderThickness.toFloat()
            }

            return TextDrawable(this, modifiedText, textPaint, borderPaint).also {
                it.paint.color = shapeColor
            }
        }
    }

    companion object {
        private const val SHADE_FACTOR = 0.9f

        fun createBuilder(): Builder {
            return BuilderImpl()
        }

        private fun getDarkerShade(color: Int): Int {
            return Color.rgb(
                (SHADE_FACTOR * Color.red(color)).toInt(),
                (SHADE_FACTOR * Color.green(color)).toInt(),
                (SHADE_FACTOR * Color.blue(color)).toInt()
            )
        }
    }
}