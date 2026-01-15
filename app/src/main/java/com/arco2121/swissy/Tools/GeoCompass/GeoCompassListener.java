package com.arco2121.swissy.Tools.GeoCompass;

import com.arco2121.swissy.Tools.ToolListener;

public interface GeoCompassListener extends ToolListener {
    void onCompassUpdate(float magneticAzimuth, float oldValue, float trueAzimuth, float nidleRotation);
    void onMagneticInterference(float strength, int level);
    void onCalibrationStart();
    void onCalibrationEnd();
    void onAccuracy(String message);
}