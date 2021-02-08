package com.genonbeta.android.database

class Progress {
    var mTotal = 0
    var mCurrent = 0
    fun addToCurrent(step: Int) {
        mCurrent += step
    }

    fun addToTotal(total: Int) {
        mTotal += total
    }

    fun getCurrent(): Int {
        return mCurrent
    }

    fun getTotal(): Int {
        return mTotal
    }

    fun setCurrent(current: Int) {
        mCurrent = current
    }

    fun setTotal(total: Int) {
        mTotal = total
    }

    interface Listener {
        fun getProgress(): Progress?
        fun setProgress(progress: Progress?)
        fun onProgressChange(progress: Progress): Boolean
    }

    abstract class SimpleListener : Listener {
        private var mProgress: Progress? = null

        override fun getProgress(): Progress? {
            return mProgress
        }

        override fun setProgress(progress: Progress?) {
            mProgress = progress
        }
    }

    companion object {
        fun dissect(listener: Listener): Progress = listener.getProgress() ?: run {
            Progress().also { listener.setProgress(it) }
        }

        fun addToCurrent(listener: Listener?, step: Int) = listener?.let { dissect(it).addToCurrent(step) }

        fun addToTotal(listener: Listener?, total: Int) = listener?.let { dissect(it).addToTotal(total) }

        fun call(listener: Listener?, addToCurrent: Int): Boolean {
            listener?.let {
                return it.onProgressChange(dissect(it).also { progress -> progress.addToCurrent(addToCurrent) })
            }

            return true
        }
    }
}