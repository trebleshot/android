/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.PreloadedGroup;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */

public class TransferGroupListAdapter extends GroupEditableListAdapter<PreloadedGroup,
        GroupEditableListAdapter.GroupViewHolder>
{
    final private List<Long> mRunningTasks = new ArrayList<>();

    private AccessDatabase mDatabase;
    private SQLQuery.Select mSelect;
    private NumberFormat mPercentFormat;

    @ColorInt
    private int mColorPending;
    private int mColorDone;
    private int mColorError;

    public TransferGroupListAdapter(Context context, AccessDatabase database)
    {
        super(context, MODE_GROUP_BY_DATE);

        mDatabase = database;
        mPercentFormat = NumberFormat.getPercentInstance();
        mColorPending = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorControlNormal));
        mColorDone = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorAccent));
        mColorError = ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorError));

        setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP));
    }

    @Override
    protected void onLoad(GroupLister<PreloadedGroup> lister)
    {
        List<Long> activeList = new ArrayList<>(mRunningTasks);

        for (PreloadedGroup group : mDatabase.castQuery(getSelect(), PreloadedGroup.class)) {
            TransferUtils.loadGroupInfo(getContext(), group);
            group.isRunning = activeList.contains(group.id);

            lister.offerObliged(this, group);
        }
    }

    @Override
    protected PreloadedGroup onGenerateRepresentative(String representativeText)
    {
        return new PreloadedGroup(representativeText);
    }

    public SQLQuery.Select getSelect()
    {
        return mSelect;
    }

    public TransferGroupListAdapter setSelect(SQLQuery.Select select)
    {
        if (select != null)
            mSelect = select;

        return this;
    }

    @NonNull
    @Override
    public GroupEditableListAdapter.GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return viewType == VIEW_TYPE_DEFAULT ? new GroupViewHolder(getInflater().inflate(R.layout.list_transfer_group,
                parent, false)) : createDefaultViews(parent, viewType, true);
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupEditableListAdapter.GroupViewHolder holder, int position)
    {
        try {
            final PreloadedGroup object = getItem(position);

            if (!holder.tryBinding(object)) {
                final View parentView = holder.itemView;
                @ColorInt
                int appliedColor;
                int percentage = (int) (object.percentage() * 100);
                String assigneesText = object.getAssigneesAsTitle(getContext());
                ProgressBar progressBar = parentView.findViewById(R.id.progressBar);
                ImageView image = parentView.findViewById(R.id.image);
                View statusLayoutWeb = parentView.findViewById(R.id.statusLayoutWeb);
                TextView text1 = parentView.findViewById(R.id.text);
                TextView text2 = parentView.findViewById(R.id.text2);
                TextView text3 = parentView.findViewById(R.id.text3);
                TextView text4 = parentView.findViewById(R.id.text4);

                parentView.setSelected(object.isSelectableSelected());

                if (object.hasIssues)
                    appliedColor = mColorError;
                else
                    appliedColor = object.numberOfCompleted() == object.numberOfTotal() ? mColorDone : mColorPending;

                if (object.isRunning) {
                    image.setImageResource(R.drawable.ic_pause_white_24dp);
                } else {
                    if (object.hasOutgoing() == object.hasIncoming())
                        image.setImageResource(object.hasOutgoing()
                                ? R.drawable.ic_compare_arrows_white_24dp
                                : R.drawable.ic_error_outline_white_24dp);
                    else
                        image.setImageResource(object.hasOutgoing()
                                ? R.drawable.ic_arrow_up_white_24dp
                                : R.drawable.ic_arrow_down_white_24dp);
                }

                statusLayoutWeb.setVisibility(object.hasOutgoing() && object.isServedOnWeb ? View.VISIBLE : View.GONE);
                text1.setText(FileUtils.sizeExpression(object.bytesTotal(), false));
                text2.setText(assigneesText.length() > 0 ? assigneesText : getContext().getString(
                        object.isServedOnWeb ? R.string.text_transferSharedOnBrowser : R.string.text_emptySymbol));
                text3.setText(mPercentFormat.format(object.percentage()));
                text4.setText(getContext().getString(R.string.text_transferStatusFiles,
                        object.numberOfCompleted(), object.numberOfTotal()));
                progressBar.setMax(100);
                if (Build.VERSION.SDK_INT >= 24)
                    progressBar.setProgress(percentage <= 0 ? 1 : percentage, true);
                else
                    progressBar.setProgress(percentage <= 0 ? 1 : percentage);
                ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(appliedColor));
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Drawable wrapDrawable = DrawableCompat.wrap(progressBar.getProgressDrawable());

                    DrawableCompat.setTint(wrapDrawable, appliedColor);
                    progressBar.setProgressDrawable(DrawableCompat.unwrap(wrapDrawable));
                } else
                    progressBar.setProgressTintList(ColorStateList.valueOf(appliedColor));
            }
        } catch (Exception ignored) {
        }
    }

    public void updateActiveList(long[] activeList)
    {
        synchronized (mRunningTasks) {
            mRunningTasks.clear();

            for (long groupId : activeList)
                mRunningTasks.add(groupId);
        }
    }
}
