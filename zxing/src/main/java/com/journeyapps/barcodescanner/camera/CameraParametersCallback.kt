package com.journeyapps.barcodescanner.camera

import android.hardware.Camera

interface CameraParametersCallback {
    fun changeCameraParameters(parameters: Camera.Parameters?): Camera.Parameters?
}