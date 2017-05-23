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

public class VideoListAdapter extends AbstractEditableListAdapter<VideoListAdapter.VideoInfo>
{
	private ContentResolver mResolver;
	private Bitmap mPlaceHolderBitmap;
	private ArrayList<VideoInfo> mList = new ArrayList<VideoInfo>();
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
		mResolver = context.getContentResolver();
		mPlaceHolderBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_autorenew_white_24dp);
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

	@Override
	public ArrayList<VideoInfo> onLoad()
	{
		ArrayList<VideoInfo> list = new ArrayList<>();
		Cursor cursor = mResolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

		if (cursor.moveToFirst())
		{
			int idIndex = cursor.getColumnIndex(MediaStore.Video.Media._ID);
			int titleIndex = cursor.getColumnIndex(MediaStore.Video.Media.TITLE);
			int lengthIndex = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);

			do
			{
				VideoInfo info = new VideoInfo(cursor.getInt(idIndex), cursor.getString(titleIndex), null, Uri.parse(MediaStore.Video.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

				long length = cursor.getLong(lengthIndex);
				info.duration = convertDuration(length);

				if (getSearchWord() == null || (getSearchWord() != null && ApplicationHelper.searchWord(info.title, getSearchWord())))
					list.add(info);
			}
			while (cursor.moveToNext());

			Collections.sort(list, mComparator);
		}

		cursor.close();

		return list;
	}

	@Override
	public void onUpdate(ArrayList<VideoInfo> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	public String convertDuration(long duration)
	{
		StringBuilder string = new StringBuilder();

		long hours = (duration / 3600000);
		long minutes = (duration - (hours * 3600000)) / 60000;
		long seconds = (duration - (hours * 3600000) - (minutes * 60000)) / 1000;

		if (hours > 0)
		{
			if (hours < 10)
				string.append("0");

			string.append(hours);
			string.append(":");
		}

		if (minutes < 10)
			string.append("0");

		string.append(minutes);
		string.append(":");

		if (seconds < 10)
			string.append("0");

		string.append(seconds);

		return string.toString();
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	@Override
	public Object getItem(int position)
	{
		return mList.get(position);
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
			holder.durationView = (TextView) convertView.findViewById(R.id.duration);
			holder.imageView = (ImageView) convertView.findViewById(R.id.image);

			convertView.setTag(holder);
		}
		else
			holder = (ViewHolder) convertView.getTag();

		holder.titleView.setText(info.title);
		holder.durationView.setText(info.duration);

		if (holder.imageView != null)
			loadBitmap(info.id, holder.imageView);

		return convertView;

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

	public static class VideoInfo
	{
		public long id;
		public String title;
		public String thumbnail;
		public Uri uri;
		public String duration;

		public VideoInfo(int id, String title, String thumbnail, Uri uri)
		{
			this.id = id;
			this.title = title;
			this.thumbnail = thumbnail;
			this.uri = uri;
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
		TextView durationView;
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
			mId = params[0];
			return MediaStore.Video.Thumbnails.getThumbnail(mResolver, params[0], MediaStore.Video.Thumbnails.MINI_KIND, null);
		}

		public long getData()
		{
			return mId;
		}

		@Override
		protected void onPostExecute(Bitmap bitmap)
		{
			if (isCancelled())
				bitmap = null;

			ImageView imageView = imageViewReference.get();

			if (imageView != null)
				imageView.setImageBitmap(bitmap == null ? mPlaceHolderBitmap : bitmap);
		}
	}
}
