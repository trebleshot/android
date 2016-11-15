package com.genonbeta.TrebleShot.fragment;

import android.view.ActionMode;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.VideoListAdapter;

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
        return null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);

        VideoListAdapter.VideoInfo videoInfo = (VideoListAdapter.VideoInfo) this.getAdapter().getItem(position);

        this.openFile(videoInfo.uri, "video/*", getString(R.string.file_open_app_chooser_msg));
    }

    private abstract class ChoiceListener extends MediaChoiceListener
    {
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
