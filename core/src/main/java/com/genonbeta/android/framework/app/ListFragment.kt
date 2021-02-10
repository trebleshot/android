/*
 * Copyright (C) 2020 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.genonbeta.android.framework.app

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import androidx.transition.TransitionManager
import com.genonbeta.android.framework.R
import com.genonbeta.android.framework.widget.ListAdapterBase

/**
 * Created by: veli
 * Date: 12/3/16 9:57 AM
 */
abstract class ListFragment<Z : ViewGroup, T, E : ListAdapterBase<T>> : Fragment(), ListFragmentBase<T> {
    lateinit var adapter: E
        private set

    open lateinit var listView: Z
        protected set

    lateinit var emptyListActionButton: Button

    lateinit var emptyListContainerView: ViewGroup

    lateinit var emptyListImageView: ImageView

    lateinit var emptyListTextView: TextView

    lateinit var progressBar: ProgressBar

    private val refreshLoaderCallback = RefreshLoaderCallback()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViewDefaultsFromMainView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        LoaderManager.getInstance(this).initLoader(TASK_ID_REFRESH, null, refreshLoaderCallback)
    }

    protected open fun onPrepareRefreshingList() {}

    protected open fun onListRefreshed() {}

    fun createLoader(): AsyncTaskLoader<MutableList<T>> {
        return ListLoader(context!!, adapter)
    }

    protected abstract fun ensureList()

    protected fun findViewDefaultsFrom(view: View?) {
        view?.let {
            listView = it.findViewById<View?>(R.id.genfw_customListFragment_listView) as Z
            emptyListContainerView = it.findViewById<View?>(R.id.genfw_customListFragment_emptyView) as ViewGroup
            emptyListTextView = it.findViewById<View?>(R.id.genfw_customListFragment_emptyTextView) as TextView
            emptyListImageView = it.findViewById<View?>(R.id.genfw_customListFragment_emptyImageView) as ImageView
            emptyListActionButton = it.findViewById<View?>(R.id.genfw_customListFragment_emptyActionButton) as Button
            progressBar = it.findViewById<View?>(R.id.genfw_customListFragment_progressBar) as ProgressBar
        }
    }

    protected fun findViewDefaultsFromMainView() {
        findViewDefaultsFrom(view)
    }

    protected abstract fun generateDefaultView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedState: Bundle?,
    ): View?

    fun getListAdapter(): E? {
        return adapter
    }

    override fun refreshList() {
        refreshLoaderCallback.requestRefresh()
    }

    protected fun setListAdapter(adapter: E) {
        val hadAdapter = this.adapter != null
        this.adapter = adapter
        setListAdapter(adapter, hadAdapter)
    }

    protected abstract fun setListAdapter(adapter: E?, hadAdapter: Boolean)

    fun setListLoading(loading: Boolean) {
        setListLoading(loading, true)
    }

    private fun setListLoading(loading: Boolean, animate: Boolean) {
        ensureList()
        // progress is shown == loading
        // container is not shown == progress cannot be shown
        if (progressBar.visibility == View.VISIBLE == loading || emptyListContainerView.visibility != View.VISIBLE)
            return

        progressBar.visibility = if (loading) View.VISIBLE else View.GONE

        if (animate)
            TransitionManager.beginDelayedTransition(emptyListContainerView)
    }

    protected fun setListShown(shown: Boolean) {
        setListShown(shown, true)
    }

    protected fun setListShown(shown: Boolean, animate: Boolean) {
        if (listView.visibility == View.VISIBLE != shown) {
            listView.visibility = if (shown) View.VISIBLE else View.GONE

            if (animate)
                listView.startAnimation(
                    AnimationUtils.loadAnimation(
                        context, if (shown) android.R.anim.fade_in else android.R.anim.fade_out
                    )
                )
        }

        if (emptyListContainerView.visibility == View.VISIBLE == shown)
            emptyListContainerView.visibility = if (shown) View.GONE else View.VISIBLE
    }

    fun showEmptyListActionButton(show: Boolean) {
        emptyListActionButton.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun useEmptyListActionButton(buttonText: String?, clickListener: View.OnClickListener?) {
        emptyListActionButton.text = buttonText
        emptyListActionButton.setOnClickListener(clickListener)

        showEmptyListActionButton(true)
    }

    private inner class RefreshLoaderCallback : LoaderManager.LoaderCallbacks<MutableList<T>> {
        var running = false
            private set

        var reloadRequested = false
            private set

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<MutableList<T>> {
            reloadRequested = false
            running = true

            setListShown(adapter?.let { it.getItemCount() == 0 } ?: false)
            setListLoading(true)
            return createLoader()
        }

        override fun onLoadFinished(loader: Loader<MutableList<T>>, data: MutableList<T>) {
            if (isResumed) {
                onPrepareRefreshingList()
                adapter?.onUpdate(data)
                adapter?.onDataSetChanged()
                setListLoading(false)
                onListRefreshed()
            }

            if (reloadRequested)
                refresh()
            running = false
        }

        override fun onLoaderReset(loader: Loader<MutableList<T>>) {}

        fun refresh() {
            LoaderManager.getInstance(this@ListFragment)
                .restartLoader(TASK_ID_REFRESH, null, refreshLoaderCallback)
        }

        fun requestRefresh(): Boolean {
            if (running && reloadRequested)
                return false

            if (!running)
                refresh()
            else
                reloadRequested = true

            return true
        }
    }

    class ListLoader<G>(
        context: Context, val adapter: ListAdapterBase<G>?,
    ) : AsyncTaskLoader<MutableList<G>>(context) {
        override fun onStartLoading() {
            super.onStartLoading()
            forceLoad()
        }

        override fun loadInBackground(): MutableList<G> {
            return adapter?.onLoad() ?: ArrayList()
        }
    }

    companion object {
        val TAG = ListFragment::class.java.simpleName
        val LAYOUT_DEFAULT_EMPTY_LIST_VIEW: Int = R.layout.genfw_layout_listfragment_emptyview
        const val TASK_ID_REFRESH = 0
    }
}