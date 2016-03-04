package com.mmlab.performance;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

/**
 * Created by mmlab on 2016/2/26.
 */
public class BatteryService {

    private static final String TAG = BatteryService.class.getName();

    private Context context;

    private Handler pHandler;

    public BatteryService(Context context) {
        this.context = context;
    }

    private static final int FETCH_BATTERY_INFO = 0;

    public void start() {
        if (pHandler == null) {
            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();

            pHandler = new Handler(handlerThread.getLooper()) {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case FETCH_BATTERY_INFO:
                            readBattery();
                            pHandler.sendEmptyMessageDelayed(FETCH_BATTERY_INFO, 1000);
                            break;
                        default:
                    }
                    super.handleMessage(msg);
                }
            };
        }
    }

    private String readBattery() {
        StringBuilder sb = new StringBuilder();
        IntentFilter batteryIntentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryIntent = context.registerReceiver(null, batteryIntentFilter);

        if (batteryIntent == null) return sb.toString();

        boolean present = batteryIntent.getExtras().getBoolean(BatteryManager.EXTRA_PRESENT);
        sb.append("PRESENT: ").append(present).append("\n");

        int status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
            sb.append("BATTERY_STATUS_CHARGING\n");
        }
        if (status == BatteryManager.BATTERY_STATUS_FULL) {
            sb.append("BATTERY_STATUS_FULL\n");
        }

        int plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        if (plugged == BatteryManager.BATTERY_PLUGGED_USB) {
            sb.append("BATTERY_PLUGGED_USB\n");
        }
        if (plugged == BatteryManager.BATTERY_PLUGGED_AC) {
            sb.append("BATTERY_PLUGGED_AC\n");
        }

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        sb.append("LEVEL: ").append(level).append("\n");

        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        sb.append("SCALE: ").append(scale).append("\n");

        int health = batteryIntent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
        sb.append("health: ").append(convHealth(health)).append("\n");

        String technology = batteryIntent.getExtras().getString(BatteryManager.EXTRA_TECHNOLOGY);
        sb.append("TECHNOLOGY: ").append(technology).append("\n");

        int temperature = batteryIntent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);
        sb.append("TEMPERATURE: ").append(temperature).append("\n");

        int voltage = batteryIntent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
        sb.append("VOLTAGE: ").append(voltage).append("\n");

        //I have no idea how to load the small icon from system resources!
        int icon_small_resourceId = batteryIntent.getIntExtra(BatteryManager.EXTRA_ICON_SMALL, 0);
        sb.append("ICON_SMALL: ").append(icon_small_resourceId).append("\n");

        return sb.toString();
    }

    private String convHealth(int health) {
        String result;
        switch (health) {
            case BatteryManager.BATTERY_HEALTH_COLD:
                result = "BATTERY_HEALTH_COLD";
                break;
            case BatteryManager.BATTERY_HEALTH_DEAD:
                result = "BATTERY_HEALTH_DEAD";
                break;
            case BatteryManager.BATTERY_HEALTH_GOOD:
                result = "BATTERY_HEALTH_GOOD";
                break;
            case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                result = "BATTERY_HEALTH_OVERHEAT";
                break;
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                result = "BATTERY_HEALTH_OVER_VOLTAGE";
                break;
            case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                result = "BATTERY_HEALTH_UNKNOWN";
                break;
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                result = "BATTERY_HEALTH_UNSPECIFIED_FAILURE";
                break;
            default:
                result = "unkknown";
        }

        return result;
    }

    public void restart() {

    }

    public void stop() {
        pHandler.removeCallbacksAndMessages(null);

        pHandler = null;
    }
}
