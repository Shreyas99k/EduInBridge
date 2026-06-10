package com.example.eduinsight;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class UIUtils {

    /**
     * Applies a staggered overshoot entry animation to a list of views.
     */
    public static void applyStaggeredAnimation(View... views) {
        for (int i = 0; i < views.length; i++) {
            View v = views[i];
            if (v == null) continue;
            
            v.setScaleX(0.8f);
            v.setScaleY(0.8f);
            v.setAlpha(0f);
            
            v.animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setStartDelay(i * 80)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();
        }
    }

    public static void performHaptic(View view) {
        if (view != null) {
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        }
    }

    /**
     * Creates a breathing/pulsing animation for status indicators.
     */
    public static void applyPulseAnimation(View view) {
        if (view == null) return;
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, View.SCALE_X, 1f, 1.15f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1f, 1.15f, 1f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.6f, 1f);

        AnimatorSet set = new AnimatorSet();
        set.playTogether(scaleX, scaleY, alpha);
        set.setDuration(2000);
        set.setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator());
        set.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (view.getVisibility() == View.VISIBLE) {
                    set.start();
                }
            }
        });
        set.start();
    }
}
