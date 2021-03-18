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
package com.journeyapps.barcodescanner.camera

import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.Camera.Parameters.FLASH_MODE_ON
import android.hardware.Camera.Parameters.FLASH_MODE_TORCH
import android.os.Build
import android.util.Log
import android.view.Surface
import com.google.zxing.client.android.AmbientLightManager
import com.google.zxing.client.android.camera.CameraConfigurationUtils
import com.google.zxing.client.android.camera.open.Cameras
import com.journeyapps.barcodescanner.Size
import com.journeyapps.barcodescanner.SourceData
import java.io.IOException
import java.util.*

class CameraManager(private val context: Context) {
    private val defaultCameraParameters: Camera.Parameters?
        get() = camera?.parameters?.also {
            if (defaultParameters == null) {
                defaultParameters = it.flatten()
            } else {
                it.unflatten(defaultParameters)
            }
        }

    private var autoFocusManager: AutoFocusManager? = null

    private var ambientLightManager: AmbientLightManager? = null

    var camera: Camera? = null
        private set

    private var cameraInfo: CameraInfo? = null

    var cameraRotation = -1
        private set

    var cameraSettings = CameraSettings()

    private var defaultParameters: String? = null

    var displayConfiguration: DisplayConfiguration? = null

    private var naturalPreviewSize: Size? = null

    private var previewing = false

    var resolution: Size? = null

    private fun calculateDisplayRotation(): Int {
        // http://developer.android.com/reference/android/hardware/Camera.html#setDisplayOrientation(int)
        val rotation = displayConfiguration?.rotation ?: 0
        var degrees = 0

        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
        }

        cameraInfo?.let {
            var result: Int

            if (it.facing == CameraInfo.CAMERA_FACING_FRONT) {
                result = (it.orientation + degrees) % 360
                result = (360 - result) % 360 // compensate the mirror
            } else {  // back-facing
                result = (it.orientation - degrees + 360) % 360
            }

            Log.i(TAG, "Camera Display Orientation: $result")
            return result
        } ?: throw IllegalStateException()
    }

    fun changeCameraParameters(callback: CameraParametersCallback) {
        camera?.let { camera ->
            try {
                camera.parameters = callback.changeCameraParameters(camera.parameters)
            } catch (e: RuntimeException) {
                // Camera error. Could happen if the camera is being closed.
                Log.e(TAG, "Failed to change camera parameters", e)
            }
        }
    }

    fun close() {
        camera?.let {
            it.release()
            camera = null
        }
    }

    fun configure() {
        if (camera == null) {
            throw RuntimeException("Camera not open")
        }
        setParameters()
    }

    fun getPreviewSize(): Size? {
        return when {
            naturalPreviewSize == null -> null
            isCameraRotated() -> naturalPreviewSize?.rotate()
            else -> naturalPreviewSize
        }
    }

    private fun isCameraRotated(): Boolean {
        check(cameraRotation != -1) { "Rotation not calculated yet. Call configure() first." }
        return cameraRotation % 180 != 0
    }

    fun isOpen(): Boolean = camera != null

    private fun isTorchOn(): Boolean = camera?.parameters?.flashMode.let {
        it != null && (FLASH_MODE_ON == it || FLASH_MODE_TORCH == it)
    }

    fun open() {
        camera = Cameras.open(cameraSettings.requestedCameraId)
        cameraInfo = CameraInfo()

        Camera.getCameraInfo(Cameras.getCameraId(cameraSettings.requestedCameraId), cameraInfo)
    }

    fun requestPreviewFrame(callback: PreviewCallback?) {
        camera?.let {
            if (previewing) {
                it.setOneShotPreviewCallback(CameraPreviewCallback(callback))
            }
        }
    }

    private fun setCameraDisplayOrientation(rotation: Int) {
        camera?.setDisplayOrientation(rotation)
    }

    private fun setDesiredParameters(safeMode: Boolean) {
        val parameters = defaultCameraParameters ?: run {
            Log.w(TAG, "Device error: no camera parameters are available. Proceeding without configuration.")
            return
        }

        Log.i(TAG, "Initial camera parameters: " + parameters.flatten())

        if (safeMode) {
            Log.w(TAG, "In camera config safe mode -- most settings will not be honored")
        }

        CameraConfigurationUtils.setFocus(parameters, cameraSettings.focusMode, safeMode)

        if (!safeMode) {
            CameraConfigurationUtils.setTorch(parameters, false)
            if (cameraSettings.scanInverted) {
                CameraConfigurationUtils.setInvertColor(parameters)
            }
            if (cameraSettings.barcodeSceneModeEnabled) {
                CameraConfigurationUtils.setBarcodeSceneMode(parameters)
            }
            if (cameraSettings.meteringEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    CameraConfigurationUtils.setVideoStabilization(parameters)
                    CameraConfigurationUtils.setFocusArea(parameters)
                    CameraConfigurationUtils.setMetering(parameters)
                }
            }
        }

        displayConfiguration?.getBestPreviewSize(
            getPreviewSizes(parameters),
            isCameraRotated()
        )?.let { requestedPreviewSize ->
            parameters.setPreviewSize(requestedPreviewSize.width, requestedPreviewSize.height)
        }

        if (Build.DEVICE == "glass-1") {
            // We need to set the FPS on Google Glass devices, otherwise the preview is scrambled.
            CameraConfigurationUtils.setBestPreviewFPS(parameters)
        }
        Log.i(TAG, "Final camera parameters: " + parameters.flatten())
        camera?.parameters = parameters
    }

    private fun setParameters() {
        try {
            cameraRotation = calculateDisplayRotation()
            setCameraDisplayOrientation(cameraRotation)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to set rotation.")
        }
        try {
            setDesiredParameters(false)
        } catch (e: Exception) {
            // Failed, use safe mode
            try {
                setDesiredParameters(true)
            } catch (e2: Exception) {
                // Well, darn. Give up
                Log.w(TAG, "Camera rejected even safe-mode parameters! No configuration")
            }
        }

        camera?.parameters?.previewSize?.let {
            naturalPreviewSize = Size(it.width, it.height)
            resolution = naturalPreviewSize
        }
    }

    @Throws(IOException::class)
    fun setPreviewDisplay(surface: CameraSurface?) {
        surface?.setPreview(camera)
    }

    fun setTorch(on: Boolean) {
        val camera = camera ?: return

        try {
            if (on != isTorchOn()) {
                autoFocusManager?.stop()

                val parameters = camera.parameters

                CameraConfigurationUtils.setTorch(parameters, on)

                if (cameraSettings.exposureEnabled) {
                    CameraConfigurationUtils.setBestExposure(parameters, on)
                }

                camera.parameters = parameters

                autoFocusManager?.start()
            }
        } catch (e: RuntimeException) {
            // Camera error. Could happen if the camera is being closed.
            Log.e(TAG, "Failed to set torch", e)
        }
    }

    fun startPreview() {
        camera?.takeIf { !previewing }?.let { camera ->
            camera.startPreview()

            previewing = true
            autoFocusManager = AutoFocusManager(camera, cameraSettings)
            ambientLightManager = AmbientLightManager(context, this, cameraSettings).also {
                it.start()
            }
        }
    }

    fun stopPreview() {
        autoFocusManager?.let {
            it.stop()
            autoFocusManager = null
        }

        ambientLightManager?.let {
            it.stop()
            ambientLightManager = null
        }

        camera?.takeIf { previewing }?.let {
            it.stopPreview()
            previewing = false
        }
    }

    private inner class CameraPreviewCallback(private val callback: PreviewCallback?) : Camera.PreviewCallback {
        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            val cameraResolution = resolution
            val callback = callback

            if (cameraResolution != null && callback != null) {
                try {
                    val format = camera.parameters.previewFormat
                    val source = SourceData(
                        data, cameraResolution.width, cameraResolution.height, format, cameraRotation
                    )
                    callback.onPreview(source)
                } catch (e: RuntimeException) {
                    Log.e(TAG, "Camera preview failed", e)
                    callback.onPreviewError(e)
                }
            } else {
                Log.d(TAG, "Got preview callback, but no handler or resolution available")
                callback?.onPreviewError(Exception("No resolution available"))
            }
        }
    }

    companion object {
        private val TAG = CameraManager::class.simpleName

        private fun getPreviewSizes(parameters: Camera.Parameters): List<Size> {
            val rawSupportedSizes = parameters.supportedPreviewSizes
            val previewSizes: MutableList<Size> = ArrayList()

            if (rawSupportedSizes == null) {
                val defaultSize = parameters.previewSize

                if (defaultSize != null) {
                    // Work around potential platform bugs
                    previewSizes.add(Size(defaultSize.width, defaultSize.height))
                }

                return previewSizes
            }

            for (size in rawSupportedSizes) {
                previewSizes.add(Size(size.width, size.height))
            }

            return previewSizes
        }

        fun newInstance() {

        }
    }
}