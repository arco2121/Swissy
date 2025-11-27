package com.arco2121.swissy.App.Managers;

import android.Manifest;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.*;

public class LocationManager {
    public interface Callback {
        void onLocationReady(Location location);
    }
    private final FusedLocationProviderClient provider;
    public final int requestCode = 2001;
    public final String[] permissionList = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    public LocationManager(FusedLocationProviderClient provider) {
        this.provider = provider;
    }

    public void getLocation(PermissionManager pm, Callback callback) {
        if (!pm.hasPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})) {
            callback.onLocationReady(null);
            return;
        }
        provider.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                callback.onLocationReady(loc);
            } else {
                requestNewLocation(callback);
            }
        });
    }

    private void requestNewLocation(Callback callback) {
        LocationRequest req = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500).setMaxUpdates(1).build();
        provider.requestLocationUpdates(req, new LocationCallback() {
                @Override
                public void onLocationResult(@NonNull LocationResult res) {
                    provider.removeLocationUpdates(this);
                    if (res.getLastLocation() != null) {
                        callback.onLocationReady(res.getLastLocation());
                    } else {
                        callback.onLocationReady(null);
                    }
                }
            },
            Looper.getMainLooper()
        );
    }
}
