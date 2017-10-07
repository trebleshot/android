package com.genonbeta.TrebleShot.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.dialog.CreateFolderDialog;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;

import java.io.File;

/**
 * Created by: veli
 * Date: 5/30/17 10:47 AM
 */

public class FileExplorerFragment extends Fragment
{
	private RecyclerView mRecyclerView;
	private ImageView mHomeButton;
	private FileListFragment mFileListFragment;
	private LinearLayoutManager mLayoutManager;
	private PathResolverRecyclerAdapter mAdapter;
	private FloatingActionButton mButtonOfEverything;

	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_fileexplorer, container, false);

		mRecyclerView = (RecyclerView) view.findViewById(R.id.fragment_fileexplorer_pathresolver);
		mButtonOfEverything = (FloatingActionButton) view.findViewById(R.id.fragment_fileexplorer_boe);
		mHomeButton = (ImageView) view.findViewById(R.id.fragment_fileexplorer_pathresolver_home);
		mFileListFragment = new FileListFragment();

		mRecyclerView.setHasFixedSize(true);

		getFragmentManager()
				.beginTransaction()
				.replace(R.id.fragment_fileexplorer_fragment_files, mFileListFragment)
				.commit();

		mLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		mAdapter = new PathResolverRecyclerAdapter();

		mRecyclerView.setLayoutManager(mLayoutManager);
		mLayoutManager.setStackFromEnd(true);
		mRecyclerView.setAdapter(mAdapter);

		mAdapter.setOnClickListener(new PathResolverRecyclerAdapter.OnClickListener()
		{
			@Override
			public void onClick(PathResolverRecyclerAdapter.Holder holder)
			{
				mFileListFragment.goPath(holder.file);
			}
		});

		mHomeButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				mFileListFragment.goPath(null);
			}
		});

		mFileListFragment.setOnPathChangedListener(new FileListFragment.OnPathChangedListener()
		{
			@Override
			public void onPathChanged(File file)
			{
				mAdapter.goTo(file);
				mAdapter.notifyDataSetChanged();

				if (mAdapter.getItemCount() > 0)
					mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);

				mButtonOfEverything.setVisibility(file == null || !file.canWrite() ? View.GONE : View.VISIBLE);
			}
		});

		mButtonOfEverything.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				new CreateFolderDialog(getContext(), mFileListFragment.getAdapter().getPath(), new CreateFolderDialog.OnCreatedListener()
				{
					@Override
					public void onCreated()
					{
						mFileListFragment.refreshList();
					}
				}).show();
			}
		});

		return view;
	}

	@Override
	public void onStart()
	{
		super.onStart();

		mFileListFragment.getListView().setPadding(0, 0, 0, 200);
		mFileListFragment.getListView().setClipToPadding(false);

		if (mFileListFragment.getAdapter().getPath() == null)
			mFileListFragment.goPath(ApplicationHelper.getApplicationDirectory(getActivity()));
	}

	public FloatingActionButton getButtonOfEverything()
	{
		return mButtonOfEverything;
	}

	public FileListFragment getFileListFragment()
	{
		return mFileListFragment;
	}

	public PathResolverRecyclerAdapter getRecyclerAdapter()
	{
		return mAdapter;
	}

	public RecyclerView getRecyclerView()
	{
		return mRecyclerView;
	}
}
