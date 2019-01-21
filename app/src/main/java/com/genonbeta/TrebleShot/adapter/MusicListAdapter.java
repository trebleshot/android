package com.genonbeta.TrebleShot.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;

import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.framework.util.listing.merger.StringMerger;

import java.io.File;
import java.util.List;
import java.util.Map;

public class MusicListAdapter
        extends GroupEditableListAdapter<MusicListAdapter.SongHolder, GroupEditableListAdapter.GroupViewHolder>
        implements GroupEditableListAdapter.GroupLister.CustomGroupLister<MusicListAdapter.SongHolder>
{
    public static final int MODE_GROUP_BY_ALBUM = MODE_GROUP_BY_NOTHING + 1;
    public static final int MODE_GROUP_BY_ARTIST = MODE_GROUP_BY_ALBUM + 1;
    public static final int MODE_GROUP_BY_FOLDER = MODE_GROUP_BY_ARTIST + 1;

    private ContentResolver mResolver;

    public MusicListAdapter(Context context)
    {
        super(context, MODE_GROUP_BY_ARTIST);
        mResolver = context.getContentResolver();
    }

    @Override
    protected void onLoad(GroupLister<SongHolder> lister)
    {
        Map<Integer, AlbumHolder> albumList = new ArrayMap<>();
        Cursor songCursor = mResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null,
                MediaStore.Audio.Media.IS_MUSIC + "=?",
                new String[]{String.valueOf(1)},
                null);

        Cursor albumCursor = mResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null, null, null, null);

        if (albumCursor != null) {
            if (albumCursor.moveToFirst()) {
                int idIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID);
                int artIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART);
                int titleIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM);

                do {
                    albumList.put(albumCursor.getInt(idIndex), new AlbumHolder(albumCursor.getInt(idIndex), albumCursor.getString(titleIndex), albumCursor.getString(artIndex)));
                } while (albumCursor.moveToNext());
            }

            albumCursor.close();
        }

        if (songCursor != null) {
            if (songCursor.moveToFirst()) {
                int idIndex = songCursor.getColumnIndex(MediaStore.Audio.Media._ID);
                int artistIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int songIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int folderIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
                int albumIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
                int nameIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                int dateIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED);
                int sizeIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.SIZE);
                int typeIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE);

                do {
                    lister.offerObliged(this, new SongHolder(
                            songCursor.getLong(idIndex),
                            songCursor.getString(nameIndex),
                            songCursor.getString(artistIndex),
                            songCursor.getString(songIndex),
                            extractFolderName(songCursor.getString(folderIndex)),
                            songCursor.getString(typeIndex),
                            songCursor.getInt(albumIndex),
                            albumList.get(songCursor.getInt(albumIndex)),
                            songCursor.getLong(dateIndex) * 1000,
                            songCursor.getLong(sizeIndex),
                            Uri.parse(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI + "/" + songCursor.getInt(idIndex))));
                }
                while (songCursor.moveToNext());
            }

            songCursor.close();
        }
    }

    @Override
    protected SongHolder onGenerateRepresentative(String representativeText)
    {
        return new SongHolder(representativeText);
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        if (viewType == VIEW_TYPE_REPRESENTATIVE)
            return new GroupViewHolder(getInflater().inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text);

        return new GroupViewHolder(getInflater().inflate(R.layout.list_music, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupViewHolder holder, int position)
    {
        try {
            final SongHolder object = getItem(position);
            final View parentView = holder.getView();

            if (!holder.tryBinding(object)) {
                ImageView image = parentView.findViewById(R.id.image);
                TextView text1 = parentView.findViewById(R.id.text);
                TextView text2 = parentView.findViewById(R.id.text2);
                TextView text3 = parentView.findViewById(R.id.text3);
                TextView textSeparator1 = parentView.findViewById(R.id.textSeparator1);

                text1.setText(object.song);

                if (getGroupBy() == MODE_GROUP_BY_ALBUM
                        || getGroupBy() == MODE_GROUP_BY_ARTIST) {
                    text2.setText(getGroupBy() == MODE_GROUP_BY_ALBUM
                            ? object.artist
                            : object.albumHolder.title);

                    text3.setVisibility(View.GONE);
                    textSeparator1.setVisibility(View.GONE);
                } else {
                    text2.setText(object.artist);
                    text3.setText(object.albumHolder.title);
                    text3.setVisibility(View.VISIBLE);
                    textSeparator1.setVisibility(View.VISIBLE);
                }

                parentView.setSelected(object.isSelectableSelected());

                GlideApp.with(getContext())
                        .load(object.albumHolder.art)
                        .placeholder(R.drawable.ic_music_note_white_24dp)
                        .override(160)
                        .centerCrop()
                        .into(image);
            }
        } catch (Exception e) {

        }
    }

    @Override
    public boolean onCustomGroupListing(GroupLister<SongHolder> lister, int mode, SongHolder object)
    {
        if (mode == MODE_GROUP_BY_ALBUM)
            lister.offer(object, new StringMerger<SongHolder>(object.albumHolder.title));
        else if (mode == MODE_GROUP_BY_ARTIST)
            lister.offer(object, new StringMerger<SongHolder>(object.artist));
        else if (mode == MODE_GROUP_BY_FOLDER)
            lister.offer(object, new StringMerger<SongHolder>(object.folder));
        else
            return false;

        return true;
    }

    public GroupLister<SongHolder> createLister(List<SongHolder> loadedList, int groupBy)
    {
        return super.createLister(loadedList, groupBy)
                .setCustomLister(this);
    }

    public String extractFolderName(String folder)
    {
        if (folder.contains(File.separator)) {
            String[] split = folder.split(File.separator);

            if (split.length >= 2)
                folder = split[split.length - 2];
        }

        return folder;
    }

    @NonNull
    @Override
    public String getSectionName(int position, SongHolder object)
    {
        if (!object.isGroupRepresentative()) {
            if (getGroupBy() == MODE_GROUP_BY_ARTIST)
                return object.artist;
            else if (getGroupBy() == MODE_GROUP_BY_FOLDER)
                return object.folder;
            else if (getGroupBy() == MODE_GROUP_BY_ALBUM)
                return object.albumHolder.title;
        }

        return super.getSectionName(position, object);
    }

    public static class SongHolder extends GroupEditableListAdapter.GroupShareable
    {
        public String artist;
        public String song;
        public String folder;
        public int albumId;
        public AlbumHolder albumHolder;

        public SongHolder(String representativeText)
        {
            super(VIEW_TYPE_REPRESENTATIVE, representativeText);
        }

        public SongHolder(long id, String displayName, String artist, String song, String folder, String mimeType, int albumId, AlbumHolder albumHolder, long date, long size, Uri uri)
        {
            super(id, song + " - " + artist, displayName, mimeType, date, size, uri);

            this.artist = artist;
            this.song = song;
            this.folder = folder;
            this.albumId = albumId;
            this.albumHolder = albumHolder == null ? new AlbumHolder(albumId, "-", null) : albumHolder;
        }

        @Override
        public boolean applyFilter(String[] filteringKeywords)
        {
            if (super.applyFilter(filteringKeywords))
                return true;

            for (String keyword : filteringKeywords)
                if (folder.toLowerCase().contains(keyword.toLowerCase()))
                    return true;

            return false;
        }

        @Override
        public String getComparableName()
        {
            return song;
        }

        @Override
        public boolean searchMatches(String searchWord)
        {
            if (isGroupRepresentative())
                return super.searchMatches(searchWord);

            return TextUtils.searchWord(artist, searchWord)
                    || TextUtils.searchWord(song, searchWord)
                    || TextUtils.searchWord(albumHolder.title, searchWord);
        }
    }

    public static class AlbumHolder
    {
        public int id;
        public String art;
        public String title;

        public AlbumHolder(int id, String title, String art)
        {
            this.id = id;
            this.art = art;
            this.title = title;
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof AlbumHolder ? ((AlbumHolder) obj).id == id : super.equals(obj);
        }
    }
}
