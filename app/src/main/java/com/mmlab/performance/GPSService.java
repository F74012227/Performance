package com.mmlab.performance;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

/**
 * Created by mmlab on 2016/2/26.
 */
public class GPSService implements LocationListener {

    private static final String TAG = GPSService.class.getName();

    public static final String GPS_PROVIDER_DISABLE = "GPS_PROVIDER_DISABLE";
    public static final String GPS_PROVIDER_ENABLE = "GPS_PROVIDER_ENABLE";
    public static final String NETWORK_PROVIDER_DISABLE = "NETWORK_PROVIDER_DISABLE";
    public static final String NETWORK_PROVIDER_ENABLE = "NETWORK_PROVIDER_ENABLE";

    public static final String LOCATION_CHANGED = "LOCATION_CHANGED";

    public static final int GPS_PROVIDER = 0;
    public static final int NETWORK_PROVIDER = 1;

    private Context context;

    private LocationManager locationManager;

    private Location location;

    private static final String[] PROVIDERS = {LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER};

    private int index_providers = GPS_PROVIDER;

    public GPSService(Context context) {
        this.context = context;
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    public void start() throws SecurityException {
        switch (index_providers) {
            case GPS_PROVIDER:
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    context.sendBroadcast(new Intent().setAction(GPS_PROVIDER_ENABLE));
                } else {
                    context.sendBroadcast(new Intent().setAction(GPS_PROVIDER_DISABLE));
                    return;
                }
                break;
            case NETWORK_PROVIDER:
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    context.sendBroadcast(new Intent().setAction(NETWORK_PROVIDER_ENABLE));
                } else {
                    context.sendBroadcast(new Intent().setAction(NETWORK_PROVIDER_DISABLE));
                    return;
                }
                break;
            default:
        }

        location = locationManager.getLastKnownLocation(PROVIDERS[index_providers]);
        locationManager.requestLocationUpdates(PROVIDERS[index_providers], 10000, 10, this);
    }

    public void restart() {
        stop();
        start();
    }

    public void setProviderIndex(int index_providers) {

        if (this.index_providers != index_providers) {
            this.index_providers = index_providers;
            restart();
        }
    }

    public int getProviderIndex() {
        return index_providers;
    }

    public void stop() throws SecurityException {
        locationManager.removeUpdates(this);

        context = null;
        locationManager = null;
        location = null;
    }

    public Location getLocation() {
        return location;
    }

    public void onLocationChanged(Location location) {
        this.location = location;
        context.sendBroadcast(new Intent().setAction(LOCATION_CHANGED));
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            context.sendBroadcast(new Intent().setAction(GPS_PROVIDER_ENABLE));
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            context.sendBroadcast(new Intent().setAction(NETWORK_PROVIDER_ENABLE));
        }
    }

    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            context.sendBroadcast(new Intent().setAction(GPS_PROVIDER_DISABLE));
        } else if (provider.equals(LocationManager.NETWORK_PROVIDER)) {
            context.sendBroadcast(new Intent().setAction(NETWORK_PROVIDER_DISABLE));
        }
    }
}
