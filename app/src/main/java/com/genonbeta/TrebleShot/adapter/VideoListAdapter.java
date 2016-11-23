package com.genonbeta.TrebleShot.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class VideoListAdapter extends AbstractEditableListAdapter
{
	private ContentResolver mResolver;
	private String mSearchWord;
	private Bitmap mPlaceHolderBitmap;
	private ArrayList<VideoInfo> mList = new ArrayList<VideoInfo>();
	private ArrayList<VideoInfo> mPendingList = new ArrayList<VideoInfo>();
	private Comparator<VideoInfo> mComparator = new Comparator<VideoInfo>()
	{
		@Override
		public int compare(VideoInfo compareFrom, VideoInfo compareTo)
		{
			return compareFrom.title.toLowerCase().compareTo(compareTo.title.toLowerCase());
		}
	};

	public VideoListAdapter(Context context)
	{
		super(context);
		this.mResolver = context.getContentResolver();
		this.mPlaceHolderBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_autorenew_white);
	}

	protected void onUpdate()
	{
		this.mPendingList.clear();

		Cursor cursor = this.mResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

		if (cursor.moveToFirst())
		{
			int idIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
			int titleIndex = cursor.getColumnIndex(MediaStore.Video.Media.TITLE);

			do
			{
				VideoInfo info = new VideoInfo(cursor.getInt(idIndex), cursor.getString(titleIndex), null, Uri.parse(MediaStore.Video.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

				if (this.mSearchWord == null || (this.mSearchWord != null && ApplicationHelper.searchWord(info.title, this.mSearchWord)))
					this.mPendingList.add(info);
			}
			while (cursor.moveToNext());

			Collections.sort(this.mPendingList, this.mComparator);
		}

		cursor.close();
	}

	@Override
	protected void onSearch(String word)
	{
		this.mSearchWord = word;
	}

	@Override
	public int getCount()
	{
		return this.mList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return this.mList.get(position);
	}

	@Override
	public long getItemId(int p1)
	{
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		final VideoInfo info = (VideoInfo) this.getItem(position);
		final ViewHolder holder;

		if (convertView == null)
		{
			convertView = getInflater().inflate(R.layout.list_video, null);
			holder = new ViewHolder();
			holder.titleView = (TextView) convertView.findViewById(R.id.text);
			holder.imageView = (ImageView) convertView.findViewById(R.id.image);
			convertView.setTag(holder);
		}
		else
			holder = (ViewHolder) convertView.getTag();

		holder.titleView.setText(info.title);

		if (holder.imageView != null)
			loadBitmap(info.id, holder.imageView);

		return convertView;

	}

	@Override
	public void notifyDataSetChanged()
	{
		if (mPendingList.size() > 0)
		{
			this.mList.clear();
			this.mList.addAll(this.mPendingList);

			this.mPendingList.clear();
		}

		super.notifyDataSetChanged();
	}

	public static class VideoInfo
	{
		public long id;
		public String title;
		public String thumbnail;
		public Uri uri;

		public VideoInfo(int id, String title, String thumbnail, Uri uri)
		{
			this.id = id;
			this.title = title;
			this.thumbnail = thumbnail;
			this.uri = uri;
		}
	}

	private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView)
	{
		if (imageView != null)
		{
			final Drawable drawable = imageView.getDrawable();

			if (drawable instanceof AsyncDrawable)
			{
				final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
				return asyncDrawable.getBitmapWorkerTask();
			}
		}

		return null;
	}

	public static boolean cancelPotentialWork(long data, ImageView imageView)
	{
		final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);

		if (bitmapWorkerTask != null)
		{
			final long bitmapData = bitmapWorkerTask.getData();
			// If bitmapData is not yet set or it differs from the new data
			if (bitmapData == 0 || bitmapData != data)
			{
				// Cancel previous task
				bitmapWorkerTask.cancel(true);
			}
			else
			{
				// The same work is already in progress
				return false;
			}
		}
		// No task associated with the ImageView, or an existing task was cancelled
		return true;
	}

	public void loadBitmap(long id, ImageView imageView)
	{
		if (cancelPotentialWork(id, imageView))
		{
			final BitmapWorkerTask task = new BitmapWorkerTask(imageView);
			final AsyncDrawable asyncDrawable = new AsyncDrawable(getContext().getResources(), mPlaceHolderBitmap, task);
			imageView.setImageDrawable(asyncDrawable);
			task.execute(id);
		}
	}

	static class AsyncDrawable extends BitmapDrawable
	{
		private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

		public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask)
		{
			super(res, bitmap);
			bitmapWorkerTaskReference = new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
		}

		public BitmapWorkerTask getBitmapWorkerTask()
		{
			return bitmapWorkerTaskReference.get();
		}
	}

	private class ViewHolder
	{
		TextView titleView;
		ImageView imageView;
	}

	private class BitmapWorkerTask extends AsyncTask<Long, Void, Bitmap>
	{
		private final WeakReference<ImageView> imageViewReference;
		private long mId = 0;

		public BitmapWorkerTask(ImageView imageView)
		{
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		@Override
		protected Bitmap doInBackground(Long... params)
		{
			this.mId = params[0];
			return MediaStore.Video.Thumbnails.getThumbnail(mResolver, params[0], MediaStore.Video.Thumbnails.MINI_KIND, null);
		}

		public long getData()
		{
			return this.mId;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			if (isCancelled())
				bitmap = null;

			if (imageViewReference != null)
			{
				ImageView imageView = imageViewReference.get();

				if (imageView != null)
					imageView.setImageBitmap(bitmap == null ? mPlaceHolderBitmap : bitmap);
			}
		}
	}
}
