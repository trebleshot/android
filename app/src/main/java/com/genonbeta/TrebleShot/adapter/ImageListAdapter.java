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
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

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
            return compareFrom.friendlyName.toLowerCase().compareTo(compareTo.friendlyName.toLowerCase());
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
            int titleIndex = cursor.getColumnIndex(MediaStore.Images.Media.TITLE);
            int displayIndex = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME);

            do {
                ImageHolder info = new ImageHolder(cursor.getInt(idIndex), cursor.getString(titleIndex), cursor.getString(displayIndex), null, Uri.parse(MediaStore.Images.Media.EXTERNAL_CONTENT_URI + "/" + cursor.getInt(idIndex)));

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

        ImageHolder info = (ImageHolder) this.getItem(position);

        TextView titleView = convertView.findViewById(R.id.text);
        ImageView imageView = convertView.findViewById(R.id.image);

        titleView.setText(info.friendlyName);

        SweetImageLoader.load(this, getContext(), imageView, info);

        return convertView;
    }

    public static class ImageHolder extends Shareable
    {
        public long id;
        public String thumbnail;

        public ImageHolder(int id, String friendlyName, String displayName, String thumbnail, Uri uri)
        {
            super(friendlyName, displayName, uri);

            this.id = id;
            this.thumbnail = thumbnail;
        }
    }
}
