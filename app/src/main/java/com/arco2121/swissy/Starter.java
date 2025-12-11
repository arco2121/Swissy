package com.arco2121.swissy;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.arco2121.swissy.Managers.LocationProvider;
import com.arco2121.swissy.Utility.LogPrinter;
import com.arco2121.swissy.Managers.PermissionManager;
import com.arco2121.swissy.Tools.Torch.Torch;

import java.util.ArrayList;
import java.util.Arrays;

public class Starter extends AppCompatActivity {
    private PermissionManager permissionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        permissionManager = new PermissionManager(this);
        setContentView(R.layout.starter);
        FrameLayout logo = findViewById(R.id.logo);
        logo.setScaleX(0.8f);
        logo.setAlpha(0f);
        logo.setScaleY(0.8f);
        logo.animate().scaleX(1.1f).scaleY(1.1f).alpha(1f).setInterpolator(new OvershootInterpolator(2f)).setDuration(350).setStartDelay(750)
                .withEndAction(this::redirect).start();
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
        permissionManager.requestPermissions(new PermissionManager.Callback() {
            @Override
            public void onGranted() {
                FrameLayout logo = findViewById(R.id.logo);
                logo.animate().alpha(0f).scaleX(0.8f).scaleY(0.8f).setDuration(300).withEndAction(() -> {
                    //Load the app
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