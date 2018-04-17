package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.MathUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.listing.ComparableMerger;
import com.genonbeta.TrebleShot.util.listing.Merger;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */

public class TransactionListAdapter
		extends GroupEditableListAdapter<TransactionListAdapter.GroupEditableTransferObject, GroupEditableListAdapter.GroupViewHolder>
		implements GroupEditableListAdapter.GroupLister.CustomGroupListener<TransactionListAdapter.GroupEditableTransferObject>
{
	public static final int MODE_SORT_BY_DEFAULT = MODE_SORT_BY_NAME - 1;
	public static final int MODE_GROUP_BY_DEFAULT = MODE_GROUP_BY_NOTHING + 1;

	private AccessDatabase mDatabase;
	private SQLQuery.Select mSelect;
	private String mPath;
	private int mGroupId;
	private PathChangedListener mListener;

	public TransactionListAdapter(Context context, AccessDatabase database)
	{
		super(context, MODE_GROUP_BY_DEFAULT);

		mDatabase = database;

		setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER));
	}

	@Override
	protected void onLoad(GroupLister<GroupEditableTransferObject> lister)
	{
		ArrayList<GroupEditableTransferObject> mergedList = new ArrayList<>();
		String currentPath = getPath();

		for (GroupEditableTransferObject transferObject : mDatabase.castQuery(getSelect()
				.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + " LIKE ?",
						String.valueOf(getGroupId()),
						currentPath != null ? currentPath + File.separator + "%" : "%")
				.setGroupBy(AccessDatabase.FIELD_TRANSFER_DIRECTORY), GroupEditableTransferObject.class)) {
			String cleanedName = currentPath != null ? transferObject.directory.substring(currentPath.length() + File.separator.length()) : transferObject.directory;
			int obtainSlash = cleanedName.indexOf(File.separator);

			if (obtainSlash != -1)
				cleanedName = cleanedName.substring(0, obtainSlash);

			TransferFolder transactionFolder = new TransferFolder();

			transactionFolder.friendlyName = cleanedName;
			transactionFolder.directory = currentPath != null ? currentPath + File.separator + cleanedName : cleanedName;

			boolean addThis = true;

			for (TransferObject testObject : mergedList) {
				if (!(testObject instanceof TransferFolder))
					continue;

				TransferFolder testFolder = (TransferFolder) testObject;

				if (testFolder.friendlyName.equals(transactionFolder.friendlyName)) {
					addThis = false;
					break;
				}
			}

			if (addThis)
				mergedList.add(transactionFolder);
		}

		for (GroupEditableTransferObject object : mergedList)
			lister.offer(object);

		for (GroupEditableTransferObject object : mDatabase.castQuery((currentPath == null
				? getSelect().setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + " IS NULL", String.valueOf(getGroupId()))
				: getSelect().setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + "=?",
				String.valueOf(getGroupId()), currentPath)
		).setGroupBy(null), GroupEditableTransferObject.class))
			lister.offer(object);
	}

	@Override
	protected GroupEditableTransferObject onGenerateRepresentative(String representativeText)
	{
		return new GroupEditableTransferObject(representativeText);
	}

	@Override
	public boolean onCustomGroupListing(GroupLister<GroupEditableTransferObject> lister, int mode, GroupEditableTransferObject object)
	{
		if (mode == MODE_GROUP_BY_DEFAULT)
			lister.offer(object, new GroupEditableTransferObjectMerger(object));
		else
			return false;

		return true;
	}

	@Override
	public int compareItems(int sortingCriteria, int sortingOrder, GroupEditableTransferObject objectOne, GroupEditableTransferObject objectTwo)
	{
		if (sortingCriteria == MODE_SORT_BY_DEFAULT)
			return MathUtils.compare(objectTwo.requestId, objectOne.requestId);

		return 1;
	}

	public GroupLister<GroupEditableTransferObject> createLister(ArrayList<GroupEditableTransferObject> loadedList, int groupBy)
	{
		return super.createLister(loadedList, groupBy)
				.setCustomLister(this);
	}

	public int getGroupId()
	{
		return mGroupId;
	}

	public String getPath()
	{
		return mPath;
	}

	@Override
	public String getRepresentativeText(Merger merger)
	{
		if (merger instanceof GroupEditableTransferObjectMerger) {
			switch (((GroupEditableTransferObjectMerger) merger).getType()) {
				case FOLDER:
					return getContext().getString(R.string.text_folder);
				default:
					return getContext().getString(R.string.text_file);
			}
		}

		return super.getRepresentativeText(merger);
	}

	public SQLQuery.Select getSelect()
	{
		return mSelect;
	}

	public TransactionListAdapter setSelect(SQLQuery.Select select)
	{
		if (select != null)
			mSelect = select;

		return this;
	}

	public void setGroupId(int groupId)
	{
		mGroupId = groupId;
	}

	public void setPath(String path)
	{
		mPath = path;

		if (mListener != null)
			mListener.onPathChange(path);
	}

	public void setPathChangedListener(PathChangedListener listener)
	{
		mListener = listener;
	}

	@NonNull
	@Override
	public GroupEditableListAdapter.GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		if (viewType == VIEW_TYPE_REPRESENTATIVE)
			return new GroupViewHolder(getInflater().inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text);

		return new GroupEditableListAdapter.GroupViewHolder(getInflater().inflate(R.layout.list_transaction, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull final GroupEditableListAdapter.GroupViewHolder holder, int position)
	{
		final GroupEditableTransferObject object = getItem(position);

		if (!holder.tryBinding(object)) {
			final View parentView = holder.getView();

			ImageView image = parentView.findViewById(R.id.image);
			TextView mainText = parentView.findViewById(R.id.text);
			TextView statusText = parentView.findViewById(R.id.text2);
			TextView sizeText = parentView.findViewById(R.id.text3);

			parentView.setSelected(object.isSelectableSelected());

			if (object instanceof TransferFolder) {
				image.setImageResource(R.drawable.ic_folder_black_24dp);
				mainText.setText(object.friendlyName);
				statusText.setText(R.string.text_folder);
				sizeText.setText(null);
			} else {
				boolean isIncoming = object.type.equals(TransferObject.Type.INCOMING);

				image.setImageResource(isIncoming ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_upload_black_24dp);
				mainText.setText(object.friendlyName);
				statusText.setText(getContext().getString(TextUtils.getTransactionFlagString(object.flag)).toLowerCase());
				sizeText.setText(FileUtils.sizeExpression(object.fileSize, false));
			}
		}
	}

	public interface PathChangedListener
	{
		void onPathChange(String path);
	}

	public static class GroupEditableTransferObject
			extends TransferObject
			implements GroupEditableListAdapter.GroupEditable
	{
		public int viewType;
		public String representativeText;

		public GroupEditableTransferObject()
		{

		}

		public GroupEditableTransferObject(String representativeText)
		{
			this.viewType = VIEW_TYPE_REPRESENTATIVE;
			setRepresentativeText(representativeText);
		}

		@Override
		public int getViewType()
		{
			return this.viewType;
		}

		@Override
		public String getRepresentativeText()
		{
			return this.representativeText;
		}

		@Override
		public boolean isGroupRepresentative()
		{
			return this.viewType == VIEW_TYPE_REPRESENTATIVE;
		}

		@Override
		public void setDate(long date)
		{
			// stamp
		}

		@Override
		public void setSize(long size)
		{
			this.fileSize = size;
		}

		@Override
		public void setRepresentativeText(CharSequence representativeText)
		{
			this.representativeText = String.valueOf(representativeText);
		}
	}

	public static class TransferFolder extends GroupEditableTransferObject
	{
		public TransferFolder()
		{
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof TransferFolder
					&& directory != null
					&& directory.equals(((TransferFolder) obj).directory);
		}
	}

	public static class GroupEditableTransferObjectMerger extends ComparableMerger<GroupEditableTransferObject>
	{
		private Type mType;

		public GroupEditableTransferObjectMerger(GroupEditableTransferObject holder)
		{
			if (holder instanceof TransferFolder)
				mType = Type.FOLDER;
			else
				mType = Type.FILE;
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof GroupEditableTransferObjectMerger
					&& ((GroupEditableTransferObjectMerger) obj).getType().equals(getType());
		}

		public Type getType()
		{
			return mType;
		}

		@Override
		public int compareTo(@NonNull ComparableMerger<GroupEditableTransferObject> o)
		{
			if (o instanceof GroupEditableTransferObjectMerger)
				return MathUtils.compare(((GroupEditableTransferObjectMerger) o).getType().ordinal(), getType().ordinal());

			return 1;
		}

		public enum Type
		{
			FOLDER,
			FILE
		}
	}
}
