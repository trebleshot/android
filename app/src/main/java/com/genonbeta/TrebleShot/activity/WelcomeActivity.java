package com.genonbeta.TrebleShot.activity;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.widget.DynamicViewPagerAdapter;

public class WelcomeActivity extends Activity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        ViewPager viewPager = findViewById(R.id.activity_welcome_view_pager);
        DynamicViewPagerAdapter pagerAdapter = new DynamicViewPagerAdapter();

        pagerAdapter.addView(getLayoutInflater().inflate(R.layout.layout_welcome_page_1, null, false));

        viewPager.setAdapter(pagerAdapter);
    }
}
