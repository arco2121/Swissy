package com.arco2121.swissy.Utility;

import static android.view.animation.AnimationUtils.loadInterpolator;

import android.animation.TimeInterpolator;
import android.content.Context;
import android.hardware.SensorManager;
import android.view.MotionEvent;
import android.view.View;

import com.arco2121.swissy.Managers.SettingsManager;

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
                if(SettingsManager.getPropreties(window.getContext()).getBoolean("vibration", false)) VibrationMaker.vibrate(window, VibrationMaker.Vibration.Short);
                break;
        }
        return consumeEvent;
    }
    //OverLoad
    public static boolean animateButton(View window, MotionEvent event, float littleX, float littleY, long duration, VibrationMaker.Vibration vibstyle, Runnable func, boolean consumeEvent) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                window.animate().scaleX(littleX).scaleY(littleY).setDuration(duration).start();
                break;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:
                window.animate().scaleX(1f).scaleY(1f).setDuration(duration)
                        .withEndAction(func)
                        .start();
                if(SettingsManager.getPropreties(window.getContext()).getBoolean("vibration", false)) VibrationMaker.vibrate(window, vibstyle);
                break;
        }
        return consumeEvent;
    }

    public static int calibrateSensorsDelay(Context ct, int prio) {
        if(!SettingsManager.getPropreties(ct).getBoolean("energy_safer", false)) return prio == 1 ? SensorManager.SENSOR_DELAY_GAME :  SensorManager.SENSOR_DELAY_FASTEST;
        else return SensorManager.SENSOR_DELAY_NORMAL;
    }
}
