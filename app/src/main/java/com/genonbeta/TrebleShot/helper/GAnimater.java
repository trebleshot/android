package com.genonbeta.TrebleShot.helper;

import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LayoutAnimationController;
import android.view.animation.TranslateAnimation;

public class GAnimater
{
	public static int APPEAR = 1;
	public static int DISAPPEAR = 2;
	
	public static void applyLayoutAnimation(ViewGroup viewGroup, int mode)
	{
		LayoutAnimationController controller = new LayoutAnimationController(getAnimation(mode), 0.5f);
		viewGroup.setLayoutAnimation(controller);	
	}
	
	public static AnimationSet getAnimation(int mode)
	{
		AnimationSet set = new AnimationSet(true);
		
		if (mode == APPEAR)
		{
			Animation animation = new AlphaAnimation(0.0f, 1.0f);
			animation.setDuration(200);
			set.addAnimation(animation);

			animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, -1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
			);

			animation.setDuration(200);
			set.addAnimation(animation);
		}
		else if (mode == DISAPPEAR)
		{
			Animation animation = new AlphaAnimation(1.0f, 0.0f);
			animation.setDuration(200);
			set.addAnimation(animation);

			animation = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF, 0.0f
			);

			animation.setDuration(200);
			set.addAnimation(animation);
		}
		
		return set;
	}
}
