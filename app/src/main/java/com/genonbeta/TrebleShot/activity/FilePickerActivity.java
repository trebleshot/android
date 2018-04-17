package com.genonbeta.TrebleShot.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.fragment.FileExplorerFragment;
import com.genonbeta.TrebleShot.io.DocumentFile;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;

/**
 * Created by: veli
 * Date: 5/29/17 3:18 PM
 */

public class FilePickerActivity extends Activity
{
	public static final String ACTION_CHOOSE_DIRECTORY = "com.genonbeta.intent.action.CHOOSE_DIRECTORY";
	public static final String ACTION_CHOOSE_FILE = "com.genonbeta.intent.action.CHOOSE_FILE";

	public static final String EXTRA_CHOSEN_PATH = "chosenPath";

	private FileExplorerFragment mFileExplorerFragment;
	private FloatingActionButton mFAB;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_filepicker);

		mFileExplorerFragment = (FileExplorerFragment) getSupportFragmentManager().findFragmentById(R.id.activitiy_filepicker_fragment_files);
		mFAB = findViewById(R.id.content_fab);
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		if (getIntent() != null) {
			if (ACTION_CHOOSE_DIRECTORY.equals(getIntent().getAction())) {
				getSupportActionBar().setTitle(R.string.text_chooseFolder);

				mFileExplorerFragment
						.getFileListFragment()
						.getAdapter()
						.setConfiguration(true, false, null);

				mFileExplorerFragment.getFileListFragment().refreshList();

				mFileExplorerFragment.getFileListFragment().getListView().setPadding(0, 0, 0, 200);
				mFileExplorerFragment.getFileListFragment().getListView().setClipToPadding(false);

				mFAB.setVisibility(View.VISIBLE);
				mFAB.setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						DocumentFile selectedPath = mFileExplorerFragment.getFileListFragment().getAdapter().getPath();

						if (selectedPath != null && selectedPath.canWrite())
							finishWithResult(selectedPath);
						else
							Snackbar.make(v, R.string.mesg_currentPathUnavailable, Snackbar.LENGTH_SHORT).show();
					}
				});
			} else if (ACTION_CHOOSE_FILE.equals(getIntent().getAction())) {
				getSupportActionBar().setTitle(R.string.text_chooseFile);

				mFileExplorerFragment.getFileListFragment().setLayoutClickListener(new EditableListFragment.LayoutClickListener<EditableListAdapter.EditableViewHolder>()
				{
					@Override
					public boolean onLayoutClick(EditableListFragment listFragment, EditableListAdapter.EditableViewHolder holder, boolean longClick)
					{
						if (longClick)
							return false;

						FileListAdapter.GenericFileHolder fileHolder = mFileExplorerFragment.getFileListFragment()
								.getAdapter()
								.getItem(holder.getAdapterPosition());

						if (!(fileHolder instanceof FileListAdapter.FileHolder))
							return false;

						finishWithResult(((FileListAdapter.FileHolder) fileHolder).file);

						return true;
					}
				});
			} else
				finish();
		} else
			finish();
	}

	private void finishWithResult(DocumentFile file)
	{
		setResult(Activity.RESULT_OK, new Intent(ACTION_CHOOSE_DIRECTORY)
				.putExtra(EXTRA_CHOSEN_PATH, file.getUri()));

		finish();
	}
}
