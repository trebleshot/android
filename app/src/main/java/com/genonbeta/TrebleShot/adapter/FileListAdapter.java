package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
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

import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.FileShortcutObject;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.object.WritablePathObject;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.MimeIconUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.CursorItem;
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

public class FileListAdapter
        extends GroupEditableListAdapter<FileListAdapter.GenericFileHolder, GroupEditableListAdapter.GroupViewHolder>
        implements GroupEditableListAdapter.GroupLister.CustomGroupLister<FileListAdapter.GenericFileHolder>
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
    protected void onLoad(GroupLister<GenericFileHolder> lister)
    {
        mShowThumbnails = AppUtils.getDefaultPreferences(getContext())
                .getBoolean("load_thumbnails", true);

        DocumentFile path = getPath();

        if (path != null) {
            DocumentFile[] fileIndex = path.listFiles();

            if (fileIndex != null && fileIndex.length > 0) {
                for (DocumentFile file : fileIndex) {
                    if ((mFileMatch != null && !file.getName().matches(mFileMatch)))
                        continue;

                    if (file.isDirectory() && mShowDirectories)
                        lister.offerObliged(this, new DirectoryHolder(file, getContext().getString(R.string.text_folder), R.drawable.ic_folder_white_24dp));
                    else if (file.isFile() && mShowFiles) {
                        if (AppConfig.EXT_FILE_PART.equals(FileUtils.getFileFormat(file.getName()))) {
                            TransferObject existingObject = null;

                            try {
                                CursorItem cursorItem = AppUtils.getDatabase(getContext())
                                        .getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                                                .setWhere(AccessDatabase.FIELD_TRANSFER_FILE + "=?", file.getName()));

                                if (cursorItem != null)
                                    existingObject = new TransferObject(cursorItem);
                            } catch (Exception e) {
                            }

                            lister.offerObliged(this, new ReceivedFileHolder(getContext(), file, existingObject));
                        } else
                            lister.offerObliged(this, new FileHolder(getContext(), file));
                    }
                }
            }
        } else {
            List<File> referencedDirectoryList = new ArrayList<>();
            DocumentFile defaultFolder = FileUtils.getApplicationDirectory(getContext());

            lister.offerObliged(this, new DirectoryHolder(defaultFolder, getContext().getString(R.string.text_receivedFiles), R.drawable.ic_trebleshot_rounded_white_24dp_static));

            lister.offerObliged(this, new PublicDirectoryHolder(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
                    getContext().getString(R.string.text_photo), R.drawable.ic_photo_white_24dp));

            if (Build.VERSION.SDK_INT >= 19)
                lister.offerObliged(this, new PublicDirectoryHolder(Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                        getContext().getString(R.string.text_documents), R.drawable.ic_library_books_white_24dp));

            lister.offerObliged(this, new PublicDirectoryHolder(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    getContext().getString(R.string.text_downloads), R.drawable.ic_file_download_white_24dp));

            lister.offerObliged(this, new PublicDirectoryHolder(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                    getContext().getString(R.string.text_music), R.drawable.ic_music_note_white_24dp));

            File fileSystemRoot = new File(".");

            if (fileSystemRoot.canRead())
                lister.offerObliged(this, new DirectoryHolder(DocumentFile.fromFile(fileSystemRoot),
                        getContext().getString(R.string.text_fileRoot),
                        getContext().getString(R.string.text_folder),
                        R.drawable.ic_folder_white_24dp));

            if (Build.VERSION.SDK_INT >= 21)
                referencedDirectoryList.addAll(Arrays.asList(getContext().getExternalMediaDirs()));
            else if (Build.VERSION.SDK_INT >= 19)
                referencedDirectoryList.addAll(Arrays.asList(getContext().getExternalFilesDirs(null)));
            else
                referencedDirectoryList.add(Environment.getExternalStorageDirectory());

            for (File mediaDir : referencedDirectoryList) {
                if (mediaDir == null || !mediaDir.canWrite())
                    continue;

                StorageHolder fileHolder = new StorageHolder(DocumentFile.fromFile(mediaDir), getContext().getString(R.string.text_storage), R.drawable.ic_save_white_24dp);
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

            List<FileShortcutObject> shortcutList = AppUtils.getDatabase(getContext())
                    .castQuery(new SQLQuery.Select(AccessDatabase.TABLE_FILEBOOKMARK), FileShortcutObject.class);

            for (FileShortcutObject object : shortcutList) {
                try {
                    lister.offerObliged(this, new ShortcutDirectoryHolder(getContext(), object));
                } catch (Exception e) {
                    // do nothing
                }
            }

            List<WritablePathObject> mountedPathList = AppUtils.getDatabase(getContext())
                    .castQuery(new SQLQuery.Select(AccessDatabase.TABLE_WRITABLEPATH), WritablePathObject.class);

            if (Build.VERSION.SDK_INT >= 21) {
                for (WritablePathObject pathObject : mountedPathList)
                    try {
                        lister.offerObliged(this, new WritablePathHolder(DocumentFile.fromUri(getContext(), pathObject.path, true),
                                pathObject,
                                getContext().getString(R.string.text_storage)));
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }

                lister.offerObliged(this, new WritablePathHolder(GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON,
                        R.drawable.ic_folder_network_white_24dp, getContext().getString(R.string.butn_mountDirectory), REQUEST_CODE_MOUNT_FOLDER));
            }

            {
                // Testing whether the files checked as completed exist takes too much time
                // causing long load times when those files are deleted. To overcome this,
                // we we will skip those transfer groups that provided 2 files that are not existing.
                List<TransferGroup> transferGroups = AppUtils.getDatabase(getContext())
                        .castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP)
                                .setOrderBy(String.format("%s DESC", AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED)), TransferGroup.class);
                List<DocumentFile> pickedRecentFiles = new ArrayList<>();

                try {
                    for (TransferGroup group : transferGroups) {
                        int errorLimit = 3;

                        List<TransferObject> recentFiles = AppUtils.getDatabase(getContext())
                                .castQuery(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                                        .setWhere(String.format("%s = ? AND %s = ? AND %s = ?", AccessDatabase.FIELD_TRANSFER_FLAG,
                                                AccessDatabase.FIELD_TRANSFER_TYPE, AccessDatabase.FIELD_TRANSFER_GROUPID),
                                                TransferObject.Flag.DONE.toString(),
                                                TransferObject.Type.INCOMING.toString(),
                                                String.valueOf(group.groupId))
                                        .setOrderBy(String.format("`%s` DESC, `%s` DESC",
                                                AccessDatabase.FIELD_TRANSFER_DIRECTORY,
                                                AccessDatabase.FIELD_TRANSFER_NAME)), TransferObject.class);

                        for (TransferObject recentFile : recentFiles) {
                            if (pickedRecentFiles.size() >= 20)
                                throw new Exception("Reached the threshold for picking recent files");

                            if (errorLimit == 0)
                                break;

                            try {
                                DocumentFile documentFile = FileUtils.getIncomingPseudoFile(getContext(), recentFile,
                                        group, false);

                                if (documentFile.exists() && !pickedRecentFiles.contains(documentFile))
                                    pickedRecentFiles.add(documentFile);
                                else
                                    errorLimit--;
                            } catch (IOException e) {
                                errorLimit--;
                            }
                        }
                    }
                } catch (Exception e) {
                    // This is a more proper way to break the loop.
                } finally {
                    for (DocumentFile documentFile : pickedRecentFiles)
                        lister.offerObliged(this, new RecentFileHolder(getContext(), documentFile));
                }
            }
        }
    }

    @Override
    protected GenericFileHolder onGenerateRepresentative(String representativeText)
    {
        return new GenericFileHolder(representativeText);
    }

    @Override
    public boolean onCustomGroupListing(GroupLister<GenericFileHolder> lister, int mode, GenericFileHolder object)
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
        if (viewType == VIEW_TYPE_REPRESENTATIVE)
            return new GroupViewHolder(getInflater().inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text);
        if (viewType == VIEW_TYPE_ACTION_BUTTON)
            return new GroupViewHolder(getInflater().inflate(R.layout.layout_list_action_button, parent, false), R.id.text);

        return new GroupViewHolder(getInflater().inflate(R.layout.list_file, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupViewHolder holder, final int position)
    {
        try {
            final GenericFileHolder object = getItem(position);

            if (!holder.tryBinding(object)) {
                final View parentView = holder.getView();

                ImageView thumbnail = parentView.findViewById(R.id.thumbnail);
                ImageView image = parentView.findViewById(R.id.image);
                TextView text1 = parentView.findViewById(R.id.text);
                TextView text2 = parentView.findViewById(R.id.text2);

                holder.getView().setSelected(object.isSelectableSelected());

                text1.setText(object.friendlyName);
                text2.setText(object.info);

                if (!mShowThumbnails || !object.loadThumbnail(thumbnail)) {
                    image.setImageResource(object.iconRes);
                    thumbnail.setImageDrawable(null);
                } else
                    image.setImageDrawable(null);
            } else if (holder.getItemViewType() == GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON)
                ((ImageView) holder.getView().findViewById(R.id.icon)).setImageResource(object.iconRes);
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

    public GroupLister<GenericFileHolder> createLister(List<GenericFileHolder> loadedList, int groupBy)
    {
        return super.createLister(loadedList, groupBy)
                .setCustomLister(this);
    }

    @Override
    public int getSortingCriteria(GenericFileHolder objectOne, GenericFileHolder objectTwo)
    {
        // Checking the path is null will help keep the performance high when we are not showing 'Home'.
        if (getPath() == null && objectOne instanceof RecentFileHolder
                && objectTwo instanceof RecentFileHolder)
            return MODE_SORT_BY_DATE;

        return super.getSortingCriteria(objectOne, objectTwo);
    }

    @Override
    public int getSortingOrder(GenericFileHolder objectOne, GenericFileHolder objectTwo)
    {
        // Checking the path is null will help keep the performance high when we are not showing 'Home'.
        if (getPath() == null && objectOne instanceof RecentFileHolder
                && objectTwo instanceof RecentFileHolder)
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
    public String getRepresentativeText(Merger merger)
    {
        if (merger instanceof FileHolderMerger) {
            switch (((FileHolderMerger) merger).getType()) {
                case STORAGE:
                    return getContext().getString(R.string.text_storage);
                case PUBLIC_FOLDER:
                    return getContext().getString(R.string.text_shortcuts);
                case FOLDER:
                    return getContext().getString(R.string.text_folder);
                case RECENT_FILE:
                    return getContext().getString(R.string.text_recentFiles);
                case FILE_PART:
                    return getContext().getString(R.string.text_pendingTransfers);
                default:
                    return getContext().getString(R.string.text_file);
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

    public interface StorageHolderImpl
    {
    }

    public static class GenericFileHolder extends GroupEditableListAdapter.GroupShareable
    {
        public DocumentFile file;
        public String info;
        public int iconRes;
        public int requestCode;

        public GenericFileHolder(String representativeText)
        {
            this(FileListAdapter.VIEW_TYPE_REPRESENTATIVE, representativeText);
        }

        public GenericFileHolder(int viewType, String representativeText)
        {
            super(viewType, representativeText);
        }

        public GenericFileHolder(DocumentFile file, String friendlyName, String info, int iconRes,
                                 long date, long size, Uri uri)
        {
            // 'id' will be generated in getId() method
            super(0, friendlyName, friendlyName, file.getType(), date, size, uri);

            this.file = file;
            this.info = info;
            this.iconRes = iconRes;
        }

        @Override
        public long getId()
        {
            if (super.getId() == 0)
                setId(String.format("%s_%s", file.getUri().toString(),
                        getClass().getName()).hashCode());

            return super.getId();
        }

        @Override
        public int getRequestCode()
        {
            return requestCode;
        }

        public boolean loadThumbnail(ImageView imageView)
        {
            return false;
        }
    }

    public static class FileHolder extends GenericFileHolder
    {
        public FileHolder(Context context, DocumentFile file)
        {
            super(file,
                    file.getName(),
                    FileUtils.sizeExpression(file.length(), false),
                    MimeIconUtils.loadMimeIcon(file.getType()),
                    file.lastModified(),
                    file.length(),
                    FileUtils.getSecureUriSilently(context, file));
        }

        @Override
        public boolean loadThumbnail(ImageView imageView)
        {
            String type = file.getType();

            if (type != null) {
                String[] format = type.split(File.separator);

                if (format.length > 0)
                    if ("image".equals(format[0]) || "video".equals(format[0])) {
                        GlideApp.with(imageView.getContext())
                                .load(file.getUri())
                                .error(iconRes)
                                .override(160)
                                .centerCrop()
                                .into(imageView);

                        return true;
                    }
            }

            return super.loadThumbnail(imageView);
        }
    }

    public static class ReceivedFileHolder extends FileHolder
    {
        public ReceivedFileHolder(Context context, DocumentFile file, TransferObject transferObject)
        {
            super(context, file);

            this.info = transferObject == null
                    ? context.getString(R.string.mesg_notValidTransfer)
                    : String.format("%s / %s", FileUtils.sizeExpression(getComparableSize(), false),
                    FileUtils.sizeExpression(transferObject.fileSize, false));

            this.iconRes = transferObject == null
                    ? R.drawable.ic_block_white_24dp
                    : MimeIconUtils.loadMimeIcon(transferObject.fileMimeType);

            if (transferObject != null)
                this.friendlyName = transferObject.friendlyName;
        }
    }

    public static class DirectoryHolder extends GenericFileHolder
    {
        public DirectoryHolder(DocumentFile file, String info, int iconRes)
        {
            this(file, file.getName(), info, iconRes);
        }

        public DirectoryHolder(DocumentFile file, String friendlyName, String info, int iconRes)
        {
            super(file, friendlyName, info, iconRes, file.lastModified(), 0, file.getUri());
        }

        @Override
        public long getId()
        {
            return super.getId();
        }
    }

    public static class ShortcutDirectoryHolder extends DirectoryHolder
    {
        private FileShortcutObject mShortcutObject;

        public ShortcutDirectoryHolder(Context context, FileShortcutObject shortcutObject) throws FileNotFoundException
        {
            super(FileUtils.fromUri(context, shortcutObject.path), shortcutObject.title,
                    context.getString(R.string.text_shortcut), R.drawable.ic_bookmark_white_24dp);

            mShortcutObject = shortcutObject;
        }

        public FileShortcutObject getShortcutObject()
        {
            return mShortcutObject;
        }
    }

    public static class StorageHolder
            extends DirectoryHolder
            implements StorageHolderImpl
    {
        public StorageHolder(DocumentFile file, String info, int iconRes)
        {
            super(file, file.getName(), info, iconRes);
        }

        public StorageHolder(DocumentFile file, String friendlyName, String info, int iconRes)
        {
            super(file, friendlyName, info, iconRes);
        }

        // Don't let these folders be selected
        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            return false;
        }
    }

    public static class WritablePathHolder
            extends GenericFileHolder
            implements StorageHolderImpl
    {
        public WritablePathObject pathObject;

        public WritablePathHolder(int viewType, @DrawableRes int iconRes, String representativeText, int requestCode)
        {
            super(viewType, representativeText);
            this.iconRes = iconRes;
            this.requestCode = requestCode;
        }

        public WritablePathHolder(DocumentFile file, WritablePathObject object, String info)
        {
            super(file, object.title, info, R.drawable.ic_save_white_24dp, 0, 0, object.path);
            this.pathObject = object;
        }

        @Override
        public boolean comparisonSupported()
        {
            return getViewType() != GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON
                    && super.comparisonSupported();
        }

        @Override
        public long getId()
        {
            return getViewType() != GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON
                    ? super.getId()
                    : String.format("%s_%s_%s", getClass().getName(), String.valueOf(iconRes), getRepresentativeText().hashCode()).hashCode();
        }

        // Don't let these folders to be selected
        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            return false;
        }
    }

    public static class FileHolderMerger extends ComparableMerger<GenericFileHolder>
    {
        private Type mType;

        public FileHolderMerger(GenericFileHolder holder)
        {
            if (holder instanceof StorageHolderImpl)
                mType = Type.STORAGE;
            else if (holder instanceof PublicDirectoryHolder)
                mType = Type.PUBLIC_FOLDER;
            else if (holder instanceof DirectoryHolder)
                mType = Type.FOLDER;
            else if (holder instanceof RecentFileHolder)
                mType = Type.RECENT_FILE;
            else if (holder instanceof ReceivedFileHolder)
                mType = Type.FILE_PART;
            else
                mType = Type.FILE;
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof FileHolderMerger
                    && ((FileHolderMerger) obj).getType().equals(getType());
        }

        public Type getType()
        {
            return mType;
        }

        @Override
        public int compareTo(@NonNull ComparableMerger<GenericFileHolder> o)
        {
            if (o instanceof FileHolderMerger)
                return MathUtils.compare(((FileHolderMerger) o).getType().ordinal(), getType().ordinal());

            return 1;
        }

        public enum Type
        {
            STORAGE,
            FOLDER,
            PUBLIC_FOLDER,
            RECENT_FILE,
            FILE_PART,
            FILE
        }
    }

    public class RecentFileHolder extends FileHolder
    {
        public RecentFileHolder(Context context, DocumentFile file)
        {
            super(context, file);
        }
    }

    public class PublicDirectoryHolder extends DirectoryHolder
    {
        public PublicDirectoryHolder(File file, String info, int iconRes)
        {
            this(DocumentFile.fromFile(file), info, iconRes);

            String[] files = file.list();
            int fileCount = files != null ? files.length : 0;

            this.friendlyName = info;
            this.info = getContext().getResources().getQuantityString(R.plurals.text_files, fileCount, fileCount);
        }

        public PublicDirectoryHolder(DocumentFile file, String info, int iconRes)
        {
            super(file, info, iconRes);
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            return false;
        }
    }
}
