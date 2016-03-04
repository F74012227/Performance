package com.mmlab.performance;

import android.app.Application;

/**
 * Created by mmlab on 2016/2/26.
 */
public class GlobalVariable extends Application {

    private static final String TAG = GlobalVariable.class.getName();

    private GPSService gpsService;
    private BroadcastService broadcastService;

    public void onCreate() {
        super.onCreate();

        if (gpsService == null)
            gpsService = new GPSService(getApplicationContext());
        if (broadcastService == null)
            broadcastService = new BroadcastService(getApplicationContext());
    }

    public GPSService getGpsService() {
        return gpsService;
    }

    public BroadcastService getBroadcastService() {
        return broadcastService;
    }
}
