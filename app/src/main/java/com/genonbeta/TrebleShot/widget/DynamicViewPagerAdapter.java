package com.genonbeta.TrebleShot.widget;

import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

public class DynamicViewPagerAdapter extends PagerAdapter
{
    // This holds all the currently displayable views, in order from left to right.
    private List<View> views = new ArrayList<View>();

    //-----------------------------------------------------------------------------
    // Used by ViewPager.  "Object" represents the page; tell the ViewPager where the
    // page should be displayed, from left-to-right.  If the page no longer exists,
    // return POSITION_NONE.
    @Override
    public int getItemPosition(Object object)
    {
        int index = views.indexOf(object);
        if (index == -1)
            return POSITION_NONE;
        else
            return index;
    }

    //-----------------------------------------------------------------------------
    // Used by ViewPager.  Called when ViewPager needs a page to display; it is our job
    // to add the page to the container, which is normally the ViewPager itself.  Since
    // all our pages are persistent, we simply retrieve it from our "views" ArrayList.
    @Override
    public Object instantiateItem(ViewGroup container, int position)
    {
        View v = views.get(position);
        container.addView(v);
        return v;
    }

    //-----------------------------------------------------------------------------
    // Used by ViewPager.  Called when ViewPager no longer needs a page to display; it
    // is our job to remove the page from the container, which is normally the
    // ViewPager itself.  Since all our pages are persistent, we do nothing to the
    // contents of our "views" ArrayList.
    @Override
    public void destroyItem(ViewGroup container, int position, Object object)
    {
        container.removeView(views.get(position));
    }

    //-----------------------------------------------------------------------------
    // Used by ViewPager; can be used by app as well.
    // Returns the total number of pages that the ViewPage can display.  This must
    // never be 0.
    @Override
    public int getCount()
    {
        return views.size();
    }

    //-----------------------------------------------------------------------------
    // Used by ViewPager.
    @Override
    public boolean isViewFromObject(View view, Object object)
    {
        return view == object;
    }

    //-----------------------------------------------------------------------------
    // Add "view" to right end of "views".
    // Returns the position of the new view.
    // The app should call this to add pages; not used by ViewPager.
    public int addView(View v)
    {
        return addView(v, views.size());
    }

    //-----------------------------------------------------------------------------
    // Add "view" at "position" to "views".
    // Returns position of new view.
    // The app should call this to add pages; not used by ViewPager.
    public int addView(View v, int position)
    {
        views.add(position, v);
        return position;
    }

    //-----------------------------------------------------------------------------
    // Removes "view" from "views".
    // Retuns position of removed view.
    // The app should call this to remove pages; not used by ViewPager.
    public int removeView(ViewPager pager, View v)
    {
        return removeView(pager, views.indexOf(v));
    }

    //-----------------------------------------------------------------------------
    // Removes the "view" at "position" from "views".
    // Retuns position of removed view.
    // The app should call this to remove pages; not used by ViewPager.
    public int removeView(ViewPager pager, int position)
    {
        // ViewPager doesn't have a delete method; the closest is to set the adapter
        // again.  When doing so, it deletes all its views.  Then we can delete the view
        // from from the adapter and finally set the adapter to the pager again.  Note
        // that we set the adapter to null before removing the view from "views" - that's
        // because while ViewPager deletes all its views, it will call destroyItem which
        // will in turn cause a null pointer ref.
        pager.setAdapter(null);
        views.remove(position);
        pager.setAdapter(this);

        return position;
    }

    //-----------------------------------------------------------------------------
    // Returns the "view" at "position".
    // The app should call this to retrieve a view; not used by ViewPager.
    public View getView(int position)
    {
        return views.get(position);
    }

    // Other relevant methods:

    // finishUpdate - called by the ViewPager - we don't care about what pages the
    // pager is displaying so we don't use this method.
}
