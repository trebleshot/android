package com.genonbeta.TrebleShot.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.displayingbitmaps.util.AsyncTask;
import com.android.displayingbitmaps.util.RecyclingImageView;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class VideoListAdapter extends AbstractFlexibleAdapter
{
	private Context mContext;
	private ContentResolver mResolver;
	private String mSearchWord;
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
		this.mContext = context;
		this.mResolver = context.getContentResolver();
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
	public View getView(int position, View view, ViewGroup viewGroup)
	{
		return this.getViewAt(LayoutInflater.from(mContext).inflate(R.layout.list_video, viewGroup, false), position);
	}

	public View getViewAt(View view, int position)
	{
		final VideoInfo info = (VideoInfo) this.getItem(position);
		TextView text = (TextView)view.findViewById(R.id.text);
		final RecyclingImageView image = (RecyclingImageView)view.findViewById(R.id.image);

		new BitmapWorkerTask(info.id, image).executeOnExecutor(AsyncTask.DUAL_THREAD_EXECUTOR);

		text.setText(info.title);

		return view;
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

	private class BitmapWorkerTask extends AsyncTask<Void, Void, BitmapDrawable>
	{
        private long mId;
        private final WeakReference<ImageView> imageViewReference;

        public BitmapWorkerTask(long id, ImageView imageView)
		{
            this.mId = id;
            imageViewReference = new WeakReference<ImageView>(imageView);
        }

        @Override
        protected BitmapDrawable doInBackground(Void... params)
		{
			BitmapDrawable thumb = null;

			if (getAttachedImageView().isShown())
			{
				Bitmap bitmap = MediaStore.Video.Thumbnails.getThumbnail(mResolver, mId, MediaStore.Video.Thumbnails.MINI_KIND, null);
				
				if (bitmap != null)
					thumb = new BitmapDrawable(mContext.getResources(), bitmap);
			}
			
            return thumb;
        }

        @Override
        protected void onPostExecute(BitmapDrawable value)
		{
			if (value == null)
				return;

            ImageView imageView = getAttachedImageView();

			if (imageView != null)
				imageView.setImageDrawable(value);
        }

        private ImageView getAttachedImageView()
		{
            return imageViewReference.get();
        }
    }
}
