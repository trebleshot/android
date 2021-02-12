package com.genonbeta.android.database

class Progress(var total: Int = 0, var progress: Int = 0) {
    interface Context {
        var progress: Progress?
        
        fun onProgressChange(progress: Progress): Boolean

        fun getProgress(): Int

        fun getTotal(): Int

        fun increaseBy(increase: Int): Boolean

        fun increaseTotalBy(increase: Int): Boolean
    }

    abstract class SimpleContext : Context {
        override var progress: Progress? = null

        override fun getProgress(): Int = dissect(this).progress

        override fun getTotal(): Int = dissect(this).total

        override fun increaseBy(increase: Int): Boolean {
            val progress = dissect(this)
            progress.progress += increase
            return onProgressChange(progress)
        }

        override fun increaseTotalBy(increase: Int): Boolean {
            val progress = dissect(this)
            progress.total += increase
            return onProgressChange(progress)
        }
    }

    companion object {
        fun dissect(listener: Context): Progress = listener.progress ?: run {
            Progress().also { listener.progress = it }
        }
    }
}