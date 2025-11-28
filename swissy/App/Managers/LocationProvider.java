package com.arco2121.swissy.App.Managers;

import android.Manifest;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.*;
import android.location.LocationManager;

public class LocationProvider {
    public interface Callback {
        void onLocationReady(Location location);
    }
    private final FusedLocationProviderClient provider;
    private final LocationManager oldProvider;
    public final int requestCode = 2001;
    public final String[] permissionList = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    public LocationProvider(FusedLocationProviderClient provider, LocationManager oldProvider) {
        this.provider = provider;
        this.oldProvider = oldProvider;
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
        }).addOnFailureListener(e -> {
            requestFallbackLocation(callback);
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
    private void requestFallbackLocation(Callback callback) {
        try {
            if (oldProvider.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                oldProvider.requestSingleUpdate(
                    LocationManager.NETWORK_PROVIDER,
                    new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            callback.onLocationReady(location);
                        }
                        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                        @Override public void onProviderEnabled(@NonNull String provider) {}
                        @Override public void onProviderDisabled(@NonNull String provider) {}
                    },
                    Looper.getMainLooper()
                );
                return;
            }
            if (oldProvider.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                oldProvider.requestSingleUpdate(LocationManager.GPS_PROVIDER, new LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            callback.onLocationReady(location);
                        }
                        @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
                        @Override public void onProviderEnabled(@NonNull String provider) {}
                        @Override public void onProviderDisabled(@NonNull String provider) {}
                }, Looper.getMainLooper());
            }
            callback.onLocationReady(null);

        } catch (Exception e) {
            callback.onLocationReady(null);
        }
    }
}
