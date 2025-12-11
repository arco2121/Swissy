package com.arco2121.swissy.Tools.AmbientStatus;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.arco2121.swissy.Tools.ToolStructure;

public class AmbientStatus implements ToolStructure {
    private final SensorManager sm;
    private final Sensor umiditySensor;
    private final Sensor pressureSensor;
    private final Sensor tempSensor;
    private final SensorEventListener umiditySensorListen;
    private final SensorEventListener pressureSensorListen;
    private final SensorEventListener tempSensorListen;
    private AmbientStatusListener listener;

    public AmbientStatus(SensorManager sm) {
        this.sm = sm;
        umiditySensor = sm.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);
        pressureSensor = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        tempSensor = sm.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        tempSensorListen = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {}

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(listener != null) listener.onTemperature(sensorEvent.values[0]);
            }
        };
        umiditySensorListen = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {}

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(listener != null) listener.onUmidity(sensorEvent.values[0]);
            }
        };
        pressureSensorListen = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {}

            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if(listener != null) listener.onPressure(sensorEvent.values[0]);
            }
        };
        startSensors();
    }

    @Override
    public void startSensors() {
        if(pressureSensor != null) sm.registerListener(pressureSensorListen, pressureSensor, SensorManager.SENSOR_DELAY_GAME);
        if(tempSensor != null) sm.registerListener(tempSensorListen, tempSensor, SensorManager.SENSOR_DELAY_GAME);
        if(umiditySensor != null) sm.registerListener(umiditySensorListen, umiditySensor, SensorManager.SENSOR_DELAY_GAME);
    }
    @Override
    public void stopSensors() {
        if(pressureSensor != null) sm.unregisterListener(pressureSensorListen, pressureSensor);
        if(tempSensor != null) sm.unregisterListener(tempSensorListen, tempSensor);
        if(umiditySensor != null) sm.unregisterListener(umiditySensorListen, umiditySensor);
    }
    @Override
    public void setListener(Object listener) {
        this.listener = (AmbientStatusListener) listener;
    }
}
