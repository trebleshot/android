/*
 * Copyright (C) 2010 ZXing authors
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
package com.google.zxing.client.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Vibrator
import android.util.Log
import com.journeyapps.barcodescanner.R
import java.io.IOException

/**
 * Manages beeps and vibrations.
 */
class BeepManager(activity: Activity) {
    private val context: Context

    /**
     * Call updatePrefs() after setting this.
     *
     * If the device is in silent mode, it will not beep.
     */
    var isBeepEnabled = true

    /**
     * Call updatePrefs() after setting this.
     */
    private var isVibrateEnabled = false

    @SuppressLint("MissingPermission")
    @Synchronized
    fun playBeepSoundAndVibrate() {
        if (isBeepEnabled) {
            playBeepSound()
        }
        if (isVibrateEnabled) {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
            vibrator?.vibrate(VIBRATE_DURATION)
        }
    }

    fun playBeepSound(): MediaPlayer? {
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
        mediaPlayer.setOnCompletionListener { mp ->
            mp.stop()
            mp.release()
        }
        mediaPlayer.setOnErrorListener { mp, what, extra ->
            Log.w(TAG, "Failed to beep $what, $extra")
            // possibly media player error, so release and recreate
            mp.stop()
            mp.release()
            true
        }
        return try {
            val file = context.resources.openRawResourceFd(R.raw.zxing_beep)
            try {
                mediaPlayer.setDataSource(file.fileDescriptor, file.startOffset, file.length)
            } finally {
                file.close()
            }
            mediaPlayer.setVolume(BEEP_VOLUME, BEEP_VOLUME)
            mediaPlayer.prepare()
            mediaPlayer.start()
            mediaPlayer
        } catch (ioe: IOException) {
            Log.w(TAG, ioe)
            mediaPlayer.release()
            null
        }
    }

    companion object {
        private val TAG = BeepManager::class.simpleName

        private const val BEEP_VOLUME = 0.10f

        private const val VIBRATE_DURATION = 200L
    }

    init {
        activity.volumeControlStream = AudioManager.STREAM_MUSIC

        // We do not keep a reference to the Activity itself, to prevent leaks
        context = activity.applicationContext
    }
}