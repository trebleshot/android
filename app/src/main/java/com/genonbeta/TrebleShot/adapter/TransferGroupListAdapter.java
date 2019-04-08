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

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.ShowingAssignee;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.widget.ImageViewCompat;

/**
 * created by: Veli
 * date: 9.11.2017 23:39
 */

public class TransferGroupListAdapter
        extends GroupEditableListAdapter<TransferGroupListAdapter.PreloadedGroup, GroupEditableListAdapter.GroupViewHolder>
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
            mDatabase.calculateTransactionSize(group.groupId, group.index);

            StringBuilder assigneesText = new StringBuilder();

            for (ShowingAssignee showingAssignee : group.index.assignees) {
                if (assigneesText.length() > 0)
                    assigneesText.append(", ");

                assigneesText.append(showingAssignee.device.nickname);
            }

            if (assigneesText.length() == 0 && group.isServedOnWeb)
                assigneesText.append(getContext().getString(R.string.text_transferSharedOnBrowser));

            group.assignees = assigneesText.length() > 0
                    ? assigneesText.toString()
                    : getContext().getString(R.string.text_emptySymbol);

            group.isRunning = activeList.contains(group.groupId);
            group.totalCount = group.index.incomingCount + group.index.outgoingCount;
            group.totalBytes = group.index.incoming + group.index.outgoing;
            group.totalBytesCompleted = group.index.incomingCompleted + group.index.outgoingCompleted;
            group.totalCountCompleted = group.index.incomingCountCompleted + group.index.outgoingCountCompleted;

            group.totalPercent = group.totalBytesCompleted == 0 || group.totalBytes == 0
                    ? 0.0 : Long.valueOf(group.totalBytesCompleted).doubleValue() / Long.valueOf(group.totalBytes).doubleValue();

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
        if (viewType == VIEW_TYPE_REPRESENTATIVE)
            return new GroupViewHolder(getInflater().inflate(R.layout.layout_list_title_no_padding, parent, false), R.id.layout_list_title_text);

        return new GroupEditableListAdapter.GroupViewHolder(getInflater().inflate(R.layout.list_transfer_group, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final GroupEditableListAdapter.GroupViewHolder holder, int position)
    {
        try {
            final PreloadedGroup object = getItem(position);

            if (!holder.tryBinding(object)) {
                final View parentView = holder.getView();
                @ColorInt
                int appliedColor;
                int percentage = (int) (object.totalPercent * 100);
                ProgressBar progressBar = parentView.findViewById(R.id.progressBar);
                ImageView image = parentView.findViewById(R.id.image);
                View statusLayoutWeb = parentView.findViewById(R.id.statusLayoutWeb);
                TextView text1 = parentView.findViewById(R.id.text);
                TextView text2 = parentView.findViewById(R.id.text2);
                TextView text3 = parentView.findViewById(R.id.text3);
                TextView text4 = parentView.findViewById(R.id.text4);

                parentView.setSelected(object.isSelectableSelected());

                if (object.index.hasIssues)
                    appliedColor = mColorError;
                else
                    appliedColor = object.totalCount == object.totalCountCompleted
                            ? mColorDone
                            : mColorPending;

                if (object.isRunning) {
                    image.setImageResource(R.drawable.ic_pause_white_24dp);
                } else {
                    if ((object.index.outgoingCount == 0 && object.index.incomingCount == 0)
                            || (object.index.outgoingCount > 0 && object.index.incomingCount > 0))
                        image.setImageResource(object.index.outgoingCount > 0
                                ? R.drawable.ic_compare_arrows_white_24dp
                                : R.drawable.ic_error_outline_white_24dp);
                    else
                        image.setImageResource(object.index.outgoingCount > 0
                                ? R.drawable.ic_arrow_up_white_24dp
                                : R.drawable.ic_arrow_down_white_24dp);
                }

                statusLayoutWeb.setVisibility(object.index.outgoingCount > 0 && object.isServedOnWeb
                        ? View.VISIBLE : View.GONE);
                text1.setText(object.assignees);
                text2.setText(FileUtils.sizeExpression(object.totalBytes, false));
                text3.setText(mPercentFormat.format(object.totalPercent));
                text4.setText(getContext().getString(R.string.text_transferStatusFiles, object.totalCountCompleted, object.totalCount));
                progressBar.setMax(100);
                progressBar.setProgress(percentage <= 0 ? 1 : percentage);
                ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(appliedColor));

                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    Drawable wrapDrawable = DrawableCompat.wrap(progressBar.getProgressDrawable());

                    DrawableCompat.setTint(wrapDrawable, appliedColor);
                    progressBar.setProgressDrawable(DrawableCompat.unwrap(wrapDrawable));
                } else
                    progressBar.setProgressTintList(ColorStateList.valueOf(appliedColor));
            }
        } catch (Exception e) {
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

    public static class PreloadedGroup
            extends TransferGroup
            implements GroupEditableListAdapter.GroupEditable
    {
        public int viewType;
        public String representativeText;

        public Index index = new Index();
        public String assignees;

        public int totalCount;
        public int totalCountCompleted;
        public long totalBytes;
        public long totalBytesCompleted;
        public double totalPercent;
        public boolean isRunning;

        public PreloadedGroup()
        {
        }

        public PreloadedGroup(String representativeText)
        {
            this.viewType = TransferGroupListAdapter.VIEW_TYPE_REPRESENTATIVE;
            this.representativeText = representativeText;
        }

        @Override
        public boolean applyFilter(String[] filteringKeywords)
        {
            for (String keyword : filteringKeywords)
                if (assignees.toLowerCase().contains(keyword.toLowerCase()))
                    return true;

            return false;
        }

        @Override
        public boolean comparisonSupported()
        {
            return true;
        }

        @Override
        public String getComparableName()
        {
            return getSelectableTitle();
        }

        @Override
        public long getComparableDate()
        {
            return dateCreated;
        }

        @Override
        public long getComparableSize()
        {
            return totalCount;
        }

        @Override
        public long getId()
        {
            return groupId;
        }

        @Override
        public void setId(long id)
        {
            this.groupId = id;
        }

        @Override
        public String getSelectableTitle()
        {
            return String.format("%s (%s)", assignees, FileUtils.sizeExpression(totalBytes, false));
        }

        @Override
        public int getRequestCode()
        {
            return 0;
        }

        @Override
        public int getViewType()
        {
            return viewType;
        }

        @Override
        public String getRepresentativeText()
        {
            return representativeText;
        }

        @Override
        public void setRepresentativeText(CharSequence representativeText)
        {
            this.representativeText = String.valueOf(representativeText);
        }

        @Override
        public boolean isGroupRepresentative()
        {
            return representativeText != null;
        }

        @Override
        public void setDate(long date)
        {
            this.dateCreated = date;
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            return !isGroupRepresentative() && super.setSelectableSelected(selected);
        }

        @Override
        public void setSize(long size)
        {
            this.totalCount = ((Long) size).intValue();
        }
    }
}
