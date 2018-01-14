package com.genonbeta.TrebleShot.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Shareable;
import com.genonbeta.TrebleShot.util.SweetImageLoader;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * created by: Veli
 * date: 18.11.2017 13:32
 */

public class ImageListAdapter
        extends ShareableListAdapter<ImageListAdapter.ImageHolder>
        implements SweetImageLoader.Handler<ImageListAdapter.ImageHolder, Bitmap>
{
    private ContentResolver mResolver;
    private Bitmap mDefaultImageBitmap;
    private ArrayList<ImageHolder> mList = new ArrayList<>();
    private Comparator<ImageHolder> mComparator = new Comparator<ImageHolder>()
    {
        @Override
        public int compare(ImageHolder compareFrom, ImageHolder compareTo)
        {
            return (int)(compareTo.dateTaken - compareFrom.dateTaken);
        }
    };

    public ImageListAdapter(Context context)
    {
        super(context);

        mResolver = context.getContentResolver();
        mDefaultImageBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_refresh_white_24dp);
    }

    @Override
    public Bitmap onLoadBitmap(ImageHolder object)
    {
        Bitmap bitmap = MediaStore.Images.Thumbnails.getThumbnail(mResolver, object.id, MediaStore.Images.Thumbnails.MINI_KIND, null);
        return bitmap == null ? mDefaultImageBitmap : bitmap;
    }

    @Override
    public ArrayList<ImageHolder> onLoad()
    {
        ArrayList<ImageHolder> list = new ArrayList<>();
        Cursor cursor = mResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);

        if (cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
            int displayIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);
            int dateTaken = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);

            do {
                ImageHolder info = new ImageHolder(
                        cursor.getInt(idIndex),
                        cursor.getString(displayIndex),
                        cursor.getString(displayIndex),
                        null,
                        Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)),
                        Long.parseLong(cursor.getString(dateTaken)));

                list.add(info);
            }
            while (cursor.moveToNext());

            Collections.sort(list, mComparator);
        }

        cursor.close();

        return list;
    }

    @Override
    public void onUpdate(ArrayList<ImageHolder> passedItem)
    {
        mList.clear();
        mList.addAll(passedItem);
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

    public ArrayList<ImageHolder> getList()
    {
        return mList;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null)
            convertView = getInflater().inflate(R.layout.list_image, parent, false);

        final ImageHolder holder = (ImageHolder) this.getItem(position);

		final ImageView image = convertView.findViewById(R.id.image);
		TextView text1 = convertView.findViewById(R.id.text);
		TextView text2 = convertView.findViewById(R.id.text2);

        text1.setText(holder.friendlyName);
        text2.setText(holder.dateTakenString);

		image.setSelected(getSelectionConnection().isSelected(holder));

		image.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				getSelectionConnection().setSelected(holder);
				image.setSelected(holder.isSelectableSelected());
			}
		});

        SweetImageLoader.load(this, getContext(), image, holder);

        return convertView;
    }

    public static class ImageHolder extends Shareable
    {
        public long id;
        public String thumbnail;
        public long dateTaken;
        public String dateTakenString;

        public ImageHolder(int id, String friendlyName, String filename, String thumbnail, Uri uri, long mod_time)
        {
            super(friendlyName, filename, uri);

            SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.getDefault());

            //sdf.setTimeZone(TimeZone.getTimeZone("UTC"));

            this.id = id;
            this.thumbnail = thumbnail;
            this.dateTaken = mod_time/1000;
            this.dateTakenString = dateFormat.format(new Date(mod_time));
        }
    }
}
