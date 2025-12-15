package com.arco2121.swissy.Tools.Ruler;

import android.app.Activity;
import android.util.DisplayMetrics;
import android.view.View;
import com.arco2121.swissy.Tools.ToolStructure;

public class Ruler implements ToolStructure {
    public enum RulerUnit {
        MILLIMETER,
        CENTIMETER,
        INCH,
        PIXEL
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
        calibrationFactor = factor;
    }

    private float mmToPx(float mm) {
        return mm * pxPerMm * calibrationFactor;
    }

    public View createRulerView(Activity activity, float lengthMm, RulerUnit unit) {
        return new RulerView(activity, lengthMm, pxPerMm, unit, calibrationFactor, listener);
    }

    @Override
    public void startSensors() { }

    @Override
    public void stopSensors() { }

    @Override
    public void setListener(Object listener) {
        this.listener = (RulerListener) listener;
    }
}