package com.journeyapps.barcodescanner.camera

import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.view.SurfaceHolder
import java.io.IOException

interface CameraSurface {
    @Throws(IOException::class)
    fun setPreview(camera: Camera?)

    companion object {
        fun create(surfaceTexture: SurfaceTexture): CameraSurface = TextureCameraSurface(surfaceTexture)

        fun create(surfaceHolder: SurfaceHolder): CameraSurface = HolderCameraSurface(surfaceHolder)
    }
}

private class TextureCameraSurface(val surfaceTexture: SurfaceTexture) : CameraSurface {
    override fun setPreview(camera: Camera?) {
        camera?.setPreviewTexture(surfaceTexture)
    }
}

private class HolderCameraSurface(val surfaceHolder: SurfaceHolder) : CameraSurface {
    override fun setPreview(camera: Camera?) {
        camera?.setPreviewDisplay(surfaceHolder)
    }
}