package com.arco2121.swissy.Tools.AmbientStatus;

import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.arco2121.swissy.Tools.ToolStructure;
import com.arco2121.swissy.Tools.Torch.TorchListener;

public class AmbientStatus implements ToolStructure {
    private final SensorManager sm;
    private final Sensor umiditySensor;
    private final Sensor pressureSensor;
    private TorchListener listener;
    private SensorEventListener brightnessListener;

    public AmbientStatus(SensorManager sm) throws Exception {
        this.sm = sm;
        umiditySensor = sm.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        pressureSensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if(umiditySensor == null || pressureSensor == null) throw new Exception("Status not available");
    }

    @Override
    public void startSensors() {

    }
    @Override
    public void stopSensors() {

    }
    @Override
    public void setListener(Object listener) {

    }
}
