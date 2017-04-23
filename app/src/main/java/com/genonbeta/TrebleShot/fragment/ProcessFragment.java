package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.support.FragmentTitle;

/**
 * Created by: veli
 * Date: 4/9/17 5:30 PM
 */

public class ProcessFragment extends Fragment implements FragmentTitle
{
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.layout_process, container, false);
		return view;
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.ongoing_process);
	}
}
