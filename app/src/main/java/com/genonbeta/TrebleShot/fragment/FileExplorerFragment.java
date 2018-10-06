package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.dialog.FolderCreationDialog;
import com.genonbeta.TrebleShot.object.WritablePathObject;
import com.genonbeta.TrebleShot.ui.callback.DetachListener;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/30/17 10:47 AM
 */

public class FileExplorerFragment
		extends FileListFragment
		implements Activity.OnBackPressedListener, DetachListener, TitleSupport, SnackbarSupport
{
	public static final String TAG = FileExplorerFragment.class.getSimpleName();

	public final static int REQUEST_WRITE_ACCESS = 264;

	private RecyclerView mPathView;
	private FilePathResolverRecyclerAdapter mPathAdapter;
	private DocumentFile mRequestedPath = null;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
	{
		View adaptedView = getLayoutInflater().inflate(R.layout.layout_file_explorer, null, false);
		listViewContainer.addView(adaptedView);

		mPathView = adaptedView.findViewById(R.id.fragment_fileexplorer_pathresolver);
		mPathAdapter = new FilePathResolverRecyclerAdapter(getContext());

		mPathAdapter.setOnClickListener(new PathResolverRecyclerAdapter.OnClickListener<DocumentFile>()
		{
			@Override
			public void onClick(PathResolverRecyclerAdapter.Holder<DocumentFile> holder)
			{
				goPath(holder.index.object);
			}
		});

		LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		layoutManager.setStackFromEnd(true);

		mPathView.setLayoutManager(layoutManager);
		mPathView.setHasFixedSize(true);
		mPathView.setAdapter(mPathAdapter);

		return super.onListView(mainContainer, (FrameLayout) adaptedView.findViewById(R.id.fragment_fileexplorer_listViewContainer));
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		goPath(null);

		if (mRequestedPath != null)
			requestPath(mRequestedPath);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK)
			switch (requestCode) {
				case REQUEST_WRITE_ACCESS:
					Uri pathUri = data.getData();

					if (Build.VERSION.SDK_INT >= 23 && pathUri != null) {
						String pathString = pathUri.toString();
						String title = null;

						try {
							title = FileUtils.fromUri(getContext(), pathUri).getName();
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}

						if (title == null)
							title = pathString.substring(pathString.lastIndexOf(File.separator) + 1);

						AppUtils.getDatabase(getContext())
								.publish(new WritablePathObject(title, pathUri));

						if (getContext() != null)
							getContext().getContentResolver().takePersistableUriPermission(pathUri,
									Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

						goPath(null);
					}
					break;
			}
	}

	@Override
	public boolean onBackPressed()
	{
		DocumentFile path = getAdapter().getPath();

		if (path == null)
			return false;

		DocumentFile parentFile = getReadableFolder(path);

		if (parentFile == null || File.separator.equals(parentFile.getName()))
			goPath(null);
		else
			goPath(parentFile);

		return true;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_file_explorer, menu);

		MenuItem mountDirectory = menu.findItem(R.id.actions_file_explorer_mount_directory);

		if (Build.VERSION.SDK_INT >= 21
				&& mountDirectory != null)
			mountDirectory.setVisible(true);
	}

	@Override
	protected void onListRefreshed()
	{
		super.onListRefreshed();

		mPathAdapter.goTo(getAdapter().getPath());
		mPathAdapter.notifyDataSetChanged();

		if (mPathAdapter.getItemCount() > 0)
			mPathView.smoothScrollToPosition(mPathAdapter.getItemCount() - 1);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_file_explorer_create_folder) {
			if (getAdapter().getPath() != null && getAdapter().getPath().canWrite())
				new FolderCreationDialog(getContext(), getAdapter().getPath(), new FolderCreationDialog.OnFolderCreatedListener()
				{
					@Override
					public void onFolderCreated(DocumentFile directoryFile)
					{
						refreshList();
					}
				}).show();
			else
				Snackbar.make(getListView(), R.string.mesg_currentPathUnavailable, Snackbar.LENGTH_SHORT).show();
		} else if (id == R.id.actions_file_explorer_mount_directory) {
			startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_WRITE_ACCESS);
			Toast.makeText(getActivity(), R.string.mesg_mountDirectoryHelp, Toast.LENGTH_LONG).show();
		} else
			return super.onOptionsItemSelected(item);

		return true;
	}

	public PathResolverRecyclerAdapter getPathAdapter()
	{
		return mPathAdapter;
	}

	public RecyclerView getPathView()
	{
		return mPathView;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_fileExplorer);
	}


	public static DocumentFile getReadableFolder(DocumentFile documentFile)
	{
		DocumentFile parent = documentFile.getParentFile();

		if (parent == null)
			return null;

		return parent.canRead()
				? parent
				: getReadableFolder(parent);
	}

	public void requestPath(DocumentFile file)
	{
		if (!isAdded()) {
			mRequestedPath = file;
			return;
		}

		mRequestedPath = null;

		goPath(file);
	}

	private class FilePathResolverRecyclerAdapter extends PathResolverRecyclerAdapter<DocumentFile>
	{
		public FilePathResolverRecyclerAdapter(Context context)
		{
			super(context);
		}

		public void goTo(DocumentFile file)
		{
			ArrayList<Holder.Index<DocumentFile>> pathIndex = new ArrayList<>();
			DocumentFile currentFile = file;

			while (currentFile != null) {
				Holder.Index<DocumentFile> index = new Holder.Index<>(currentFile.getName(), currentFile);

				pathIndex.add(index);

				currentFile = currentFile.getParentFile();

				if (currentFile == null && ".".equals(index.title))
					index.title = getString(R.string.text_fileRoot);
			}

			synchronized (getList()) {
				getList().clear();
				getList().add(new Holder.Index<>(getContext().getString(R.string.text_home), R.drawable.ic_home_white_24dp, (DocumentFile) null));

				while (pathIndex.size() != 0) {
					int currentStage = pathIndex.size() - 1;

					getList().add(pathIndex.get(currentStage));
					pathIndex.remove(currentStage);
				}
			}
		}
	}
}
