package com.arco2121.swissy.Tools.Impact;

public interface ImpactListener {
    void onCalibrated();
    void onImpact(float value, float range, float raw);
    void onCalibrationProgress(float progress);
}