package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.VideoListAdapter;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.util.TitleSupport;

public class VideoListFragment
		extends ShareableListFragment<VideoListAdapter.VideoHolder, VideoListAdapter>
		implements TitleSupport
{
	@Override
	public VideoListAdapter onAdapter()
	{
		return new VideoListAdapter(getActivity());
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_video);
	}
}
