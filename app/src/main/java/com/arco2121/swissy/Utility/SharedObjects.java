package com.arco2121.swissy.Utility;

import static android.view.animation.AnimationUtils.loadInterpolator;

import android.animation.TimeInterpolator;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SharedObjects {
    static final HashMap<String, Object[]> sharedObjects = new HashMap<>();

    public static Object addObj(String key, Object io, Object[] drawableResorces) {
        Object[] entry = { io, drawableResorces };
        sharedObjects.put(key, entry);
        return io;
    }
    public static Object[] getObj(String io) {
        return sharedObjects.get(io);
    }
    public static int findIndex(String ele) { return asList().indexOf(ele); }
    public static String asString() {
        return sharedObjects.keySet().toString();
    }
    public static List<String> asList() {
        return new ArrayList<>(sharedObjects.keySet());
    }
    public static String[] asArray() {
        return sharedObjects.keySet().toArray(new String[0]);
    }

    public static boolean animateButton(View window, MotionEvent event, float littleX, float littleY, long duration, Runnable func, boolean consumeEvent) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                window.animate().scaleX(littleX).scaleY(littleY).setDuration(duration).start();
                break;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:
                window.animate().scaleX(1f).scaleY(1f).setDuration(duration)
                        .withEndAction(func)
                        .start();
                break;
        }
        return consumeEvent && window.performClick();
    }
    //OverLoad
    public static boolean animateButton(View window, MotionEvent event, float littleX, float littleY, long duration, int interpolator, Runnable func, boolean consumeEvent) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                window.animate().scaleX(littleX).scaleY(littleY).setInterpolator(loadInterpolator(window.getContext(), interpolator)).setDuration(duration).start();
                break;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:
                window.animate().scaleX(1f).scaleY(1f).setInterpolator(loadInterpolator(window.getContext(), interpolator)).setDuration(duration)
                        .withEndAction(func)
                        .start();
                break;
        }
        return consumeEvent && window.performClick();
    }
}
