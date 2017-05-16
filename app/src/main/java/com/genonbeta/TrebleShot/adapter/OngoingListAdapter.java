package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.FileUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 4/15/17 12:29 PM
 */

public class OngoingListAdapter extends AbstractEditableListAdapter
{
	public static final String FIELD_TRANSFER_TOTAL_COUNT = "pseudoTotalCount";

	private Transaction mTransaction;
	private ArrayList<CursorItem> mList = new ArrayList<>();
	private SQLQuery.Select mSelect;

	public OngoingListAdapter(Context context)
	{
		super(context);
		mTransaction = new Transaction(context);
		mSelect = new SQLQuery.Select(Transaction.TABLE_TRANSFER)
				.setOrderBy(Transaction.FIELD_TRANSFER_ID + " DESC")
				.setGroupBy(MainDatabase.FIELD_TRANSFER_ACCEPTID)
				.setLoadListener(new SQLQuery.Select.LoadListener()
				{
					@Override
					public void onOpen(SQLiteDatabase db, Cursor cursor)
					{

					}

					@Override
					public void onLoad(SQLiteDatabase db, Cursor cursor, CursorItem item)
					{
						ArrayList<CursorItem> itemList = mTransaction.getTable(new SQLQuery.Select(Transaction.TABLE_TRANSFER)
								.setWhere(Transaction.FIELD_TRANSFER_ACCEPTID + "=?", item.getString(Transaction.FIELD_TRANSFER_ACCEPTID)));

						item.putAll(itemList.get(0)); // First item will be loaded first so better show it

						item.put(FIELD_TRANSFER_TOTAL_COUNT, itemList.size());
					}
				});
	}

	public OngoingListAdapter(Context context, int acceptId)
	{
		this(context);
		mSelect = new SQLQuery.Select(Transaction.TABLE_TRANSFER)
				.setWhere(Transaction.FIELD_TRANSFER_ACCEPTID + "=?", String.valueOf(acceptId));
	}

	public OngoingListAdapter(Context context, String ipAddress)
	{
		super(context);
		mSelect = new SQLQuery.Select(Transaction.TABLE_TRANSFER)
				.setWhere(Transaction.FIELD_TRANSFER_USERIP + "=?", ipAddress);
	}

	@Override
	protected void onSearch(String word)
	{

	}

	@Override
	protected void onUpdate()
	{
		mList.clear();
		mList.addAll(mTransaction.getTable(mSelect));
	}

	@Override
	public int getCount()
	{
		return mList.size();
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

	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		if (view == null)
			view = getInflater().inflate(R.layout.list_ongoing, viewGroup, false);

		final CursorItem thisItem = (CursorItem) getItem(i);
		final boolean isIncoming = thisItem.getInt(MainDatabase.FIELD_TRANSFER_TYPE) == MainDatabase.TYPE_TRANSFER_TYPE_INCOMING;

		ImageView typeImage = (ImageView) view.findViewById(R.id.list_process_type_image);
		ImageView clearImage = (ImageView) view.findViewById(R.id.list_process_clear_image);
		TextView mainText = (TextView) view.findViewById(R.id.list_process_name_text);
		TextView statusText = (TextView) view.findViewById(R.id.list_process_status_text);
		TextView countText = (TextView) view.findViewById(R.id.list_process_count_text);

		typeImage.setImageResource(isIncoming ? R.drawable.ic_file_download_black_24dp : R.drawable.ic_file_upload_black_24dp);
		mainText.setText(thisItem.getString(MainDatabase.FIELD_TRANSFER_NAME));
		statusText.setText(thisItem.getString(MainDatabase.FIELD_TRANSFER_FLAG));

		if (thisItem.exists(FIELD_TRANSFER_TOTAL_COUNT))
			countText.setText((thisItem.getInt(FIELD_TRANSFER_TOTAL_COUNT) > 1) ? "+" + (thisItem.getInt(FIELD_TRANSFER_TOTAL_COUNT) - 1) : "");

		clearImage.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);

				dialog.setTitle(R.string.dialog_title_remove_queue_job);
				dialog.setMessage(thisItem.exists(FIELD_TRANSFER_TOTAL_COUNT) ?
						mContext.getString(R.string.dialog_msg_remove_queue_job, thisItem.getInt(FIELD_TRANSFER_TOTAL_COUNT)) :
						"Delete " + thisItem.getString(Transaction.FIELD_TRANSFER_NAME) + " from the queue");

				dialog.setNegativeButton(R.string.close, null);
				dialog.setPositiveButton(R.string.proceed, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						if (isIncoming)
						{
							ArrayList<AwaitedFileReceiver> receivers = mTransaction.getReceivers(new SQLQuery.Select(Transaction.TABLE_TRANSFER)
									.setWhere(Transaction.FIELD_TRANSFER_ACCEPTID + "=?", thisItem.getString(Transaction.FIELD_TRANSFER_ACCEPTID)));

							for (AwaitedFileReceiver receiver : receivers)
							{
								if (receiver.flag.equals(Transaction.Flag.PENDING))
									continue;

								File tmpFile = new File(FileUtils.getSaveLocationForFile(mContext, receiver.fileName));

								if (tmpFile.isFile())
									tmpFile.delete();
							}
						}

						mTransaction.removeTransactionGroup(thisItem.getInt(Transaction.FIELD_TRANSFER_ACCEPTID));
					}
				}).show();
			}
		});

		return view;
	}
}
