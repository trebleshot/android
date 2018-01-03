package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.widget.ShareableListAdapter;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */

public class TransactionGroupListAdapter extends ShareableListAdapter<TransactionGroupListAdapter.PreloadedGroup>
{
	public static final String FIELD_TRANSACTIONCOUNT = "transactionCount";
	public static final String FIELD_DEVICENAME = "deviceName";

	private AccessDatabase mDatabase;
	private ArrayList<TransactionGroupListAdapter.PreloadedGroup> mList = new ArrayList<>();
	private SQLQuery.Select mSelect;

	public TransactionGroupListAdapter(Context context)
	{
		super(context);

		mDatabase = new AccessDatabase(context);

		setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP));
	}

	@Override
	public ArrayList<PreloadedGroup> onLoad()
	{
		return mDatabase.castQuery(getSelect()
				.setOrderBy(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED + " DESC")
				.setLoadListener(new SQLQuery.Select.LoadListener()
				{
					@Override
					public void onOpen(SQLiteDatabase db, Cursor cursor)
					{

					}

					@Override
					public void onLoad(SQLiteDatabase db, Cursor cursor, CursorItem item)
					{
						int groupId = item.getInt(AccessDatabase.FIELD_TRANSFERGROUP_ID);
						String deviceId = item.getString(AccessDatabase.FIELD_TRANSFERGROUP_DEVICEID);

						NetworkDevice device = new NetworkDevice(deviceId);

						try {
							mDatabase.reconstruct(device);
							item.put(FIELD_DEVICENAME, device.nickname);
						} catch (Exception e) {
							item.put(FIELD_DEVICENAME, "-");
						}

						item.put(FIELD_TRANSACTIONCOUNT, db.getTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER, AccessDatabase.FIELD_TRANSFER_TYPE)
								.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId))).size());
					}
				}), PreloadedGroup.class);
	}

	@Override
	public void onUpdate(ArrayList<PreloadedGroup> passedItem)
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
	public Object getItem(int i)
	{
		return mList.get(i);
	}

	@Override
	public long getItemId(int i)
	{
		return 0;
	}

	public ArrayList<PreloadedGroup> getList()
	{
		return mList;
	}

	public SQLQuery.Select getSelect()
	{
		return mSelect;
	}

	@Override
	public View getView(int i, View view, ViewGroup viewGroup)
	{
		if (view == null)
			view = getInflater().inflate(R.layout.list_transaction_group, viewGroup, false);

		final PreloadedGroup group = (PreloadedGroup) getItem(i);

		ImageView userImage = view.findViewById(R.id.list_transaction_group_image_device);
		TextView titleText = view.findViewById(R.id.list_transaction_group_text_title);
		TextView text1 = view.findViewById(R.id.list_transaction_group_text_text1);
		TextView text2 = view.findViewById(R.id.list_transaction_group_text_text2);

		String firstLetters = TextUtils.getFirstLetters(group.deviceName, 1);

		TextDrawable drawable = TextDrawable.builder().buildRoundRect(firstLetters.length() > 0 ? firstLetters : "?", ContextCompat.getColor(mContext, R.color.networkDeviceRipple), 100);

		titleText.setText(group.deviceName);
		text1.setText(getContext().getResources().getQuantityString(R.plurals.text_files, group.transactionCount, group.transactionCount));
		text2.setText(TimeUtils.getTimeAgo(getContext(), group.dateCreated));
		userImage.setImageDrawable(drawable);

		return view;
	}

	public TransactionGroupListAdapter setSelect(SQLQuery.Select select)
	{
		if (select != null)
			mSelect = select;

		return this;
	}

	public static class PreloadedGroup extends TransactionObject.Group
	{
		public int transactionCount;
		public String deviceName;

		@Override
		public void reconstruct(CursorItem item)
		{
			super.reconstruct(item);

			transactionCount = item.getInt(FIELD_TRANSACTIONCOUNT);
			deviceName = item.getString(FIELD_DEVICENAME);
		}
	}
}
