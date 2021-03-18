package com.journeyapps.barcodescanner

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.*
import android.graphics.*
import android.os.*
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.TextureView.SurfaceTextureListener
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.camera.*
import java.util.*
import kotlin.math.max
import kotlin.math.min

class BarcodeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val cameraView: View

    private val displayRotation: Int
        get() = windowManager.defaultDisplay.rotation

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            currentSurfaceSize = null
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            currentSurfaceSize = Size(width, height)
        }
    }

    private var containerSize: Size? = null

    var currentSurfaceSize: Size? = null
        private set

    private var displayConfiguration: DisplayConfiguration? = null

    var framingRect: Rect? = null
        private set

    var framingRectSize: Size? = null

    private var marginFraction = 0.1
        set(value) {
            require(marginFraction < 0.5) { "The margin fraction must be less than 0.5" }
            field = value
        }

    var previewFramingRect: Rect? = null
        private set

    private var previewScalingStrategy: PreviewScalingStrategy? = null

    var previewSize: Size? = null
        private set

    var surfaceRect: Rect? = null
        private set

    private var torchOn = false

    // ViewfinderView starts here

    private val paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val maskColor: Int

    private val resultColor: Int

    private val laserColor: Int

    private val resultPointColor: Int

    private var resultBitmap: Bitmap? = null

    private var scannerAlpha: Int = 0

    private var possibleResultPoints = ArrayList<ResultPoint>(MAX_RESULT_POINTS)

    private var lastPossibleResultPoints = ArrayList<ResultPoint>(MAX_RESULT_POINTS)

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        addView(cameraView)
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        containerSize = Size(r - l, b - t)

        val surfaceRect = surfaceRect

        when (cameraView) {
            is SurfaceView -> if (surfaceRect == null) {
                cameraView.layout(0, 0, width, height)
            } else {
                cameraView.layout(surfaceRect.left, surfaceRect.top, surfaceRect.right, surfaceRect.bottom)
            }
            is TextureView -> cameraView.layout(0, 0, width, height)
        }
    }

    private fun calculateFrames() {
        val containerSize = containerSize
        val previewSize = previewSize
        val displayConfiguration = displayConfiguration

        if (containerSize == null || previewSize == null || displayConfiguration == null) {
            previewFramingRect = null
            framingRect = null
            surfaceRect = null

            throw IllegalStateException("containerSize or previewSize is not set yet")
        }

        val previewWidth = previewSize.width
        val previewHeight = previewSize.height
        val width = containerSize.width
        val height = containerSize.height
        val surfaceRect = displayConfiguration.scalePreview(previewSize).also { surfaceRect = it }
        val container = Rect(0, 0, width, height)
        val frameInPreview = calculateFramingRect(container, surfaceRect).also {
            framingRect = Rect(it)
            it.offset(-surfaceRect.left, -surfaceRect.top)
        }
        val previewFramingRect = Rect(
            frameInPreview.left * previewWidth / surfaceRect.width(),
            frameInPreview.top * previewHeight / surfaceRect.height(),
            frameInPreview.right * previewWidth / surfaceRect.width(),
            frameInPreview.bottom * previewHeight / surfaceRect.height()
        ).also { previewFramingRect = it }
        if (previewFramingRect.width() <= 0 || previewFramingRect.height() <= 0) {
            this.previewFramingRect = null
            framingRect = null
            Log.w(TAG, "Preview frame is too small")
        } else {
            invalidate()
        }
    }

    private fun calculateFramingRect(container: Rect?, surface: Rect): Rect {
        val framingRectSize = framingRectSize
        val intersection = Rect(container)
        val intersects = intersection.intersect(surface)

        if (framingRectSize != null) {
            val horizontalMargin = max(0, (intersection.width() - framingRectSize.width) / 2)
            val verticalMargin = max(0, (intersection.height() - framingRectSize.height) / 2)
            intersection.inset(horizontalMargin, verticalMargin)
            return intersection
        }

        val margin = min(intersection.width() * marginFraction, intersection.height() * marginFraction).toInt()

        intersection.inset(margin, margin)
        if (intersection.height() > intersection.width()) {
            intersection.inset(0, (intersection.height() - intersection.width()) / 2)
        } else if (intersection.width() > intersection.height()) {
            intersection.inset((intersection.width() - intersection.height()) / 2, 0)
        }
        return intersection
    }

    fun calculateTextureTransform(textureSize: Size, previewSize: Size): Matrix {
        val ratioTexture = textureSize.width.toFloat() / textureSize.height.toFloat()
        val ratioPreview = previewSize.width.toFloat() / previewSize.height.toFloat()
        val scaleX: Float
        val scaleY: Float

        if (ratioTexture < ratioPreview) {
            scaleX = ratioPreview / ratioTexture
            scaleY = 1f
        } else {
            scaleX = 1f
            scaleY = ratioTexture / ratioPreview
        }

        val scaledWidth = textureSize.width * scaleX
        val scaledHeight = textureSize.height * scaleY
        val dx = (textureSize.width - scaledWidth) / 2
        val dy = (textureSize.height - scaledHeight) / 2

        return Matrix().apply {
            setScale(scaleX, scaleY)
            postTranslate(dx, dy)
        }
    }

    @TargetApi(14)
    private fun createSurfaceTextureListener(): SurfaceTextureListener {
        return object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                onSurfaceTextureSizeChanged(surface, width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                currentSurfaceSize = Size(width, height)
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun getPreviewScalingStrategy(): PreviewScalingStrategy {
        previewScalingStrategy?.let { return it }
        return if (cameraView is TextureView) CenterCropStrategy() else FitCenterStrategy()
    }

    private fun sizePreview(size: Size) {
        previewSize = size

        if (containerSize != null) {
            calculateFrames() // only here
            requestLayout()
        }
    }

    // BarcodeView method start here

    // ViewfinderView starts here

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)

        val frame = framingRect ?: return
        val previewFrame = previewFramingRect ?: return
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
            if (lastPossibleResultPoints.isNotEmpty()) {
                paint.alpha = CURRENT_POINT_OPACITY / 2
                paint.color = resultPointColor
                val radius = POINT_SIZE / 2.0f

                for (point in lastPossibleResultPoints) {
                    canvas.drawCircle(
                        (frameLeft + (point.x * scaleX).toInt()).toFloat(),
                        (frameTop + (point.y * scaleY).toInt()).toFloat(),
                        radius,
                        paint
                    )
                }
                lastPossibleResultPoints.clear()
            }

            // draw current possible result points
            if (possibleResultPoints.isNotEmpty()) {
                paint.alpha = CURRENT_POINT_OPACITY
                paint.color = resultPointColor

                for (point in possibleResultPoints) {
                    canvas.drawCircle(
                        (frameLeft + (point.x * scaleX).toInt()).toFloat(),
                        (frameTop + (point.y * scaleY).toInt()).toFloat(),
                        POINT_SIZE.toFloat(),
                        paint
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

    fun addPossibleResultPoint(point: ResultPoint) {
        if (possibleResultPoints.size < MAX_RESULT_POINTS) possibleResultPoints.add(point)
    }

    fun drawResultBitmap(result: Bitmap?) {
        resultBitmap = result
        invalidate()
    }

    fun drawViewfinder() {
        val resultBitmap = resultBitmap
        this.resultBitmap = null
        resultBitmap?.recycle()
        invalidate()
    }

    companion object {
        private val TAG = BarcodeView::class.simpleName

        private const val ANIMATION_DELAY = 80L

        private const val CURRENT_POINT_OPACITY = 0xA0

        private const val MAX_RESULT_POINTS = 20

        private const val POINT_SIZE = 6

        private val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
    }

    init {
        val attributes = context.obtainStyledAttributes(attrs, R.styleable.zxing_camera_preview)
        val framingRectWidth = attributes.getDimension(
            R.styleable.zxing_camera_preview_zxing_framing_rect_width, -1f
        ).toInt()
        val framingRectHeight = attributes.getDimension(
            R.styleable.zxing_camera_preview_zxing_framing_rect_height, -1f
        ).toInt()

        if (framingRectWidth > 0 && framingRectHeight > 0) {
            framingRectSize = Size(framingRectWidth, framingRectHeight)
        }
        val useTextureView = attributes.getBoolean(
            R.styleable.zxing_camera_preview_zxing_use_texture_view, true
        )
        previewScalingStrategy = when (
            attributes.getInteger(R.styleable.zxing_camera_preview_zxing_preview_scaling_strategy, -1)
        ) {
            1 -> CenterCropStrategy()
            2 -> FitCenterStrategy()
            3 -> FitXYStrategy()
            else -> previewScalingStrategy
        }
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

        if (background == null) {
            setBackgroundColor(Color.BLACK)
        }

        cameraView = if (useTextureView) TextureView(context).also {
            it.surfaceTextureListener = createSurfaceTextureListener()
        } else SurfaceView(context).also {
            it.holder.addCallback(surfaceCallback)
        }
    }
}