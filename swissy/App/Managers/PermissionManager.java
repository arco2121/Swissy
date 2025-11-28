package com.arco2121.swissy.App.Managers;

import android.app.Activity;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class PermissionManager {
    public interface Callback {
        void onGranted();
        void onDenied();
    }

    private final Activity activity;
    private final int requestCode;
    private Callback callback;

    public PermissionManager(Activity activity, int requestCode) {
        this.activity = activity;
        this.requestCode = requestCode;
    }
    public boolean hasPermissions(String[] permissions) {
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(activity, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    public void requestPermissions(Callback callback, String ...permissions) {
        this.callback = callback;
        if (hasPermissions(permissions)) {
            callback.onGranted();
            return;
        }
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }

    public void handleResult(int reqCode, @NonNull int[] grantResults) {
        if (reqCode != requestCode || callback == null) return;
        boolean allGranted = true;
        for (int res : grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        if (allGranted) {
            callback.onGranted();
        } else {
            callback.onDenied();
        }
    }
}