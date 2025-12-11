package com.arco2121.swissy.Tools.Impact;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import com.arco2121.swissy.Tools.ToolStructure;

public class Impact implements ToolStructure {

    private final SensorManager sensorManager;
    private final Sensor linearAccSensor;
    private final Sensor accelerometerSensor;
    private static final float GRAVITY = 9.81f;
    private ImpactListener listener;
    private boolean usingLinearAcceleration = false;
    private float baseline = 0f;
    private float noiseLevel = 0f;
    private boolean isCalibrated = false;
    private long calibrationStartTime = 0;
    private static final long CALIBRATION_DURATION_MS = 2000;
    private static final int MIN_CALIBRATION_SAMPLES = 50;
    private int calibrationSampleCount = 0;
    private float calibrationSum = 0f;
    private float calibrationSumOfSquares = 0f;
    private float smoothedValue = 0f;
    private static final float SMOOTHING_FACTOR = 0.7f;
    private long lastImpactTimestamp = 0;
    private static final long MIN_IMPACT_INTERVAL_MS = 300;
    private static final int BUFFER_SIZE = 8;
    private final float[] valueBuffer = new float[BUFFER_SIZE];
    private int bufferIndex = 0;
    private boolean isBufferFilled = false;
    private static final float SIGMA_MULTIPLIER = 3.5f;
    private static final float MIN_ABSOLUTE_THRESHOLD = 0.4f;
    private static final float MAX_INTENSITY = 100f;
    private enum State { IDLE, CALIBRATING, READY }
    private State currentState = State.IDLE;
    private final SensorEventListener sensorListener;

    public Impact(SensorManager sensorManager) throws Exception {
        this.sensorManager = sensorManager;
        this.linearAccSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (linearAccSensor == null && accelerometerSensor == null) {
            throw new Exception("No acceleration sensor available on this device");
        }
        sensorListener = new SensorEventListener() {
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) { }

            @Override
            public void onSensorChanged(SensorEvent event) {
                handleSensorData(event);
            }
        };
    }

    private void handleSensorData(SensorEvent event) {
        float rawMagnitude = calculateAccelerationMagnitude(event);
        smoothedValue = smoothedValue * SMOOTHING_FACTOR + rawMagnitude * (1 - SMOOTHING_FACTOR);

        long currentTime = System.currentTimeMillis();

        switch (currentState) {
            case CALIBRATING:
                processCalibration(rawMagnitude, currentTime);
                break;
            case READY:
                processImpactDetection(rawMagnitude, currentTime);
                break;
            case IDLE:
                break;
        }
    }

    private float calculateAccelerationMagnitude(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float magnitude = (float) Math.sqrt(x * x + y * y + z * z);

        if (!usingLinearAcceleration) {
            magnitude = Math.abs(magnitude - GRAVITY);
        }

        return magnitude;
    }

    private void processCalibration(float value, long currentTime) {
        if (calibrationSampleCount == 0) {
            calibrationStartTime = currentTime;
        }

        calibrationSum += value;
        calibrationSumOfSquares += value * value;
        calibrationSampleCount++;

        if (calibrationSampleCount % 10 == 0 && listener != null) {
            long elapsed = currentTime - calibrationStartTime;
            float progress = Math.min(100f, (elapsed * 100f) / CALIBRATION_DURATION_MS);
            listener.onCalibrationProgress(progress);
        }

        if (calibrationSampleCount >= MIN_CALIBRATION_SAMPLES &&
                (currentTime - calibrationStartTime) >= CALIBRATION_DURATION_MS) {
            finalizeCalibration();
        }
    }
    private void finalizeCalibration() {
        baseline = calibrationSum / calibrationSampleCount;

        float mean = baseline;
        float variance = (calibrationSumOfSquares / calibrationSampleCount) - (mean * mean);
        noiseLevel = (float) Math.sqrt(Math.max(0, variance));

        if (noiseLevel < 0.05f) {
            noiseLevel = 0.1f;
        }

        isCalibrated = true;
        currentState = State.READY;

        if (listener != null) {
            listener.onCalibrated();
        }
    }
    private void processImpactDetection(float value, long currentTime) {
        if (!isCalibrated) return;

        valueBuffer[bufferIndex] = value;
        bufferIndex = (bufferIndex + 1) % BUFFER_SIZE;

        if (bufferIndex == 0 && !isBufferFilled) {
            isBufferFilled = true;
        }

        if (!isBufferFilled) return;
        float peakValue = findPeakInBuffer();

        float dynamicThreshold = baseline + (noiseLevel * SIGMA_MULTIPLIER);
        float finalThreshold = Math.max(dynamicThreshold, MIN_ABSOLUTE_THRESHOLD);

        if (currentTime - lastImpactTimestamp < MIN_IMPACT_INTERVAL_MS) {
            return;
        }
        if (peakValue > finalThreshold) {
            triggerImpactDetection(peakValue, currentTime);
        }
    }

    private float findPeakInBuffer() {
        float maxValue = 0f;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            if (valueBuffer[i] > maxValue) {
                maxValue = valueBuffer[i];
            }
        }
        return maxValue;
    }

    private void triggerImpactDetection(float peakValue, long currentTime) {
        float magnitude = peakValue - baseline;

        float normalizedIntensity = (magnitude / noiseLevel) * 10f;
        float intensity = Math.min(MAX_INTENSITY, normalizedIntensity);

        lastImpactTimestamp = currentTime;

        if (listener != null) {
            listener.onImpact(intensity, magnitude, peakValue);
        }

        clearBuffer();
    }

    private void clearBuffer() {
        for (int i = 0; i < BUFFER_SIZE; i++) {
            valueBuffer[i] = 0f;
        }
        isBufferFilled = false;
        bufferIndex = 0;
    }


    @Override
    public void startSensors() {
        Sensor sensorToUse;
        int sensorDelay = SensorManager.SENSOR_DELAY_GAME;

        if (linearAccSensor != null) {
            sensorToUse = linearAccSensor;
            usingLinearAcceleration = true;
        } else {
            sensorToUse = accelerometerSensor;
            usingLinearAcceleration = false;
        }
        sensorManager.registerListener( sensorListener, sensorToUse, sensorDelay);
    }

    @Override
    public void stopSensors() {
        sensorManager.unregisterListener(sensorListener);
    }

    @Override
    public void setListener(Object listener) {
        this.listener = (ImpactListener) listener;
    }

    public void start() {
        reset();
        startSensors();
        currentState = State.CALIBRATING;
    }

    public void reset() {
        stopSensors();

        // Reset calibrazione
        baseline = 0f;
        noiseLevel = 0f;
        isCalibrated = false;
        calibrationSampleCount = 0;
        calibrationSum = 0f;
        calibrationSumOfSquares = 0f;
        calibrationStartTime = 0;

        // Reset rilevamento
        smoothedValue = 0f;
        lastImpactTimestamp = 0;
        clearBuffer();

        currentState = State.IDLE;
    }
}