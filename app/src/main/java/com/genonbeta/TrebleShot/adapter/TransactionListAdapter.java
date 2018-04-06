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
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */

public class TransactionListAdapter
		extends EditableListAdapter<TransferObject, EditableListAdapter.EditableViewHolder>
{
	public static final int MODE_SORT_BY_DEFAULT = MODE_SORT_BY_NAME - 1;

	private AccessDatabase mDatabase;
	private SQLQuery.Select mSelect;
	private String mPath;
	private int mGroupId;
	private PathChangedListener mListener;

	public TransactionListAdapter(Context context, AccessDatabase database)
	{
		super(context);

		mDatabase = database;

		setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER));
	}

	@Override
	public ArrayList<TransferObject> onLoad()
	{
		ArrayList<TransferObject> mergedList = new ArrayList<>();
		String currentPath = getPath();

		for (TransferObject transferObject : mDatabase.castQuery(getSelect()
				.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + " LIKE ?",
						String.valueOf(getGroupId()),
						currentPath != null ? currentPath + File.separator + "%" : "%")
				.setGroupBy(AccessDatabase.FIELD_TRANSFER_DIRECTORY), TransferObject.class)) {
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

		ArrayList<TransferObject> mainItems = mDatabase.castQuery((currentPath == null
				? getSelect().setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + " IS NULL", String.valueOf(getGroupId()))
				: getSelect().setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + "=?",
				String.valueOf(getGroupId()), currentPath)
		).setGroupBy(null), TransferObject.class);

		Collections.sort(mainItems, getDefaultComparator());

		mergedList.addAll(mainItems);

		return mergedList;
	}

	@Override
	public int compareItems(int sortingCriteria, int sortingOrder, TransferObject objectOne, TransferObject objectTwo)
	{
		if (sortingCriteria == MODE_SORT_BY_DEFAULT)
			return MathUtils.compare(objectTwo.requestId, objectOne.requestId);

		return 1;
	}

	public int getGroupId()
	{
		return mGroupId;
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

	public String getPath()
	{
		return mPath;
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
	public EditableListAdapter.EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return new EditableListAdapter.EditableViewHolder(getInflater().inflate(R.layout.list_transaction, parent, false))
				.setSelectionOrientedLayout(R.id.layout_image, getSelectionConnection())
				.setClickableLayout(getSelectionConnection());
	}

	@Override
	public void onBindViewHolder(@NonNull final EditableListAdapter.EditableViewHolder holder, int position)
	{
		final TransferObject object = getItem(position);
		final View parentView = holder.getView();

		ImageView image = parentView.findViewById(R.id.image);
		TextView mainText = parentView.findViewById(R.id.text);
		TextView statusText = parentView.findViewById(R.id.text2);
		TextView sizeText = parentView.findViewById(R.id.text3);

		if (getSelectionConnection() != null)
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

	public interface PathChangedListener
	{
		void onPathChange(String path);
	}

	public static class TransferFolder extends TransferObject
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
}
