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
 * created by: Veli
 * date: 15.11.2017 16:36
 */

public class InitiatedFileExplorerFragment extends Fragment implements FragmentTitle
{
	@Nullable
	@Override
	public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.fragment_initatedfileexplorer, container, false);
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.text_fileExplorer);
	}
}
