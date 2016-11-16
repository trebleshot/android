package com.genonbeta.TrebleShot.fragment;

import android.view.ActionMode;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.VideoListAdapter;

public class VideoListFragment extends AbstractEditableListFragment<VideoListAdapter>
{
    @Override
    protected VideoListAdapter onAdapter()
    {
        return new VideoListAdapter(getActivity());
    }

    @Override
    protected ActionModeListener onActionModeListener() { return new ChoiceListener(); }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        super.onListItemClick(l, v, position, id);

        VideoListAdapter.VideoInfo videoInfo = (VideoListAdapter.VideoInfo) this.getAdapter().getItem(position);

        this.openFile(videoInfo.uri, "video/*", getString(R.string.file_open_app_chooser_msg));
    }

    private class ChoiceListener extends ActionModeListener
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
