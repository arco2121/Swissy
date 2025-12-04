package com.arco2121.swissy;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.*;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.TextView;

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
import com.arco2121.swissy.Utility.LogPrinter;
import com.arco2121.swissy.Utility.SharedObjects;
import com.arco2121.swissy.Utility.SwipeDetect;
import com.google.android.gms.location.*;

import java.util.List;

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
    String[] availableTools;
    int indexTool = 0;

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
        ImageButton settings = findViewById(R.id.settingsBt);
        ImageButton currentButton = findViewById(R.id.currentstatus);
        TextView title = findViewById(R.id.currentstatuslabel);
        sensors = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        camera = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        permissionManager = new PermissionManager(this);
        locationManager = new LocationProvider(LocationServices.getFusedLocationProviderClient(Main.this), (LocationManager) getSystemService(Context.LOCATION_SERVICE));
        locationManager.getLocation(permissionManager, locationRequested -> {
            location = locationRequested;
            try { ambientStatus = (AmbientStatus) SharedObjects.addObj("Status" , new AmbientStatus(sensors), R.drawable.status); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { compass = (GeoCompass) SharedObjects.addObj("Compass", new GeoCompass(sensors, location), R.drawable.compass); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { torch = (Torch) SharedObjects.addObj("Torch" , new Torch(sensors, camera), R.drawable.torch); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            try { livella = (Livella) SharedObjects.addObj("Livella" , new Livella(sensors), R.drawable.livella); } catch (Exception e) { LogPrinter.printToast(this, e.getMessage()); }
            availableTools = SharedObjects.asArray();
            currentButton.setImageResource((int)SharedObjects.getObj(availableTools[indexTool])[1]);
            title.setText(availableTools[indexTool]);
        });
        //UI Components
        float scale = (float) getResources().getInteger(R.integer.scaleMinus) / 100;
        long duration = getResources().getInteger(R.integer.icon_duration);
        float scale_long = (float) getResources().getInteger(R.integer.scalePlus) / 100;
        float scale_long_log = (scale_long * 3) / 2.5f;
        long duration_long = getResources().getInteger(R.integer.icon_duration_long);
        SwipeDetect currentToolSelect = new SwipeDetect(this, 300, () -> {
            if(indexTool + 1 < availableTools.length) indexTool++;
            currentButton.setImageResource((int)SharedObjects.getObj(availableTools[indexTool])[1]);
            title.setText(availableTools[indexTool]);
        }, () -> {
            if(indexTool - 1 >= 0) indexTool--;
            currentButton.setImageResource((int)SharedObjects.getObj(availableTools[indexTool])[1]);
            title.setText(availableTools[indexTool]);
        });
        settings.setOnTouchListener((v, event) -> animateButton((ImageButton) v, event, scale, scale, duration, () -> {}));
        currentButton.setOnTouchListener((v, event) -> {
            currentToolSelect.detector.onTouchEvent(event);
            return animateButton((ImageButton) v, event, scale_long, scale_long_log, duration_long, () -> {});
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
    boolean animateButton(ImageButton window, MotionEvent event, float littleX, float littleY, long duration, Runnable func) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                window.animate().scaleX(littleX).scaleY(littleY).setDuration(duration).start();
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                window.animate().scaleX(1f).scaleY(1f).setDuration(duration).withEndAction(func).start();
        }
        return false;
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