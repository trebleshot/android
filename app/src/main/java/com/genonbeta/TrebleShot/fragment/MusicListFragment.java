package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.MusicListAdapter;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.util.TitleSupport;

public class MusicListFragment
		extends ShareableListFragment<MusicListAdapter.SongHolder, MusicListAdapter>
		implements TitleSupport
{
	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_library_music_white_24dp);
		setEmptyText(getString(R.string.text_listEmptyMusic));
	}

	@Override
	public void onResume()
	{
		super.onResume();

		getContext().getContentResolver()
				.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, getDefaultContentObserver());
	}

	@Override
	public void onPause()
	{
		super.onPause();

		getContext().getContentResolver()
				.unregisterContentObserver(getDefaultContentObserver());
	}

	@Override
	public MusicListAdapter onAdapter()
	{
		return new MusicListAdapter(getActivity());
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_music);
	}
}
