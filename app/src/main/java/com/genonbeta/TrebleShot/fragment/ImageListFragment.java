package com.genonbeta.TrebleShot.fragment;

/**
 * Created by gabm on 11/01/18.
 */

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.ImageListAdapter;
import com.genonbeta.TrebleShot.app.GalleryGroupEditableListFragment;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

public class ImageListFragment
        extends GalleryGroupEditableListFragment<ImageListAdapter.ImageHolder, GroupEditableListAdapter.GroupViewHolder, ImageListAdapter>
        implements TitleSupport
{
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setDefaultOrderingCriteria(ImageListAdapter.MODE_SORT_ORDER_DESCENDING);
        setDefaultSortingCriteria(ImageListAdapter.MODE_SORT_BY_DATE);
        setDefaultViewingGridSize(2, 4);
        setUseDefaultPaddingDecoration(false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setEmptyImage(R.drawable.ic_photo_white_24dp);
        setEmptyText(getString(R.string.text_listEmptyImage));
    }

    @Override
    public void onResume()
    {
        super.onResume();

        getContext().getContentResolver()
                .registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, getDefaultContentObserver());
    }

    @Override
    public void onPause()
    {
        super.onPause();

        getContext().getContentResolver()
                .unregisterContentObserver(getDefaultContentObserver());
    }

    @Override
    public ImageListAdapter onAdapter()
    {
        final AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder> quickActions = new AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder>()
        {
            @Override
            public void onQuickActions(final GroupEditableListAdapter.GroupViewHolder clazz)
            {
                if (!clazz.isRepresentative()) {
                    registerLayoutViewClicks(clazz);

                    clazz.getView().findViewById(R.id.visitImage)
                            .setOnClickListener(new View.OnClickListener()
                            {
                                @Override
                                public void onClick(View v)
                                {
                                    performLayoutClickOpenUri(clazz);
                                }
                            });
                }
            }
        };

        return new ImageListAdapter(getActivity())
        {
            @NonNull
            @Override
            public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
            }
        };
    }

    @Override
    public boolean onDefaultClickAction(GroupEditableListAdapter.GroupViewHolder holder)
    {
        performLayoutClickOpenUri(holder);
        return true;
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_photo);
    }
}
