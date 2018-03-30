package com.genonbeta.TrebleShot.widget;

import android.content.Context;
import android.net.Uri;

import com.genonbeta.TrebleShot.util.listing.merger.StringMerger;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 30.03.2018 14:58
 */
abstract public class GalleryGroupShareableListAdapter<T extends GalleryGroupShareableListAdapter.GalleryGroupShareable, V extends GroupShareableListAdapter.ViewHolder>
		extends GroupShareableListAdapter<T, V>
		implements GroupShareableListAdapter.GroupLister.CustomGroupListener<T>
{
	public static final int MODE_GROUP_BY_ALBUM = 101;

	public GalleryGroupShareableListAdapter(Context context, int groupBy)
	{
		super(context, groupBy);
	}

	@Override
	public boolean onCustomGroupListing(GroupLister<T> lister, int mode, T object)
	{
		if (mode == MODE_GROUP_BY_ALBUM) {
			lister.offer(object, new StringMerger<T>(object.albumName));
			return true;
		}

		return false;
	}

	@Override
	public GroupLister<T> createLister(ArrayList<T> loadedList, int groupBy)
	{
		return super.createLister(loadedList, groupBy)
				.setCustomLister(this);
	}

	public static class GalleryGroupShareable extends GroupShareableListAdapter.GroupShareable
	{
		public String albumName;

		public GalleryGroupShareable(int viewType, String representativeText)
		{
			super(viewType, representativeText);
		}

		public GalleryGroupShareable(String friendlyName, String fileName, String albumName, String mimeType, long date, long size, Uri uri)
		{
			super(friendlyName, fileName, mimeType, date, size, uri);
			this.albumName = albumName;
		}
	}
}
