package com.genonbeta.TrebleShot.app;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ImageListAdapter;
import com.genonbeta.TrebleShot.widget.GroupShareableListAdapter;

import java.util.Map;

/**
 * created by: veli
 * date: 30.03.2018 18:15
 */
abstract public class GalleryGroupShareableListFragment<T extends GroupShareableListAdapter.GroupShareable, V extends GroupShareableListAdapter.ViewHolder, E extends GroupShareableListAdapter<T, V>>
		extends GroupShareableListFragment<T, V, E>
{
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setDefaultGroupingCriteria(ImageListAdapter.MODE_GROUP_BY_DATE);
	}

	@Override
	public int onGridSpanSize(int viewType, int currentSpanSize)
	{
		return viewType == ImageListAdapter.VIEW_TYPE_REPRESENTATIVE
				? currentSpanSize
				: super.onGridSpanSize(viewType, currentSpanSize);
	}

	@Override
	public void onGroupingOptions(Map<String, Integer> options)
	{
		super.onGroupingOptions(options);

		options.put(getString(R.string.text_groupByNothing), ImageListAdapter.MODE_GROUP_BY_NOTHING);
		options.put(getString(R.string.text_groupByDate), ImageListAdapter.MODE_GROUP_BY_DATE);
		options.put(getString(R.string.text_groupByAlbum), ImageListAdapter.MODE_GROUP_BY_ALBUM);
	}
}
