package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

import java.util.ArrayList;

/**
 * created by: veli
 * date: 06.04.2018 12:46
 */
public class TransferAssigneeListAdapter extends RecyclerViewAdapter<TransferAssigneeListAdapter.ShowingAssignee, RecyclerViewAdapter.ViewHolder>
{
	private ArrayList<ShowingAssignee> mList = new ArrayList<>();
	private TransferGroup mGroup;
	private AccessDatabase mDatabase;

	public TransferAssigneeListAdapter(Context context, AccessDatabase database)
	{
		super(context);
		mDatabase = database;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return new ViewHolder(getInflater().inflate(R.layout.list_assignee, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position)
	{
		ShowingAssignee assignee = getList().get(position);

		String firstLetters = TextUtils.getLetters(assignee.device.nickname, 0);
		ImageView image = holder.getView().findViewById(R.id.image);
		TextView text1 = holder.getView().findViewById(R.id.text);
		TextView text2 = holder.getView().findViewById(R.id.text2);

		text1.setText(assignee.device.nickname);
		text2.setText(TextUtils.getAdapterName(getContext(), assignee.connection));

		image.setImageDrawable(TextDrawable.builder().buildRoundRect(firstLetters.length() > 0
				? firstLetters
				: "?", ContextCompat.getColor(mContext, R.color.networkDeviceRipple), 100));
	}

	@Override
	public ArrayList<ShowingAssignee> onLoad()
	{
		SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
				.setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(mGroup.groupId));

		return mDatabase.castQuery(select, ShowingAssignee.class, new SQLiteDatabase.CastQueryListener<ShowingAssignee>()
		{
			@Override
			public void onObjectReconstructed(SQLiteDatabase db, CursorItem item, ShowingAssignee object)
			{
				object.device = new NetworkDevice(object.deviceId);
				object.connection = new NetworkDevice.Connection(object);

				try {
					db.reconstruct(object.device);
					db.reconstruct(object.connection);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void onUpdate(ArrayList<ShowingAssignee> passedItem)
	{
		mList = passedItem;
	}

	@Override
	public int getItemCount()
	{
		return mList.size();
	}

	@Override
	public ArrayList<ShowingAssignee> getList()
	{
		return mList;
	}

	public TransferAssigneeListAdapter setGroup(TransferGroup group)
	{
		mGroup = group;
		return this;
	}

	public static class ShowingAssignee extends TransferGroup.Assignee
	{
		public NetworkDevice device;
		public NetworkDevice.Connection connection;

		public ShowingAssignee()
		{

		}
	}
}
