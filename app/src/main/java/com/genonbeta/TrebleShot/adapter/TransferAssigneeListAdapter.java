package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.ArrayList;

/**
 * created by: veli
 * date: 06.04.2018 12:46
 */
public class TransferAssigneeListAdapter extends RecyclerViewAdapter<ShowingAssignee, RecyclerViewAdapter.ViewHolder>
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
        return new ViewHolder(getInflater().inflate(isHorizontalOrientation()
                ? R.layout.list_assignee_grid
                : R.layout.list_assignee, parent, false));
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
        NetworkDeviceLoader.showPictureIntoView(assignee.device, image, mIconBuilder);
    }

    @Override
    public ArrayList<ShowingAssignee> onLoad()
    {
        return TransferUtils.loadAssigneeList(mDatabase, mGroup.groupId);
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
}
