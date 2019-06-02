package com.genonbeta.TrebleShot.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.transition.TransitionManager;
import androidx.viewpager.widget.ViewPager;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.DynamicViewPagerAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class WelcomeActivity extends Activity
{
    public final static String TAG = WelcomeActivity.class.getSimpleName();

    private ViewGroup mSplashView;
    private ViewGroup mProfileView;
    private ViewGroup mPermissionsView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        setSkipPermissionRequest(true);
        setWelcomePageDisallowed(true);

        final FloatingActionButton nextButton = findViewById(R.id.activity_welcome_view_next);
        final AppCompatImageView previousButton = findViewById(R.id.activity_welcome_view_previous);
        final ProgressBar progressBar = findViewById(R.id.activity_welcome_progress_bar);
        final ViewPager viewPager = findViewById(R.id.activity_welcome_view_pager);
        final DynamicViewPagerAdapter pagerAdapter = new DynamicViewPagerAdapter();

        {
            @ColorInt
            int appliedColor = ContextCompat.getColor(this, AppUtils.getReference(this, R.attr.colorSecondary));

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                Drawable wrapDrawable = DrawableCompat.wrap(progressBar.getProgressDrawable());

                DrawableCompat.setTint(wrapDrawable, appliedColor);
                progressBar.setProgressDrawable(DrawableCompat.unwrap(wrapDrawable));
            } else
                progressBar.setProgressTintList(ColorStateList.valueOf(appliedColor));
        }

        {
            mSplashView = (ViewGroup) getLayoutInflater().inflate(R.layout.layout_welcome_page_1, null, false);
            pagerAdapter.addView(mSplashView);
        }

        if (Build.VERSION.SDK_INT >= 23) {
            mPermissionsView = (ViewGroup) getLayoutInflater().inflate(R.layout.layout_welcome_page_3, null, false);
            pagerAdapter.addView(mPermissionsView);
            checkPermissionsState();

            mPermissionsView.findViewById(R.id.layout_welcome_page_3_request_button)
                    .setOnClickListener(new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View v)
                        {
                            requestRequiredPermissions(false);
                        }
                    });
        }

        {
            mProfileView = (ViewGroup) getLayoutInflater().inflate(R.layout.layout_welcome_page_2, null, false);
            pagerAdapter.addView(mProfileView);
            setUserProfile();
        }

        pagerAdapter.addView(getLayoutInflater().inflate(R.layout.layout_welcome_page_4, null, false));

        {
            View view = getLayoutInflater().inflate(R.layout.layout_welcome_page_5, null, false);
            AlphaAnimation alphaAnimation = new AlphaAnimation(0.3f, 1.0f);

            alphaAnimation.setDuration(2000);
            alphaAnimation.setRepeatCount(Animation.INFINITE);
            alphaAnimation.setRepeatMode(Animation.REVERSE);

            view.findViewById(R.id.layout_welcome_page_5_text)
                    .setAnimation(alphaAnimation);

            pagerAdapter.addView(view);
        }
        progressBar.setMax((pagerAdapter.getCount() - 1) * 100);

        previousButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (viewPager.getCurrentItem() - 1 >= 0)
                    viewPager.setCurrentItem(viewPager.getCurrentItem() - 1, true);
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (viewPager.getCurrentItem() + 1 < pagerAdapter.getCount())
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                else {
                    // end presentation
                    getDefaultPreferences().edit()
                            .putBoolean("introduction_shown", true)
                            .apply();

                    startActivity(new Intent(WelcomeActivity.this, HomeActivity.class));
                    finish();
                }
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
                progressBar.setProgress((position * 100) + (int) (positionOffset * 100));

                if (position == 0) {
                    progressBar.setAlpha(positionOffset);
                    previousButton.setAlpha(positionOffset);
                } else {
                    progressBar.setAlpha(1.0f);
                    previousButton.setAlpha(1.0f);
                }
            }

            @Override
            public void onPageSelected(int position)
            {
                OvershootInterpolator interpolator = new OvershootInterpolator();

                nextButton.setImageResource(position + 1 >= pagerAdapter.getCount()
                        ? R.drawable.ic_check_white_24dp
                        : R.drawable.ic_navigate_next_white_24dp);
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {
            }
        });

        viewPager.setAdapter(pagerAdapter);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        slideSplashView();
        setUserProfile();
        checkPermissionsState();
    }

    @Override
    public void onUserProfileUpdated()
    {
        super.onUserProfileUpdated();
        setUserProfile();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissionsState();
    }

    protected void checkPermissionsState()
    {
        if (Build.VERSION.SDK_INT < 23)
            return;

        boolean permissionsOk = AppUtils.checkRunningConditions(this);

        mPermissionsView.findViewById(R.id.layout_welcome_page_3_perm_ok_image)
                .setVisibility(permissionsOk ? View.VISIBLE : View.GONE);

        mPermissionsView.findViewById(R.id.layout_welcome_page_3_request_button)
                .setVisibility(permissionsOk ? View.GONE : View.VISIBLE);
    }

    protected void setUserProfile()
    {
        if (mProfileView != null) {
            NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

            ImageView imageView = mProfileView.findViewById(R.id.layout_profile_picture_image_default);
            ImageView editImageView = mProfileView.findViewById(R.id.layout_profile_picture_image_preferred);
            TextView deviceNameText = mProfileView.findViewById(R.id.header_default_device_name_text);
            TextView versionText = mProfileView.findViewById(R.id.header_default_device_version_text);

            deviceNameText.setText(localDevice.nickname);
            versionText.setText(localDevice.versionName);
            loadProfilePictureInto(localDevice.nickname, imageView);

            editImageView.setOnClickListener(new View.OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    startProfileEditor();
                }
            });

            TransitionManager.beginDelayedTransition(mProfileView);
        }
    }

    protected void slideSplashView()
    {
        mSplashView.findViewById(R.id.layout_welcome_page_1_splash_image)
                .setAnimation(AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom_centered));

        mSplashView.findViewById(R.id.layout_welcome_page_1_details)
                .setAnimation(AnimationUtils.loadAnimation(this, R.anim.enter_from_bottom));
    }
}
