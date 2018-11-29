package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.ArrayList;

import androidx.annotation.NonNull;

/**
 * created by: veli
 * date: 06.04.2018 12:46
 */
public class TransferAssigneeListAdapter extends RecyclerViewAdapter<TransferAssigneeListAdapter.ShowingAssignee, RecyclerViewAdapter.ViewHolder>
{
    private ArrayList<ShowingAssignee> mList = new ArrayList<>();
    private TransferGroup mGroup;
    private AccessDatabase mDatabase;
    private TextDrawable.IShapeBuilder mIconBuilder;

    public TransferAssigneeListAdapter(Context context, AccessDatabase database)
    {
        super(context);
        mDatabase = database;
        mIconBuilder = AppUtils.getDefaultIconBuilder(context);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return new ViewHolder(isHorizontalOrientation()
                ? getInflater().inflate(R.layout.list_assignee_grid, parent, false)
                : getInflater().inflate(R.layout.list_assignee, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        ShowingAssignee assignee = getList().get(position);

        ImageView image = holder.getView().findViewById(R.id.image);
        TextView text1 = holder.getView().findViewById(R.id.text);
        TextView text2 = holder.getView().findViewById(R.id.text2);

        text1.setText(assignee.device.nickname);
        text2.setText(TextUtils.getAdapterName(getContext(), assignee.connection));
        image.setImageDrawable(mIconBuilder.buildRound(assignee.device.nickname));
    }

    @Override
    public ArrayList<ShowingAssignee> onLoad()
    {
        return loadAssigneeList(mDatabase, mGroup.groupId);
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

    public static ArrayList<ShowingAssignee> loadAssigneeList(SQLiteDatabase database, long groupId)
    {
        SQLQuery.Select select = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
                .setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(groupId));

        return database.castQuery(select, ShowingAssignee.class, new SQLiteDatabase.CastQueryListener<ShowingAssignee>()
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

    public static class ShowingAssignee extends TransferGroup.Assignee
    {
        public NetworkDevice device;
        public NetworkDevice.Connection connection;

        public ShowingAssignee()
        {

        }
    }
}
