package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.MusicListAdapter;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.util.TitleSupport;

public class MusicListFragment extends ShareableListFragment<MusicListAdapter.SongHolder, MusicListAdapter> implements TitleSupport
{
	@Override
	public MusicListAdapter onAdapter()
	{
		return new MusicListAdapter(getActivity());
	}


	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		MusicListAdapter.SongHolder musicInfo = (MusicListAdapter.SongHolder) getAdapter().getItem(position);
		openFile(musicInfo.uri, "audio/*", getString(R.string.text_fileOpenAppChoose));
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_music);
	}
}
