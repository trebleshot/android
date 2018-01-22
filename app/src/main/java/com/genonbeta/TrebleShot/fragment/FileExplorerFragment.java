package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatImageButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.dialog.CreateFolderDialog;
import com.genonbeta.TrebleShot.util.DetachListener;
import com.genonbeta.TrebleShot.util.TitleSupport;

import java.io.File;

/**
 * Created by: veli
 * Date: 5/30/17 10:47 AM
 */

public class FileExplorerFragment
		extends Fragment
		implements TitleSupport, DetachListener
{
	private RecyclerView mRecyclerView;
	private AppCompatImageButton mHomeButton;
	private FileListFragment mFileListFragment;
	private LinearLayoutManager mLayoutManager;
	private PathResolverRecyclerAdapter mAdapter;
	private File mRequestedPath = null;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_fileexplorer, container, false);

		mRecyclerView = view.findViewById(R.id.fragment_fileexplorer_pathresolver);
		mHomeButton = view.findViewById(R.id.fragment_fileexplorer_pathresolver_home);
		mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		mAdapter = new PathResolverRecyclerAdapter();
		mFileListFragment = (FileListFragment) getChildFragmentManager()
				.findFragmentById(R.id.fragment_fileexplorer_fragment_files);

		mAdapter.setOnClickListener(new PathResolverRecyclerAdapter.OnClickListener()
		{
			@Override
			public void onClick(PathResolverRecyclerAdapter.Holder holder)
			{
				requestPath(new File(holder.index.path));
			}
		});

		mHomeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				requestPath(null);
			}
		});

		mFileListFragment.setOnPathChangedListener(new FileListFragment.OnPathChangedListener()
		{
			@Override
			public void onPathChanged(File file)
			{
				mAdapter.goTo(file == null ? null : file.getAbsolutePath().split(File.separator));
				mAdapter.notifyDataSetChanged();

				if (mAdapter.getItemCount() > 0)
					mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
			}
		});

		mRecyclerView.setHasFixedSize(true);

		mRecyclerView.setLayoutManager(mLayoutManager);
		mLayoutManager.setStackFromEnd(true);
		mRecyclerView.setAdapter(mAdapter);

		return view;
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		if (mRequestedPath != null)
			requestPath(mRequestedPath);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_file_explorer, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_file_explorer_create_folder) {
			if (mFileListFragment.getAdapter().getPath() != null && mFileListFragment.getAdapter().getPath().canWrite())
				new CreateFolderDialog(getContext(), mFileListFragment.getAdapter().getPath(), new CreateFolderDialog.OnCreatedListener()
				{
					@Override
					public void onCreated()
					{
						mFileListFragment.refreshList();
					}
				}).show();
			else
				Snackbar.make(mFileListFragment.getListView(), R.string.mesg_currentPathUnavailable, Snackbar.LENGTH_SHORT).show();

			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPrepareDetach()
	{
		if (mFileListFragment != null)
			mFileListFragment.onPrepareDetach();
	}

	public FileListFragment getFileListFragment()
	{
		return mFileListFragment;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_fileExplorer);
	}

	public PathResolverRecyclerAdapter getRecyclerAdapter()
	{
		return mAdapter;
	}

	public RecyclerView getRecyclerView()
	{
		return mRecyclerView;
	}

	public void requestPath(File file)
	{
		if (getFileListFragment() == null || !getFileListFragment().isAdded()) {
			mRequestedPath = file;
			return;
		}

		mRequestedPath = null;

		getFileListFragment().goPath(file);
	}
}
