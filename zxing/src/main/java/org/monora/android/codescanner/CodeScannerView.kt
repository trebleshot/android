/*
 * MIT License
 *
 * Copyright (c) 2017 Yuriy Budiyev [yuriy.budiyev@yandex.ru]
 * Copyright (c) 2021 Veli Tasalı [me@velitasali.com]
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

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.Rect
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.Px
import androidx.appcompat.widget.AppCompatImageButton
import kotlin.math.roundToInt

/**
 * A view to display code scanner preview.
 *
 * @see CodeScanner
 */
class CodeScannerView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {
    var previewView = SurfaceView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
        private set

    var viewFinderView = ViewFinderView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }
        private set

    private var autoFocusButton = AppCompatImageButton(context).apply {
        layoutParams = LayoutParams(buttonSize, buttonSize)
        scaleType = ImageView.ScaleType.CENTER
        setBackgroundResource(R.drawable.zxing_round_selector)
        setImageResource(R.drawable.zxing_ic_baseline_filter_center_focus_24)
        setOnClickListener(AutoFocusClickListener())
    }

    private var flashButton = AppCompatImageButton(context).apply {
        layoutParams = LayoutParams(buttonSize, buttonSize)
        scaleType = ImageView.ScaleType.CENTER
        setBackgroundResource(R.drawable.zxing_round_selector)
        setImageResource(R.drawable.zxing_ic_baseline_flash_off_24)
        setOnClickListener(FlashClickListener())
    }

    var previewSize: CartesianCoordinate? = null
        set(value) {
            field = value
            requestLayout()
        }

    var sizeListener: SizeListener? = null

    var codeScanner: CodeScanner? = null
        set(value) {
            check(field == null) { "Code scanner has already been set" }

            field = value
            field?.let {
                setAutoFocusEnabled(it.autoFocusEnabled)
                setFlashEnabled(it.flashEnabled)
            }
        }

    private var buttonSize: Int

    /**
     * Auto-focus button color.
     */
    @get:ColorInt
    var autoFocusButtonColor = 0
        set(value) {
            field = value
            autoFocusButton.setColorFilter(value)
        }

    private var focusAreaSize = 0

    /**
     * Flash button color.
     */
    @get:ColorInt
    var flashButtonColor = 0
        set(value) {
            field = value
            flashButton.setColorFilter(value)
        }

    /**
     * Frame color.
     */
    @get:ColorInt
    var frameColor: Int
        get() = viewFinderView.frameColor
        set(color) {
            viewFinderView.frameColor = color
        }

    /**
     * Frame thickness.
     */
    @get:Px
    var frameThickness: Int
        get() = viewFinderView.frameThickness
        set(thickness) {
            require(thickness >= 0) { "Frame thickness can't be negative" }
            viewFinderView.frameThickness = thickness
        }

    /**
     * Frame corner radius.
     */
    @get:Px
    var frameCornersRadius: Int
        get() = viewFinderView.frameCornersRadius
        set(radius) {
            require(radius >= 0) { "Frame corners radius can't be negative" }
            viewFinderView.frameCornersRadius = radius
        }

    /**
     * Frame corners' size.
     */
    @get:Px
    var frameCornersSize: Int
        get() = viewFinderView.frameCornersSize
        set(size) {
            require(size >= 0) { "Frame corners size can't be negative" }
            viewFinderView.frameCornersSize = size
        }

    /**
     * Frame aspect-ratio width.
     */
    @get:FloatRange(from = 0.0, fromInclusive = false)
    var frameAspectRatioWidth: Float
        get() = viewFinderView.frameAspectRatioWidth
        set(ratioWidth) {
            require(ratioWidth > 0) { "Frame aspect ratio values should be greater than zero" }
            viewFinderView.frameAspectRatioWidth = ratioWidth
        }

    /**
     * Frame aspect-ratio height.
     */
    @get:FloatRange(from = 0.0, fromInclusive = false)
    var frameAspectRatioHeight: Float
        get() = viewFinderView.frameAspectRatioHeight
        set(ratioHeight) {
            require(ratioHeight > 0) { "Frame aspect ratio values should be greater than zero" }
            viewFinderView.frameAspectRatioHeight = ratioHeight
        }

    val frameRect: Rect?
        get() = viewFinderView.frameRect

    /**
     * Frame size.
     */
    @get:FloatRange(from = 0.1, to = 1.0)
    var frameSize: Float
        get() = viewFinderView.frameSize
        set(size) {
            require(!(size < 0.1 || size > 1)) { "Max frame size value should be between 0.1 and 1, inclusive" }
            viewFinderView.frameSize = size
        }

    /**
     * Whether if auto focus button is currently visible.
     */
    var isAutoFocusButtonVisible: Boolean
        get() = autoFocusButton.visibility == VISIBLE
        set(visible) {
            autoFocusButton.visibility = if (visible) VISIBLE else INVISIBLE
        }

    private var isFlashButtonVisible: Boolean
        get() = flashButton.visibility == VISIBLE
        set(visible) {
            flashButton.visibility = if (visible) VISIBLE else INVISIBLE
        }

    /**
     * Whether if mask is currently visible.
     */
    var isMaskVisible: Boolean
        get() = viewFinderView.visibility == VISIBLE
        set(visible) {
            viewFinderView.visibility = if (visible) VISIBLE else INVISIBLE
        }

    /**
     * Mask color.
     */
    @get:ColorInt
    var maskColor: Int
        get() = viewFinderView.maskColor
        set(color) {
            viewFinderView.maskColor = color
        }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        performLayout(right - left, bottom - top)
    }

    override fun onSizeChanged(width: Int, height: Int, oldWidth: Int, oldHeight: Int) {
        performLayout(width, height)
        sizeListener?.onSizeChanged(width, height)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val codeScanner = codeScanner
        val frameRect = frameRect
        val x = event.x.toInt()
        val y = event.y.toInt()

        if (codeScanner != null && frameRect != null
            && codeScanner.isAutoFocusSupportedOrUnknown
            && codeScanner.isTouchFocusEnabled
            && event.action == MotionEvent.ACTION_DOWN
            && frameRect.isPointInside(x, y)
        ) {
            val areaSize = focusAreaSize
            codeScanner.performTouchFocus(
                Rect(
                    x - areaSize,
                    y - areaSize,
                    x + areaSize,
                    y + areaSize
                ).fitIn(frameRect)
            )
        }
        return super.onTouchEvent(event)
    }

    private fun performLayout(width: Int, height: Int) {
        previewSize?.let { previewSize ->
            var frameLeft = 0
            var frameTop = 0
            var frameRight = width
            var frameBottom = height
            val previewWidth = previewSize.x

            if (previewWidth > width) {
                val d = (previewWidth - width) / 2
                frameLeft -= d
                frameRight += d
            }

            val previewHeight = previewSize.y

            if (previewHeight > height) {
                val d = (previewHeight - height) / 2
                frameTop -= d
                frameBottom += d
            }

            previewView.layout(frameLeft, frameTop, frameRight, frameBottom)
        } ?: previewView.layout(0, 0, width, height)

        viewFinderView.layout(0, 0, width, height)
        buttonSize.let { buttonSize ->
            val margin = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                8f,
                resources.displayMetrics
            ).toInt()

            autoFocusButton.layout(margin, margin, buttonSize, buttonSize)
            flashButton.layout(width - buttonSize, margin, width - margin, buttonSize)
        }
    }

    fun setAutoFocusEnabled(enabled: Boolean) {
        autoFocusButton.setImageResource(
            if (enabled) R.drawable.zxing_ic_baseline_filter_center_focus_24
            else R.drawable.zxing_ic_baseline_center_focus_weak_24
        )
    }

    fun setFlashEnabled(enabled: Boolean) {
        flashButton.setImageResource(
            if (enabled) R.drawable.zxing_ic_baseline_flash_on_24
            else R.drawable.zxing_ic_baseline_flash_off_24
        )
    }

    /**
     * Set frame aspect ratio (ex. 1:1, 15:10, 16:9, 4:3).
     *
     * @param ratioWidth  Frame aspect ratio width.
     * @param ratioHeight Frame aspect ratio height.
     */
    private fun setFrameAspectRatio(
        @FloatRange(from = 0.0, fromInclusive = false) ratioWidth: Float,
        @FloatRange(from = 0.0, fromInclusive = false) ratioHeight: Float,
    ) {
        require(!(ratioWidth <= 0 || ratioHeight <= 0)) {
            "Frame aspect ratio values should be greater than zero"
        }
        viewFinderView.setFrameAspectRatio(ratioWidth, ratioHeight)
    }

    interface SizeListener {
        fun onSizeChanged(width: Int, height: Int)
    }

    private inner class AutoFocusClickListener : OnClickListener {
        override fun onClick(view: View) {
            codeScanner?.takeIf { it.isAutoFocusSupportedOrUnknown }?.let {
                val enabled = !it.autoFocusEnabled
                it.autoFocusEnabled = enabled
                setAutoFocusEnabled(enabled)
            }
        }
    }

    private inner class FlashClickListener : OnClickListener {
        override fun onClick(view: View) {
            codeScanner?.takeIf { it.isAutoFocusSupportedOrUnknown }?.let {
                val enabled = !it.flashEnabled
                it.flashEnabled = enabled
                setFlashEnabled(enabled)
            }
        }
    }

    companion object {
        private const val DEFAULT_AUTO_FOCUS_BUTTON_VISIBLE = true

        private const val DEFAULT_FLASH_BUTTON_VISIBLE = true

        private const val DEFAULT_AUTO_FOCUS_BUTTON_VISIBILITY = VISIBLE

        private const val DEFAULT_FLASH_BUTTON_VISIBILITY = VISIBLE

        private const val DEFAULT_MASK_COLOR = 0x77000000

        private const val DEFAULT_FRAME_COLOR = Color.WHITE

        private const val DEFAULT_AUTO_FOCUS_BUTTON_COLOR = Color.WHITE

        private const val DEFAULT_FLASH_BUTTON_COLOR = Color.WHITE

        private const val DEFAULT_FRAME_THICKNESS_DP = 2f

        private const val DEFAULT_FRAME_ASPECT_RATIO_WIDTH = 1f

        private const val DEFAULT_FRAME_ASPECT_RATIO_HEIGHT = 1f

        private const val DEFAULT_FRAME_CORNER_SIZE_DP = 50f

        private const val DEFAULT_FRAME_CORNERS_RADIUS_DP = 0f

        private const val DEFAULT_FRAME_SIZE = 0.75f

        private const val BUTTON_SIZE_DP = 56f

        private const val FOCUS_AREA_SIZE_DP = 20f
    }

    init {
        val density = context.resources.displayMetrics.density
        buttonSize = (density * BUTTON_SIZE_DP).roundToInt()
        focusAreaSize = (density * FOCUS_AREA_SIZE_DP).roundToInt()

        if (attrs == null) {
            viewFinderView.setFrameAspectRatio(
                DEFAULT_FRAME_ASPECT_RATIO_WIDTH, DEFAULT_FRAME_ASPECT_RATIO_HEIGHT
            )
            viewFinderView.maskColor = DEFAULT_MASK_COLOR
            viewFinderView.frameColor = DEFAULT_FRAME_COLOR
            viewFinderView.frameThickness = (DEFAULT_FRAME_THICKNESS_DP * density).roundToInt()
            viewFinderView.frameCornersSize = (DEFAULT_FRAME_CORNER_SIZE_DP * density).roundToInt()
            viewFinderView.frameCornersRadius = (DEFAULT_FRAME_CORNERS_RADIUS_DP * density).roundToInt()
            viewFinderView.frameSize = DEFAULT_FRAME_SIZE
            autoFocusButton.setColorFilter(DEFAULT_AUTO_FOCUS_BUTTON_COLOR)
            flashButton.setColorFilter(DEFAULT_FLASH_BUTTON_COLOR)
            autoFocusButton.visibility = DEFAULT_AUTO_FOCUS_BUTTON_VISIBILITY
            flashButton.visibility = DEFAULT_FLASH_BUTTON_VISIBILITY
        } else {
            var a: TypedArray? = null
            try {
                a = context.theme.obtainStyledAttributes(
                    attrs,
                    R.styleable.zxing,
                    defStyleAttr,
                    defStyleRes
                )
                maskColor = a.getColor(R.styleable.zxing_zxing_maskColor, DEFAULT_MASK_COLOR)
                frameColor = a.getColor(R.styleable.zxing_zxing_frameColor, DEFAULT_FRAME_COLOR)
                frameThickness = a.getDimensionPixelOffset(
                    R.styleable.zxing_zxing_frameThickness,
                    (DEFAULT_FRAME_THICKNESS_DP * density).roundToInt()
                )
                frameCornersSize = a.getDimensionPixelOffset(
                    R.styleable.zxing_zxing_frameCornersSize,
                    (DEFAULT_FRAME_CORNER_SIZE_DP * density).roundToInt()
                )
                frameCornersRadius = a.getDimensionPixelOffset(
                    R.styleable.zxing_zxing_frameCornersRadius,
                    (DEFAULT_FRAME_CORNERS_RADIUS_DP * density).roundToInt()
                )
                setFrameAspectRatio(
                    a.getFloat(R.styleable.zxing_zxing_frameAspectRatioWidth, DEFAULT_FRAME_ASPECT_RATIO_WIDTH),
                    a.getFloat(R.styleable.zxing_zxing_frameAspectRatioHeight, DEFAULT_FRAME_ASPECT_RATIO_HEIGHT)
                )
                frameSize = a.getFloat(R.styleable.zxing_zxing_frameSize, DEFAULT_FRAME_SIZE)
                isAutoFocusButtonVisible = a.getBoolean(
                    R.styleable.zxing_zxing_autoFocusButtonVisible,
                    DEFAULT_AUTO_FOCUS_BUTTON_VISIBLE
                )
                isFlashButtonVisible = a.getBoolean(
                    R.styleable.zxing_zxing_flashButtonVisible,
                    DEFAULT_FLASH_BUTTON_VISIBLE
                )
                autoFocusButtonColor = a.getColor(
                    R.styleable.zxing_zxing_autoFocusButtonColor,
                    DEFAULT_AUTO_FOCUS_BUTTON_COLOR
                )
                flashButtonColor = a.getColor(
                    R.styleable.zxing_zxing_flashButtonColor,
                    DEFAULT_FLASH_BUTTON_COLOR
                )
            } finally {
                a?.recycle()
            }
        }
        addView(previewView)
        addView(viewFinderView)
        addView(autoFocusButton)
        addView(flashButton)
    }
}