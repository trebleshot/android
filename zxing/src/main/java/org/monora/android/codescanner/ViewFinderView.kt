/*
 * MIT License
 *
 * Copyright (c) 2017 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.monora.android.codescanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.view.View
import androidx.annotation.FloatRange
import androidx.annotation.ColorInt
import androidx.annotation.Px
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class ViewFinderView(context: Context) : View(context) {
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val framePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val path = Path()

    var frameRect: Rect? = null
        private set

    private var mFrameCornersSize = 0

    private var mFrameCornersRadius = 0

    private var frameRatioWidth = 1f

    private var frameRatioHeight = 1f

    private var mFrameSize = 0.75f

    private var laidOut = false

    @get:FloatRange(from = 0.0, fromInclusive = false)
    var frameAspectRatioWidth: Float
        get() = frameRatioWidth
        set(ratioWidth) {
            frameRatioWidth = ratioWidth
            invalidateFrameRect()
            if (laidOut) {
                invalidate()
            }
        }

    @get:FloatRange(from = 0.0, fromInclusive = false)
    var frameAspectRatioHeight: Float
        get() = frameRatioHeight
        set(ratioHeight) {
            frameRatioHeight = ratioHeight
            invalidateFrameRect()
            if (laidOut) {
                invalidate()
            }
        }

    @get:ColorInt
    var maskColor: Int
        get() = maskPaint.color
        set(color) {
            maskPaint.color = color
            if (laidOut) {
                invalidate()
            }
        }

    @get:ColorInt
    var frameColor: Int
        get() = framePaint.color
        set(color) {
            framePaint.color = color
            if (laidOut) {
                invalidate()
            }
        }

    @get:Px
    var frameThickness: Int
        get() = framePaint.strokeWidth.toInt()
        set(thickness) {
            framePaint.strokeWidth = thickness.toFloat()
            if (laidOut) {
                invalidate()
            }
        }

    @get:Px
    var frameCornersSize: Int
        get() = mFrameCornersSize
        set(size) {
            mFrameCornersSize = size
            if (laidOut) {
                invalidate()
            }
        }

    @get:Px
    var frameCornersRadius: Int
        get() = mFrameCornersRadius
        set(radius) {
            mFrameCornersRadius = radius
            if (laidOut) {
                invalidate()
            }
        }

    @get:FloatRange(from = 0.1, to = 1.0)
    var frameSize: Float
        get() = mFrameSize
        set(size) {
            mFrameSize = size
            invalidateFrameRect()
            if (laidOut) {
                invalidate()
            }
        }

    override fun onDraw(canvas: Canvas) {
        val frame = frameRect ?: return
        val width = width
        val height = height
        val top = frame.top.toFloat()
        val left = frame.left.toFloat()
        val right = frame.right.toFloat()
        val bottom = frame.bottom.toFloat()
        val frameCornersSize = mFrameCornersSize.toFloat()
        val frameCornersRadius = mFrameCornersRadius.toFloat()
        val path = path

        if (frameCornersRadius > 0) {
            val normalizedRadius = min(frameCornersRadius, max(frameCornersSize - 1, 0f))
            path.reset()
            path.moveTo(left, top + normalizedRadius)
            path.quadTo(left, top, left + normalizedRadius, top)
            path.lineTo(right - normalizedRadius, top)
            path.quadTo(right, top, right, top + normalizedRadius)
            path.lineTo(right, bottom - normalizedRadius)
            path.quadTo(right, bottom, right - normalizedRadius, bottom)
            path.lineTo(left + normalizedRadius, bottom)
            path.quadTo(left, bottom, left, bottom - normalizedRadius)
            path.lineTo(left, top + normalizedRadius)
            path.moveTo(0f, 0f)
            path.lineTo(width.toFloat(), 0f)
            path.lineTo(width.toFloat(), height.toFloat())
            path.lineTo(0f, height.toFloat())
            path.lineTo(0f, 0f)
            canvas.drawPath(path, maskPaint)
            path.reset()
            path.moveTo(left, top + frameCornersSize)
            path.lineTo(left, top + normalizedRadius)
            path.quadTo(left, top, left + normalizedRadius, top)
            path.lineTo(left + frameCornersSize, top)
            path.moveTo(right - frameCornersSize, top)
            path.lineTo(right - normalizedRadius, top)
            path.quadTo(right, top, right, top + normalizedRadius)
            path.lineTo(right, top + frameCornersSize)
            path.moveTo(right, bottom - frameCornersSize)
            path.lineTo(right, bottom - normalizedRadius)
            path.quadTo(right, bottom, right - normalizedRadius, bottom)
            path.lineTo(right - frameCornersSize, bottom)
            path.moveTo(left + frameCornersSize, bottom)
            path.lineTo(left + normalizedRadius, bottom)
            path.quadTo(left, bottom, left, bottom - normalizedRadius)
            path.lineTo(left, bottom - frameCornersSize)
            canvas.drawPath(path, framePaint)
        } else {
            path.reset()
            path.moveTo(left, top)
            path.lineTo(right, top)
            path.lineTo(right, bottom)
            path.lineTo(left, bottom)
            path.lineTo(left, top)
            path.moveTo(0f, 0f)
            path.lineTo(width.toFloat(), 0f)
            path.lineTo(width.toFloat(), height.toFloat())
            path.lineTo(0f, height.toFloat())
            path.lineTo(0f, 0f)
            canvas.drawPath(path, maskPaint)
            path.reset()
            path.moveTo(left, top + frameCornersSize)
            path.lineTo(left, top)
            path.lineTo(left + frameCornersSize, top)
            path.moveTo(right - frameCornersSize, top)
            path.lineTo(right, top)
            path.lineTo(right, top + frameCornersSize)
            path.moveTo(right, bottom - frameCornersSize)
            path.lineTo(right, bottom)
            path.lineTo(right - frameCornersSize, bottom)
            path.moveTo(left + frameCornersSize, bottom)
            path.lineTo(left, bottom)
            path.lineTo(left, bottom - frameCornersSize)
            canvas.drawPath(path, framePaint)
        }
    }

    override fun onLayout(
        changed: Boolean,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        laidOut = true
        invalidateFrameRect(right - left, bottom - top)
    }

    fun setFrameAspectRatio(
        @FloatRange(from = 0.0, fromInclusive = false) ratioWidth: Float,
        @FloatRange(from = 0.0, fromInclusive = false) ratioHeight: Float
    ) {
        frameRatioWidth = ratioWidth
        frameRatioHeight = ratioHeight

        invalidateFrameRect()

        if (laidOut) {
            invalidate()
        }
    }

    private fun invalidateFrameRect(width: Int = getWidth(), height: Int = getHeight()) {
        if (width < 1 || height < 1) return
        val viewAR = width.toFloat() / height.toFloat()
        val frameAR = frameRatioWidth / frameRatioHeight
        val frameWidth: Int
        val frameHeight: Int
        if (viewAR <= frameAR) {
            frameWidth = (width * mFrameSize).roundToInt()
            frameHeight = (frameWidth / frameAR).roundToInt()
        } else {
            frameHeight = (height * mFrameSize).roundToInt()
            frameWidth = (frameHeight * frameAR).roundToInt()
        }
        val frameLeft = (width - frameWidth) / 2
        val frameTop = (height - frameHeight) / 2
        frameRect = Rect(frameLeft, frameTop, frameLeft + frameWidth, frameTop + frameHeight)
    }

    init {
        maskPaint.style = Paint.Style.FILL
        framePaint.style = Paint.Style.STROKE
        path.fillType = Path.FillType.EVEN_ODD
    }
}