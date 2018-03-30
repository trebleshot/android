package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ImageListAdapter;
import com.genonbeta.TrebleShot.adapter.MusicListAdapter;
import com.genonbeta.TrebleShot.app.GroupShareableListFragment;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.GroupShareableListAdapter;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;

import java.util.Map;

public class MusicListFragment
		extends GroupShareableListFragment<MusicListAdapter.SongHolder, GroupShareableListAdapter.ViewHolder, MusicListAdapter>
		implements TitleSupport
{
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setDefaultGroupingCriteria(ImageListAdapter.MODE_GROUP_BY_ALBUM);
	}

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
		return new MusicListAdapter(getActivity())
		{
			@Override
			public void onBindViewHolder(@NonNull final ViewHolder holder, int position)
			{
				super.onBindViewHolder(holder, position);

				if (!holder.isRepresentative())
					holder.getView().setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							performLayoutClick(v, holder);
						}
					});
			}
		};
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_music);
	}
}
