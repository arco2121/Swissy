package com.arco2121.swissy.App;

import android.content.Context;
import android.hardware.*;
import android.location.Location;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.arco2121.swissy.R;
import com.arco2121.swissy.App.Managers.*;
import com.arco2121.swissy.Tools.GeoCompass.*;
import com.arco2121.swissy.Tools.Livella.*;
import com.google.android.gms.location.*;

public class Main extends AppCompatActivity implements GeoCompassListener, LivellaListener {
    private PermissionManager permissionManager;
    private LocationProvider locationManager;
    private Location location;
    private SensorManager sensors;
    private GeoCompass compass;
    private Livella livella;

    //App Startup
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
        permissionManager = new PermissionManager(this, locationManager.requestCode);
        permissionManager.requestPermissions(new PermissionManager.Callback() {
            @Override
            public void onGranted() {
                LogPrinter.printToast(Main.this, "Granted");
                sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
                locationManager = new LocationProvider(LocationServices.getFusedLocationProviderClient(this), getSystemService(Context.LOCATION_SERVICE));
                locationManager.getLocation(permissionManager, locationRequested -> location = locationRequested);
                compass = new GeoCompass(sensors, location);
                livella = new Livella(sensors);
            }

            @Override
            public void onDenied() {
                LogPrinter.printToast(Main.this, "Location required for the app to work properly");
                permissionManager.requestPermissions(this,locationManager.permissionList);
            }
        }, locationManager.permissionList);
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
}