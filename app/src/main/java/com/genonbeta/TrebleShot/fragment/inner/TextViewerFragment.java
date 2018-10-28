package com.genonbeta.TrebleShot.fragment.inner;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.android.framework.app.Fragment;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * created by: veli
 * date: 9/4/18 12:03 AM
 */
public class TextViewerFragment
		extends Fragment
		implements IconSupport, TitleSupport
{
	private TextView mMainText;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		return inflater.inflate(R.layout.layout_text_viewer, container, false);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_text_viewer_fragment, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == R.id.actions_text_viewer_fragment_edit
				&& getActivity() instanceof ReadyLoadListener)
			((ReadyLoadListener) getActivity()).onTextViewerEditRequested();
		else
			return super.onOptionsItemSelected(item);

		return true;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		mMainText = view.findViewById(R.id.layout_text_viewer_text);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		updateText();
	}

	@DrawableRes
	@Override
	public int getIconRes()
	{
		return R.drawable.ic_forum_white_24dp;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_shareTextShort);
	}

	public boolean updateText()
	{
		if (mMainText != null
				&& getActivity() instanceof ReadyLoadListener) {
			mMainText.setText(((ReadyLoadListener) getActivity()).onTextViewerReadyLoad());
			return true;
		}

		return false;
	}

	public interface ReadyLoadListener
	{
		CharSequence onTextViewerReadyLoad();

		void onTextViewerEditRequested();
	}
}
