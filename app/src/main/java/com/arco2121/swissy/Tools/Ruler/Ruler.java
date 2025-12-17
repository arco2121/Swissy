package com.arco2121.swissy.Tools.Ruler;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.View;

import androidx.annotation.NonNull;

import com.arco2121.swissy.Tools.ToolStructure;

public class Ruler implements ToolStructure {
    public enum RulerUnit {
        MILLIMETER,
        CENTIMETER,
        INCH,
        PIXEL;

        @NonNull
        @Override
        public String toString() {
            switch (this) {
                case CENTIMETER: return "cm";
                case MILLIMETER: return "mm";
                case INCH: return "in";
                case PIXEL: return "px";
            }
            return "";
        }
    }
    public final float pxPerMm;
    private RulerListener listener;
    public float calibrationFactor = 1f;

    public Ruler(Activity activity) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        pxPerMm = metrics.xdpi / 25.4f;
    }

    public void setCalibrationFactor(float factor) {
        calibrationFactor = Math.max(Math.min(factor, 10f), 0.5f);
    }

    private float mmToPx(float mm) {
        return mm * pxPerMm * calibrationFactor;
    }

    public View createRulerView(Activity activity, float lengthMm, RulerUnit unit) {
        return new RulerView(activity, lengthMm, pxPerMm, unit, calibrationFactor, listener);
    }

    @Override
    public void startSensors(Context c) { }

    @Override
    public void stopSensors() { }

    @Override
    public void setListener(Object listener) {
        this.listener = (RulerListener) listener;
    }
}