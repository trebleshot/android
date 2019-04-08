package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.util.AddressedInterface;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * created by: veli
 * date: 4/7/19 10:35 PM
 */
public class ActiveConnectionListAdapter extends EditableListAdapter<ActiveConnectionListAdapter
        .AddressedEditableInterface, EditableListAdapter.EditableViewHolder>
{
    public ActiveConnectionListAdapter(Context context)
    {
        super(context);
    }

    @Override
    public List<AddressedEditableInterface> onLoad()
    {
        List<AddressedEditableInterface> resultList = new ArrayList<>();
        List<AddressedInterface> interfaceList = NetworkUtils.getInterfaces(true,
                AppConfig.DEFAULT_DISABLED_INTERFACES);

        for (AddressedInterface addressedInterface : interfaceList) {
            AddressedEditableInterface editableInterface = new AddressedEditableInterface(
                    addressedInterface, TextUtils.getAdapterName(getContext(), addressedInterface));

            if (filterItem(editableInterface))
                resultList.add(editableInterface);
        }

        return resultList;
    }

    @NonNull
    @Override
    public EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return new EditableViewHolder(getInflater().inflate(R.layout.list_active_connection,
                parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EditableViewHolder holder, int position)
    {
        try {
            AddressedEditableInterface object = getItem(position);
            View parentView = holder.getView();

            TextView text1 = parentView.findViewById(R.id.text);
            TextView text2 = parentView.findViewById(R.id.text2);

            text1.setText(object.getSelectableTitle());
            text2.setText(TextUtils.makeWebShareLink(getContext(), object.getInterface()
                    .getAssociatedAddress()));
        } catch (NotReadyException e) {
            e.printStackTrace();
        }
    }

    public static class AddressedEditableInterface implements Editable
    {
        private AddressedInterface mInterface;
        private String mName;
        private boolean mSelected = false;

        public AddressedEditableInterface(AddressedInterface addressedInterface, String name)
        {
            mInterface = addressedInterface;
            mName = name;
        }

        @Override
        public boolean applyFilter(String[] filteringKeywords)
        {
            for (String word : filteringKeywords) {
                String wordLC = word.toLowerCase();

                if (mInterface.getNetworkInterface().getDisplayName().toLowerCase().contains(wordLC)
                        || mInterface.getAssociatedAddress().toLowerCase().contains(wordLC)
                        || mName.toLowerCase().contains(wordLC))
                    return true;
            }

            return false;
        }

        @Override
        public long getId()
        {
            return mInterface.getAssociatedAddress().hashCode();
        }

        @Override
        public void setId(long id)
        {
            // not required
        }

        @Override
        public boolean comparisonSupported()
        {
            return false;
        }

        @Override
        public String getComparableName()
        {
            return mName;
        }

        @Override
        public long getComparableDate()
        {
            return 0;
        }

        @Override
        public long getComparableSize()
        {
            return 0;
        }

        public AddressedInterface getInterface()
        {
            return mInterface;
        }

        public String getName()
        {
            return mName;
        }

        @Override
        public String getSelectableTitle()
        {
            return mName;
        }

        @Override
        public boolean isSelectableSelected()
        {
            return mSelected;
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            mSelected = selected;
            return true;
        }
    }
}
