package com.arco2121.swissy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.hardware.*;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Layout;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.arco2121.swissy.Managers.*;
import com.arco2121.swissy.Tools.AmbientStatus.AmbientStatus;
import com.arco2121.swissy.Tools.GeoCompass.*;
import com.arco2121.swissy.Tools.Livella.*;
import com.arco2121.swissy.Tools.Torch.*;
import com.arco2121.swissy.Utility.LogPrinter;
import com.arco2121.swissy.Utility.SharedObjects;
import com.arco2121.swissy.Utility.SwipeDetect;
import com.google.android.gms.location.*;

import org.w3c.dom.Text;

import java.util.List;

public class Main extends AppCompatActivity implements GeoCompassListener, TorchListener, LivellaListener{
    private PermissionManager permissionManager;
    private LocationProvider locationManager;
    private SensorManager sensors;
    private CameraManager camera;
    private GeoCompass compass = null;
    private AmbientStatus ambientStatus = null;
    private Livella livella = null;
    private Torch torch = null;
    String[] availableTools;
    int indexTool = 0;
    private float currentNorth = 0f;
    private float previousNeedleAzimuth = 0f;
    private boolean lastTorchState = false;
    private float lastBrightnessSet = -1f;

    //App
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        //Initialize components
        ImageButton settings = findViewById(R.id.settingsBt);
        ImageButton currentButton = findViewById(R.id.currentstatus);
        TextView title = findViewById(R.id.currentstatuslabel);
        FrameLayout currentTool = findViewById(R.id.currentTool);
        //
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        camera = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        permissionManager = new PermissionManager(this);
        locationManager = new LocationProvider(LocationServices.getFusedLocationProviderClient(Main.this), (LocationManager) getSystemService(Context.LOCATION_SERVICE));
        locationManager.getLocation(permissionManager, locationRequested -> {
            try { ambientStatus = (AmbientStatus) SharedObjects.addObj("Status" , new AmbientStatus(sensors),  new Object[]{ R.drawable.status, R.layout.compass_view }); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { compass = (GeoCompass) SharedObjects.addObj("Compass", new GeoCompass(sensors, locationRequested), new Object[]{ R.drawable.compass, R.layout.compass_view }); compass.setListener(this);} catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { livella = (Livella) SharedObjects.addObj("Livella" , new Livella(sensors),  new Object[]{ R.drawable.livella, R.layout.livella_view }); livella.setListener(this);} catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { torch = (Torch) SharedObjects.addObj("Torch" , new Torch(sensors, camera),  new Object[]{ R.drawable.torch, R.layout.torch_view }); torch.setListener(this); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            availableTools = SharedObjects.asArray();
            Object[] toolPropriety = (Object[]) SharedObjects.getObj(availableTools[indexTool])[1];
            int img = (int)toolPropriety[0];
            int view = (int)toolPropriety[1];
            currentButton.setImageResource(img);
            title.setText(availableTools[indexTool]);
            currentTool.removeAllViews();
            View v = getLayoutInflater().inflate(view, currentTool, false);
            setupSpecificButtons(v);
            currentTool.addView(v);
        });
        //UI Components
        float scale = (float) getResources().getInteger(R.integer.scaleMinus) / 100;
        long duration = getResources().getInteger(R.integer.icon_duration);
        float scale_long = (float) getResources().getInteger(R.integer.scalePlus) / 100;
        float scale_long_log = (scale_long * 3) / 2.5f;
        long duration_long = getResources().getInteger(R.integer.icon_duration_long);
        SwipeDetect currentToolSelect = new SwipeDetect(this, 300, () -> {
            if(indexTool + 1 < availableTools.length) indexTool++; else return;
            Object[] toolPropriety = (Object[]) SharedObjects.getObj(availableTools[indexTool])[1];
            int img = (int)toolPropriety[0];
            int view = (int)toolPropriety[1];
            currentButton.setImageResource(img);
            currentTool.removeAllViews();
            View v = getLayoutInflater().inflate(view, currentTool, false);
            currentTool.addView(v);
            setupSpecificButtons(v);
            title.setText(availableTools[indexTool]);
        }, () -> {
            if(indexTool - 1 >= 0) indexTool--; else return;
            Object[] toolPropriety = (Object[]) SharedObjects.getObj(availableTools[indexTool])[1];
            int img = (int)toolPropriety[0];
            int view = (int)toolPropriety[1];
            currentButton.setImageResource(img);
            currentTool.removeAllViews();
            View v = getLayoutInflater().inflate(view, currentTool, false);
            currentTool.addView(v);
            setupSpecificButtons(v);
            title.setText(availableTools[indexTool]);
        });
        settings.setOnTouchListener((v, event) -> animateButton(v, event, scale, scale, duration, () -> {}, true));
        currentButton.setOnTouchListener((v, event) -> {
            currentToolSelect.detector.onTouchEvent(event);
            return animateButton(v, event, scale_long, scale_long_log, duration_long, () -> {}, false);
        });
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (compass != null) compass.stopSensors();
        if (livella != null) livella.stopSensors();
        if (torch != null) {
            torch.useTorch(this,false);
            torch.stopSensors();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (compass != null) compass.startSensors();
        if (livella != null) livella.startSensors();
        if (torch != null) torch.startSensors();
    }

    //UI
    boolean animateButton(View window, MotionEvent event, float littleX, float littleY, long duration, Runnable func, boolean consumeEvent) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                window.animate().scaleX(littleX).scaleY(littleY).setDuration(duration).start();
                break;

            case MotionEvent.ACTION_UP:

            case MotionEvent.ACTION_CANCEL:
                window.animate().scaleX(1f).scaleY(1f).setDuration(duration)
                        .withEndAction(func)
                        .start();
                break;
        }
        return consumeEvent;
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupSpecificButtons(View toolView) {
        float scale = (float) getResources().getInteger(R.integer.scaleMinus) / 100;
        long duration = getResources().getInteger(R.integer.icon_duration);
        TextView setTarget = toolView.findViewById(R.id.compassSetTarget);
        TextView doCalibration = toolView.findViewById(R.id.compassRecalibrate);
        ImageView torchIco = toolView.findViewById(R.id.torch_toggle);
        TextView torchMode = toolView.findViewById(R.id.torch_mode);
        TextView resetLi = toolView.findViewById(R.id.livellaReset);
        TextView doCalibrationLive = toolView.findViewById(R.id.livellaRecalibrate);
        if (setTarget != null && doCalibration != null) {
            setTarget.setOnTouchListener((view, event) -> animateButton(view, event, scale, scale, duration, () -> {
                if(compass.isCalibrating) return;
                if (!compass.isCustomNorthActive()) {
                    compass.setCustomNorth(currentNorth);
                    setTarget.setText("Clear Target");
                } else {
                    compass.clearCustomNorth();
                    setTarget.setText("Set Target");
                }
            }, true));
            doCalibration.setOnTouchListener((view, event) -> animateButton(view, event, scale, scale, duration, () -> {
                if(compass.isCalibrating) return;
                doCalibration.setText("Calibrating");
                compass.calibration = true;
            }, true));
        }
        if(torchIco != null) {
            if(torch.toggleTorch)
                torchIco.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.tool)
                ));
            else
                torchIco.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.tool_in)
                ));
            torchMode.setText(String.format("Torch Mode : %s",torch.toggleTorch ? "Auto" : "Off"));
            torchIco.setOnTouchListener((view, event) -> animateButton(view, event, scale, scale, duration, () -> {
                if(torch.toggleTorch) {
                    torchIco.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.tool_in)
                    ));
                    torch.useTorch(this, false);
                }
                else
                    torchIco.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.tool)
                    ));
                torch.toggleTorch = !torch.toggleTorch;
                torchMode.setText(String.format("Torch Mode : %s",torch.toggleTorch ? "Auto" : "Off"));
            }, true));
        }
        if(doCalibrationLive != null && resetLi != null) {
            doCalibrationLive.setOnTouchListener((view, event) -> animateButton(view, event, scale, scale, duration, () -> {
                livella.calibrate();
            }, true));
            resetLi.setOnTouchListener((view, event) -> animateButton(view, event, scale, scale, duration, () -> {
                livella.calibrate();
            }, true));
        }
    }

    //Compass
    @Override
    public void onCompassUpdate(float magneticAzimuth, float oldValue, float trueAzimuth, float nidleRotation) {
        try {
            TextView degree = findViewById(R.id.compassDegree);
            TextView cord = findViewById(R.id.compassCord);
            double lat = locationManager.location.getLatitude();
            double lon = locationManager.location.getLongitude();
            cord.setText(String.format("LAT %.2f\nLON %.2f", lat, lon));
            if(compass.isCalibrating) {
                return;
            }
            ImageView compassNidle = findViewById(R.id.compass_nidle);
            View[] compassGrid = {
                    findViewById(R.id.compass_letter),
                    findViewById(R.id.compass_out),
                    findViewById(R.id.compass_tags)
            };
            String direction = GeoCompass.getDirectionRange(trueAzimuth);
            RotateAnimation rotateAnimation = new RotateAnimation(-oldValue, -magneticAzimuth, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(R.integer.icon_duration);
            rotateAnimation.setFillAfter(true);
            RotateAnimation rotateNidleAnimation;
            if(compass.isCustomNorthActive()) {
                rotateNidleAnimation = new RotateAnimation(-previousNeedleAzimuth, -nidleRotation, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                degree.setText(String.format("%.0f°\n(%.1f° %s)", nidleRotation, trueAzimuth, direction));
                previousNeedleAzimuth = nidleRotation;
                compassGrid[1].startAnimation(rotateAnimation);
            } else {
                rotateNidleAnimation = new RotateAnimation(-previousNeedleAzimuth, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                degree.setText(String.format("%.0f° %s\n(%.2f°)", magneticAzimuth, direction, trueAzimuth));
                previousNeedleAzimuth = previousNeedleAzimuth > 180 && previousNeedleAzimuth < 360 ? previousNeedleAzimuth + 10 : previousNeedleAzimuth < 180 && previousNeedleAzimuth > 0 ? previousNeedleAzimuth - 10 : 0f;
                for (View imageView : compassGrid) {
                    imageView.startAnimation(rotateAnimation);
                }
            }
            rotateNidleAnimation.setDuration(R.integer.icon_duration);
            rotateNidleAnimation.setFillAfter(true);
            compassNidle.startAnimation(rotateNidleAnimation);
            currentNorth = magneticAzimuth;
        } catch(Exception ignored) {}
    }

    @Override
    public void onMagneticInterference(float strength, int level) {

    }

    @Override
    public void onCalibrationStart() {
        try {
            ImageView compassNidle = findViewById(R.id.compass_nidle);
            TextView degree = findViewById(R.id.compassDegree);
            View[] compassGrid = {
                    findViewById(R.id.compass_letter),
                    findViewById(R.id.compass_out),
                    findViewById(R.id.compass_tags)
            };
            RotateAnimation rotateAnimation = new RotateAnimation(previousNeedleAzimuth, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(R.integer.icon_duration);
            rotateAnimation.setFillAfter(true);
            for (View imageView : compassGrid) {
                imageView.startAnimation(rotateAnimation);
            }
            rotateAnimation = new RotateAnimation(previousNeedleAzimuth, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(R.integer.icon_duration);
            rotateAnimation.setFillAfter(true);
            compassNidle.startAnimation(rotateAnimation);
            degree.setText("---");
        } catch(Exception ignored) {}
    }

    @Override
    public void onCalibrationEnd() {
        try {
            TextView doCalibration = findViewById(R.id.compassRecalibrate);
            compass.calibration = false;
            doCalibration.setText("Recalibrate");
        } catch(Exception ignored) {}
    }

    @Override
    public void onAccuracy(String message) {
        TextView doCalibration = findViewById(R.id.compassRecalibrate);
        doCalibration.setText("Calibrating");
        compass.calibration = true;
        TextView degree = findViewById(R.id.compassDegree);
        degree.setText(message);
    }

    //Torch
    @Override
    public void onTorchMoment(float brightness, float lux, boolean torchOn) {
        try {
            TextView lum = findViewById(R.id.torch_lum);
            lum.setText(String.format("Brightness:\n%.2f lux", lux));
            if (!torch.toggleTorch) {
                if (lastTorchState || lastBrightnessSet != -1f) {
                    torch.setBrightness(this, -1);
                    torch.useTorch(this, false);
                    lastTorchState = false;
                    lastBrightnessSet = -1f;
                }
                return;
            }
            boolean brightnessChanged = Math.abs(brightness - lastBrightnessSet) > 0.05f;
            boolean torchStateChanged = torchOn != lastTorchState;
            if (brightnessChanged) {
                torch.setBrightness(this, brightness);
                lastBrightnessSet = brightness;
            }
            if (torchStateChanged || (torchOn && brightnessChanged)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    torch.useTorchByBrightness(this, brightness, torchOn);
                } else {
                    torch.useTorch(this, torchOn);
                }
                lastTorchState = torchOn;
            }
        } catch(Exception ignored) {}
    }

    //Livella
    @Override
    public void onLevelChange(float rotation, float pitch, float roll) {
        try {
            TextView xRai = findViewById(R.id.livellaPi);
            TextView yRai = findViewById(R.id.livellaRo);
            TextView Azim = findViewById(R.id.livellaAzi);
            xRai.setText(String.format("X : %f", pitch));
            yRai.setText(String.format("Y : %f", roll));
            Azim.setText(String.format("Angle : %f", rotation));
            float sensitivity = 8f;
            float newX = (roll * sensitivity);
            float newY = (-pitch * sensitivity);
            View container = findViewById(R.id.livella_cont);
            ImageView ballView = findViewById(R.id.livella_point);
            float maxMovement = (container.getWidth() - ballView.getWidth()) / 2f;
            newX = Math.max(-maxMovement, Math.min(newX, maxMovement));
            newY = Math.max(-maxMovement, Math.min(newY, maxMovement));
            ballView.setTranslationX(newX);
            ballView.setTranslationY(newY);
        } catch (Exception ignored) {}
    }
}