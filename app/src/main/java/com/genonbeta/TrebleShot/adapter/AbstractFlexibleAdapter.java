package com.genonbeta.TrebleShot.adapter;

import android.widget.*;

abstract public class AbstractFlexibleAdapter extends BaseAdapter
{
	protected boolean mLockRequested =false;
	
	protected abstract void onSearch(String word);
	protected abstract void onUpdate();
	
	public boolean search(String word)
	{
		this.onSearch(word);
		return true;
	}
	
	public boolean update()
	{
		if (mLockRequested)
			return false;
			
		mLockRequested = true;
		
		this.onUpdate();
		
		mLockRequested = false;
		
		return true;
	}
}
