package com.arco2121.swissy;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.arco2121.swissy.Managers.LocationProvider;
import com.arco2121.swissy.Managers.SettingsManager;
import com.arco2121.swissy.Tools.AmbientStatus.AmbientNoise;
import com.arco2121.swissy.Utility.LogPrinter;
import com.arco2121.swissy.Managers.PermissionManager;
import com.arco2121.swissy.Tools.Torch.Torch;

import java.util.ArrayList;
import java.util.Arrays;

public class Starter extends AppCompatActivity {
    private PermissionManager permissionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingsManager.LoadPropreties(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.starter);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.starter), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        permissionManager = new PermissionManager(this);
        ImageView logo = findViewById(R.id.logo);
        logo.setScaleX(0.75f);
        logo.setScaleY(0.75f);
        logo.animate().scaleX(1.15f).scaleY(1.15f).setInterpolator(new OvershootInterpolator(2f)).setDuration(350).setStartDelay(550)
                .withEndAction(() -> logo.animate().scaleX(1f).scaleY(1f).setDuration(350)
                        .withEndAction(this::redirect).start()).start();
    }

    //Catch the permissions results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.redirectResults(requestCode, permissions, grantResults);
    }

    private void redirect() {
        ArrayList<String> permissionsNeeded = new ArrayList<>();
        //Permissions Needed
        permissionsNeeded.addAll(Arrays.asList(LocationProvider.permissionList));
        permissionsNeeded.addAll(Arrays.asList(Torch.permissionList));
        permissionsNeeded.addAll(Arrays.asList(AmbientNoise.permissionList));
        permissionManager.requestPermissions(new PermissionManager.Callback() {
            @Override
            public void onGranted() {
                ImageView logo = findViewById(R.id.logo);
                logo.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(450).withEndAction(() -> {
                    //Load the app
                    logo.setAlpha(0f);
                    Intent intent = new Intent(Starter.this, Main.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }).start();
            }

            @Override
            public void onDenied(String[] denied) {
                StringBuilder listPerm = new StringBuilder();
                for (String permission : denied) {
                    int find = java.util.Arrays.asList(LocationProvider.permissionList).indexOf(permission);
                    if (find != -1) {
                        if (!listPerm.toString().isEmpty()) listPerm.append(" and ");
                        listPerm.append("Location");
                        continue;
                    }
                    find = java.util.Arrays.asList(Torch.permissionList).indexOf(permission);
                    if (find != -1) {
                        if (!listPerm.toString().isEmpty()) listPerm.append(" and ");
                        listPerm.append("Camera");
                        continue;
                    }
                    find = java.util.Arrays.asList(AmbientNoise.permissionList).indexOf(permission);
                    if (find != -1) {
                        if (!listPerm.toString().isEmpty()) listPerm.append(" and ");
                        listPerm.append("Microphone");
                    }
                }
                if (permissionManager.askAgain(Starter.this, denied)) {
                    LogPrinter.printToast(Starter.this, listPerm + " required for the app to work");
                    permissionManager.requestPermissions(this, denied);
                } else {
                    LogPrinter.printToast(Starter.this, "Enable " + listPerm + " permission from Settings");
                    permissionManager.openSettings(Starter.this);
                    finish();
                }
            }
        }, permissionsNeeded.toArray(new String[0]));
    }
}