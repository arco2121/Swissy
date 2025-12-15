package com.arco2121.swissy.Tools.GeoCompass;

import android.hardware.*;
import android.location.Location;
import android.os.Build;
import android.view.HapticFeedbackConstants;
import android.view.View;

import com.arco2121.swissy.Tools.ToolStructure;
import com.arco2121.swissy.Utility.VibrationMaker;

public class GeoCompass implements ToolStructure {
    private GeoCompassListener listener;
    private final SensorManager sm;
    private final Sensor rotationSensor;
    private final Sensor magneticSensor;
    private final Sensor accelerometer;
    private GeomagneticField geoField;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientation = new float[3];
    private final float[] accelValues = new float[3];
    private final float[] magnetValues = new float[3];
    private long lastShapeTimestamp = 0;
    public boolean isCalibrating = false;
    public boolean calibration = false;
    private SensorEventListener rotationListener;
    private final SensorEventListener magneticListener;
    private final SensorEventListener accelListener;
    private Float customNorth = null;
    private boolean accelReady = false;
    private boolean magnetReady = false;
    private float filteredAzimuth = 0f;
    private boolean aziReady = false;
    public final float ALPHA = 0.03f;
    private int magneticAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
    private int accelAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
    private long lastAccuracyWarning = 0;
    private Float lastTriggered = null;

    public GeoCompass(SensorManager sm, Location location) throws Exception {
        this.sm = sm;
        rotationSensor = sm.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR);
        magneticSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        accelerometer = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if(magneticSensor == null || accelerometer== null) throw new Exception("Compass not available");
        geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis()
        );
        if(rotationSensor != null) {
            rotationListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent event) {
                    getNorth(event);
                }
                @Override
                public void onAccuracyChanged(Sensor sensor, int accuracy) {
                    handleAccuracyChange(sensor, accuracy);
                }
            };
        }
        magneticListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                applyLowPass(event);
                if(rotationSensor == null && accelReady) {
                    getNorth(null);
                    accelReady = false;
                    magnetReady = false;
                }
                getInterference(event);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                handleAccuracyChange(sensor, accuracy);
            }
        };
        accelListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                applyLowPass(event);
                if(rotationSensor == null && magnetReady) {
                    getNorth(null);
                    accelReady = false;
                    magnetReady = false;
                }
                if(calibration) doCalibrate(event);
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                handleAccuracyChange(sensor, accuracy);
            }
        };
        startSensors();
    }

    private void handleAccuracyChange(Sensor sensor, int accuracy) {
        int type = sensor.getType();

        if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticAccuracy = accuracy;
        } else if (type == Sensor.TYPE_ACCELEROMETER) {
            accelAccuracy = accuracy;
        } else if (type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR ||
                type == Sensor.TYPE_ROTATION_VECTOR) {
            magneticAccuracy = accuracy;
            accelAccuracy = accuracy;
        }

        long currentTime = System.currentTimeMillis();

        long ACCURACY_WARNING_DELAY = 6000;
        if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            aziReady = false;
            filteredAzimuth = 0f;
            accelReady = false;
            magnetReady = false;

            if (currentTime - lastAccuracyWarning > ACCURACY_WARNING_DELAY) {
                if (listener != null) {
                    listener.onAccuracy("To much noise");
                }
                lastAccuracyWarning = currentTime;
            }
        } else if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW) {
            if (currentTime - lastAccuracyWarning > ACCURACY_WARNING_DELAY) {
                if (listener != null && !isCalibrating) {
                    listener.onAccuracy("Need recalibration");
                }
                lastAccuracyWarning = currentTime;
            }
        }
    }

    public int getOverallAccuracy() {
        return Math.min(magneticAccuracy, accelAccuracy);
    }

    public boolean isAccurate() {
        return getOverallAccuracy() >= SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM;
    }

    @Override
    public void startSensors() {
        if(rotationSensor != null) {
            sm.registerListener(rotationListener, rotationSensor, SensorManager.SENSOR_DELAY_GAME);
        }
        sm.registerListener(magneticListener, magneticSensor, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(accelListener, accelerometer, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void stopSensors() {
        if(rotationSensor != null) {
            sm.unregisterListener(rotationListener, rotationSensor);
        }
        sm.unregisterListener(magneticListener, magneticSensor);
        sm.unregisterListener(accelListener, accelerometer);
    }

    @Override
    public void setListener(Object listener) {
        this.listener = (GeoCompassListener) listener;
    }

    private void getNorth(SensorEvent event) {
        if (!isAccurate() && !aziReady) {
            return;
        }

        float oldAzi = filteredAzimuth;
        float rawAzimuth;
        if (rotationSensor != null && event != null) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientation);
            rawAzimuth = (float) Math.toDegrees(orientation[0]);
        } else {
            float[] R = new float[9];
            float[] I = new float[9];
            if (!SensorManager.getRotationMatrix(R, I, accelValues, magnetValues))
                return;

            float[] ori = new float[3];
            SensorManager.getOrientation(R, ori);
            rawAzimuth = (float) Math.toDegrees(ori[0]);
        }

        float magnetic = (rawAzimuth + 360) % 360;
        if (!aziReady) {
            filteredAzimuth = magnetic;
            aziReady = true;
        } else {
            float diff = magnetic - filteredAzimuth;
            if (diff > 180f) diff -= 360f;
            if (diff < -180f) diff += 360f;
            filteredAzimuth += ALPHA * diff;
            filteredAzimuth = (filteredAzimuth + 360f) % 360f;
        }
        float trueNorth = correctDeclination(filteredAzimuth);

        float output = (customNorth != null) ? (filteredAzimuth - customNorth + 360f) % 360f : filteredAzimuth;

        if (listener != null)
            listener.onCompassUpdate(filteredAzimuth, oldAzi, trueNorth, output);
    }

    private void getInterference(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float magneticStrength = (float) Math.sqrt(x * x + y * y + z * z);
        float magneticLevel;
        if (magneticStrength < 20) magneticLevel = 0;
        else if (magneticStrength <= 65) magneticLevel = 1;
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

    private void applyLowPass(SensorEvent event) {
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR ||
                type == Sensor.TYPE_ROTATION_VECTOR) {
            return;
        }
        if (type == Sensor.TYPE_ACCELEROMETER) {
            if (!accelReady) {
                System.arraycopy(event.values, 0, accelValues, 0, 3);
                accelReady = true;
                return;
            }
            for (int i = 0; i < 3; i++) {
                accelValues[i] = accelValues[i] + ALPHA * (event.values[i] - accelValues[i]);
            }
            accelReady = true;
            return;
        }
        if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            if (!magnetReady) {
                System.arraycopy(event.values, 0, magnetValues, 0, 3);
                magnetReady = true;
                return;
            }
            for (int i = 0; i < 3; i++) {
                magnetValues[i] = magnetValues[i] + ALPHA * (event.values[i] - magnetValues[i]);
            }
            magnetReady = true;
        }
    }

    public void updateLocation(Location location) {
        geoField = new GeomagneticField(
                (float) location.getLatitude(),
                (float) location.getLongitude(),
                (float) location.getAltitude(),
                System.currentTimeMillis()
        );
    }

    public void setCustomNorth(float northDegrees) {
        customNorth = (northDegrees + 360) % 360;
    }

    public void clearCustomNorth() {
        customNorth = null;
    }

    public boolean isCustomNorthActive() {
        return customNorth != null;
    }
    public void triggerHapticIfCardinal(float angle, View view) {
        float[] cardinals = {0f, 90f, 180f, 270f};
        float threshold = 3f;
        float normalized = (angle % 360 + 360) % 360;

        boolean inZone = false;

        for (float point : cardinals) {
            if (Math.abs(normalized - point) <= threshold) {
                inZone = true;
                if (lastTriggered == null || !lastTriggered.equals(point)) {
                    VibrationMaker.vibrate(view, VibrationMaker.Vibration.High);
                    lastTriggered = point;
                }
                break;
            }
        }
        if (!inZone) {
            lastTriggered = null;
        }
    }
    public static String getDirectionRange(float azimuth) {
        if (azimuth >= 337.5 || azimuth < 22.5) return "N";
        if (azimuth < 67.5) return "NE";
        if (azimuth < 112.5) return "E";
        if (azimuth < 157.5) return "SE";
        if (azimuth < 202.5) return "S";
        if (azimuth < 247.5) return "SO";
        if (azimuth < 292.5) return "O";
        return "NO";
    }
}