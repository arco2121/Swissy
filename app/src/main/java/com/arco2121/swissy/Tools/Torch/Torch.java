package com.arco2121.swissy.Tools.Torch;

import static com.arco2121.swissy.Utility.SharedObjects.calibrateSensorsDelay;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Handler;
import android.view.WindowManager;
import androidx.annotation.RequiresApi;

import com.arco2121.swissy.Tools.ToolListener;
import com.arco2121.swissy.Utility.LogPrinter;
import com.arco2121.swissy.Tools.ToolStructure;

public class Torch implements ToolStructure {
    private final SensorManager sm;
    private final Sensor brightnessSensor;
    private TorchListener listener;
    private final SensorEventListener brightnessListener;
    private float brightnessLux = -1f;
    private final float TORCH_ON_THRESHOLD = 15f;
    private final float TORCH_OFF_THRESHOLD = 20f;
    private boolean manageTorch = false;
    private boolean isTorchCurrentlyOn = false;
    private long lastTorchChangeTime = 0;
    private final long TORCH_CHANGE_DELAY = 25;
    private final CameraManager cm;
    public int toggleTorch = 0;
    private final Handler pollingHandler = new Handler();
    private final Runnable pollingRunnable;
    private final long POLLING_INTERVAL = 50;
    public static final String[] permissionList = {Manifest.permission.CAMERA};

    public Torch(SensorManager sm, CameraManager cm, Context c) throws Exception {
        this.sm = sm;
        this.cm = cm;
        brightnessSensor = sm.getDefaultSensor(Sensor.TYPE_LIGHT);

        if(brightnessSensor == null) throw new Exception("Torch not available");

        brightnessListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                onBrightness(sensorEvent);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                    brightnessLux = -1f;
                }
            }
        };

        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                if (manageTorch && brightnessLux >= 0) {
                    processBrightnessUpdate(brightnessLux);
                }
                pollingHandler.postDelayed(this, POLLING_INTERVAL);
            }
        };

        startSensors(c);
    }

    @Override
    public void startSensors(Context c) {
        sm.registerListener(brightnessListener, brightnessSensor, calibrateSensorsDelay(c, 0));
        manageTorch = true;
        brightnessLux = -1f;
        isTorchCurrentlyOn = false;
        pollingHandler.post(pollingRunnable);
    }

    @Override
    public void stopSensors() {
        sm.unregisterListener(brightnessListener, brightnessSensor);
        manageTorch = false;
        isTorchCurrentlyOn = false;
        brightnessLux = -1f;
        pollingHandler.removeCallbacks(pollingRunnable);
    }

    @Override
    public void setListener(ToolListener listener) {
        this.listener = (TorchListener) listener;
    }

    private void onBrightness(SensorEvent event) {
        brightnessLux = event.values[0];
    }

    private void processBrightnessUpdate(float currentLux) {
        float brightness = luxToBrightness(currentLux);

        if(!manageTorch) return;

        boolean shouldTorchBeOn;
        if (isTorchCurrentlyOn) {
            shouldTorchBeOn = currentLux < TORCH_OFF_THRESHOLD;
        } else {
            shouldTorchBeOn = currentLux < TORCH_ON_THRESHOLD;
        }

        long currentTime = System.currentTimeMillis();

        if (shouldTorchBeOn != isTorchCurrentlyOn) {
            if (currentTime - lastTorchChangeTime > TORCH_CHANGE_DELAY) {
                isTorchCurrentlyOn = shouldTorchBeOn;
                lastTorchChangeTime = currentTime;

                if(listener != null) {
                    listener.onTorchMoment(brightness, currentLux, isTorchCurrentlyOn);
                }
            }
        } else {
            lastTorchChangeTime = currentTime;

            if (isTorchCurrentlyOn && listener != null) {
                listener.onTorchMoment(brightness, currentLux, true);
            }
        }
    }

    private float luxToBrightness(float lux) {
        lux = Math.max(0, Math.min(lux, 1500));
        float normalized = (float) (Math.log10(lux + 1) / Math.log10(1500 + 1));
        return Math.max(0.05f, Math.min(normalized, 1f));
    }

    public void setBrightness(Activity activity, float brightness) {
        if(activity == null || activity.isFinishing()) return;
        WindowManager.LayoutParams layout = activity.getWindow().getAttributes();
        if(brightness == -1) {
            layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        } else {
            layout.screenBrightness = brightness;
        }
        activity.getWindow().setAttributes(layout);
    }

    public void useTorch(Activity activity, boolean doTorch) {
        if(activity == null) return;
        try {
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics c = cm.getCameraCharacteristics(id);
                Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    cm.setTorchMode(id, doTorch);
                    break;
                }
            }
        } catch (Exception e) {
            LogPrinter.printToast(activity, e.toString());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void useTorchByBrightness(Activity activity, float brightness, boolean doTor) {
        if(activity == null) return;
        try {
            if(!doTor) {
                useTorch(activity, false);
                return;
            }
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics c = cm.getCameraCharacteristics(id);
                Integer lf = c.get(CameraCharacteristics.LENS_FACING);

                if (lf == null || lf != CameraCharacteristics.LENS_FACING_BACK)
                    continue;

                Integer maxStrength = c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);

                if (maxStrength == null) {
                    useTorch(activity, true);
                    return;
                }
                int strength = (int) ((1f - brightness) * maxStrength);
                strength = Math.max(1, Math.min(strength, maxStrength));
                cm.turnOnTorchWithStrengthLevel(id, strength);
                return;
            }
        } catch (Exception e) {
            LogPrinter.printToast(activity, e.toString());
        }
    }

}