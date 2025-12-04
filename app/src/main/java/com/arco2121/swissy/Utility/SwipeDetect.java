package com.arco2121.swissy.Utility;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class SwipeDetect {
    public final GestureDetector detector;
    private long canOperate = 0;

    public SwipeDetect(Context ctx, long delay, Runnable onUp, Runnable onDown) {
        detector = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    long now = System.currentTimeMillis();
                    if (now - canOperate >= delay) {
                        canOperate = System.currentTimeMillis();
                        if (diffY < 0) onUp.run();
                        else onDown.run();
                        return true;
                    }
                }
                return false;
            }
        });
    }
}
