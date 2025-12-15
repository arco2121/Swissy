package com.arco2121.swissy.Tools.Livella;

import android.hardware.*;
import android.view.View;

import androidx.annotation.NonNull;

import com.arco2121.swissy.Tools.ToolStructure;
import com.arco2121.swissy.Utility.VibrationMaker;

public class Livella implements ToolStructure {
    private final SensorManager sensorManager;
    private final Sensor rotationSensor;
    private LivellaListener listener;
    private final SensorEventListener rotationListener;
    private float pitch = 0f;
    private float roll = 0f;
    private float azimuth = 0f;
    private float pitchOffset = 0f;
    private float rollOffset = 0f;
    private float azimuthOffset = 0f;
    public float smoothness = 0.2f;
    private static final float RAD_TO_DEG = (float) (180.0 / Math.PI);
    private boolean isLevelTriggered = false;

    public Livella(@NonNull SensorManager sm) throws Exception {
        this.sensorManager = sm;
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        if(rotationSensor == null) throw new Exception("Livella not available");
        rotationListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                getLevel(event);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        startSensors();
    }

    @Override
    public void startSensors() {
        if(rotationSensor != null) sensorManager.registerListener(rotationListener, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void stopSensors() {
        if(rotationSensor != null) sensorManager.unregisterListener(rotationListener, rotationSensor);
    }

    @Override
    public void setListener(Object listener) {
        this.listener = (LivellaListener) listener;
    }

    public void calibrate() {
        pitchOffset = pitch;
        rollOffset = roll;
        azimuthOffset = azimuth;
    }

    public void resetCalibration() {
        pitchOffset = 0f;
        rollOffset = 0f;
        azimuthOffset = 0f;
    }

    public float getCalibratedPitch() {
        return pitch - pitchOffset;
    }

    public float getCalibratedRoll() {
        return roll - rollOffset;
    }

    public float getCalibratedAzimuth() {
        return normalizeAngle(azimuth - azimuthOffset);
    }

    private float normalizeAngle(float angle) {
        while (angle > 180f) angle -= 360f;
        while (angle < -180f) angle += 360f;
        return angle;
    }

    public void triggerHapticIfLevel(float x, float y, View view) {
        float threshold = 0.5f;
        if (Math.abs(x) <= threshold || Math.abs(y) <= threshold) {
            if (!isLevelTriggered) {
                VibrationMaker.vibrate(view, VibrationMaker.Vibration.High);
                isLevelTriggered = true;
            }
        } else {
            isLevelTriggered = false;
        }
    }
    private void getLevel(SensorEvent event) {
        if (event == null) return;
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        float[] orientation = new float[3];
        SensorManager.getOrientation(rotationMatrix, orientation);

        float newAzimuth = orientation[0] * RAD_TO_DEG;
        float newPitch = orientation[1] * RAD_TO_DEG;
        float newRoll  = orientation[2] * RAD_TO_DEG;

        azimuth = azimuth * (1 - smoothness) + newAzimuth * smoothness;
        pitch   = pitch   * (1 - smoothness) + newPitch   * smoothness;
        roll    = roll    * (1 - smoothness) + newRoll    * smoothness;

        if(listener != null) {
            listener.onLevelChange(
                    getCalibratedAzimuth(),
                    getCalibratedPitch(),
                    getCalibratedRoll()
            );
        }
    }
}