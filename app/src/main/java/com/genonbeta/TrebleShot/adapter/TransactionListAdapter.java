package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransactionObject;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */

public class TransactionListAdapter extends AbstractEditableListAdapter<TransactionObject>
{
	private AccessDatabase mDatabase;
	private ArrayList<TransactionObject> mList = new ArrayList<>();
	private SQLQuery.Select mSelect;

	private String mPath;
	private int mGroupId;
	private PathChangedListener mListener;

	public TransactionListAdapter(Context context)
	{
		super(context);
		initialize(context);
		setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER));
	}

	@Override
	public ArrayList<TransactionObject> onLoad()
	{
		ArrayList<TransactionObject> mergedList = new ArrayList<>();

		for (TransactionObject transactionObject : mDatabase.castQuery(getSelect()
				.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + " LIKE ?",
						String.valueOf(getGroupId()),
						getPath() != null ? getPath() + File.separator + "%" : "%")
				.setGroupBy(AccessDatabase.FIELD_TRANSFER_DIRECTORY), TransactionObject.class)) {
			String cleanedName = getPath() != null ? transactionObject.directory.substring(getPath().length() + File.separator.length()) : transactionObject.directory;
			int obtainSlash = cleanedName.indexOf(File.separator);

			if (obtainSlash != -1)
				cleanedName = cleanedName.substring(0, obtainSlash);

			TransactionFolder transactionFolder = new TransactionFolder();

			transactionFolder.friendlyName = cleanedName;
			transactionFolder.directory = getPath() != null ? getPath() + File.separator + cleanedName : cleanedName;

			boolean addThis = true;

			for (TransactionObject testObject : mergedList) {
				if (!(testObject instanceof TransactionFolder))
					continue;

				TransactionFolder testFolder = (TransactionFolder) testObject;

				if (testFolder.friendlyName.equals(transactionFolder.friendlyName)) {
					addThis = false;
					break;
				}
			}

			if (addThis)
				mergedList.add(transactionFolder);
		}

		mergedList.addAll(mDatabase.castQuery((getPath() == null
				? getSelect().setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + " IS NULL", String.valueOf(getGroupId()))
				: getSelect().setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_DIRECTORY + "=?",
						String.valueOf(getGroupId()), getPath())).setGroupBy(null), TransactionObject.class));

		return mergedList;
	}

	@Override
	public void onUpdate(ArrayList<TransactionObject> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	private void initialize(Context context)
	{
		mDatabase = new AccessDatabase(context);
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	public int getGroupId()
	{
		return mGroupId;
	}

	@Override
	public Object getItem(int i)
	{
		return mList.get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return 0;
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

	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		if (view == null)
			view = getInflater().inflate(R.layout.list_transaction, viewGroup, false);

		final TransactionObject transactionObject = (TransactionObject) getItem(i);

		ImageView typeImage = (ImageView) view.findViewById(R.id.list_process_type_image);
		TextView mainText = (TextView) view.findViewById(R.id.list_process_name_text);
		TextView statusText = (TextView) view.findViewById(R.id.list_process_status_text);

		if (transactionObject instanceof TransactionFolder) {
			typeImage.setImageResource(R.drawable.ic_folder_black_24dp);
			mainText.setText(transactionObject.friendlyName);
			statusText.setText(R.string.text_folder);
		} else {
			final boolean isIncoming = transactionObject.type.equals(TransactionObject.Type.INCOMING);

			typeImage.setImageResource(isIncoming ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_upload_black_24dp);
			mainText.setText(transactionObject.friendlyName);
			statusText.setText(getContext().getString(TextUtils.getTransactionFlagString(transactionObject.flag)).toLowerCase());
		}

		return view;
	}

	public void setGroupId(int groupId)
	{
		this.mGroupId = groupId;
	}

	public void setPath(String path)
	{
		this.mPath = path;

		if (mListener != null)
			mListener.onPathChange(getPath());
	}

	public void setPathChangedListener(PathChangedListener listener)
	{
		this.mListener = listener;
	}

	public interface PathChangedListener
	{
		public void onPathChange(String path);
	}

	public static class TransactionFolder extends TransactionObject
	{
		public TransactionFolder()
		{
		}
	}
}
