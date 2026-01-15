package com.arco2121.swissy.Tools.ScannerEye;

import androidx.camera.core.Preview;

public interface ScannerEyeListener {
    void onScanCode(String result);
    void onScanObject(String result, float precision);
}
