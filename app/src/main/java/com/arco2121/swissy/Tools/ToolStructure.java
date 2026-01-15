package com.arco2121.swissy.Tools;

import android.content.Context;

public interface ToolStructure {
    void startSensors(Context c);
    void stopSensors();
    void setListener(ToolListener listener);
}