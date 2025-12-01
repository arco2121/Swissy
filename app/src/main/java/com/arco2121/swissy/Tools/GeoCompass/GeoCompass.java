package com.arco2121.swissy.Tools.GeoCompass;

import android.hardware.*;
import android.location.Location;

import com.arco2121.swissy.Tools.ToolStructure;

public class GeoCompass implements ToolStructure {
    private GeoCompassListener listener;
    private final SensorManager sm;
    private final Sensor rotationSensor;
    private final Sensor magneticSensor;
    private final Sensor accelerometer;
    private GeomagneticField geoField;
    private float north = 0f;
    private float truenorth = 0f;
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];
    private float magneticStrength = 0f;
    private float magneticLevel = 0f;
    private long lastShapeTimestamp = 0;
    private boolean isCalibrating = false;
    private boolean calibration = false;
    private SensorEventListener rotationListener;
    private SensorEventListener magneticListener;
    private SensorEventListener accelListener;

    public GeoCompass(SensorManager sm, Location location, GeoCompassListener listener) {
        this.sm = sm;
        rotationSensor = sm.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        magneticSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis()
        );
        SensorEventListener rotationListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                getNorth(event);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        SensorEventListener magneticListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                getInterference(event);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        SensorEventListener accelListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if(calibration)
                    doCalibrate(event);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        startSensors();
        this.setListener(listener);
    }
    @Override
    public void startSensors() {
        sm.registerListener(rotationListener, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(magneticListener, magneticSensor, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(accelListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }
    @Override
    public void stopSensors() {
        sm.unregisterListener(rotationListener, rotationSensor);
        sm.unregisterListener(magneticListener, magneticSensor);
        sm.unregisterListener(accelListener, accelerometer);
    }
    @Override
    public void setListener(Object listener) {
        this.listener = (GeoCompassListener) listener;
    }

    private void getNorth(SensorEvent event) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
        SensorManager.getOrientation(rotationMatrix, orientation);
        float rawAzimuth = (float) Math.toDegrees(orientation[0]);
        north = (rawAzimuth + 360) % 360;
        truenorth = correctDeclination(north);
        if (listener != null) {
            listener.onCompassUpdate(north, truenorth);
        }
    }
    private void getInterference(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        magneticStrength = (float) Math.sqrt(x * x + y * y + z * z);
        if (magneticStrength < 20) magneticLevel = 1;
        else if (magneticStrength <= 65) magneticLevel = 0;
        else if (magneticStrength <= 90) magneticLevel = 2;
        else magneticLevel = 3;
        if (listener != null) {
            listener.onMagneticInterference(magneticStrength, (int) magneticLevel);
        }
    }
    private void doCalibrate(SensorEvent event) {
        float ax = event.values[0];
        float ay = event.values[1];
        float az = event.values[2];
        float accelMagnitude = (float) Math.sqrt(ax * ax + ay * ay + az * az);
        if (accelMagnitude > 15) {
            if (!isCalibrating) {
                isCalibrating = true;
                if (listener != null) listener.onCalibrationStart();
            }
            lastShapeTimestamp = System.currentTimeMillis();
        }
        if (isCalibrating && System.currentTimeMillis() - lastShapeTimestamp > 2000) {
            isCalibrating = false;
            if (listener != null) listener.onCalibrationEnd();
        }
    }
    private float correctDeclination(double magneticAzimuth) {
        float decl = geoField.getDeclination();
        return (float)(magneticAzimuth + decl + 360) % 360;
    }

    public void updateLocation(Location location) {
        geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis()
        );
    }
    public void toggleCalibration() {
        calibration = !calibration;
    }
}