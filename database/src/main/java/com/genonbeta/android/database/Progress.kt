package com.genonbeta.android.database

class Progress(var total: Int = 0, var current: Int = 0) {
    fun addToCurrent(step: Int) {
        current += step
    }

    fun addToTotal(step: Int) {
        total += step
    }

    interface Listener {
        var progress: Progress?

        fun onProgressChange(progress: Progress): Boolean
    }

    abstract class SimpleListener : Listener {
        override var progress: Progress? = null
    }

    companion object {
        fun dissect(listener: Listener): Progress = listener.progress ?: run {
            Progress().also { listener.progress = it }
        }

        fun addToCurrent(listener: Listener?, step: Int) = listener?.let { dissect(it).addToCurrent(step) }

        fun addToTotal(listener: Listener?, total: Int) = listener?.let { dissect(it).addToTotal(total) }

        fun call(listener: Listener?, addToCurrent: Int): Boolean = listener?.let {
            return it.onProgressChange(dissect(it).also { progress -> progress.addToCurrent(addToCurrent) })
        } ?: false
    }
}