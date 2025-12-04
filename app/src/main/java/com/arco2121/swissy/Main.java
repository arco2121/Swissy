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
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.arco2121.swissy.Managers.*;
import com.arco2121.swissy.Tools.AmbientStatus.AmbientStatus;
import com.arco2121.swissy.Tools.GeoCompass.*;
import com.arco2121.swissy.Tools.Livella.*;
import com.arco2121.swissy.Tools.Torch.*;
import com.google.android.gms.location.*;

import java.util.function.Consumer;
import java.util.function.Function;

import kotlin.jvm.internal.Lambda;

public class Main extends AppCompatActivity {
    private PermissionManager permissionManager;
    private LocationProvider locationManager;
    private Location location;
    private SensorManager sensors;
    private CameraManager camera;
    private GeoCompass compass = null;
    private AmbientStatus ambientStatus = null;
    private Livella livella = null;
    private Torch torch = null;

    //App
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
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        camera = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        permissionManager = new PermissionManager(this);
        locationManager = new LocationProvider(LocationServices.getFusedLocationProviderClient(Main.this), (LocationManager) getSystemService(Context.LOCATION_SERVICE));
        locationManager.getLocation(permissionManager, locationRequested -> {
            location = locationRequested;
            try { compass = (GeoCompass) SharedObjects.addObj("compass", new GeoCompass(sensors, location)); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { torch = (Torch) SharedObjects.addObj("torch" , new Torch(sensors, camera)); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { livella = (Livella) SharedObjects.addObj("livella" , new Livella(sensors)); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { ambientStatus = (AmbientStatus) SharedObjects.addObj("status" , new AmbientStatus(sensors)); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            LogPrinter.printToast(this, SharedObjects.asString());
        });
        //UI components
        ImageButton settings = findViewById(R.id.settingsBt);
        settings.setOnTouchListener((v, event) -> {
            animateButton((ImageButton) v, event);
            return false;
        });
    }
    @Override
    protected void onPause() {
        super.onPause();
        if (compass != null) compass.stopSensors();
        if (livella != null) livella.stopSensors();
        if (torch != null) torch.stopSensors();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if (compass != null) compass.startSensors();
        if (livella != null) livella.startSensors();
        if (torch != null) torch.startSensors();
    }

    //UI
    @SuppressLint("ResourceAsColor")
    void animateButton(ImageButton window, MotionEvent event, Consumer<MotionEvent> func) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                window.setImageTintList(ColorStateList.valueOf(R.color.tool_in));
                window.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                window.setImageTintList(ColorStateList.valueOf(R.color.background));
                window.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                window.performClick();
                break;
        }
    }


    //Torch's Data Update
    /*@Override
    public void onTorchMoment(float brightness, boolean turnOn) {
        if (!torch.toggleTorch) {
            torch.setBrightness(this, -1);
            torch.useTorch(this, false);
            return;
        }
        torch.setBrightness(this, brightness);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            torch.useTorchByBrightness(this, brightness);
            return;
        }
        torch.useTorch(this, turnOn);
    }*/
}