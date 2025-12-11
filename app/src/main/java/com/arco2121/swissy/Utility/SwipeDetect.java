package com.arco2121.swissy.Utility;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;

public class SwipeDetect {
    public final GestureDetector detector;
    private long canOperate = 0;
    public enum Direction {
        HORIZONTAL, VERTICAL, BOTH
    }

    public SwipeDetect(Context ctx, long delay, Runnable onOne, Runnable onTwo, Direction direction) {
        detector = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                long now = System.currentTimeMillis();
                if (now - canOperate < delay) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                boolean handled = false;

                if ((direction == Direction.VERTICAL)
                        && Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    canOperate = now;
                    if (diffY < 0) onOne.run();
                    else onTwo.run();
                    handled = true;
                }

                if ((direction == Direction.HORIZONTAL)
                        && Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    canOperate = now;
                    if (diffX > 0) onOne.run();
                    else onTwo.run();
                    handled = true;
                }

                return handled;
            }
        });
    }
    public SwipeDetect(Context ctx, long delay, Runnable onUp, Runnable onDown, Runnable onRight, Runnable onLeft, Direction direction) {
        detector = new GestureDetector(ctx, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                long now = System.currentTimeMillis();
                if (now - canOperate < delay) return false;

                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();

                boolean handled = false;

                if ((direction == Direction.VERTICAL || direction == Direction.BOTH)
                        && Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                    canOperate = now;
                    if (diffY < 0) onUp.run();
                    else onDown.run();
                    handled = true;
                }

                if ((direction == Direction.HORIZONTAL || direction == Direction.BOTH)
                        && Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    canOperate = now;
                    if (diffX > 0) onRight.run();
                    else onLeft.run();
                    handled = true;
                }

                return handled;
            }
        });
    }
}