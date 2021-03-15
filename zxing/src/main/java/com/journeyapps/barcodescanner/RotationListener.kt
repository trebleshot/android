package com.journeyapps.barcodescanner

import android.content.*
import android.hardware.SensorManager
import android.view.OrientationEventListener
import android.view.WindowManager

/**
 * Hack to detect when screen rotation is reversed, since that does not cause a configuration change.
 *
 *
 * If it is changed through something other than the sensor (e.g. programmatically), this may not work.
 *
 *
 * See http://stackoverflow.com/q/9909037
 */
class RotationListener {
    private var lastRotation = 0

    private var windowManager: WindowManager? = null

    private var orientationEventListener: OrientationEventListener? = null

    private var rotationCallback: RotationCallback? = null

    fun listen(context: Context, callback: RotationCallback?) {
        val appContext = context.applicationContext
        stop()

        rotationCallback = callback
        windowManager = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        orientationEventListener = object : OrientationEventListener(appContext, SensorManager.SENSOR_DELAY_NORMAL) {
            override fun onOrientationChanged(orientation: Int) {
                val localWindowManager = windowManager
                val localCallback = this@RotationListener.rotationCallback

                if (localWindowManager != null && localCallback != null) {
                    val newRotation = localWindowManager.defaultDisplay.rotation
                    if (newRotation != lastRotation) {
                        lastRotation = newRotation
                        localCallback.onRotationChanged(newRotation)
                    }
                }
            }
        }.also { it.enable() }
        lastRotation = windowManager?.defaultDisplay?.rotation ?: lastRotation
    }

    fun stop() {
        orientationEventListener?.disable()

        orientationEventListener = null
        windowManager = null
        rotationCallback = null
    }
}