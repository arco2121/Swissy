package com.arco2121.swissy.Tools.Impact;

import com.arco2121.swissy.Tools.ToolListener;

public interface ImpactListener extends ToolListener {
    void onCalibrated();
    void onImpact(float value, float range, float raw);
    void onCalibrationProgress(float progress);
}