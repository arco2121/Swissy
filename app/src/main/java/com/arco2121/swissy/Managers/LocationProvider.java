package com.arco2121.swissy.Managers;

import android.Manifest;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.android.gms.location.*;

import android.location.LocationManager;

public class LocationProvider {
    public interface Callback {
        void onLocationReady(Location location);
    }
    private final FusedLocationProviderClient provider;
    public Location location = null;
    private final LocationManager oldProvider;
    public static final String[] permissionList = { Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION };

    public LocationProvider(FusedLocationProviderClient provider, LocationManager oldProvider) {
        this.provider = provider;
        this.oldProvider = oldProvider;
    }

    public void getLocation(PermissionManager pm, Callback callback) {
        if (!pm.hasPermissions(permissionList)) {
            callback.onLocationReady(null);
            return;
        }
        provider.getLastLocation().addOnSuccessListener(loc -> {
            if (loc != null) {
                location = loc;
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
                            location = res.getLastLocation();
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
            String providerToUse = null;

            if (oldProvider.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                providerToUse = LocationManager.NETWORK_PROVIDER;
            } else if (oldProvider.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                providerToUse = LocationManager.GPS_PROVIDER;
            }

            if (providerToUse == null) {
                callback.onLocationReady(null);
                return;
            }

            final android.location.LocationListener oneTimeListener =
                    new android.location.LocationListener() {
                        @Override
                        public void onLocationChanged(@NonNull Location location) {
                            oldProvider.removeUpdates(this);
                            callback.onLocationReady(location);
                        }

                        @Override
                        public void onProviderEnabled(@NonNull String provider) {}

                        @Override
                        public void onProviderDisabled(@NonNull String provider) {}
                    };

            oldProvider.requestLocationUpdates(providerToUse, 0, 0, oneTimeListener, Looper.getMainLooper());

        } catch (Exception e) {
            callback.onLocationReady(null);
        }
    }
}