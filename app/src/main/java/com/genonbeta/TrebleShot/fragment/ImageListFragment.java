package com.genonbeta.TrebleShot.fragment;

/**
 * Created by gabm on 11/01/18.
 */

import android.content.Context;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ImageListAdapter;
import com.genonbeta.TrebleShot.app.ShareableListFragment;
import com.genonbeta.TrebleShot.util.TitleSupport;

public class ImageListFragment
		extends ShareableListFragment<ImageListAdapter.ImageHolder, ImageListAdapter>
		implements TitleSupport
{
	@Override
	public ImageListAdapter onAdapter()
	{
		return new ImageListAdapter(getActivity());
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_image);
	}
}
