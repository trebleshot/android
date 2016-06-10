package com.genonbeta.TrebleShot.fragment;

import android.view.ActionMode;
import android.view.View;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.MusicListAdapter;

public class MusicListFragment extends AbstractMediaListFragment<MusicListAdapter> {
    @Override
    protected MusicListAdapter onAdapter() {
        return new MusicListAdapter(getActivity());
    }

    @Override
    protected MediaChoiceListener onChoiceListener() {
        return new ChoiceListener();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        MusicListAdapter.MusicInfo musicInfo = (MusicListAdapter.MusicInfo) getAdapter().getItem(position);

        this.openFile(musicInfo.uri, "audio/*", getString(R.string.file_open_app_chooser_msg));
    }

    private class ChoiceListener extends MediaChoiceListener {
        @Override
        public void onItemChecked(ActionMode mode, int pos, long id, boolean isChecked) {
            MusicListAdapter.MusicInfo info = (MusicListAdapter.MusicInfo) getAdapter().getItem(pos);

            if (isChecked)
                mCheckedList.add(info.uri);
            else
                mCheckedList.remove(info.uri);
        }
    }
}
