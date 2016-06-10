package com.genonbeta.TrebleShot.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.PendingProcessListAdapter;

public class PendingProcessListFragment extends ListFragment {
    private PendingProcessListAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mAdapter = new PendingProcessListAdapter(getActivity(), null);

        setEmptyText(getString(R.string.list_empty_msg));
        setListAdapter(mAdapter);
    }
}
