package com.arco2121.swissy.Tools.Livella;

public interface LivellaListener extends ToolListenerStructure {
    void onLevelChange(float rotation, float pitch, float roll);
}