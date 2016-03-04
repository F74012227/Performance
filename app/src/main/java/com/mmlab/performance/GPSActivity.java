package com.mmlab.performance;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class GPSActivity extends AppCompatActivity {

    public GlobalVariable globalVariable;
    public GPSService gpsService;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gps);

        globalVariable = (GlobalVariable) getApplication();
        gpsService = globalVariable.getGpsService();

        Button button_gps = (Button) findViewById(R.id.button_gps);
        Button button_network = (Button) findViewById(R.id.button_network);
        button_gps.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gpsService.setProviderIndex(GPSService.GPS_PROVIDER);
            }
        });
        button_network.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                gpsService.setProviderIndex(GPSService.NETWORK_PROVIDER);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_g, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onStart() {
        super.onStart();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(GPSService.GPS_PROVIDER_ENABLE);
        intentFilter.addAction(GPSService.GPS_PROVIDER_DISABLE);
        intentFilter.addAction(GPSService.NETWORK_PROVIDER_ENABLE);
        intentFilter.addAction(GPSService.NETWORK_PROVIDER_DISABLE);
        intentFilter.addAction(GPSService.LOCATION_CHANGED);

        registerReceiver(gpsReceiver, intentFilter);

        gpsService.start();
    }

    protected void onStop() {
        super.onStop();

        gpsService.stop();

        unregisterReceiver(gpsReceiver);
    }

    private BroadcastReceiver gpsReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(GPSService.GPS_PROVIDER_ENABLE)) {
                Toast.makeText(GPSActivity.this, GPSService.GPS_PROVIDER_ENABLE, Toast.LENGTH_SHORT).show();
            } else if (action.equals(GPSService.GPS_PROVIDER_DISABLE)) {
                Toast.makeText(GPSActivity.this, GPSService.GPS_PROVIDER_DISABLE, Toast.LENGTH_SHORT).show();
            } else if (action.equals(GPSService.NETWORK_PROVIDER_ENABLE)) {
                Toast.makeText(GPSActivity.this, GPSService.NETWORK_PROVIDER_ENABLE, Toast.LENGTH_SHORT).show();
            } else if (action.equals(GPSService.NETWORK_PROVIDER_ENABLE)) {
                Toast.makeText(GPSActivity.this, GPSService.NETWORK_PROVIDER_ENABLE, Toast.LENGTH_SHORT).show();
            } else if (action.equals(GPSService.LOCATION_CHANGED)) {
                Toast.makeText(GPSActivity.this, GPSService.LOCATION_CHANGED, Toast.LENGTH_SHORT).show();
            }
        }
    };
}
