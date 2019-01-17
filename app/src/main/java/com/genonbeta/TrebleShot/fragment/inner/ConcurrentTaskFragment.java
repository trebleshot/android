package com.genonbeta.TrebleShot.fragment.inner;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.android.framework.app.Fragment;
import com.genonbeta.android.framework.app.FragmentImpl;

/**
 * created by: veli
 * date: 8/6/18 8:44 AM
 */
public class ConcurrentTaskFragment
        extends Fragment
        implements FragmentImpl
{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.layout_concurrent_task_fragment, container, false);

        return view;
    }
}
