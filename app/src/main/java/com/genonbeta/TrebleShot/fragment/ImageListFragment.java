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

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ImageListAdapter;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.GroupShareableListAdapter;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;

public class ImageListFragment
		extends ShareableListFragment<ImageListAdapter.ImageHolder, GroupShareableListAdapter.ViewHolder, ImageListAdapter>
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
		return new ImageListAdapter(getActivity())
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
	public int onGridSpanSize(int viewType, int currentSpanSize)
	{
		return viewType == ImageListAdapter.VIEW_TYPE_REPRESENTATIVE
				? currentSpanSize
				: super.onGridSpanSize(viewType, currentSpanSize);
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_image);
	}
}
