package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.WritablePathObject;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.util.MathUtils;
import com.genonbeta.android.framework.util.listing.ComparableMerger;
import com.genonbeta.android.framework.util.listing.Merger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;

public class FileListAdapter
		extends GroupEditableListAdapter<FileListAdapter.GenericFileHolder, GroupEditableListAdapter.GroupViewHolder>
		implements GroupEditableListAdapter.GroupLister.CustomGroupListener<FileListAdapter.GenericFileHolder>
{
	public static final int MODE_GROUP_BY_DEFAULT = MODE_GROUP_BY_NOTHING + 1;

	private boolean mShowDirectories = true;
	private boolean mShowFiles = true;
	private String mFileMatch;
	private DocumentFile mPath;
	private AccessDatabase mDatabase;
	private SharedPreferences mPreferences;

	public FileListAdapter(Context context, AccessDatabase database, SharedPreferences sharedPreferences)
	{
		super(context, MODE_GROUP_BY_DEFAULT);

		mDatabase = database;
		mPreferences = sharedPreferences;
	}

	@Override
	protected void onLoad(GroupLister<GenericFileHolder> lister)
	{
		DocumentFile path = getPath();

		if (path != null) {
			DocumentFile[] fileIndex = path.listFiles();

			if (fileIndex != null && fileIndex.length > 0) {
				for (DocumentFile file : fileIndex) {
					if ((mFileMatch != null && !file.getName().matches(mFileMatch)))
						continue;

					if (file.isDirectory() && mShowDirectories)
						lister.offer(new DirectoryHolder(file, mContext.getString(R.string.text_folder), R.drawable.ic_folder_white_24dp));
					else if (file.isFile() && mShowFiles)
						lister.offer(new FileHolder(getContext(), file));
				}
			}
		} else {
			ArrayList<File> referencedDirectoryList = new ArrayList<>();
			DocumentFile defaultFolder = FileUtils.getApplicationDirectory(getContext(), mPreferences);

			lister.offer(new DirectoryHolder(defaultFolder, getContext().getString(R.string.text_receivedFiles), R.drawable.ic_whatshot_white_24dp));
			lister.offer(new DirectoryHolder(DocumentFile.fromFile(new File(".")),
					mContext.getString(R.string.text_fileRoot),
					mContext.getString(R.string.text_folder),
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

				lister.offer(fileHolder);
			}

			ArrayList<WritablePathObject> objectList = mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_WRITABLEPATH), WritablePathObject.class);

			if (Build.VERSION.SDK_INT >= 21) {
				for (WritablePathObject pathObject : objectList)
					try {
						lister.offer(new WritablePathHolder(DocumentFile.fromUri(getContext(), pathObject.path, true),
								pathObject,
								getContext().getString(R.string.text_storage)));
					} catch (FileNotFoundException e) {
						e.printStackTrace();
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

		return new GroupViewHolder(getInflater().inflate(R.layout.list_file, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull final GroupViewHolder holder, final int position)
	{
		try {
			final GenericFileHolder object = getItem(position);

			if (!holder.tryBinding(object)) {
				final View parentView = holder.getView();

				ImageView image = parentView.findViewById(R.id.image);
				TextView text1 = parentView.findViewById(R.id.text);
				TextView text2 = parentView.findViewById(R.id.text2);

				holder.getView().setSelected(object.isSelectableSelected());

				text1.setText(object.friendlyName);
				text2.setText(object.info);
				image.setImageResource(object.iconRes);
			}
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

	public GroupLister<GenericFileHolder> createLister(ArrayList<GenericFileHolder> loadedList, int groupBy)
	{
		return super.createLister(loadedList, groupBy)
				.setCustomLister(this);
	}

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
				case FOLDER:
					return getContext().getString(R.string.text_folder);
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

	public static class GenericFileHolder extends GroupEditableListAdapter.GroupShareable
	{
		public DocumentFile file;
		public String info;
		public int iconRes;

		public GenericFileHolder(String representativeText)
		{
			super(FileListAdapter.VIEW_TYPE_REPRESENTATIVE, representativeText);
		}

		public GenericFileHolder(DocumentFile file, String friendlyName, String info, int iconRes, long date, long size, Uri uri)
		{
			super(file.getUri().toString().hashCode(), friendlyName, friendlyName, file.getType(), date, size, uri);

			this.file = file;
			this.info = info;
			this.iconRes = iconRes;
		}
	}

	public static class FileHolder extends GenericFileHolder
	{
		public FileHolder(Context context, DocumentFile file)
		{
			super(file,
					file.getName(),
					FileUtils.sizeExpression(file.length(), false),
					R.drawable.ic_insert_drive_file_white_24dp,
					file.lastModified(),
					file.length(),
					FileUtils.getSecureUriSilently(context, file));
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

		// Don't let these folders to be selected
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

		public WritablePathHolder(DocumentFile file, WritablePathObject object, String info)
		{
			super(file, file.getName() == null ? object.title : file.getName(), info, R.drawable.ic_save_white_24dp, 0, 0, object.path);

			this.pathObject = object;
		}

		// Don't let these folders to be selected
		@Override
		public boolean setSelectableSelected(boolean selected)
		{
			return false;
		}
	}

	public interface StorageHolderImpl
	{
	}

	public static class FileHolderMerger extends ComparableMerger<GenericFileHolder>
	{
		private Type mType;

		public FileHolderMerger(GenericFileHolder holder)
		{
			if (holder instanceof StorageHolderImpl)
				mType = Type.STORAGE;
			else if (holder instanceof DirectoryHolder)
				mType = Type.FOLDER;
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
			FILE
		}
	}
}
