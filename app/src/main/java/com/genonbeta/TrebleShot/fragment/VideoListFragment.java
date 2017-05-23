package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.net.Uri;
import android.view.ActionMode;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.VideoListAdapter;
import com.genonbeta.TrebleShot.support.FragmentTitle;

public class VideoListFragment extends AbstractEditableListFragment<VideoListAdapter.VideoInfo, VideoListAdapter> implements FragmentTitle
{
	@Override
	public VideoListAdapter onAdapter()
	{
		return new VideoListAdapter(getActivity());
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

		VideoListAdapter.VideoInfo videoInfo = (VideoListAdapter.VideoInfo) getAdapter().getItem(position);
		openFile(videoInfo.uri, "video/*", getString(R.string.file_open_app_chooser_msg));
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.video);
	}

	private class ChoiceListener extends ActionModeListener
	{
		public Uri onItemChecked(ActionMode mode, int pos, long id, boolean isChecked)
		{
			VideoListAdapter.VideoInfo info = (VideoListAdapter.VideoInfo) getAdapter().getItem(pos);

			return info.uri;
		}
	}
}
