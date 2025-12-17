package com.arco2121.swissy.Tools.AmbientStatus;

public interface AmbientStatusListener {
    void onTemperature(float temp);
    void onUmidity(float umi);
    void onPressure(float pressure);
    void onNoise(double decibels);
}
