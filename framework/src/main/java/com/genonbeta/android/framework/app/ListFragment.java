package com.genonbeta.android.framework.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.genonbeta.android.framework.R;
import com.genonbeta.android.framework.widget.ListAdapterImpl;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 12/3/16 9:57 AM
 */

public abstract class ListFragment<Z extends ViewGroup, T, E extends ListAdapterImpl<T>>
		extends Fragment
		implements ListFragmentImpl<T>
{
	public static final String TAG = "ListFragment";

	public static final int TASK_ID_REFRESH = 0;

	private E mAdapter;
	private FrameLayout mListViewContainer;
	private FrameLayout mCustomViewContainer;
	private FrameLayout mDefaultViewContainer;
	private FrameLayout mContainer;
	private RelativeLayout mEmptyView;
	private TextView mEmptyText;
	private ImageView mEmptyImage;
	private ProgressBar mProgressView;
	private Button mEmptyActionButton;
	private LoaderCallbackRefresh mLoaderCallbackRefresh = new LoaderCallbackRefresh();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mAdapter = onAdapter();
	}

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		super.onCreateView(inflater, container, savedInstanceState);

		View view = getLayoutInflater().inflate(R.layout.genfw_abstract_list_fragment, container, false);

		mCustomViewContainer = view.findViewById(R.id.genfw_customListFragment_customViewContainer);
		mDefaultViewContainer = view.findViewById(R.id.genfw_customListFragment_defaultViewContainer);
		mListViewContainer = view.findViewById(R.id.genfw_customListFragment_listViewContainer);
		mContainer = view.findViewById(R.id.genfw_customListFragment_container);
		mEmptyView = view.findViewById(R.id.genfw_customListFragment_emptyView);
		mEmptyText = view.findViewById(R.id.genfw_customListFragment_emptyTextView);
		mEmptyImage = view.findViewById(R.id.genfw_customListFragment_emptyImageView);
		mProgressView = view.findViewById(R.id.genfw_customListFragment_progressView);
		mEmptyActionButton = view.findViewById(R.id.genfw_customListFragment_emptyActionButton);

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		if (getListView() != null
				&& getListView().getId() != R.id.genfw_customListFragment_listView)
			getListView().setId(R.id.genfw_customListFragment_listView);
	}

	@Override
	public void onActivityCreated(@Nullable Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		setListAdapter(mAdapter);
		getLoaderManager().initLoader(TASK_ID_REFRESH, null, mLoaderCallbackRefresh);
	}

	public abstract E onAdapter();

	protected abstract void onEnsureList();

	protected abstract Z onListView(View mainContainer, ViewGroup listViewContainer);

	public abstract boolean onSetListAdapter(E adapter);

	public abstract Z getListView();

	protected void onListRefreshed()
	{
	}

	public AsyncTaskLoader<ArrayList<T>> createLoader()
	{
		return new ListLoader<>(mAdapter);
	}

	public E getAdapter()
	{
		return mAdapter;
	}

	protected FrameLayout getContainer()
	{
		return mContainer;
	}

	public FrameLayout getCustomViewContainer()
	{
		return mCustomViewContainer;
	}

	public FrameLayout getDefaultViewContainer()
	{
		return mDefaultViewContainer;
	}

	public ImageView getEmptyImage()
	{
		onEnsureList();
		return mEmptyImage;
	}

	public TextView getEmptyText()
	{
		onEnsureList();
		return mEmptyText;
	}

	protected RelativeLayout getEmptyView()
	{
		return mEmptyView;
	}

	protected FrameLayout getListViewContainer()
	{
		return mListViewContainer;
	}

	public LoaderCallbackRefresh getLoaderCallbackRefresh()
	{
		return mLoaderCallbackRefresh;
	}

	public E getListAdapter()
	{
		return mAdapter;
	}

	public ProgressBar getProgressView()
	{
		onEnsureList();
		return mProgressView;
	}

	public Button getEmptyActionButton()
	{
		return mEmptyActionButton;
	}

	public void refreshList()
	{
		getLoaderCallbackRefresh().requestRefresh();
	}

	public void setEmptyImage(int resId)
	{
		onEnsureList();
		mEmptyImage.setImageResource(resId);
	}

	public void setEmptyText(CharSequence text)
	{
		onEnsureList();
		mEmptyText.setText(text);
	}

	public void setListAdapter(E adapter)
	{
		boolean hadAdapter = mAdapter != null;
		mAdapter = adapter;

		if (onSetListAdapter(adapter)) {
			if (mContainer.getVisibility() != View.VISIBLE && !hadAdapter)
				setListShown(true, getView().getWindowToken() != null);
		}
	}

	public void setListShown(boolean shown)
	{
		setListShown(shown, true);
	}

	public void setListShown(boolean shown, boolean animate)
	{
		onEnsureList();

		if ((mContainer.getVisibility() == View.VISIBLE) == shown)
			return;

		if (animate) {
			Animation fadeOut = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_out);
			Animation fadeIn = AnimationUtils.loadAnimation(getContext(), android.R.anim.fade_in);

			mProgressView.startAnimation(shown ? fadeOut : fadeIn);
			mContainer.startAnimation(shown ? fadeIn : fadeOut);
		} else {
			mProgressView.clearAnimation();
			mContainer.clearAnimation();
		}

		mContainer.setVisibility(shown ? View.VISIBLE : View.GONE);
		mProgressView.setVisibility(shown ? View.GONE : View.VISIBLE);
	}

	public void showCustomView(boolean shown)
	{
		mCustomViewContainer.setVisibility(shown ? View.VISIBLE : View.GONE);
		mDefaultViewContainer.setVisibility(shown ? View.GONE : View.VISIBLE);
	}

	public boolean toggleCustomView()
	{
		boolean isVisible = getCustomViewContainer().getVisibility() == View.VISIBLE;

		showCustomView(!isVisible);

		return !isVisible;
	}

	public void useEmptyActionButton(String buttonText, View.OnClickListener clickListener)
	{
		mEmptyActionButton.setText(buttonText);
		mEmptyActionButton.setOnClickListener(clickListener);

		useEmptyActionButton(true);
	}

	public void useEmptyActionButton(boolean use)
	{
		mEmptyActionButton.setVisibility(use ? View.VISIBLE : View.GONE);
	}

	private class LoaderCallbackRefresh implements LoaderManager.LoaderCallbacks<ArrayList<T>>
	{
		private boolean mRunning = false;
		private boolean mReloadRequested = false;

		@NonNull
		@Override
		public Loader<ArrayList<T>> onCreateLoader(int id, Bundle args)
		{
			mReloadRequested = false;
			mRunning = true;

			if (mAdapter.getCount() == 0)
				setListShown(false);

			return createLoader();
		}

		@Override
		public void onLoadFinished(Loader<ArrayList<T>> loader, ArrayList<T> data)
		{
			if (isResumed()) {
				mAdapter.onUpdate(data);
				mAdapter.onDataSetChanged();

				setListShown(true);
				onListRefreshed();
			}

			if (isReloadRequested())
				refresh();

			mRunning = false;
		}

		@Override
		public void onLoaderReset(@NonNull Loader<ArrayList<T>> loader)
		{

		}

		public boolean isRunning()
		{
			return mRunning;
		}

		public boolean isReloadRequested()
		{
			return mReloadRequested;
		}

		public void refresh()
		{
			getLoaderManager().restartLoader(TASK_ID_REFRESH, null, mLoaderCallbackRefresh);
		}

		public boolean requestRefresh()
		{
			if (isRunning() && isReloadRequested())
				return false;

			if (!isRunning())
				refresh();
			else
				mReloadRequested = true;

			return true;
		}
	}

	public static class ListLoader<G> extends AsyncTaskLoader<ArrayList<G>>
	{
		private ListAdapterImpl<G> mAdapter;

		public ListLoader(ListAdapterImpl<G> adapter)
		{
			super(adapter.getContext());
			mAdapter = adapter;
		}

		@Override
		protected void onStartLoading()
		{
			super.onStartLoading();
			forceLoad();
		}

		@Override
		public ArrayList<G> loadInBackground()
		{
			return mAdapter.onLoad();
		}
	}
}
