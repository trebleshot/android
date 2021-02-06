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

import android.R
import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import kotlin.Throws
import com.genonbeta.android.framework.io.StreamInfo.FolderStateException
import android.provider.OpenableColumns
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.StreamDocumentFile
import androidx.annotation.RequiresApi
import android.provider.DocumentsContract
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageView
import androidx.loader.app.LoaderManager
import androidx.loader.content.AsyncTaskLoader
import androidx.loader.content.Loader
import androidx.transition.TransitionManager
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

/**
 * Created by: veli
 * Date: 12/3/16 9:57 AM
 */
abstract class ListFragment<Z : ViewGroup?, T, E : ListAdapterBase<T?>?> : Fragment(), ListFragmentBase<T?> {
    private var mAdapter: E? = null
    private var mListView: Z? = null
    private var mEmptyListContainerView: ViewGroup? = null
    private var mEmptyListTextView: TextView? = null
    private var mEmptyListImageView: ImageView? = null
    private var mProgressBar: ProgressBar? = null
    private var mEmptyListActionButton: Button? = null
    private val mRefreshLoaderCallback: RefreshLoaderCallback? = RefreshLoaderCallback()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findViewDefaultsFromMainView()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        LoaderManager.getInstance(this).initLoader(TASK_ID_REFRESH, null, mRefreshLoaderCallback)
    }

    protected fun onPrepareRefreshingList() {}
    protected open fun onListRefreshed() {}
    fun createLoader(): AsyncTaskLoader<MutableList<T?>?>? {
        return ListLoader<T?>(mAdapter)
    }

    protected abstract fun ensureList()
    protected fun findViewDefaultsFrom(view: View?) {
        if (view == null) return
        setListView(view.findViewById<View?>(R.id.genfw_customListFragment_listView) as Z)
        setEmptyListContainerView(view.findViewById<View?>(R.id.genfw_customListFragment_emptyView) as ViewGroup)
        setEmptyListTextView(view.findViewById<View?>(R.id.genfw_customListFragment_emptyTextView) as TextView)
        setEmptyListImageView(view.findViewById<View?>(R.id.genfw_customListFragment_emptyImageView) as ImageView)
        setEmptyListActionButton(view.findViewById<View?>(R.id.genfw_customListFragment_emptyActionButton) as Button)
        setProgressBar(view.findViewById<View?>(R.id.genfw_customListFragment_progressBar) as ProgressBar)
    }

    protected fun findViewDefaultsFromMainView() {
        findViewDefaultsFrom(view)
    }

    fun getAdapter(): E? {
        return mAdapter
    }

    protected abstract fun generateDefaultView(
        inflater: LayoutInflater?,
        container: ViewGroup?,
        savedState: Bundle?
    ): View?

    fun getEmptyListContainerView(): ViewGroup? {
        ensureList()
        return mEmptyListContainerView
    }

    fun getEmptyListImageView(): ImageView? {
        ensureList()
        return mEmptyListImageView
    }

    fun getEmptyListTextView(): TextView? {
        ensureList()
        return mEmptyListTextView
    }

    fun getEmptyListActionButton(): Button? {
        ensureList()
        return mEmptyListActionButton
    }

    fun getListAdapter(): E? {
        return mAdapter
    }

    protected fun getListViewInternal(): Z? {
        return mListView
    }

    fun getListView(): Z? {
        ensureList()
        return getListViewInternal()
    }

    fun getLoaderCallbackRefresh(): RefreshLoaderCallback? {
        return mRefreshLoaderCallback
    }

    fun getProgressBar(): ProgressBar? {
        ensureList()
        return mProgressBar
    }

    override fun refreshList() {
        getLoaderCallbackRefresh().requestRefresh()
    }

    protected fun setEmptyListContainerView(container: ViewGroup?) {
        mEmptyListContainerView = container
    }

    protected fun setEmptyListActionButton(button: Button?) {
        mEmptyListActionButton = button
    }

    protected fun setEmptyListImage(resId: Int) {
        getEmptyListImageView().setImageResource(resId)
    }

    protected fun setEmptyListImageView(view: ImageView?) {
        mEmptyListImageView = view
    }

    protected fun setEmptyListText(text: CharSequence?) {
        getEmptyListTextView().setText(text)
    }

    protected fun setEmptyListTextView(view: TextView?) {
        mEmptyListTextView = view
    }

    protected fun setListAdapter(adapter: E?) {
        val hadAdapter = mAdapter != null
        mAdapter = adapter
        setListAdapter(adapter, hadAdapter)
    }

    protected abstract fun setListAdapter(adapter: E?, hadAdapter: Boolean)
    fun setListLoading(loading: Boolean) {
        setListLoading(loading, true)
    }

    fun setListLoading(loading: Boolean, animate: Boolean) {
        ensureList()
        val progressBar: ProgressBar? = getProgressBar()
        val emptyListContainer: ViewGroup? = getEmptyListContainerView()

        // progress is shown == loading
        // container is not shown == progress cannot be shown
        if (progressBar == null || progressBar.getVisibility() == View.VISIBLE == loading || emptyListContainer == null || emptyListContainer.getVisibility() != View.VISIBLE) return
        progressBar.setVisibility(if (loading) View.VISIBLE else View.GONE)
        if (animate) TransitionManager.beginDelayedTransition(emptyListContainer)
    }

    protected fun setListShown(shown: Boolean) {
        setListShown(shown, true)
    }

    protected fun setListShown(shown: Boolean, animate: Boolean) {
        val listView = getListView()
        val emptyListContainer: ViewGroup? = getEmptyListContainerView()
        if (listView != null && listView.getVisibility() == View.VISIBLE != shown) {
            listView.setVisibility(if (shown) View.VISIBLE else View.GONE)
            if (animate) listView.startAnimation(
                AnimationUtils.loadAnimation(
                    context,
                    if (shown) R.anim.fade_in else R.anim.fade_out
                )
            )
        }
        if (emptyListContainer != null && emptyListContainer.getVisibility() == View.VISIBLE == shown) emptyListContainer.setVisibility(
            if (shown) View.GONE else View.VISIBLE
        )
    }

    protected open fun setListView(listView: Z?) {
        mListView = listView
    }

    protected fun setProgressBar(progressBar: ProgressBar?) {
        mProgressBar = progressBar
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
        private var mRunning = false
        private var mReloadRequested = false
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<MutableList<T?>?> {
            mReloadRequested = false
            mRunning = true
            if (getAdapter().getCount() == 0) setListShown(false)
            setListLoading(true)
            return createLoader()
        }

        override fun onLoadFinished(loader: Loader<MutableList<T?>?>, data: MutableList<T?>?) {
            if (isResumed) {
                onPrepareRefreshingList()
                getAdapter().onUpdate(data)
                getAdapter().onDataSetChanged()
                setListLoading(false)
                onListRefreshed()
            }
            if (isReloadRequested()) refresh()
            mRunning = false
        }

        override fun onLoaderReset(loader: Loader<MutableList<T?>?>) {}
        fun isRunning(): Boolean {
            return mRunning
        }

        fun isReloadRequested(): Boolean {
            return mReloadRequested
        }

        fun refresh() {
            LoaderManager.getInstance(this@ListFragment)
                .restartLoader(TASK_ID_REFRESH, null, mRefreshLoaderCallback)
        }

        fun requestRefresh(): Boolean {
            if (isRunning() && isReloadRequested()) return false
            if (!isRunning()) refresh() else mReloadRequested = true
            return true
        }
    }

    class ListLoader<G>(adapter: ListAdapterBase<G?>?) : AsyncTaskLoader<MutableList<G?>?>(adapter.getContext()) {
        private val mAdapter: ListAdapterBase<G?>?
        override fun onStartLoading() {
            super.onStartLoading()
            forceLoad()
        }

        override fun loadInBackground(): MutableList<G?>? {
            return mAdapter.onLoad()
        }

        init {
            mAdapter = adapter
        }
    }

    companion object {
        val TAG: String? = ListFragment::class.java.simpleName
        val LAYOUT_DEFAULT_EMPTY_LIST_VIEW: Int = R.layout.genfw_layout_listfragment_emptyview
        const val TASK_ID_REFRESH = 0
    }
}