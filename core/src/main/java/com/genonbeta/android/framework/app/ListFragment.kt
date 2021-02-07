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
    var adapter: E? = null
        private set

    var listView: Z? = null
        get() {
            ensureList()
            return getListViewInternal()
        }
        private set

    private var emptyListContainerView: ViewGroup? = null

    private var emptyListTextView: TextView? = null

    private var emptyListImageView: ImageView? = null

    private var progressBar: ProgressBar? = null

    private var emptyListActionButton: Button? = null

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
        return ListLoader(adapter)
    }

    protected abstract fun ensureList()

    protected fun findViewDefaultsFrom(view: View?) {
        view?.let {
            setListView(it.findViewById<View?>(R.id.genfw_customListFragment_listView) as Z)
            setEmptyListContainerView(it.findViewById<View?>(R.id.genfw_customListFragment_emptyView) as ViewGroup)
            setEmptyListTextView(it.findViewById<View?>(R.id.genfw_customListFragment_emptyTextView) as TextView)
            setEmptyListImageView(it.findViewById<View?>(R.id.genfw_customListFragment_emptyImageView) as ImageView)
            setEmptyListActionButton(it.findViewById<View?>(R.id.genfw_customListFragment_emptyActionButton) as Button)
            setProgressBar(it.findViewById<View?>(R.id.genfw_customListFragment_progressBar) as ProgressBar)
        }
    }

    protected fun findViewDefaultsFromMainView() {
        findViewDefaultsFrom(view)
    }

    protected abstract fun generateDefaultView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedState: Bundle?
    ): View?

    fun getEmptyListContainerView(): ViewGroup? {
        ensureList()
        return emptyListContainerView
    }

    fun getEmptyListImageView(): ImageView? {
        ensureList()
        return emptyListImageView
    }

    fun getEmptyListTextView(): TextView? {
        ensureList()
        return emptyListTextView
    }

    fun getEmptyListActionButton(): Button {
        ensureList()
        return emptyListActionButton
    }

    fun getListAdapter(): E? {
        return adapter
    }

    protected fun getListViewInternal(): Z? {
        return listView
    }

    fun getLoaderCallbackRefresh(): RefreshLoaderCallback? {
        return refreshLoaderCallback
    }

    fun getProgressBar(): ProgressBar? {
        ensureList()
        return progressBar
    }

    override fun refreshList() {
        getLoaderCallbackRefresh().requestRefresh()
    }

    protected fun setEmptyListContainerView(container: ViewGroup?) {
        emptyListContainerView = container
    }

    protected fun setEmptyListActionButton(button: Button?) {
        emptyListActionButton = button
    }

    protected fun setEmptyListImage(resId: Int) {
        getEmptyListImageView().setImageResource(resId)
    }

    protected fun setEmptyListImageView(view: ImageView?) {
        emptyListImageView = view
    }

    protected fun setEmptyListText(text: CharSequence?) {
        getEmptyListTextView().setText(text)
    }

    protected fun setEmptyListTextView(view: TextView?) {
        emptyListTextView = view
    }

    protected fun setListAdapter(adapter: E?) {
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
        val progressBar: ProgressBar? = getProgressBar()
        val emptyListContainer: ViewGroup? = getEmptyListContainerView()

        // progress is shown == loading
        // container is not shown == progress cannot be shown
        if (progressBar == null || progressBar.visibility == View.VISIBLE == loading || emptyListContainer == null
            || emptyListContainer.visibility != View.VISIBLE
        )
            return

        progressBar.visibility = if (loading) View.VISIBLE else View.GONE

        if (animate)
            TransitionManager.beginDelayedTransition(emptyListContainer)
    }

    protected fun setListShown(shown: Boolean) {
        setListShown(shown, true)
    }

    protected fun setListShown(shown: Boolean, animate: Boolean) {
        val listView = getListView()
        val emptyListContainer: ViewGroup? = getEmptyListContainerView()

        if (listView != null && listView.visibility == View.VISIBLE != shown) {
            listView.visibility = if (shown) View.VISIBLE else View.GONE

            if (animate)
                listView.startAnimation(
                    AnimationUtils.loadAnimation(
                        context,
                        if (shown) android.R.anim.fade_in else android.R.anim.fade_out
                    )
                )
        }

        if (emptyListContainer != null && emptyListContainer.visibility == View.VISIBLE == shown)
            emptyListContainer.setVisibility(if (shown) View.GONE else View.VISIBLE)
    }

    protected open fun setListView(listView: Z?) {
        this.listView = listView
    }

    protected fun setProgressBar(progressBar: ProgressBar?) {
        this.progressBar = progressBar
    }

    fun showEmptyListActionButton(show: Boolean) {
        getEmptyListActionButton().setVisibility(if (show) View.VISIBLE else View.GONE)
    }

    fun useEmptyListActionButton(buttonText: String?, clickListener: View.OnClickListener?) {
        val actionButton = getEmptyListActionButton()
        actionButton.setText(buttonText)
        actionButton.setOnClickListener(clickListener)
        showEmptyListActionButton(true)
    }

    private inner class RefreshLoaderCallback : LoaderManager.LoaderCallbacks<MutableList<T?>?> {
        var running = false
            private set

        var reloadRequested = false
            private set

        override fun onCreateLoader(id: Int, args: Bundle?): Loader<MutableList<T>> {
            reloadRequested = false
            running = true

            setListShown(adapter?.let { it.getCount() == 0 } ?: false)
            setListLoading(true)
            return createLoader()
        }

        override fun onLoadFinished(loader: Loader<MutableList<T?>?>, data: MutableList<T?>?) {
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

        override fun onLoaderReset(loader: Loader<MutableList<T?>?>) {}

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

    class ListLoader<G>(val adapter: ListAdapterBase<G>?) : AsyncTaskLoader<MutableList<G>>(adapter.context) {
        override fun onStartLoading() {
            super.onStartLoading()
            forceLoad()
        }

        override fun loadInBackground(): MutableList<G> {
            return adapter?.onLoad() ?: ArrayList<G>()
        }
    }

    companion object {
        val TAG = ListFragment::class.java.simpleName
        val LAYOUT_DEFAULT_EMPTY_LIST_VIEW: Int = R.layout.genfw_layout_listfragment_emptyview
        const val TASK_ID_REFRESH = 0
    }
}