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
package com.genonbeta.TrebleShot.graphics.drawable

import android.graphics.*
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.graphics.drawable.shapes.RectShape
import android.graphics.drawable.shapes.RoundRectShape
import com.genonbeta.TrebleShot.util.TextUtils

class TextDrawable private constructor(builder: Builder) : ShapeDrawable(builder.mShape) {
    private val mTextPaint: Paint
    private val mBorderPaint: Paint
    private val mText: String
    private val mShape: RectShape
    private val mColor: Int
    private val mHeight: Int
    private val mWidth: Int
    private val mFontSize: Int
    private val mBorderThickness: Int
    private val mRadius: Float
    private fun getDarkerShade(color: Int): Int {
        return Color.rgb(
            (SHADE_FACTOR * Color.red(color)).toInt(),
            (SHADE_FACTOR * Color.green(color)).toInt(),
            (SHADE_FACTOR * Color.blue(color)).toInt()
        )
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        val r: Rect = bounds

        // draw border
        if (mBorderThickness > 0) drawBorder(canvas)
        val count = canvas.save()
        canvas.translate(r.left.toFloat(), r.top.toFloat())

        // draw text
        val width = if (mWidth < 0) r.width() else mWidth
        val height = if (mHeight < 0) r.height() else mHeight
        val fontSize = if (mFontSize < 0) Math.min(width, height) / 2 else mFontSize
        mTextPaint.textSize = fontSize.toFloat()
        canvas.drawText(
            mText,
            (width / 2).toFloat(),
            height / 2 - (mTextPaint.descent() + mTextPaint.ascent()) / 2,
            mTextPaint
        )
        canvas.restoreToCount(count)
    }

    private fun drawBorder(canvas: Canvas) {
        val rect = RectF(getBounds())
        rect.inset((mBorderThickness / 2).toFloat(), (mBorderThickness / 2).toFloat())
        if (mShape is OvalShape) {
            canvas.drawOval(rect, mBorderPaint)
        } else if (mShape is RoundRectShape) {
            canvas.drawRoundRect(rect, mRadius, mRadius, mBorderPaint)
        } else {
            canvas.drawRect(rect, mBorderPaint)
        }
    }

    override fun setAlpha(alpha: Int) {
        mTextPaint.alpha = alpha
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mTextPaint.colorFilter = cf
    }

    override fun getOpacity(): Int {
        return PixelFormat.TRANSLUCENT
    }

    override fun getIntrinsicWidth(): Int {
        return mWidth
    }

    override fun getIntrinsicHeight(): Int {
        return mHeight
    }

    interface IConfigBuilder {
        fun bold(): IConfigBuilder
        fun firstLettersOnly(use: Boolean): IConfigBuilder
        fun fontSize(size: Int): IConfigBuilder
        fun height(height: Int): IConfigBuilder
        fun shapeColor(color: Int): IConfigBuilder
        fun textColor(color: Int): IConfigBuilder
        fun textMaxLength(length: Int): IConfigBuilder
        fun toUpperCase(): IConfigBuilder
        fun useFont(font: Typeface): IConfigBuilder
        fun width(width: Int): IConfigBuilder
        fun withBorder(thickness: Int): IConfigBuilder
        fun endConfig(): IShapeBuilder
    }

    interface IBuilder {
        fun build(text: String): TextDrawable
    }

    interface IShapeBuilder {
        fun beginConfig(): IConfigBuilder
        fun rect(): IBuilder
        fun round(): IBuilder
        fun roundRect(radius: Int): IBuilder
        fun buildRect(text: String): TextDrawable
        fun buildRound(text: String): TextDrawable
        fun buildRoundRect(text: String, radius: Int): TextDrawable
    }

    class Builder : IConfigBuilder, IShapeBuilder, IBuilder {
        var mColor: Int
        var mBorderThickness: Int
        var mWidth: Int
        var mHeight: Int
        var mTextColor: Int
        var mTextMaxLength: Int
        var mFontSize: Int
        var mRadius = 0f
        var mToUpperCase: Boolean
        var mIsBold: Boolean
        var mFirstLetters = false
        var mText = ""
        var mFont: Typeface
        var mShape: RectShape
        override fun width(width: Int): IConfigBuilder {
            mWidth = width
            return this
        }

        override fun height(height: Int): IConfigBuilder {
            mHeight = height
            return this
        }

        override fun shapeColor(color: Int): IConfigBuilder {
            mColor = color
            return this
        }

        override fun textColor(color: Int): IConfigBuilder {
            mTextColor = color
            return this
        }

        override fun textMaxLength(length: Int): IConfigBuilder {
            mTextMaxLength = length
            return this
        }

        override fun withBorder(thickness: Int): IConfigBuilder {
            mBorderThickness = thickness
            return this
        }

        override fun useFont(font: Typeface): IConfigBuilder {
            mFont = font
            return this
        }

        override fun fontSize(size: Int): IConfigBuilder {
            mFontSize = size
            return this
        }

        override fun bold(): IConfigBuilder {
            mIsBold = true
            return this
        }

        override fun toUpperCase(): IConfigBuilder {
            mToUpperCase = true
            return this
        }

        override fun firstLettersOnly(use: Boolean): IConfigBuilder {
            mFirstLetters = use
            return this
        }

        override fun beginConfig(): IConfigBuilder {
            return this
        }

        override fun endConfig(): IShapeBuilder {
            return this
        }

        override fun rect(): IBuilder {
            mShape = RectShape()
            return this
        }

        override fun round(): IBuilder {
            mShape = OvalShape()
            return this
        }

        override fun roundRect(radius: Int): IBuilder {
            mRadius = radius.toFloat()
            val radii = floatArrayOf(
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat(),
                radius.toFloat()
            )
            mShape = RoundRectShape(radii, null, null)
            return this
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
            mText = text
            return TextDrawable(this)
        }

        init {
            mColor = Color.GRAY
            mTextColor = Color.WHITE
            mBorderThickness = 0
            mWidth = -1
            mHeight = -1
            mShape = RectShape()
            mFont = Typeface.create("sans-serif", Typeface.NORMAL)
            mTextMaxLength = -1
            mFontSize = -1
            mIsBold = false
            mToUpperCase = false
        }
    }

    companion object {
        private const val SHADE_FACTOR = 0.9f
        fun builder(): IShapeBuilder {
            return Builder()
        }
    }

    init {

        // shape properties
        mShape = builder.mShape
        mHeight = builder.mHeight
        mWidth = builder.mWidth
        mRadius = builder.mRadius

        // text and color
        var processedText: String = if (builder.mToUpperCase) builder.mText.toUpperCase() else builder.mText
        if (builder.mTextMaxLength >= 0) {
            processedText = when {
                builder.mFirstLetters -> TextUtils.getLetters(processedText, builder.mTextMaxLength)
                processedText.length > builder.mTextMaxLength -> processedText.substring(0, builder.mTextMaxLength)
                else -> processedText
            }
        }
        mText = processedText
        mColor = builder.mColor

        // text paint settings
        mFontSize = builder.mFontSize
        mTextPaint = Paint()
        mTextPaint.color = builder.mTextColor
        mTextPaint.isAntiAlias = true
        mTextPaint.isFakeBoldText = builder.mIsBold
        mTextPaint.style = Paint.Style.FILL
        mTextPaint.typeface = builder.mFont
        mTextPaint.textAlign = Paint.Align.CENTER
        mTextPaint.strokeWidth = builder.mBorderThickness.toFloat()

        // border paint settings
        mBorderThickness = builder.mBorderThickness
        mBorderPaint = Paint()
        mBorderPaint.color = getDarkerShade(mColor)
        mBorderPaint.style = Paint.Style.STROKE
        mBorderPaint.strokeWidth = mBorderThickness.toFloat()

        // drawable paint mColor
        val paint: Paint = getPaint()
        paint.color = mColor
    }
}