package com.journeyapps.barcodescanner.camera

import com.google.zxing.client.android.camera.open.Cameras

class CameraSettings {
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

    var autoTorchEnabled = false

    var barcodeSceneModeEnabled = false

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

    var focusMode: FocusMode? = FocusMode.AUTO

    var meteringEnabled = false

    var requestedCameraId = Cameras.NO_REQUESTED_CAMERA

    var scanInverted = false

    enum class FocusMode {
        AUTO, CONTINUOUS, INFINITY, MACRO
    }
}