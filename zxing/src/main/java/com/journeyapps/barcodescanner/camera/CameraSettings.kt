package com.journeyapps.barcodescanner.camera

import com.google.zxing.client.android.camera.open.OpenCameraInterface

class CameraSettings {
    var requestedCameraId = OpenCameraInterface.NO_REQUESTED_CAMERA

    var scanInverted = false

    var barcodeSceneModeEnabled = false

    var meteringEnabled = false

    var autoFocusEnabled = true
        set(value) {
            field = value
            focusMode = if (value && continuousFocusEnabled) {
                FocusMode.CONTINUOUS
            } else if (autoFocusEnabled) {
                FocusMode.AUTO
            } else {
                null
            }
        }

    var continuousFocusEnabled = false
        set(value) {
            field = value
            focusMode = when {
                value -> FocusMode.CONTINUOUS
                autoFocusEnabled -> FocusMode.AUTO
                else -> null
            }
        }

    var exposureEnabled = false

    var autoTorchEnabled = false

    var focusMode: FocusMode? = FocusMode.AUTO

    enum class FocusMode {
        AUTO, CONTINUOUS, INFINITY, MACRO
    }
}