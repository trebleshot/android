/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.journeyapps.barcodescanner

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.CameraPreview.StateListener
import com.journeyapps.barcodescanner.ViewfinderView
import java.util.*

/**
 * This view is overlaid on top of the camera preview. It adds the viewfinder rectangle and partial
 * transparency outside it, as well as the laser scanner animation and result points.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
open class ViewfinderView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    protected val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    protected val maskColor: Int

    protected val resultColor: Int

    protected val laserColor: Int

    protected val resultPointColor: Int

    protected var resultBitmap: Bitmap? = null

    protected var scannerAlpha: Int

    protected var possibleResultPoints: MutableList<ResultPoint>

    protected var lastPossibleResultPoints: MutableList<ResultPoint>

    var cameraPreview: CameraPreview? = null
        set(value) {
            field = value
            value?.addStateListener(
                object : StateListener {
                    override fun previewSized() {
                        refreshSizes()
                        invalidate()
                    }

                    override fun previewStarted() {

                    }

                    override fun previewStopped() {

                    }

                    override fun cameraError(error: Exception?) {

                    }

                    override fun cameraClosed() {

                    }
                }
            )
        }

    // Cache the framingRect and previewFramingRect, so that we can still draw it after the preview
    // stopped.
    protected var framingRect: Rect? = null

    protected var previewFramingRect: Rect? = null

    protected fun refreshSizes() {
        val framingRect = cameraPreview?.framingRect
        val previewFramingRect = cameraPreview?.previewFramingRect

        if (framingRect != null && previewFramingRect != null) {
            this.framingRect = framingRect
            this.previewFramingRect = previewFramingRect
        }
    }

    public override fun onDraw(canvas: Canvas) {
        refreshSizes()
        val frame: Rect = framingRect ?: return
        val previewFrame: Rect = previewFramingRect ?: return
        val width = width
        val height = height
        val resultBitmap = resultBitmap

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.color = if (resultBitmap != null) resultColor else maskColor
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), paint)
        canvas.drawRect(0f, frame.top.toFloat(), frame.left.toFloat(), (frame.bottom + 1).toFloat(), paint)
        canvas.drawRect(
            (frame.right + 1).toFloat(),
            frame.top.toFloat(),
            width.toFloat(),
            (frame.bottom + 1).toFloat(),
            paint
        )
        canvas.drawRect(0f, (frame.bottom + 1).toFloat(), width.toFloat(), height.toFloat(), paint)
        if (resultBitmap != null) {
            // Draw the opaque result bitmap over the scanning rectangle
            paint.alpha = CURRENT_POINT_OPACITY
            canvas.drawBitmap(resultBitmap, null, frame, paint)
        } else {
            // Draw a red "laser scanner" line through the middle to show decoding is active
            paint.color = laserColor
            paint.alpha = SCANNER_ALPHA[scannerAlpha]
            scannerAlpha = (scannerAlpha + 1) % SCANNER_ALPHA.size
            val middle = frame.height() / 2 + frame.top
            canvas.drawRect(
                (frame.left + 2).toFloat(),
                (middle - 1).toFloat(),
                (frame.right - 1).toFloat(),
                (middle + 2).toFloat(),
                paint
            )
            val scaleX = frame.width() / previewFrame.width().toFloat()
            val scaleY = frame.height() / previewFrame.height().toFloat()
            val frameLeft = frame.left
            val frameTop = frame.top

            // draw the last possible result points
            if (!lastPossibleResultPoints.isEmpty()) {
                paint.alpha = CURRENT_POINT_OPACITY / 2
                paint.color = resultPointColor
                val radius = POINT_SIZE / 2.0f
                for (point in lastPossibleResultPoints) {
                    canvas.drawCircle(
                        (
                                frameLeft + (point.x * scaleX).toInt()).toFloat(), (
                                frameTop + (point.y * scaleY).toInt()).toFloat(),
                        radius, paint
                    )
                }
                lastPossibleResultPoints.clear()
            }

            // draw current possible result points
            if (!possibleResultPoints.isEmpty()) {
                paint.alpha = CURRENT_POINT_OPACITY
                paint.color = resultPointColor
                for (point in possibleResultPoints) {
                    canvas.drawCircle(
                        (
                                frameLeft + (point.x * scaleX).toInt()).toFloat(), (
                                frameTop + (point.y * scaleY).toInt()).toFloat(),
                        POINT_SIZE.toFloat(), paint
                    )
                }

                // swap and clear buffers
                val temp = possibleResultPoints
                possibleResultPoints = lastPossibleResultPoints
                lastPossibleResultPoints = temp
                possibleResultPoints.clear()
            }

            // Request another update at the animation interval, but only repaint the laser line,
            // not the entire viewfinder mask.
            postInvalidateDelayed(
                ANIMATION_DELAY,
                frame.left - POINT_SIZE,
                frame.top - POINT_SIZE,
                frame.right + POINT_SIZE,
                frame.bottom + POINT_SIZE
            )
        }
    }

    fun drawViewfinder() {
        val resultBitmap = resultBitmap
        this.resultBitmap = null
        resultBitmap?.recycle()
        invalidate()
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live scanning display.
     *
     * @param result An image of the result.
     */
    fun drawResultBitmap(result: Bitmap?) {
        resultBitmap = result
        invalidate()
    }

    /**
     * Only call from the UI thread.
     *
     * @param point a point to draw, relative to the preview frame
     */
    fun addPossibleResultPoint(point: ResultPoint) {
        if (possibleResultPoints.size < MAX_RESULT_POINTS) possibleResultPoints.add(point)
    }

    companion object {
        protected val TAG = ViewfinderView::class.java.simpleName
        protected val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
        protected const val ANIMATION_DELAY = 80L
        protected const val CURRENT_POINT_OPACITY = 0xA0
        protected const val MAX_RESULT_POINTS = 20
        protected const val POINT_SIZE = 6
    }

    // This constructor is used when the class is built from an XML resource.
    init {
        // Initialize these once for performance rather than calling them every time in onDraw().
        val resources = resources
        val attributes = getContext().obtainStyledAttributes(attrs, R.styleable.zxing_finder)

        maskColor = attributes.getColor(
            R.styleable.zxing_finder_zxing_viewfinder_mask,
            resources.getColor(R.color.zxing_viewfinder_mask, context.theme)
        )
        resultColor = attributes.getColor(
            R.styleable.zxing_finder_zxing_result_view,
            resources.getColor(R.color.zxing_result_view, context.theme)
        )
        laserColor = attributes.getColor(
            R.styleable.zxing_finder_zxing_viewfinder_laser,
            resources.getColor(R.color.zxing_viewfinder_laser, context.theme)
        )
        resultPointColor = attributes.getColor(
            R.styleable.zxing_finder_zxing_possible_result_points,
            resources.getColor(R.color.zxing_possible_result_points, context.theme)
        )
        attributes.recycle()
        scannerAlpha = 0
        possibleResultPoints = ArrayList(MAX_RESULT_POINTS)
        lastPossibleResultPoints = ArrayList(MAX_RESULT_POINTS)
    }
}