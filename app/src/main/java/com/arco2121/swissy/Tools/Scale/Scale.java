package com.arco2121.swissy.Tools.Scale;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import com.arco2121.swissy.Tools.ToolStructure;

public class Scale implements ToolStructure {
    private final SensorManager sm;
    private final Sensor pressure;
    private final Sensor linearAcc;
    private final SensorEventListener evento;
    private final Sensor accel;
    private ScaleListener listener;
    private enum State { IDLE, WAIT_OBJECT, DONE }
    private State state = State.IDLE;
    private float baseValue = 0;
    private boolean usingPressure = false;
    private boolean usingLinear = false;
    private float smooth = 0;
    public Scale(SensorManager sm) throws Exception {
        this.sm = sm;
        pressure = sm.getDefaultSensor(Sensor.TYPE_PRESSURE);
        linearAcc = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accel = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (pressure == null && linearAcc == null && accel == null) {
            throw new Exception("Scale not available");
        }

        evento = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }

            @Override
            public void onSensorChanged(SensorEvent e) {
                float raw;
                if (usingPressure) raw = e.values[0];
                else if (usingLinear) {
                    float x = e.values[0];
                    float y = e.values[1];
                    float z = e.values[2];
                    raw = (float) Math.sqrt(x * x + y * y + z * z);
                } else {
                    float x = e.values[0];
                    float y = e.values[1];
                    float z = e.values[2];
                    raw = (float) Math.sqrt(x * x + y * y + z * z);
                }
                smooth = smooth * 0.85f + raw * 0.15f;
                switch(state) {

                    case WAIT_OBJECT:
                        if (baseValue == 0) baseValue = smooth;

                        float threshold = usingPressure ? 0.3f : 0.1f;

                        if (smooth > baseValue + threshold) {
                            float grams = convert(smooth - baseValue);
                            state = State.DONE;
                            listener.onWeightReady(grams);
                        }
                        break;

                    default:
                        break;
                }
            }
        };
    }

    @Override
    public void startSensors() {
        if (pressure != null) {
            usingPressure = true;
            sm.registerListener(evento, pressure, SensorManager.SENSOR_DELAY_FASTEST);
        } else if (linearAcc != null) {
            usingLinear = true;
            sm.registerListener(evento, linearAcc, SensorManager.SENSOR_DELAY_FASTEST);
        } else {
            sm.registerListener(evento, accel, SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    public void stopSensors() {
        if (pressure != null) {
            sm.unregisterListener(evento, pressure);
        } else if (linearAcc != null) {
            sm.unregisterListener(evento, linearAcc);
        } else {
            sm.unregisterListener(evento, accel);
        }
    }

    @Override
    public void setListener(Object listener) {
        this.listener = (ScaleListener) listener;
    }

    public void start() {
        state = State.WAIT_OBJECT;
        startSensors();
    }
    public void reset() {
        stopSensors();
        state = State.IDLE;
        baseValue = 0;
        smooth = 0;
    }
    private float convert(float diff) {
        if (usingPressure) return diff * 100;
        else return diff * 500;
    }
}