package com.arco2121.swissy.Tools.Livella;

import com.arco2121.swissy.Tools.ToolListener;

public interface LivellaListener extends ToolListener {
    void onLevelChange(float rotation, float pitch, float roll);
}