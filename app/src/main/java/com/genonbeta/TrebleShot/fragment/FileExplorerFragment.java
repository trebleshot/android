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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.EditableListFragmentModelImpl;
import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.dialog.FolderCreationDialog;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.TrebleShot.object.WritablePathObject;
import com.genonbeta.TrebleShot.ui.callback.DetachListener;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.widget.EditableListAdapterImpl;
import com.genonbeta.android.framework.widget.PowerfulActionMode;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/30/17 10:47 AM
 */

public class FileExplorerFragment
		extends com.genonbeta.android.framework.app.Fragment
		implements EditableListFragmentImpl<FileListAdapter.GenericFileHolder>, EditableListFragmentModelImpl<GroupEditableListAdapter.GroupViewHolder>, Activity.OnBackPressedListener, DetachListener, TitleSupport, SnackbarSupport, com.genonbeta.android.framework.app.FragmentImpl
{
	public static final String TAG = FileExplorerFragment.class.getSimpleName();

	public final static int REQUEST_WRITE_ACCESS = 264;

	private RecyclerView mPathView;
	private FileListFragment mFileListFragment;
	private FilePathResolverRecyclerAdapter mPathAdapter;
	private DocumentFile mRequestedPath = null;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.layout_file_explorer, container, false);

		mPathView = view.findViewById(R.id.fragment_fileexplorer_pathresolver);
		mPathAdapter = new FilePathResolverRecyclerAdapter(getContext());
		mFileListFragment = (FileListFragment) getChildFragmentManager()
				.findFragmentById(R.id.fragment_fileexplorer_fragment_files);

		mPathAdapter.setOnClickListener(new PathResolverRecyclerAdapter.OnClickListener<DocumentFile>()
		{
			@Override
			public void onClick(PathResolverRecyclerAdapter.Holder<DocumentFile> holder)
			{
				requestPath(holder.index.object);
			}
		});

		mFileListFragment.setOnPathChangedListener(new FileListFragment.OnPathChangedListener()
		{
			@Override
			public void onPathChanged(DocumentFile file)
			{
				mPathAdapter.goTo(file);
				mPathAdapter.notifyDataSetChanged();

				if (mPathAdapter.getItemCount() > 0)
					mPathView.smoothScrollToPosition(mPathAdapter.getItemCount() - 1);
			}
		});

		LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
		layoutManager.setStackFromEnd(true);

		mPathView.setLayoutManager(layoutManager);
		mPathView.setHasFixedSize(true);
		mPathView.setAdapter(mPathAdapter);

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		mFileListFragment.goPath(null);

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
						String title = pathString.substring(pathString.lastIndexOf(File.separator));

						AppUtils.getDatabase(getContext()).publish(new WritablePathObject(title, pathUri));

						if (getContext() != null)
							getContext().getContentResolver().takePersistableUriPermission(pathUri,
									Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

						requestPath(null);
					}
					break;
			}
	}

	@Override
	public boolean onBackPressed()
	{
		DocumentFile path = getFileListFragment().getAdapter().getPath();

		if (getFileListFragment() == null || path == null)
			return false;

		DocumentFile parentFile = getReadableFolder(path);

		if (parentFile == null || File.separator.equals(parentFile.getName()))
			requestPath(null);
		else
			requestPath(parentFile);

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
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_file_explorer_create_folder) {
			if (mFileListFragment.getAdapter().getPath() != null && mFileListFragment.getAdapter().getPath().canWrite())
				new FolderCreationDialog(getContext(), mFileListFragment.getAdapter().getPath(), new FolderCreationDialog.OnFolderCreatedListener()
				{
					@Override
					public void onFolderCreated(DocumentFile directoryFile)
					{
						mFileListFragment.refreshList();
					}
				}).show();
			else
				Snackbar.make(mFileListFragment.getListView(), R.string.mesg_currentPathUnavailable, Snackbar.LENGTH_SHORT).show();
		} else if (id == R.id.actions_file_explorer_mount_directory) {
			startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_WRITE_ACCESS);
					/*.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
							| Intent.FLAG_GRANT_WRITE_URI_PERMISSION
							| Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
							| Intent.FLAG_GRANT_PREFIX_URI_PERMISSION), REQUEST_WRITE_ACCESS);*/
			Toast.makeText(getActivity(), R.string.mesg_mountDirectoryHelp, Toast.LENGTH_LONG).show();
		} else
			return super.onOptionsItemSelected(item);

		return true;
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

	public void requestPath(DocumentFile file)
	{
		if (getFileListFragment() == null || !getFileListFragment().isAdded()) {
			mRequestedPath = file;
			return;
		}

		mRequestedPath = null;

		getFileListFragment().goPath(file);
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

	@Override
	public boolean applyViewingChanges(int gridSize)
	{
		return getFileListFragment().applyViewingChanges(gridSize);
	}

	@Override
	public void changeGridViewSize(int gridSize)
	{
		getFileListFragment().changeGridViewSize(gridSize);
	}

	@Override
	public void changeOrderingCriteria(int id)
	{
		getFileListFragment().changeOrderingCriteria(id);
	}

	@Override
	public void changeSortingCriteria(int id)
	{
		getFileListFragment().changeSortingCriteria(id);
	}

	@Override
	public EditableListAdapterImpl<FileListAdapter.GenericFileHolder> getAdapterImpl()
	{
		return getFileListFragment().getAdapterImpl();
	}

	@Override
	public int getOrderingCriteria()
	{
		return getFileListFragment().getOrderingCriteria();
	}

	@Override
	public PowerfulActionMode.SelectorConnection<FileListAdapter.GenericFileHolder> getSelectionConnection()
	{
		return getFileListFragment().getSelectionConnection();
	}

	@Override
	public EditableListFragment.SelectionCallback<FileListAdapter.GenericFileHolder> getSelectionCallback()
	{
		return getFileListFragment().getSelectionCallback();
	}

	@Override
	public int getSortingCriteria()
	{
		return getFileListFragment().getSortingCriteria();
	}

	@Override
	public String getUniqueSettingKey(String setting)
	{
		return getFileListFragment().getUniqueSettingKey(setting);
	}

	@Override
	public boolean isRefreshLocked()
	{
		return getFileListFragment().isRefreshLocked();
	}

	@Override
	public boolean isRefreshRequested()
	{
		return getFileListFragment().isRefreshRequested();
	}

	@Override
	public boolean isSortingSupported()
	{
		return getFileListFragment().isSortingSupported();
	}

	@Override
	public boolean loadIfRequested()
	{
		return getFileListFragment().loadIfRequested();
	}

	@Override
	public boolean openUri(Uri uri, String chooserText)
	{
		return getFileListFragment().openUri(uri, chooserText);
	}

	@Override
	public void setLayoutClickListener(EditableListFragment.LayoutClickListener<GroupEditableListAdapter.GroupViewHolder> clickListener)
	{
		getFileListFragment().setLayoutClickListener(clickListener);
	}

	@Override
	public void setSelectorConnection(PowerfulActionMode.SelectorConnection<FileListAdapter.GenericFileHolder> selectionConnection)
	{
		getFileListFragment().setSelectorConnection(selectionConnection);
	}

	@Override
	public void setSelectionCallback(EditableListFragment.SelectionCallback<FileListAdapter.GenericFileHolder> selectionCallback)
	{
		getFileListFragment().setSelectionCallback(selectionCallback);
	}

	@Override
	public void refreshList()
	{

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
				getList().add(new Holder.Index<>(getContext().getString(R.string.text_home), R.drawable.ic_home_black_24dp, (DocumentFile) null));

				while (pathIndex.size() != 0) {
					int currentStage = pathIndex.size() - 1;

					getList().add(pathIndex.get(currentStage));
					pathIndex.remove(currentStage);
				}
			}
		}
	}
}
