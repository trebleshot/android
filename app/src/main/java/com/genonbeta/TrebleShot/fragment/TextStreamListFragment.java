package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ShareActivity;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.adapter.ImageListAdapter;
import com.genonbeta.TrebleShot.adapter.TextStreamListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.GroupShareableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.util.FABSupport;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.GroupShareableListAdapter;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */

public class TextStreamListFragment
		extends GroupShareableListFragment<TextStreamObject, GroupShareableListAdapter.ViewHolder, TextStreamListAdapter>
		implements TitleSupport, FABSupport
{
	private IntentFilter mIntentFilter = new IntentFilter();
	private StatusReceiver mStatusReceiver = new StatusReceiver();

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setDefaultOrderingAscending(false);
		setDefaultSortingCriteria(R.id.actions_abs_editable_sort_by_date);
		setDefaultGroupingCriteria(TextStreamListAdapter.MODE_GROUP_BY_DATE);

		mIntentFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_forum_white_24dp);
		setEmptyText(getString(R.string.text_listEmptyTextStream));
	}

	@Override
	public TextStreamListAdapter onAdapter()
	{
		return new TextStreamListAdapter(getActivity())
		{
			@Override
			public void onBindViewHolder(@NonNull final ViewHolder holder, int position)
			{
				super.onBindViewHolder(holder, position);

				holder.getView().setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						if (!holder.isRepresentative() && !setItemSelected(holder)) {
							TextStreamObject textStreamObject = getAdapter().getItem(holder);

							startActivity(new Intent(getContext(), TextEditorActivity.class)
									.setAction(TextEditorActivity.ACTION_EDIT_TEXT)
									.putExtra(TextEditorActivity.EXTRA_CLIPBOARD_ID, textStreamObject.id)
									.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
						}
					}
				});
			}
		};
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mStatusReceiver, mIntentFilter);
		refreshList();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mStatusReceiver);
	}

	@Override
	public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
	{
		super.onCreateActionMenu(context, actionMode, menu);
		actionMode.getMenuInflater().inflate(R.menu.action_mode_text_stream, menu);
		return true;
	}

	@Override
	public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
	{
		int id = item.getItemId();

		ArrayList<TextStreamObject> selectionList = getSelectionConnection().getSelectedItemList();

		if (id == R.id.action_mode_text_stream_delete) {
			for (TextStreamObject textStreamObject : selectionList)
				getAdapter().getDatabase().remove(textStreamObject);
		} else if (id == R.id.action_mode_share_all_apps || id == R.id.action_mode_share_trebleshot) {
			if (selectionList.size() == 1) {
				TextStreamObject streamObject = selectionList.get(0);

				Intent shareIntent = new Intent(item.getItemId() == R.id.action_mode_share_all_apps
						? Intent.ACTION_SEND : ShareActivity.ACTION_SEND)
						.putExtra(Intent.EXTRA_TEXT, streamObject.text)
						.setType("text/*");

				startActivity((item.getItemId() == R.id.action_mode_share_all_apps) ? Intent.createChooser(shareIntent, getString(R.string.text_fileShareAppChoose)) : shareIntent);
			} else {
				Toast.makeText(context, R.string.mesg_textShareLimit, Toast.LENGTH_SHORT).show();
				return false;
			}
		} else
			return super.onActionMenuItemSelected(context, actionMode, item);

		return true;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_textStream);
	}

	@Override
	public boolean onFABRequested(FloatingActionButton floatingActionButton)
	{
		floatingActionButton.setImageResource(R.drawable.ic_add_white_24dp);
		floatingActionButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				startActivity(new Intent(getActivity(), TextEditorActivity.class)
						.setAction(TextEditorActivity.ACTION_EDIT_TEXT));
			}
		});

		return true;
	}

	private class StatusReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
					&& intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_CLIPBOARD))
				refreshList();
		}
	}
}