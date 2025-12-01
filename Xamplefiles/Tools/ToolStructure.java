package com.arco2121.swissy.Tools;

public interface ToolStructure {
    void startSensors();
    void stopSensors();
    void setListener(ToolListenerStructure listener);
}