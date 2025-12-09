package com.arco2121.swissy.Tools.GeoCompass;

public interface GeoCompassListener {
    void onCompassUpdate(float magneticAzimuth, float oldValue, float trueAzimuth, float nidleRotation);
    void onMagneticInterference(float strength, int level);
    void onCalibrationStart();
    void onCalibrationEnd();
    void onAccuracy(String message);
}