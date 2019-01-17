package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.IdRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;

import com.genonbeta.TrebleShot.R;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.navigation.NavigationView;

public class NavigationViewBottomSheetDialog extends BottomSheetDialog
{
    public NavigationViewBottomSheetDialog(@NonNull Activity activity,
                                           @MenuRes int menu,
                                           @IdRes int selectedItemId,
                                           NavigationView.OnNavigationItemSelectedListener listener)
    {
        super(activity);

        View view = LayoutInflater.from(activity).inflate(R.layout.layout_navigation_view_bottom_sheet, null, false);
        NavigationView navigationView = view.findViewById(R.id.nav_view);

        activity.getMenuInflater().inflate(menu, navigationView.getMenu());
        navigationView.setCheckedItem(selectedItemId);
        navigationView.setNavigationItemSelectedListener(listener);

        setContentView(view);
    }
}
