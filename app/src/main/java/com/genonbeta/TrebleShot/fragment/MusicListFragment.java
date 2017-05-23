package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.net.Uri;
import android.view.ActionMode;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.MusicListAdapter;
import com.genonbeta.TrebleShot.support.FragmentTitle;

public class MusicListFragment extends AbstractEditableListFragment<MusicListAdapter.MusicInfo, MusicListAdapter> implements FragmentTitle
{
	@Override
	public MusicListAdapter onAdapter()
	{
		return new MusicListAdapter(getActivity());
	}

	@Override
	protected ActionModeListener onActionModeListener()
	{
		return new ChoiceListener();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		MusicListAdapter.MusicInfo musicInfo = (MusicListAdapter.MusicInfo) getAdapter().getItem(position);
		openFile(musicInfo.uri, "audio/*", getString(R.string.file_open_app_chooser_msg));
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.music);
	}

	private class ChoiceListener extends ActionModeListener
	{
		public Uri onItemChecked(ActionMode mode, int pos, long id, boolean isChecked)
		{
			MusicListAdapter.MusicInfo info = (MusicListAdapter.MusicInfo) getAdapter().getItem(pos);

			return info.uri;
		}
	}
}
