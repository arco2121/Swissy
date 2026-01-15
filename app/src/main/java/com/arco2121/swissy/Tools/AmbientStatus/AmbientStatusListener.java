package com.arco2121.swissy.Tools.AmbientStatus;

import com.arco2121.swissy.Tools.ToolListener;

public interface AmbientStatusListener extends ToolListener {
    void onTemperature(float temp);
    void onUmidity(float umi);
    void onPressure(float pressure);
    void onNoise(double decibels);
}
