package com.arco2121.swissy;

import android.content.Context;
import android.hardware.*;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.annotation.NonNull;
import com.arco2121.swissy.Managers.*;
import com.arco2121.swissy.Tools.AmbientStatus.AmbientStatus;
import com.arco2121.swissy.Tools.GeoCompass.*;
import com.arco2121.swissy.Tools.Livella.*;
import com.arco2121.swissy.Tools.Torch.*;
import com.google.android.gms.location.*;

public class Main extends AppCompatActivity implements GeoCompassListener, LivellaListener, TorchListener {
    private PermissionManager permissionManager;
    private LocationProvider locationManager;
    private Location location;
    private SensorManager sensors;
    private CameraManager camera;
    private GeoCompass compass;
    private AmbientStatus ambientStatus;
    private Livella livella;
    private Torch torch;

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
        permissionManager = new PermissionManager(this);
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        camera = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        permissionManager.requestPermissions(new PermissionManager.Callback() {
            @Override
            public void onGranted() {
                LogPrinter.printToast(Main.this, "Granted");
                locationManager = new LocationProvider(LocationServices.getFusedLocationProviderClient(Main.this), (LocationManager) getSystemService(Context.LOCATION_SERVICE));
                locationManager.getLocation(permissionManager, locationRequested -> {
                    location = locationRequested;
                    compass = new GeoCompass(sensors, location,Main.this);
                });
            }

            @Override
            public void onDenied() {
                if (permissionManager.askAgain(Main.this, LocationProvider.permissionList)) {
                    LogPrinter.printToast(Main.this, "Location required for the app to work");
                    permissionManager.requestPermissions(this, LocationProvider.permissionList);
                } else {
                    LogPrinter.printToast(Main.this, "Enable location permission from Settings");
                    permissionManager.openSettings(Main.this);
                }
            }
        }, LocationProvider.permissionList);
        permissionManager.requestPermissions(new PermissionManager.Callback() {
            @Override
            public void onGranted() {
                torch = new Torch(sensors, camera, Main.this);
            }

            public void onDenied() {
                if (permissionManager.askAgain(Main.this, Torch.permissionList)) {
                    LogPrinter.printToast(Main.this, "Camera required to torn on the torch");
                    permissionManager.requestPermissions(this, Torch.permissionList);
                } else {
                    LogPrinter.printToast(Main.this, "Enable camera permission from Settings");
                    permissionManager.openSettings(Main.this);
                }
            }
        },Torch.permissionList);
        livella = new Livella(sensors, Main.this);
        ambientStatus = new AmbientStatus(sensors);
    }
    @Override
    protected void onPause() {
        super.onPause();
        if(compass != null) compass.stopSensors();
        if(livella != null) livella.stopSensors();
        if(torch != null) torch.stopSensors();
    }
    @Override
    protected void onResume() {
        super.onResume();
        if(compass != null) compass.startSensors();
        if(livella != null) livella.startSensors();
        if(torch != null) torch.startSensors();
    }
    //Catch the permissions results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.redirectResults(requestCode, grantResults);
    }

    //Livella's Data Update
    @Override
    public void onLevelChange(float rotation, float pitch, float roll) {

    }

    //GeoCompass's Data Update
    @Override
    public void onCompassUpdate(float magneticAzimuth, float trueAzimuth) {

    }
    @Override
    public void onMagneticInterference(float strength, int level) {

    }
    @Override
    public void onCalibrationStart() {

    }
    @Override
    public void onCalibrationEnd() {

    }

    //Torch's Data Update
    @Override
    public void onTorchMoment(float brightness, boolean turnOn) {
        if(!torch.toggleTorch) {
            torch.setBrightness(this, -1);
            torch.useTorch(this, false);
            return;
        }
        torch.setBrightness(this, brightness);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            torch.useTorchByBrightness(this, brightness);
            return;
        }
        torch.useTorch(this, turnOn);
    }
}