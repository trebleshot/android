package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.FileListAdapter;
import com.genonbeta.TrebleShot.adapter.MusicListAdapter;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;

import java.util.Map;

public class MusicListFragment
        extends GroupEditableListFragment<MusicListAdapter.SongHolder, GroupEditableListAdapter.GroupViewHolder, MusicListAdapter>
        implements TitleSupport
{
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setDefaultGroupingCriteria(MusicListAdapter.MODE_GROUP_BY_ALBUM);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
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

        getContext()
                .getContentResolver()
                .unregisterContentObserver(getDefaultContentObserver());
    }

    @Override
    public void onGroupingOptions(Map<String, Integer> options)
    {
        super.onGroupingOptions(options);

        options.put(getString(R.string.text_groupByNothing), MusicListAdapter.MODE_GROUP_BY_NOTHING);
        options.put(getString(R.string.text_groupByDate), MusicListAdapter.MODE_GROUP_BY_DATE);
        options.put(getString(R.string.text_groupByAlbum), MusicListAdapter.MODE_GROUP_BY_ALBUM);
        options.put(getString(R.string.text_groupByArtist), MusicListAdapter.MODE_GROUP_BY_ARTIST);
        options.put(getString(R.string.text_groupByFolder), MusicListAdapter.MODE_GROUP_BY_FOLDER);
    }

    @Override
    public MusicListAdapter onAdapter()
    {
        final AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder> quickActions = new AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder>()
        {
            @Override
            public void onQuickActions(final GroupEditableListAdapter.GroupViewHolder clazz)
            {
                if (!clazz.isRepresentative()) {
                    registerLayoutViewClicks(clazz);

                    clazz.getView().findViewById(R.id.visitView).setOnClickListener(
                            new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View v)
                                {
                                    performLayoutClickOpen(clazz);
                                }
                            });

                    clazz.getView().findViewById(R.id.selector).setOnClickListener(
                            new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View v)
                                {
                                    if (getSelectionConnection() != null)
                                        getSelectionConnection().setSelected(clazz.getAdapterPosition());
                                }
                            });
                }
            }
        };

        return new MusicListAdapter(getActivity())
        {
            @NonNull
            @Override
            public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
            }
        };
    }

    @Override
    public boolean onDefaultClickAction(GroupEditableListAdapter.GroupViewHolder holder)
    {
        return getSelectionConnection() != null
                ? getSelectionConnection().setSelected(holder)
                : performLayoutClickOpen(holder);
    }

    @Override
    public int onGridSpanSize(int viewType, int currentSpanSize)
    {
        return viewType == FileListAdapter.VIEW_TYPE_REPRESENTATIVE
                ? currentSpanSize
                : super.onGridSpanSize(viewType, currentSpanSize);
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_music);
    }
}
