package com.arco2121.swissy.Managers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.telecom.Call;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PermissionManager {
    public interface Callback {
        void onGranted();
        void onDenied();
    }

    private final Activity activity;
    private final Random requestCoder;
    private final Map<Integer, Callback> callbacks = new HashMap<>();

    public PermissionManager(Activity activity) {
        this.activity = activity;
        requestCoder = new Random();
    }
    public boolean askAgain(Context context, String ...permissionsList) {
        for (String perm : permissionsList) {
            if (ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_GRANTED) {
                continue;
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)) {
                return true;
            }
            if (ContextCompat.checkSelfPermission(activity, perm) == PackageManager.PERMISSION_DENIED) {
                continue;
            }
        }
        return false;
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
        int requestCode = requestCoder.nextInt();
        callbacks.put(requestCode,callback);
        if (hasPermissions(permissions)) {
            callback.onGranted();
            return;
        }
        ActivityCompat.requestPermissions(activity, permissions, requestCode);
    }
    //To add on the onRequestPermissionsResult overload of the main activity
    public void redirectResults(int reqCode, @NonNull int[] grantResults) {
        Callback callback = callbacks.remove(reqCode);
        if (callback == null) return;
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
    public void openSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }
}