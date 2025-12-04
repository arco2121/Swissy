package com.arco2121.swissy.Tools.Torch;

import android.Manifest;
import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.view.WindowManager;
import androidx.annotation.RequiresApi;

import com.arco2121.swissy.Utility.LogPrinter;
import com.arco2121.swissy.Tools.ToolStructure;

public class Torch implements ToolStructure {
    private final SensorManager sm;
    private final Sensor brightnessSensor;
    private TorchListener listener;
    private SensorEventListener brightnessListener;
    private float brightnessLux = 0f;
    private final float TORCH_ON_THRESHOLD = 20f;
    private final float SMOOTHING = 0.15f;
    private boolean torchOn = false;
    private boolean manageTorch = false;
    private final CameraManager cm;
    public boolean toggleTorch = false;
    public static final String[] permissionList = {Manifest.permission.CAMERA};

    public Torch(SensorManager sm, CameraManager cm) throws Exception {
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
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
        startSensors();
    }
    @Override
    public void startSensors() {
        sm.registerListener(brightnessListener,brightnessSensor, SensorManager.SENSOR_DELAY_GAME);
        manageTorch = true;
    }
    @Override
    public void stopSensors() {
        sm.unregisterListener(brightnessListener,brightnessSensor);
        manageTorch = false;
    }
    @Override
    public void setListener(Object listener) {
        this.listener = (TorchListener) listener;
    }

    private void onBrightness(SensorEvent event) {
        float lux = event.values[0];
        if (brightnessLux < 0) brightnessLux = lux;
        brightnessLux = brightnessLux + SMOOTHING * (lux - brightnessLux);
        float brightness = luxToBrightness(brightnessLux);
        if(!manageTorch) return;
        if(listener != null) listener.onTorchMoment(brightness, brightnessLux < TORCH_ON_THRESHOLD);
    }
    private float luxToBrightness(float lux) {
        lux = Math.max(0, Math.min(lux, 1500));
        float normalized = (float) (Math.log10(lux + 1) / Math.log10(1500 + 1));
        return Math.max(0.05f, Math.min(normalized, 1f));
    }

    public void setBrightness(Activity activity, float brightness) {
        WindowManager.LayoutParams layout = activity.getWindow().getAttributes();
        if(brightness == -1) {
            layout.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
            return;
        }
        layout.screenBrightness = brightness;
        activity.getWindow().setAttributes(layout);
    }
    public void useTorch(Activity activity, boolean doTorch) {
        try {
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics c = cm.getCameraCharacteristics(id);
                Integer lensFacing = c.get(CameraCharacteristics.LENS_FACING);
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    cm.setTorchMode(id,doTorch);
                    break;
                }
            }
        } catch (Exception e) {
            LogPrinter.printToast(activity,e.toString());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    public void useTorchByBrightness(Activity activity, float brightness) {
        try {
            for (String id : cm.getCameraIdList()) {
                CameraCharacteristics c = cm.getCameraCharacteristics(id);
                Integer lf = c.get(CameraCharacteristics.LENS_FACING);
                if (lf == null || lf != CameraCharacteristics.LENS_FACING_BACK)
                    continue;
                Integer maxStrength = c.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL);
                if (maxStrength == null) {
                    useTorch(activity, brightness < 0.2f);
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
