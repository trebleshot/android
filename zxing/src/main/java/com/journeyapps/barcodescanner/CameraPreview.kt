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
import com.journeyapps.barcodescanner.camera.*
import java.util.*
import kotlin.math.min


open class CameraPreview @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
) : ViewGroup(context, attrs, defStyleAttr) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private val displayRotation: Int
        get() = windowManager.defaultDisplay.rotation

    private val fireState: StateListener = object : StateListener {
        override fun previewSized() {
            for (listener in stateListeners) {
                listener.previewSized()
            }
        }

        override fun previewStarted() {
            for (listener in stateListeners) {
                listener.previewStarted()
            }
        }

        override fun previewStopped() {
            for (listener in stateListeners) {
                listener.previewStopped()
            }
        }

        override fun cameraError(error: Exception?) {
            for (listener in stateListeners) {
                listener.cameraError(error)
            }
        }

        override fun cameraClosed() {
            for (listener in stateListeners) {
                listener.cameraClosed()
            }
        }
    }

    private val stateCallback = Handler.Callback { message ->
        when (message.what) {
            R.id.zxing_prewiew_size_ready -> {
                sizePreview(message.obj as Size)
                return@Callback true
            }
            R.id.zxing_camera_error -> {
                val error = message.obj as Exception

                if (isActive()) {
                    pause()
                    fireState.cameraError(error)
                }
            }
            R.id.zxing_camera_closed -> {
                fireState.cameraClosed()
            }
        }
        false
    }

    private val stateListeners = mutableListOf<StateListener>()

    private val stateHandler = Handler(stateCallback)

    private val surfaceCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            currentSurfaceSize = null
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            currentSurfaceSize = Size(width, height)
            startPreviewIfReady()
        }
    }

    var cameraInstance: CameraInstance? = null
        private set

    var cameraSettings = CameraSettings()

    private var containerSize: Size? = null

    private var currentSurfaceSize: Size? = null

    private var displayConfiguration: DisplayConfiguration? = null

    var framingRect: Rect? = null
        private set

    var framingRectSize: Size? = null

    private var marginFraction = 0.1
        set(value) {
            require(marginFraction < 0.5) { "The margin fraction must be less than 0.5" }
            field = value
        }

    private var openedOrientation = -1

    var previewActive = false
        private set

    var previewFramingRect: Rect? = null
        private set

    private var previewScalingStrategy: PreviewScalingStrategy? = null

    private var previewSize: Size? = null

    private val rotationCallback = object : RotationCallback {
        override fun onRotationChanged(rotation: Int) {
            stateHandler.postDelayed({ rotationChanged() }, ROTATION_LISTENER_DELAY_MS.toLong())
        }
    }

    private var rotationListener = RotationListener()

    private var surfaceRect: Rect? = null

    private var surfaceView: SurfaceView? = null

    private var textureView: TextureView? = null

    private var torchOn = false

    private var useTextureView = false

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setupSurfaceView()
    }

    @SuppressLint("DrawAllocation")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        sizeContainer(Size(r - l, b - t))

        val surfaceView = surfaceView
        val surfaceRect = surfaceRect

        if (surfaceView != null) {
            if (surfaceRect == null) {
                surfaceView.layout(0, 0, width, height)
            } else {
                surfaceView.layout(surfaceRect.left, surfaceRect.top, surfaceRect.right, surfaceRect.bottom)
            }
        } else textureView?.layout(0, 0, width, height)
    }

    override fun onSaveInstanceState(): Parcelable? = Bundle().apply {
        putParcelable("super", super.onSaveInstanceState())
        putBoolean("torch", torchOn)
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            super.onRestoreInstanceState(state.getParcelable("super"))
            setTorch(state.getBoolean("torch"))
        } else {
            super.onRestoreInstanceState(state)
        }
    }

    fun initializeAttributes(attrs: AttributeSet?) {
        val styledAttributes = context.obtainStyledAttributes(attrs, R.styleable.zxing_camera_preview)
        val framingRectWidth = styledAttributes.getDimension(
            R.styleable.zxing_camera_preview_zxing_framing_rect_width, -1f
        ).toInt()
        val framingRectHeight = styledAttributes.getDimension(
            R.styleable.zxing_camera_preview_zxing_framing_rect_height, -1f
        ).toInt()

        if (framingRectWidth > 0 && framingRectHeight > 0) {
            framingRectSize = Size(framingRectWidth, framingRectHeight)
        }

        useTextureView = styledAttributes.getBoolean(
            R.styleable.zxing_camera_preview_zxing_use_texture_view, true
        )

        when (styledAttributes.getInteger(R.styleable.zxing_camera_preview_zxing_preview_scaling_strategy, -1)) {
            1 -> previewScalingStrategy = CenterCropStrategy()
            2 -> previewScalingStrategy = FitCenterStrategy()
            3 -> previewScalingStrategy = FitXYStrategy()
        }

        styledAttributes.recycle()
    }

    fun addStateListener(listener: StateListener) {
        stateListeners.add(listener)
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
            fireState.previewSized()
        }
    }

    private fun calculateFramingRect(container: Rect?, surface: Rect): Rect {
        val framingRectSize = framingRectSize
        val intersection = Rect(container)
        val intersects = intersection.intersect(surface)

        if (framingRectSize != null) {
            val horizontalMargin = Math.max(0, (intersection.width() - framingRectSize.width) / 2)
            val verticalMargin = Math.max(0, (intersection.height() - framingRectSize.height) / 2)
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

    private fun calculateTextureTransform(textureSize: Size, previewSize: Size): Matrix {
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

    private fun createCameraInstance() = CameraInstance(context).apply {
        setCameraSettings(cameraSettings)
    }

    @TargetApi(14)
    private fun createSurfaceTextureListener(): SurfaceTextureListener {
        return object : SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                onSurfaceTextureSizeChanged(surface, width, height)
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                currentSurfaceSize = Size(width, height)
                startPreviewIfReady()
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun getPreviewScalingStrategy(): PreviewScalingStrategy {
        previewScalingStrategy?.let { return it }
        // If we are using SurfaceTexture, it is safe to use centerCrop.
        // For SurfaceView, it's better to use fitCenter, otherwise the preview may overlap to
        // other views.
        return if (textureView != null) {
            CenterCropStrategy()
        } else {
            FitCenterStrategy()
        }
    }

    private fun initCamera() {
        if (cameraInstance != null) {
            Log.w(TAG, "initCamera called twice")
            return
        }

        cameraInstance = createCameraInstance().also {
            it.readyHandler = stateHandler
            it.open()
        }

        openedOrientation = displayRotation
    }

    private fun isActive() = cameraInstance != null

    open fun pause() {
        Util.validateMainThread()
        Log.d(TAG, "pause()")

        openedOrientation = -1
        cameraInstance?.let {
            it.close()
            cameraInstance = null
            previewActive = false
        } ?: run {
            stateHandler.sendEmptyMessage(R.id.zxing_camera_closed)
        }
        if (currentSurfaceSize == null) {
            surfaceView?.holder?.removeCallback(surfaceCallback)
            textureView?.surfaceTextureListener = null
        }
        containerSize = null
        previewSize = null
        previewFramingRect = null
        rotationListener.stop()
        fireState.previewStopped()
    }

    fun pauseAndWait() {
        val instance = cameraInstance
        pause()
        val startTime = System.nanoTime()
        while (instance != null && !instance.cameraClosed) {
            if (System.nanoTime() - startTime > 2000000000) {
                // Don't wait for longer than 2 seconds
                break
            }
            try {
                Thread.sleep(1)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    protected open fun previewStarted() {}

    fun resume() {
        Util.validateMainThread()
        Log.d(TAG, "resume()")

        initCamera()

        val surfaceView = surfaceView
        val textureView = textureView
        val surfaceTexture = textureView?.surfaceTexture

        if (currentSurfaceSize != null) {
            startPreviewIfReady()
        } else if (surfaceView != null) {
            // Install the callback and wait for surfaceCreated() to init the camera.
            surfaceView.holder.addCallback(surfaceCallback)
        } else if (textureView != null) {
            if (surfaceTexture != null) {
                createSurfaceTextureListener().onSurfaceTextureAvailable(
                    surfaceTexture, textureView.width, textureView.height
                )
            } else {
                textureView.surfaceTextureListener = createSurfaceTextureListener()
            }
        }

        requestLayout()
        rotationListener.listen(context, rotationCallback)
    }

    private fun rotationChanged() {
        if (isActive() && displayRotation != openedOrientation) {
            pause()
            resume()
        }
    }

    private fun setupSurfaceView() {
        if (useTextureView) textureView = TextureView(context).also {
            it.surfaceTextureListener = createSurfaceTextureListener()
            addView(it)
        } else surfaceView = SurfaceView(context).also {
            it.holder.addCallback(surfaceCallback)
            addView(it)
        }
    }

    fun setTorch(on: Boolean) {
        torchOn = on
        cameraInstance?.setTorch(on)
    }

    private fun sizeContainer(size: Size) {
        containerSize = size
        cameraInstance?.let { cameraInstance ->
            if (cameraInstance.displayConfiguration == null) {
                displayConfiguration = DisplayConfiguration(displayRotation, size).also {
                    displayConfiguration?.previewScalingStrategy = getPreviewScalingStrategy()
                    cameraInstance.displayConfiguration = displayConfiguration
                }

                cameraInstance.configureCamera()

                if (torchOn) {
                    cameraInstance.setTorch(torchOn)
                }
            }
        }
    }

    private fun sizePreview(size: Size) {
        previewSize = size

        if (containerSize != null) {
            calculateFrames()
            requestLayout()
            startPreviewIfReady()
        }
    }

    private fun startCameraPreview(surface: CameraSurface) {
        if (!previewActive) cameraInstance?.let {
            Log.i(TAG, "Starting preview")
            it.surface = surface
            it.startPreview()
            previewActive = true
            previewStarted()
            fireState.previewStarted()
        }
    }

    private fun startPreviewIfReady() {
        val surfaceView = surfaceView
        val surfaceRect = surfaceRect
        val previewSize = previewSize
        val textureView = textureView
        val surfaceTexture = textureView?.surfaceTexture

        if (currentSurfaceSize != null && previewSize != null && surfaceRect != null) {
            if (surfaceView != null && currentSurfaceSize == Size(surfaceRect.width(), surfaceRect.height())) {
                startCameraPreview(CameraSurface.create(surfaceView.holder))
            } else if (textureView != null && surfaceTexture != null) {
                val transform = calculateTextureTransform(
                    Size(textureView.width, textureView.height), previewSize
                )
                textureView.setTransform(transform)
                startCameraPreview(CameraSurface.create(surfaceTexture))
            }
        }
    }

    interface StateListener {
        fun previewSized()

        fun previewStarted()

        fun previewStopped()

        fun cameraError(error: Exception?)

        fun cameraClosed()
    }

    init {
        if (background == null) setBackgroundColor(Color.BLACK)
        initializeAttributes(attrs)
    }

    companion object {
        private val TAG = CameraPreview::class.simpleName

        private const val ROTATION_LISTENER_DELAY_MS = 250
    }
}