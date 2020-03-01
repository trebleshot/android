/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.adapter;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.MimeIconUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.KuickDb;
import com.genonbeta.android.database.Progress;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.util.MathUtils;
import com.genonbeta.android.framework.util.listing.ComparableMerger;
import com.genonbeta.android.framework.util.listing.Merger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class FileListAdapter extends GroupEditableListAdapter<FileListAdapter.FileHolder,
        GroupEditableListAdapter.GroupViewHolder> implements GroupEditableListAdapter.GroupLister.CustomGroupLister<
        FileListAdapter.FileHolder>
{
    public static final int MODE_GROUP_BY_DEFAULT = MODE_GROUP_BY_NOTHING + 1;
    public static final int REQUEST_CODE_MOUNT_FOLDER = 1;

    private boolean mShowDirectories = true;
    private boolean mShowFiles = true;
    private boolean mShowThumbnails = true;
    private String mFileMatch;
    private DocumentFile mPath;

    public FileListAdapter(Context context)
    {
        super(context, MODE_GROUP_BY_DEFAULT);
    }

    @Override
    protected void onLoad(GroupLister<FileHolder> lister)
    {
        mShowThumbnails = AppUtils.getDefaultPreferences(getContext()).getBoolean("load_thumbnails",
                true);

        DocumentFile path = getPath();

        if (path != null) {
            DocumentFile[] fileIndex = path.listFiles();

            if (fileIndex != null && fileIndex.length > 0) {
                for (DocumentFile file : fileIndex) {
                    if ((mFileMatch != null && !file.getName().matches(mFileMatch)))
                        continue;

                    if ((mShowDirectories && file.isDirectory() || (mShowFiles && file.isFile())))
                        lister.offerObliged(this, new FileHolder(getContext(), file));
                }
            }
        } else {
            {
                FileHolder saveDir = new FileHolder(getContext(), FileUtils.getApplicationDirectory(getContext()));
                saveDir.type = FileHolder.Type.SaveLocation;
                lister.offerObliged(this, saveDir);
            }

            {
                File rootDir = new File(".");
                if (rootDir.canRead()) {
                    FileHolder rootHolder = new FileHolder(getContext(), DocumentFile.fromFile(rootDir));
                    rootHolder.friendlyName = getContext().getString(R.string.text_fileRoot);
                    lister.offerObliged(this, rootHolder);
                }
            }

            List<File> referencedDirectoryList = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= 21)
                referencedDirectoryList.addAll(Arrays.asList(getContext().getExternalMediaDirs()));
            else if (Build.VERSION.SDK_INT >= 19)
                referencedDirectoryList.addAll(Arrays.asList(getContext().getExternalFilesDirs(null)));
            else
                referencedDirectoryList.add(Environment.getExternalStorageDirectory());

            for (File mediaDir : referencedDirectoryList) {
                if (mediaDir == null || !mediaDir.canWrite())
                    continue;

                FileHolder fileHolder = new FileHolder(getContext(), DocumentFile.fromFile(mediaDir));
                fileHolder.type = FileHolder.Type.Storage;
                String[] splitPath = mediaDir.getAbsolutePath().split(File.separator);

                if (splitPath.length >= 2 && splitPath[1].equals("storage")) {
                    if (splitPath.length >= 4 && splitPath[2].equals("emulated")) {
                        File file = new File(buildPath(splitPath, 4));

                        if (file.canWrite()) {
                            fileHolder.file = DocumentFile.fromFile(file);
                            fileHolder.friendlyName = "0".equals(splitPath[3])
                                    ? getContext().getString(R.string.text_internalStorage)
                                    : getContext().getString(R.string.text_emulatedMediaDirectory, splitPath[3]);
                        }
                    } else if (splitPath.length >= 3) {
                        File file = new File(buildPath(splitPath, 3));

                        if (!file.canWrite())
                            continue;

                        fileHolder.friendlyName = splitPath[2];
                        fileHolder.file = DocumentFile.fromFile(file);
                    }
                }

                lister.offerObliged(this, fileHolder);
            }

            {
                List<FileHolder> savedDirList = AppUtils.getKuick(getContext())
                        .castQuery(new SQLQuery.Select(Kuick.TABLE_FILEBOOKMARK), FileHolder.class);

                for (FileHolder dir : savedDirList)
                    if (dir.file != null)
                        lister.offerObliged(this, dir);
            }

            if (Build.VERSION.SDK_INT >= 21) {
                FileHolder mountButtonRep = new FileHolder(GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON,
                        getContext().getString(R.string.butn_mountDirectory));
                mountButtonRep.requestCode = REQUEST_CODE_MOUNT_FOLDER;
                mountButtonRep.type = FileHolder.Type.Storage;
                lister.offerObliged(this, mountButtonRep);
            }

            {
                List<TransferObject> objects = AppUtils.getKuick(getContext())
                        .castQuery(new SQLQuery.Select(Kuick.TABLE_TRANSFER).setWhere(
                                String.format("%s = ?", Kuick.FIELD_TRANSFER_FLAG),
                                TransferObject.Flag.DONE.toString()).setOrderBy(
                                String.format("%s DESC", Kuick.FIELD_TRANSFER_LASTCHANGETIME)),
                                TransferObject.class);

                List<DocumentFile> pickedRecentFiles = new ArrayList<>();
                Map<Long, TransferGroup> groupMap = new ArrayMap<>();

                for (TransferGroup group : AppUtils.getKuick(getContext()).castQuery(
                        new SQLQuery.Select(Kuick.TABLE_TRANSFERGROUP), TransferGroup.class))
                    groupMap.put(group.id, group);

                int errorLimit = 3;

                for (TransferObject object : objects) {
                    TransferGroup group = groupMap.get(object.groupId);

                    if (pickedRecentFiles.size() >= 20 || errorLimit == 0 || group == null)
                        break;

                    try {
                        DocumentFile documentFile = FileUtils.getIncomingPseudoFile(getContext(), object, group,
                                false);

                        if (documentFile.exists() && !pickedRecentFiles.contains(documentFile))
                            pickedRecentFiles.add(documentFile);
                        else
                            errorLimit--;
                    } catch (IOException e) {
                        errorLimit--;
                    }
                }

                for (DocumentFile documentFile : pickedRecentFiles) {
                    FileHolder holder = new FileHolder(getContext(), documentFile);
                    holder.type = FileHolder.Type.Recent;
                    lister.offerObliged(this, holder);
                }
            }
        }
    }

    @Override
    protected FileHolder onGenerateRepresentative(String text, Merger<FileHolder> merger)
    {
        return new FileHolder(VIEW_TYPE_REPRESENTATIVE, text);
    }

    @Override
    public boolean onCustomGroupListing(GroupLister<FileHolder> lister, int mode, FileHolder object)
    {
        if (mode == MODE_GROUP_BY_DEFAULT)
            lister.offer(object, new FileHolderMerger(object));
        else
            return false;

        return true;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return viewType == VIEW_TYPE_DEFAULT ? new GroupViewHolder(getInflater().inflate(R.layout.list_file, parent,
                false)) : createDefaultViews(parent, viewType, false);
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupViewHolder holder, final int position)
    {
        try {
            final FileHolder object = getItem(position);

            if (!holder.tryBinding(object)) {
                final View parentView = holder.itemView;

                ImageView thumbnail = parentView.findViewById(R.id.thumbnail);
                ImageView image = parentView.findViewById(R.id.image);
                TextView text1 = parentView.findViewById(R.id.text);
                TextView text2 = parentView.findViewById(R.id.text2);

                holder.setSelected(object.isSelectableSelected());

                text1.setText(object.friendlyName);
                text2.setText(object.getInfo(getContext()));

                if (!mShowThumbnails || !object.loadThumbnail(getContext(), thumbnail)) {
                    image.setImageResource(object.getIconRes());
                    thumbnail.setImageDrawable(null);
                } else
                    image.setImageDrawable(null);
            } else if (holder.getItemViewType() == GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON)
                ((ImageView) holder.itemView.findViewById(R.id.icon)).setImageResource(object.getIconRes());
        } catch (NotReadyException e) {
            e.printStackTrace();
        }
    }

    public String buildPath(String[] splitPath, int count)
    {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; (i < count && i < splitPath.length); i++) {
            stringBuilder.append(File.separator);
            stringBuilder.append(splitPath[i]);
        }

        return stringBuilder.toString();
    }

    public GroupLister<FileHolder> createLister(List<FileHolder> loadedList, int groupBy)
    {
        return super.createLister(loadedList, groupBy).setCustomLister(this);
    }

    @Override
    public int getGroupBy()
    {
        if (mPath != null && mPath.equals(FileUtils.getApplicationDirectory(getContext())))
            return MODE_GROUP_BY_DATE;

        return super.getGroupBy();
    }

    @Override
    public int getSortingCriteria(FileHolder objectOne, FileHolder objectTwo)
    {
        // Checking whether the path is null helps to increase the speed.
        if (getPath() == null && FileHolder.Type.Recent.equals(objectOne.getType())
                && FileHolder.Type.Recent.equals(objectTwo.getType()))
            return MODE_SORT_BY_DATE;

        return super.getSortingCriteria(objectOne, objectTwo);
    }

    @Override
    public int getSortingOrder(FileHolder objectOne, FileHolder objectTwo)
    {
        // Checking whether the path is null helps to increase the speed.
        if (getPath() == null && FileHolder.Type.Recent.equals(objectOne.getType())
                && FileHolder.Type.Recent.equals(objectTwo.getType()))
            return MODE_SORT_ORDER_DESCENDING;

        return super.getSortingOrder(objectOne, objectTwo);
    }

    @Nullable
    public DocumentFile getPath()
    {
        return mPath;
    }

    public void goPath(File path)
    {
        goPath(DocumentFile.fromFile(path));
    }

    @Override
    public String getRepresentativeText(Merger<? extends FileHolder> merger)
    {
        if (merger instanceof FileHolderMerger) {
            switch (((FileHolderMerger) merger).type) {
                case Storage:
                    return getContext().getString(R.string.text_storage);
                case PublicFolder:
                    return getContext().getString(R.string.text_shortcuts);
                case Folder:
                    return getContext().getString(R.string.text_folder);
                case PartFile:
                    return getContext().getString(R.string.text_pendingTransfers);
                case RecentFile:
                    return getContext().getString(R.string.text_recentFiles);
                case File:
                    return getContext().getString(R.string.text_file);
                case Dummy:
                default:
                    return getContext().getString(R.string.text_unknown);
            }
        }

        return super.getRepresentativeText(merger);
    }

    public void goPath(DocumentFile path)
    {
        mPath = path;
    }

    public void setConfiguration(boolean showDirectories, boolean showFiles, String fileMatch)
    {
        mShowDirectories = showDirectories;
        mShowFiles = showFiles;
        mFileMatch = fileMatch;
    }

    public static class FileHolder extends GroupEditableListAdapter.GroupShareable implements DatabaseObject<Object>
    {
        @Nullable
        public DocumentFile file;
        public TransferObject transferObject = null;
        public int requestCode;
        protected Type type = null;

        public FileHolder()
        {
            super();
        }

        public FileHolder(int viewType, String representativeText)
        {
            super(viewType, representativeText);
        }

        public FileHolder(Context context, DocumentFile file)
        {
            initialize(file);
            calculate(context);
        }

        protected void calculate(Context context)
        {
            if (file != null && AppConfig.EXT_FILE_PART.equals(FileUtils.getFileFormat(file.getName()))) {
                type = Type.Pending;
                try {
                    Kuick kuick = AppUtils.getKuick(context);
                    ContentValues data = kuick.getFirstFromTable(new SQLQuery.Select(Kuick.TABLE_TRANSFER)
                            .setWhere(Kuick.FIELD_TRANSFER_FILE + "=?", file.getName()));

                    if (data != null) {
                        transferObject = new TransferObject();
                        transferObject.reconstruct(kuick.getWritableDatabase(), kuick, data);

                        mimeType = transferObject.mimeType;
                        friendlyName = transferObject.name;
                    }
                } catch (Exception ignored) {
                }
            }
        }

        @Override
        public boolean comparisonSupported()
        {
            return getViewType() != GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON && super.comparisonSupported();
        }

        @DrawableRes
        public int getIconRes()
        {
            if (file == null)
                return 0;
            else if (file.isDirectory()) {
                switch (getType()) {
                    case Storage:
                        return R.drawable.ic_save_white_24dp;
                    case SaveLocation:
                        return R.drawable.ic_trebleshot_white_24dp_static;
                    case Bookmarked:
                    case Mounted:
                        return R.drawable.ic_bookmark_white_24dp;
                    default:
                        return R.drawable.ic_folder_white_24dp;
                }
            } else {
                if (Type.Pending.equals(getType()) && transferObject == null)
                    return R.drawable.ic_block_white_24dp;

                return MimeIconUtils.loadMimeIcon(mimeType);
            }
        }

        @Override
        public long getId()
        {
            if (super.getId() == 0 && file != null)
                setId(String.format("%s_%s", file.getUri().toString(), getType()).hashCode());

            return super.getId();
        }

        public String getInfo(Context context)
        {
            switch (getType()) {
                case Storage:
                    return context.getString(R.string.text_storage);
                case Mounted:
                    return context.getString(R.string.text_mountedDirectory);
                case Bookmarked:
                case Folder:
                case Public:
                    if (file != null && file.isDirectory()) {
                        int itemSize = file.listFiles().length;
                        return context.getResources().getQuantityString(R.plurals.text_items, itemSize, itemSize);
                    } else
                        return context.getString(R.string.text_unknown);
                case SaveLocation:
                    return context.getString(R.string.text_defaultFolder);
                case Pending:
                    return transferObject == null ? context.getString(R.string.mesg_notValidTransfer)
                            : String.format("%s / %s", FileUtils.sizeExpression(getComparableSize(), false),
                            FileUtils.sizeExpression(transferObject.size, false));
                case Recent:
                case File:
                    return FileUtils.sizeExpression(getComparableSize(), false);
                default:
                    return context.getString(R.string.text_unknown);
            }
        }

        public Type getType()
        {
            if (type == null && file == null)
                type = Type.Dummy;

            return type == null ? (file.isDirectory() ? Type.Folder : Type.File) : type;
        }

        @Override
        public int getRequestCode()
        {
            return requestCode;
        }

        @Override
        public ContentValues getValues()
        {
            ContentValues contentValues = new ContentValues();
            contentValues.put(Kuick.FIELD_FILEBOOKMARK_TITLE, friendlyName);
            contentValues.put(Kuick.FIELD_FILEBOOKMARK_PATH, uri.toString());
            return contentValues;
        }

        @Override
        public SQLQuery.Select getWhere()
        {
            return new SQLQuery.Select(Kuick.TABLE_FILEBOOKMARK).setWhere(String.format("%s = ?",
                    Kuick.FIELD_FILEBOOKMARK_PATH), uri.toString());
        }


        protected void initialize(DocumentFile file)
        {
            initialize(0, file.getName(), file.getName(), file.getType(), file.lastModified(), file.length(),
                    file.getUri());
            this.file = file;
        }

        public boolean loadThumbnail(Context context, ImageView imageView)
        {
            if (file == null || file.isDirectory() || Type.Pending.equals(getType())
                    || (!mimeType.startsWith("image/") && !mimeType.startsWith("video/")))
                return false;

            GlideApp.with(context)
                    .load(file.getUri())
                    .error(MimeIconUtils.loadMimeIcon(mimeType))
                    .override(160)
                    .centerCrop()
                    .into(imageView);

            return true;
        }

        @Override
        public void reconstruct(SQLiteDatabase db, KuickDb kuick, ContentValues item)
        {
            uri = Uri.parse(item.getAsString(Kuick.FIELD_FILEBOOKMARK_PATH));
            type = uri.toString().startsWith("file") ? Type.Bookmarked : Type.Mounted;

            try {
                initialize(FileUtils.fromUri(kuick.getContext(), uri));
                calculate(kuick.getContext());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            friendlyName = item.getAsString(Kuick.FIELD_FILEBOOKMARK_TITLE);
        }

        @Override
        public void onCreateObject(SQLiteDatabase db, KuickDb kuick, Object parent, Progress.Listener listener)
        {

        }

        @Override
        public void onUpdateObject(SQLiteDatabase db, KuickDb kuick, Object parent, Progress.Listener listener)
        {

        }

        @Override
        public void onRemoveObject(SQLiteDatabase db, KuickDb kuick, Object parent, Progress.Listener listener)
        {

        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            if (Type.Dummy.equals(getType()) || Type.Mounted.equals(getType()) || Type.Public.equals(getType())
                    || Type.Recent.equals(getType()))
                return false;

            return super.setSelectableSelected(selected);
        }

        public enum Type
        {
            Storage,
            Bookmarked,
            Mounted,
            Public,
            SaveLocation,
            Recent,
            Pending,
            Folder,
            File,
            Dummy
        }
    }

    private static class FileHolderMerger extends ComparableMerger<FileHolder>
    {
        protected Type type;

        public FileHolderMerger(FileHolder holder)
        {
            //
            switch (holder.getType()) {
                case Mounted:
                case Storage:
                    type = Type.Storage;
                    break;
                case Public:
                case Bookmarked:
                    type = Type.PublicFolder;
                    break;
                case Pending:
                    type = Type.PartFile;
                    break;
                case Recent:
                    type = Type.RecentFile;
                    break;
                case Folder:
                case SaveLocation:
                    type = Type.Folder;
                    break;
                case File:
                    type = Type.File;
                case Dummy:
                default:
                    type = Type.Dummy;
            }
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof FileHolderMerger && ((FileHolderMerger) obj).type.equals(type);
        }

        @Override
        public int compareTo(@NonNull ComparableMerger<FileHolder> o)
        {
            if (o instanceof FileHolderMerger)
                return MathUtils.compare(((FileHolderMerger) o).type.ordinal(), type.ordinal());

            return 0;
        }

        public enum Type
        {
            Storage,
            Folder,
            PublicFolder,
            RecentFile,
            PartFile,
            File,
            Dummy
        }
    }
}
