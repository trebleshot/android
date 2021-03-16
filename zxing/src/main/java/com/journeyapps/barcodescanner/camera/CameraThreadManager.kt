package com.journeyapps.barcodescanner.camera

import android.os.Handler
import android.os.HandlerThread

open class CameraThreadManager private constructor() {
    private val lock = Any()

    private var handler: Handler? = null

    private var thread: HandlerThread? = null

    private var openCount = 0

    private fun checkRunning() {
        synchronized(lock) {
            if (handler == null) {
                check(openCount > 0) { "CameraThread is not open" }
                thread = HandlerThread("CameraThread").also {
                    it.start()
                    handler = Handler(it.looper)
                }
            }
        }
    }

    fun decrementInstances() {
        synchronized(lock) {
            openCount -= 1
            if (openCount == 0) {
                quit()
            }
        }
    }

    fun enqueue(runnable: Runnable) {
        synchronized(lock) {
            checkRunning()
            handler?.post(runnable)
        }
    }

    protected fun enqueueDelayed(runnable: Runnable, delayMillis: Long) {
        synchronized(lock) {
            checkRunning()
            handler?.postDelayed(runnable, delayMillis)
        }
    }

    fun incrementAndEnqueue(runner: Runnable) {
        synchronized(lock) {
            openCount += 1
            enqueue(runner)
        }
    }

    private fun quit() {
        synchronized(lock) {
            thread?.let {
                it.quit()
                thread = null
                handler = null
            }
        }
    }

    companion object {
        private val TAG = CameraThreadManager::class.simpleName

        val INSTANCE: CameraThreadManager by lazy {
            CameraThreadManager()
        }
    }
}