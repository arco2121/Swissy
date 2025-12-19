package com.arco2121.swissy;

import static com.arco2121.swissy.Utility.SharedObjects.animateButton;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.hardware.*;
import android.hardware.camera2.CameraManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.arco2121.swissy.Managers.*;
import com.arco2121.swissy.Tools.AmbientStatus.AmbientNoise;
import com.arco2121.swissy.Tools.AmbientStatus.AmbientStatus;
import com.arco2121.swissy.Tools.AmbientStatus.AmbientStatusListener;
import com.arco2121.swissy.Tools.GeoCompass.*;
import com.arco2121.swissy.Tools.Livella.*;
import com.arco2121.swissy.Tools.Ruler.*;
import com.arco2121.swissy.Tools.Torch.*;
import com.arco2121.swissy.Tools.Impact.*;
import com.arco2121.swissy.Utility.LogPrinter;
import com.arco2121.swissy.Utility.SharedObjects;
import com.arco2121.swissy.Utility.SwipeDetect;
import com.arco2121.swissy.Utility.VibrationMaker;
import com.google.android.gms.location.*;

import java.util.Locale;
import java.util.Objects;

public class Main extends AppCompatActivity implements GeoCompassListener, TorchListener, LivellaListener, ImpactListener, AmbientStatusListener, RulerListener {
    private LocationProvider locationManager;
    private SensorManager sensors;
    private CameraManager camera;
    private GeoCompass compass = null;
    private AmbientStatus ambientStatus = null;
    private AmbientNoise ambientNoice = null;
    private Livella livella = null;
    private Torch torch = null;
    private Impact impact = null;
    private Ruler ruler = null;
    String[] availableTools;
    int indexTool = 0;
    private float currentNorth = 0f;
    private float previousNeedleAzimuth = 0f;
    private boolean lastTorchState = false;
    private float lastBrightnessSet = -1f;
    private boolean blockForCalibration = false;
    private ConstraintSet initialSet;
    private RulerView righelloView;
    private int indexUnit = 0;
    private int prec = 1;
    private float oldpitch = 0f;
    private float oldroll = 0f;

    //App
    @SuppressLint({"ClickableViewAccessibility", "MissingPermission"})
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
        String appName = getString(R.string.app_name);
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        camera = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        PermissionManager permissionManager = new PermissionManager(this);
        locationManager = new LocationProvider(LocationServices.getFusedLocationProviderClient(Main.this), (LocationManager) getSystemService(Context.LOCATION_SERVICE));
        locationManager.getLocation(permissionManager, locationRequested -> {
            try { ambientStatus = (AmbientStatus) SharedObjects.addObj(appName , new AmbientStatus(sensors, this),  new Object[]{ R.drawable.status, R.layout.status_view }); ambientStatus.setListener(this); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { torch = (Torch) SharedObjects.addObj("Torch" , new Torch(sensors, camera, this),  new Object[]{ R.drawable.torch, R.layout.torch_view }); torch.setListener(this); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { compass = (GeoCompass) SharedObjects.addObj("Compass", new GeoCompass(sensors, locationRequested, this), new Object[]{ R.drawable.compass, R.layout.compass_view }); compass.setListener(this);} catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { livella = (Livella) SharedObjects.addObj("Livella" , new Livella(sensors, this),  new Object[]{ R.drawable.livella, R.layout.livella_view }); livella.setListener(this);} catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { impact = (Impact) SharedObjects.addObj("Impact", new Impact(sensors), new Object[]{ R.drawable.impact, R.layout.impact_view}); impact.setListener(this);} catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { ruler = (Ruler) SharedObjects.addObj("Ruler", new Ruler(this), new Object[]{ R.drawable.ruler, R.layout.ruler_view }); ruler.setListener(this);} catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { ambientNoice = new AmbientNoise(this); ambientNoice.setListener(this);} catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            //UI
            ConstraintLayout mai = findViewById(R.id.main);
            ImageButton settings = findViewById(R.id.settingsBt);
            ImageButton currentButton = findViewById(R.id.currentstatus);
            TextView title = findViewById(R.id.currentstatuslabel);
            ImageButton reloadIn = findViewById(R.id.reloadInfo);
            FrameLayout currentTool = findViewById(R.id.currentTool);
            float scale = (float) getResources().getInteger(R.integer.scaleMinus) / 100;
            long duration = getResources().getInteger(R.integer.icon_duration);
            float scale_long = (float) getResources().getInteger(R.integer.scalePlus) / 100;
            float scale_long_log = (scale_long * 2.5f) / 3;
            long duration_long = getResources().getInteger(R.integer.icon_duration_long);
            availableTools = SharedObjects.asArray();
            indexTool = SharedObjects.findIndex(appName);
            Object[] toolProprietyFirst = (Object[]) SharedObjects.getObj(availableTools[indexTool])[1];
            int imgFi = (int)toolProprietyFirst[0];
            int viewFi = (int)toolProprietyFirst[1];
            currentButton.setImageResource(imgFi);
            title.setText(availableTools[indexTool]);
            currentTool.removeAllViews();
            View vFi = getLayoutInflater().inflate(viewFi, currentTool, false);
            currentTool.addView(vFi);
            SwipeDetect currentToolSelect = new SwipeDetect(this, 200, () -> {
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
            }, () -> {
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
            }, SwipeDetect.Direction.HORIZONTAL);
            settings.setOnTouchListener((v, event) -> animateButton(v, event, scale, scale, duration, () -> {
                Intent intent = new Intent(Main.this, Settings.class);
                startActivity(intent);
            }, true));
            reloadIn.setAlpha(0f);
            reloadIn.setOnTouchListener((v, event) -> {
                if(reloadIn.getAlpha() > 0f) {
                    return animateButton(v, event, scale, scale, duration, () -> {
                        try {
                            TextView alti = findViewById(R.id.status_alti);
                            TextView posi = findViewById(R.id.status_pos);
                            locationManager.getLocation(permissionManager, locationRequestedUp -> {
                                compass.updateLocation(locationRequestedUp);
                                try {
                                    posi.setText(String.format("LAT : %.2f   LON : %.2f", locationManager.location.getLatitude(), locationManager.location.getLongitude()));
                                    alti.setText(String.format("Altitude : %.2f m", locationManager.location.getAltitude()));
                                } catch (Exception ignored) {}
                            });
                        } catch (Exception ignored) {}
                    }, true);
                }
                return true;
            });
            currentButton.setOnTouchListener((v, event) -> {
                currentToolSelect.detector.onTouchEvent(event);
                return animateButton(v, event, scale_long, scale_long_log, duration_long, VibrationMaker.Vibration.Long, () -> { },false);
            });
            initialSet = new ConstraintSet();
            initialSet.clone(mai);
            setupSpecificButtons(vFi);
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
        if(impact != null) {
            impact.reset();
        }
        if(ambientStatus != null) {
            ambientStatus.stopSensors();
        }
        if(ambientNoice != null) {
            ambientNoice.stopSensors();
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (compass != null) compass.startSensors(this);
        if (livella != null) livella.startSensors(this);
        if (torch != null) torch.startSensors(this);
        if(impact != null) {
            impact.reset();
        }
        if(ambientStatus != null) {
            ambientStatus.startSensors(this);
        }
        if(ambientNoice != null) {
            ambientNoice.startSensors(this);
        }
    }

    //UI
    @SuppressLint("ClickableViewAccessibility")
    private void setupSpecificButtons(View toolView) {
        ConstraintLayout root = findViewById(R.id.main);
        initialSet.applyTo(root);
        float scale = (float) getResources().getInteger(R.integer.scaleMinus) / 100;
        long duration = getResources().getInteger(R.integer.icon_duration);
        TextView setTarget = toolView.findViewById(R.id.compassSetTarget);
        TextView doCalibration = toolView.findViewById(R.id.compassRecalibrate);
        LinearLayout statusBox = toolView.findViewById(R.id.statusBox);
        LinearLayout statusBox1 = toolView.findViewById(R.id.statusBox1);
        ImageView torchIco = toolView.findViewById(R.id.torch_toggle);
        TextView torchMode = toolView.findViewById(R.id.torch_mode);
        TextView resetLi = toolView.findViewById(R.id.livellaReset);
        TextView doCalibrationLive = toolView.findViewById(R.id.livellaRecalibrate);
        TextView doMesure = toolView.findViewById(R.id.scaleMesure);
        TextView doReset = toolView.findViewById(R.id.scaleTare);
        TextView weight = toolView.findViewById(R.id.weight);
        TextView alti = toolView.findViewById(R.id.status_alti);
        TextView posi = toolView.findViewById(R.id.status_pos);
        ConstraintLayout righello = toolView.findViewById(R.id.ruler);
        TextView changeMode = findViewById(R.id.rulerMode);
        ImageButton reloadIn = findViewById(R.id.reloadInfo);
        Ruler.RulerUnit[] units = {Ruler.RulerUnit.CENTIMETER, Ruler.RulerUnit.MILLIMETER, Ruler.RulerUnit.INCH, Ruler.RulerUnit.PIXEL};
        ruler.setCalibrationFactor(1f);
        reloadIn.setAlpha(0f);
        if(statusBox != null) {
            statusBox.setOnTouchListener((view, event) -> animateButton(view, event, scale, scale, duration, () -> {}, true));
            statusBox1.setOnTouchListener((view, event) -> animateButton(view, event, scale, scale, duration, () -> {}, true));
        }
        if (setTarget != null && doCalibration != null) {
            if (compass.isCustomNorthActive()) {
                setTarget.setText("Clear Target");
            } else {
                setTarget.setText("Set Target");
            }
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
            if(torch.toggleTorch == 0)
                torchIco.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.tool_in)
                ));
            else
                torchIco.setImageTintList(ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.tool)
                ));
            torchMode.setText(String.format("Torch Mode : %s",torch.toggleTorch == 0 ? "Off" : torch.toggleTorch == 1 ? "On" : "Auto"));
            torchIco.setOnTouchListener((view, event) -> animateButton(view, event, scale, scale, duration, () -> {
                torch.toggleTorch = torch.toggleTorch + 1 < 3 ?  torch.toggleTorch + 1 : 0;
                if(torch.toggleTorch == 0) {
                    torchIco.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.tool_in)
                    ));
                    torch.useTorch(this, false);
                }
                else if(torch.toggleTorch == 1)
                    torchIco.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.tool)
                    ));
                else {
                    torchIco.setImageTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(this, R.color.tool)
                    ));
                    torch.useTorch(this, true);
                }
                torchMode.setText(String.format("Torch Mode : %s",torch.toggleTorch == 0 ? "Off" : (torch.toggleTorch == 2 ? "On" : "Auto")));
            }, true));
        } else {
            torch.toggleTorch = 0;
            torch.useTorch(this, false);
            torch.setBrightness(this, -1f);
        }
        if(doCalibrationLive != null && resetLi != null) {
            doCalibrationLive.setOnTouchListener((view, event) -> animateButton(view, event, scale, scale, duration, () -> {
                livella.calibrate();
            }, true));
            resetLi.setOnTouchListener((view, event) -> animateButton(view, event, scale, scale, duration, () -> {
                livella.calibrate();
            }, true));
        }
        if(doMesure != null) {
            weight.setText("---");
            doReset.setOnTouchListener((v, event) -> animateButton(v, event, scale, scale, duration, () -> {
                impact.reset();
                weight.setText("---");
            }, true));
            doMesure.setOnTouchListener((v, event) -> animateButton(v, event, scale, scale, duration, () -> {
                impact.start(Main.this);
                weight.setText("---");
            }, true));
        }
        if(posi != null) {
            reloadIn.setAlpha(1f);
            posi.setText(String.format("LAT : %.2f   LON : %.2f", locationManager.location.getLatitude(), locationManager.location.getLongitude()));
            alti.setText(String.format("Altitude : %.2f m", locationManager.location.getAltitude()));
        }
        if(righello != null) {
            ConstraintSet set = new ConstraintSet();
            set.clone(root);
            set.clear(R.id.currentTool, ConstraintSet.END);
            set.constrainWidth(R.id.currentTool, ConstraintSet.MATCH_CONSTRAINT);
            set.constrainPercentWidth(R.id.currentTool, 0.9f);
            set.applyTo(root);
            righello.removeAllViews();
            righello.post(() -> {
                int heightPx = righello.getHeight();
                float lengthMm = heightPx / (ruler.pxPerMm * ruler.calibrationFactor);
                righelloView = (RulerView) ruler.createRulerView(this, lengthMm, units[indexUnit]);
                ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                        ConstraintLayout.LayoutParams.WRAP_CONTENT,
                        ConstraintLayout.LayoutParams.MATCH_PARENT);
                params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                righelloView.setLayoutParams(params);
                righello.addView(righelloView);
                changeMode.setText(units[indexUnit].toString().toUpperCase(Locale.ROOT));
            });
            float scale_long = (float) getResources().getInteger(R.integer.scaleMinus) / 100;
            long duration_long = getResources().getInteger(R.integer.icon_duration);
            TextView isura = findViewById(R.id.rulerMi);
            TextView maxC = findViewById(R.id.rulerAdd);
            TextView minC = findViewById(R.id.rulerMin);
            isura.setOnClickListener(v -> {
                String before = (String) isura.getText();
                if(Objects.equals(before, "---")) return;
                if(prec + 1 < 9) prec += 1; else prec = 1;
                Float guess = new Float(before.split(units[indexUnit].toString())[0]);
                isura.setText(String.format("%." + prec + "f %s", guess, righelloView.unit));
            });
            changeMode.setOnTouchListener((v, event) -> animateButton(v, event, scale_long, scale_long, duration_long, VibrationMaker.Vibration.Long, () -> {
                indexUnit = indexUnit + 1 < units.length ? indexUnit + 1 : 0;
                righello.removeAllViews();
                isura.setText("---");
                righello.post(() -> {
                    int heightPx = righello.getHeight();
                    float lengthMm = heightPx / (ruler.pxPerMm * ruler.calibrationFactor);
                    righelloView = (RulerView) ruler.createRulerView(this, lengthMm, units[indexUnit]);
                    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.MATCH_PARENT);
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                    righelloView.setLayoutParams(params);
                    righello.addView(righelloView);
                    changeMode.setText(units[indexUnit].toString().toUpperCase(Locale.ROOT));
                });
            },true));
            maxC.setOnTouchListener((v, event) -> animateButton(v, event, scale_long, scale_long, duration_long, VibrationMaker.Vibration.Long, () -> {
                ruler.setCalibrationFactor(ruler.calibrationFactor + 0.1f);
                righello.removeAllViews();
                isura.setText("---");
                righello.post(() -> {
                    int heightPx = righello.getHeight();
                    float lengthMm = heightPx / (ruler.pxPerMm * ruler.calibrationFactor);
                    righelloView = (RulerView) ruler.createRulerView(this, lengthMm, units[indexUnit]);
                    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.MATCH_PARENT);
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                    righelloView.setLayoutParams(params);
                    righello.addView(righelloView);
                    changeMode.setText(units[indexUnit].toString().toUpperCase(Locale.ROOT));
                });
            },true));
            minC.setOnTouchListener((v, event) -> animateButton(v, event, scale_long, scale_long, duration_long, VibrationMaker.Vibration.Long, () -> {
                ruler.setCalibrationFactor(ruler.calibrationFactor - 0.05f);
                righello.removeAllViews();
                isura.setText("---");
                righello.post(() -> {
                    int heightPx = righello.getHeight();
                    float lengthMm = heightPx / (ruler.pxPerMm * ruler.calibrationFactor);
                    righelloView = (RulerView) ruler.createRulerView(this, lengthMm, units[indexUnit]);
                    ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                            ConstraintLayout.LayoutParams.WRAP_CONTENT,
                            ConstraintLayout.LayoutParams.MATCH_PARENT);
                    params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                    params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                    righelloView.setLayoutParams(params);
                    righello.addView(righelloView);
                    changeMode.setText(units[indexUnit].toString().toUpperCase(Locale.ROOT));
                });
            },true));
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
            long duration = getResources().getInteger(R.integer.icon_duration);
            if(compass.isCalibrating || blockForCalibration) {
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
            rotateAnimation.setDuration(duration);
            rotateAnimation.setFillAfter(true);
            RotateAnimation rotateNidleAnimation;
            if(compass.isCustomNorthActive()) {
                rotateNidleAnimation = new RotateAnimation(-previousNeedleAzimuth, -nidleRotation, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                degree.setText(String.format("%.0f°\n(%.0f° %s)", nidleRotation, trueAzimuth, direction));
                previousNeedleAzimuth = nidleRotation;
                compassGrid[1].startAnimation(rotateAnimation);
            } else {
                rotateNidleAnimation = new RotateAnimation(-previousNeedleAzimuth, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                degree.setText(String.format("%.0f° %s\n(%.1f°)", magneticAzimuth, direction, trueAzimuth));
                previousNeedleAzimuth = previousNeedleAzimuth > 180 && previousNeedleAzimuth < 360 ? previousNeedleAzimuth + 10 : previousNeedleAzimuth < 180 && previousNeedleAzimuth > 0 ? previousNeedleAzimuth - 10 : 0f;
                for (View imageView : compassGrid) {
                    imageView.startAnimation(rotateAnimation);
                }
            }
            rotateNidleAnimation.setDuration(duration);
            rotateNidleAnimation.setFillAfter(true);
            compassNidle.startAnimation(rotateNidleAnimation);
            currentNorth = magneticAzimuth;
            if(SettingsManager.getPropreties(this).getBoolean("vibration", false)) {
                if(Math.abs(magneticAzimuth - oldValue) >= 1) VibrationMaker.vibrate(cord, VibrationMaker.Vibration.Low);
            }
            if(SettingsManager.getPropreties(this).getBoolean("vibration", false)) compass.triggerHapticIfCardinal(magneticAzimuth, degree);
        } catch(Exception ignored) {}
    }

    @Override
    public void onMagneticInterference(float strength, int level) {
        try {
            TextView result = findViewById(R.id.status_magne);
            result.setText(String.format("Interference : %s (%.0f)", level == 0 ? "Neutral" : level == 1 ? "Low" : level == 2 ? "Medium" : "High", strength));
        } catch (Exception ignored) { }
    }

    @Override
    public void onCalibrationStart() {
        try {
            long duration = getResources().getInteger(R.integer.icon_duration);
            ImageView compassNidle = findViewById(R.id.compass_nidle);
            blockForCalibration = false;
            TextView degree = findViewById(R.id.compassDegree);
            View[] compassGrid = {
                    findViewById(R.id.compass_letter),
                    findViewById(R.id.compass_out),
                    findViewById(R.id.compass_tags)
            };
            RotateAnimation rotateAnimation = new RotateAnimation(previousNeedleAzimuth, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(duration);
            rotateAnimation.setFillAfter(true);
            for (View imageView : compassGrid) {
                imageView.startAnimation(rotateAnimation);
            }
            rotateAnimation = new RotateAnimation(previousNeedleAzimuth, 180, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            rotateAnimation.setDuration(duration);
            rotateAnimation.setFillAfter(true);
            compassNidle.startAnimation(rotateAnimation);
            if(SettingsManager.getPropreties(this).getBoolean("vibration", false)) VibrationMaker.vibrate(degree, VibrationMaker.Vibration.High);
            degree.setText("---");
        } catch(Exception ignored) {}
    }

    @Override
    public void onCalibrationEnd() {
        try {
            TextView doCalibration = findViewById(R.id.compassRecalibrate);
            compass.calibration = false;
            if(SettingsManager.getPropreties(this).getBoolean("vibration", false)) VibrationMaker.vibrate(doCalibration, VibrationMaker.Vibration.ReverseHigh);
            doCalibration.setText("Recalibrate");
        } catch(Exception ignored) {}
    }

    @Override
    public void onAccuracy(String message) {
        try{
            TextView doCalibration = findViewById(R.id.compassRecalibrate);
            doCalibration.setText("Need Calibration");
            blockForCalibration = true;
            TextView degree = findViewById(R.id.compassDegree);
            degree.setText(message);
        } catch (Exception ignored) { }
    }

    //Torch
    @Override
    public void onTorchMoment(float brightness, float lux, boolean torchOn) {
        try {
            TextView lum = findViewById(R.id.torch_lum);
            lum.setText(String.format("Brightness:\n%.2f lux", lux));
            if (torch.toggleTorch == 0) {
                if (lastTorchState || lastBrightnessSet != -1f) {
                    torch.setBrightness(this, -1);
                    torch.useTorch(this, false);
                    lastTorchState = false;
                    lastBrightnessSet = -1f;
                }
                return;
            }
            if(torch.toggleTorch == 2) {
                if (lastTorchState || lastBrightnessSet != -1f) {
                    torch.setBrightness(this, -1);
                    torch.useTorch(this, true);
                    lastTorchState = false;
                    lastBrightnessSet = -1f;
                }
                return;
            }
            if(SettingsManager.getPropreties(this).getBoolean("vibration", false)) VibrationMaker.vibrate(lum, VibrationMaker.Vibration.Low);
            boolean brightnessChanged = Math.abs(brightness - lastBrightnessSet) > 0.05f;
            boolean torchStateChanged = torchOn != lastTorchState;
            if (brightnessChanged) {
                torch.setBrightness(this, brightness);
                lastBrightnessSet = brightness;
            }
            if (torchStateChanged || (torchOn && brightnessChanged)) {
                if(SettingsManager.getPropreties(this).getBoolean("vibration", false)) VibrationMaker.vibrate(lum, VibrationMaker.Vibration.High);
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
            xRai.setText(String.format("X : %.1f", pitch));
            yRai.setText(String.format("Y : %.1f", roll));
            Azim.setText(String.format("Angle : %.0f", rotation));
            float sensitivity = 8f;
            float newX = (roll * sensitivity);
            float newY = (-pitch * sensitivity);
            View container = findViewById(R.id.livella_cont);
            ImageView ballView = findViewById(R.id.livella_point);
            if((Math.abs(roll - oldroll) > 0.5 || Math.abs(pitch - oldpitch) > 0.5) && SettingsManager.getPropreties(this).getBoolean("vibration", false)) VibrationMaker.vibrate(container, VibrationMaker.Vibration.Low);
            if(SettingsManager.getPropreties(this).getBoolean("vibration", false)) livella.triggerHapticIfLevel(pitch, roll, container);
            float maxMovement = (container.getWidth() - ballView.getWidth()) / 2f;
            newX = Math.max(-maxMovement, Math.min(newX, maxMovement));
            newY = Math.max(-maxMovement, Math.min(newY, maxMovement));
            ballView.setTranslationX(newX);
            ballView.setTranslationY(newY);
            oldpitch = pitch;
            oldroll = roll;
        } catch (Exception ignored) {}
    }

    //Scale
    @Override
    public void onCalibrated() {
        try {
            TextView result = findViewById(R.id.weight);
            result.setText("Ready");
        } catch (Exception ignore) {}
    }

    @Override
    public void onCalibrationProgress(float value) {
        try {
            TextView result = findViewById(R.id.weight);
            result.setText(String.format("- %.0f -", value));
        } catch (Exception ignore) {}
    }

    @Override
    public void onImpact(float range, float filt, float raw) {
        try{
            TextView result = findViewById(R.id.weight);
            result.setText(String.format("%.1f m/s² (%.0f)", filt, range));
            impact.reset();
            if(SettingsManager.getPropreties(this).getBoolean("vibration", false)) VibrationMaker.vibrate(result, VibrationMaker.Vibration.ReverseHigh);
        } catch (Exception ignore) {}
    }

    //Status
    @Override
    public void onTemperature(float temp) {
        try {
            TextView result = findViewById(R.id.status_temp);
            result.setText(String.format("Temperature : %.1f °C", temp));
        } catch (Exception ignored) { }
    }

    @Override
    public void onUmidity(float umi) {
        try {
            TextView result = findViewById(R.id.status_magne);
            result.setText(String.format("Humidity : %.1f", umi) + "%");
        } catch (Exception ignored) { }
    }

    @Override
    public void onPressure(float pressure) {
        try {
            TextView result = findViewById(R.id.status_magne);
            result.setText(String.format("Pression : %.2f bar", pressure));
        } catch (Exception ignored) { }
    }

    @Override
    public void onNoise(double decibels) {
        try {
            TextView result = findViewById(R.id.status_bri);
            result.setText(String.format("Noise : %.2f db", decibels));
        } catch (Exception ignored) { }
    }

    //Ruler
    @Override
    public void onMeasure(float mm, float cm, float inch, float px) {
        try {
            TextView isura = findViewById(R.id.rulerMi);
            float mesure;
            switch (righelloView.unit) {
                case MILLIMETER: mesure = mm; break;
                case INCH: mesure = inch; break;
                case PIXEL: mesure = px; break;
                default: mesure = cm;
            }
            String out = String.format("%." + prec + "f %s", mesure, righelloView.unit);
            if(SettingsManager.getPropreties(this).getBoolean("vibration", false)) VibrationMaker.vibrate(isura, VibrationMaker.Vibration.Medium);
            isura.setText(out);
        } catch (Exception ignored) { }
    }
}