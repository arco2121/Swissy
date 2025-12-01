package com.arco2121.swissy.Tools.GeoCompass;

public interface GeoCompassListener extends ToolListenerStructure {
    void onCompassUpdate(float magneticAzimuth, float trueAzimuth);
    void onMagneticInterference(float strength, int level);
    void onCalibrationStart();
    void onCalibrationEnd();
}