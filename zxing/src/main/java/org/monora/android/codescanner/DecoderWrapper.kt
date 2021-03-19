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

import android.hardware.Camera
import android.hardware.Camera.CameraInfo

class DecoderWrapper(
    val camera: Camera,
    val cameraInfo: CameraInfo,
    val decoder: Decoder,
    val imageSize: CartesianCoordinate,
    val previewSize: CartesianCoordinate,
    val viewSize: CartesianCoordinate,
    val displayOrientation: Int,
    val autoFocusSupported: Boolean,
    val flashSupported: Boolean,
) {
    val reverseHorizontal: Boolean = cameraInfo.facing == CameraInfo.CAMERA_FACING_FRONT

    fun release() {
        camera.release()
        decoder.shutdown()
    }
}