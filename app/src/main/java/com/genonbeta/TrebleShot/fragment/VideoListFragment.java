package com.genonbeta.TrebleShot.fragment;

import android.content.*;
import android.net.*;
import android.view.*;
import android.widget.*;
import com.genonbeta.TrebleShot.*;
import com.genonbeta.TrebleShot.adapter.*;
import java.util.*;

public class VideoListFragment extends AbstractMediaListFragment<VideoListAdapter>
{
	@Override
	protected VideoListAdapter onAdapter()
	{
		return new VideoListAdapter(getActivity());
	}

	@Override
	protected MediaChoiceListener onChoiceListener()
	{
		return new ChoiceListener();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		VideoListAdapter.VideoInfo videoInfo = (VideoListAdapter.VideoInfo) this.getAdapter().getItem(position);

		this.openFile(videoInfo.uri, "video/*", getString(R.string.file_open_app_chooser_msg));	
	}

	private class ChoiceListener extends MediaChoiceListener
	{
		@Override
		public void onItemChecked(ActionMode mode, int pos, long id, boolean isChecked)
		{
			VideoListAdapter.VideoInfo info = (VideoListAdapter.VideoInfo) getAdapter().getItem(pos);

			if (isChecked)
				mCheckedList.add(info.uri);
			else
				mCheckedList.remove(info.uri);
		}		
	}
}
