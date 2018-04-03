package com.genonbeta.TrebleShot.fragment;

/**
 * Created by gabm on 11/01/18.
 */

import android.content.Context;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ImageListAdapter;
import com.genonbeta.TrebleShot.app.GalleryGroupShareableListFragment;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.GroupShareableListAdapter;

public class ImageListFragment
		extends GalleryGroupShareableListFragment<ImageListAdapter.ImageHolder, GroupShareableListAdapter.ViewHolder, ImageListAdapter>
		implements TitleSupport
{
	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setDefaultOrderingAscending(false);
		setDefaultSortingCriteria(R.id.actions_abs_editable_sort_by_date);
		setDefaultViewingGridSize(2, 4);
	}

	@Override
	public void onViewCreated(View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_photo_white_24dp);
		setEmptyText(getString(R.string.text_listEmptyImage));
	}

	@Override
	public void onResume()
	{
		super.onResume();

		getContext().getContentResolver()
				.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, getDefaultContentObserver());
	}

	@Override
	public void onPause()
	{
		super.onPause();

		getContext().getContentResolver()
				.unregisterContentObserver(getDefaultContentObserver());
	}

	@Override
	public ImageListAdapter onAdapter()
	{
		final AppUtils.QuickActions<GroupShareableListAdapter.ViewHolder> quickActions = new AppUtils.QuickActions<GroupShareableListAdapter.ViewHolder>()
		{
			@Override
			public void onQuickActions(final GroupShareableListAdapter.ViewHolder clazz)
			{
				if (!clazz.isRepresentative())
					clazz.getView().setOnClickListener(new View.OnClickListener()
					{
						@Override
						public void onClick(View v)
						{
							performLayoutClick(v, clazz);
						}
					});
			}
		};

		return new ImageListAdapter(getActivity())
		{
			@NonNull
			@Override
			public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
			{
				return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
			}
		};
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_photo);
	}
}
