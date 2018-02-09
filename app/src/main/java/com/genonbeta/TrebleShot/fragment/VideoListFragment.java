package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.VideoListAdapter;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.util.TitleSupport;

public class VideoListFragment
		extends ShareableListFragment<VideoListAdapter.VideoHolder, VideoListAdapter>
		implements TitleSupport
{
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setDefaultOrderingAscending(false);
		setDefaultSortingCriteria(R.id.actions_abs_editable_sort_by_date);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_video_library_white_24dp);
		setEmptyText(getString(R.string.text_listEmptyVideo));
	}

	@Override
	public void onResume()
	{
		super.onResume();

		getContext().getContentResolver()
				.registerContentObserver(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, getDefaultContentObserver());
	}

	@Override
	public void onPause()
	{
		super.onPause();

		getContext().getContentResolver()
				.unregisterContentObserver(getDefaultContentObserver());
	}

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
