package com.arco2121.swissy.Tools.Ruler;

import android.content.Context;
import android.util.DisplayMetrics;

public class Ruler {
    private final float MM_PER_INCH = 25.4f;
    private final float POINT_PER_INCH = 72f;
    private final float PICA_PER_INCH = 6f;
    private float pxPerMmX;
    private float pxPerMmY;
    private float calibrationFactorX = 1f;
    private float calibrationFactorY = 1f;
    public enum Scale {
        MILLIMETERS,
        CENTIMETERS,
        INCHES,
        POINTS,
        PICAS
    }

    public RulerEngine(Context context) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        pxPerMmX = metrics.xdpi / MM_PER_INCH;
        pxPerMmY = metrics.ydpi / MM_PER_INCH;
    }
    public float mmToPx(float mm, boolean horizontal) {
        return mm * (horizontal ? (pxPerMmX * calibrationFactorX)
                                : (pxPerMmY * calibrationFactorY));
    }
    public float pxToMm(float px, boolean horizontal) {
        return px / (horizontal ? (pxPerMmX * calibrationFactorX)
                                : (pxPerMmY * calibrationFactorY));
    }
    public float toPx(float value, Scale scale, boolean horizontal) {
        switch (scale) {
            case MILLIMETERS:
                return mmToPx(value, horizontal);

            case CENTIMETERS:
                return mmToPx(value * 10f, horizontal);

            case INCHES:
                return mmToPx(value * MM_PER_INCH, horizontal);

            case POINTS:
                return mmToPx((MM_PER_INCH / POINT_PER_INCH) * value, horizontal);

            case PICAS:
                return mmToPx((MM_PER_INCH / PICA_PER_INCH) * value, horizontal);
        }
        return 0;
    }

    public float fromPx(float px, Scale scale, boolean horizontal) {
        float mm = pxToMm(px, horizontal);

        switch (scale) {
            case MILLIMETERS: return mm;
            case CENTIMETERS: return mm / 10f;
            case INCHES: return mm / MM_PER_INCH;
            case POINTS: return (mm / MM_PER_INCH) * POINT_PER_INCH;
            case PICAS: return (mm / MM_PER_INCH) * PICA_PER_INCH;
        }
        return 0;
    }

    public void autoCalibrateX(float realMm, float shownMm) {
        if (shownMm > 0) calibrationFactorX = realMm / shownMm;
    }

    public void autoCalibrateY(float realMm, float shownMm) {
        if (shownMm > 0) calibrationFactorY = realMm / shownMm;
    }

    public void setCalibrationFactorX(float f) {
        calibrationFactorX = f;
    }

    public void setCalibrationFactorY(float f) {
        calibrationFactorY = f;
    }

    public float getCalibrationFactorX() {
        return calibrationFactorX;
    }

    public float getCalibrationFactorY() {
        return calibrationFactorY;
    }
}