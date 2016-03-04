package com.mmlab.performance.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.mmlab.performance.R;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class WiFiActivity extends AppCompatActivity {

    private static final String TAG = WiFiActivity.class.getName();

    private SwipeRefreshLayout swipeRefreshLayout = null;
    private List<WifiRecord> mRecords = new ArrayList<>();
    private LinkedHashMap<String, WifiRecord> mRecords_hashMap = new LinkedHashMap<>();
    private WifiRecordAdapter mAdapter = null;
    IntentFilter intentFilter = new IntentFilter();
    private WiFiManager wifiManager = null;
    private MaterialDialog materialDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi);

        wifiManager = new WiFiManager(getApplicationContext());

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            public void onRefresh() {
                wifiManager.startScan();
                // swipeRefreshLayout.setRefreshing(false);
            }
        });
        mAdapter = new WifiRecordAdapter(getApplicationContext(), mRecords);
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(linearLayoutManager);
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));
        mRecyclerView.setOnScrollListener(new RecyclerView.OnScrollListener() {
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                swipeRefreshLayout.setEnabled(linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0);
            }
        });
        mAdapter.setOnItemClickLitener(new WifiRecordAdapter.OnItemClickLitener() {
            public void onItemClick(View view, int position) {
                if (materialDialog != null) materialDialog.dismiss();

                materialDialog = ConnectionDialog.createDialog(wifiManager, WiFiActivity.this, mRecords.get(position));
            }

            public void onItemLongClick(View view, int position) {

            }
        });
        mRecyclerView.setAdapter(mAdapter);

        mRecords_hashMap = wifiManager.getWifiRecord();
        wifiManager.startScan();

//        mRecords.clear();
//        mRecords_hashMap.clear();
//        mRecords_hashMap.put(wifiManager.getWifiApConfiguration().SSID, new WifiRecord(wifiManager.getWifiApConfiguration()));
//        mRecords.addAll(new ArrayList<>(mRecords_hashMap.values()));
//        mAdapter.notifyDataSetChanged();
//        Log.d(TAG, "HOST SSID : " + wifiManager.getWifiApConfiguration().SSID);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_wi_fi, menu);
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


        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(wifiReceiver, intentFilter);
    }

    protected void onStop() {
        super.onStop();
        unregisterReceiver(wifiReceiver);
    }

    BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String SSID = "";
            if (wifiInfo != null && wifiInfo.getSSID() != null)
                SSID = WifiRecord.normalizedSSID(wifiInfo.getSSID());
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                mRecords_hashMap = wifiManager.getWifiRecord();
                if (wifiInfo != null && wifiInfo.getSSID() != null && mRecords_hashMap.containsKey(SSID)) {
                    mRecords_hashMap.get(SSID).state = WifiRecord.FINISHED;
                }
//                if (wifiInfo != null && wifiInfo.getSSID() != null) {
//                    Log.d(TAG, "wifiInfo SSID : " + SSID);
//                }
                if (swipeRefreshLayout.isRefreshing())
                    swipeRefreshLayout.setRefreshing(false);
            } else if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(intent.getAction()) | ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                mRecords_hashMap = wifiManager.getWifiRecord();
                SupplicantState supplicantState = intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE);
                if (supplicantState != null)
                    switch (supplicantState) {
                        case AUTHENTICATING:
//                            Log.d(TAG, "AUTHENTICATING...");
                            if (wifiInfo != null && wifiInfo.getSSID() != null && mRecords_hashMap.containsKey(SSID)) {
                                mRecords_hashMap.get(SSID).state = WifiRecord.AUTHENTICATING;
                            }
                            break;
                        case COMPLETED:
//                            Log.d(TAG, "COMPLETED...");
                            if (wifiInfo != null && wifiInfo.getSSID() != null && mRecords_hashMap.containsKey(SSID)) {
                                mRecords_hashMap.get(SSID).state = WifiRecord.COMPLETED;
                            }
                            break;
                        case DISCONNECTED:
//                            Log.d(TAG, "DISCONNECTED...");
                            if (wifiInfo != null && wifiInfo.getSSID() != null && mRecords_hashMap.containsKey(SSID)) {
                                mRecords_hashMap.get(SSID).state = WifiRecord.DISCONNECTED;
                            }
                            break;
                        case FOUR_WAY_HANDSHAKE:
//                            Log.d(TAG, "FOUR_WAY_HANDSHAKE...");
                            if (wifiInfo != null && wifiInfo.getSSID() != null && mRecords_hashMap.containsKey(SSID)) {
                                mRecords_hashMap.get(SSID).state = WifiRecord.AUTHENTICATING;
                            }
                            break;
                        case GROUP_HANDSHAKE:
//                            Log.d(TAG, "GROUP_HANDSHAKE...");
                            if (wifiInfo != null && wifiInfo.getSSID() != null && mRecords_hashMap.containsKey(SSID)) {
                                mRecords_hashMap.get(SSID).state = WifiRecord.AUTHENTICATING;
                            }
                            break;
                        default:
                    }
                int supl_error = intent.getIntExtra(WifiManager.EXTRA_SUPPLICANT_ERROR, -1);
                if (supl_error == WifiManager.ERROR_AUTHENTICATING) {
//                    Log.d(TAG, "ERROR_AUTHENTICATING!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                    if (wifiInfo != null && wifiInfo.getSSID() != null && mRecords_hashMap.containsKey(SSID)) {
                        mRecords_hashMap.get(SSID).state = WifiRecord.ERROR_AUTHENTICATING;
                    }
                }

                // 檢查現在是否連接上WiFi
                ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo info = connectivityManager.getActiveNetworkInfo();
                if (info != null && info.isConnected() && info.getType() == ConnectivityManager.TYPE_WIFI) {
//                    Log.d(TAG, "WiFi");
                    if (wifiInfo != null && wifiInfo.getSSID() != null && mRecords_hashMap.containsKey(SSID)) {
                        mRecords_hashMap.get(SSID).state = WifiRecord.FINISHED;
                    }
                }
            }

            // 將當前連線的熱點置於頂端
            if (wifiInfo != null && wifiInfo.getSSID() != null && mRecords_hashMap.containsKey(SSID)) {
                WifiRecord tmpWifiRecord = mRecords_hashMap.get(SSID);
                mRecords_hashMap.remove(SSID);
                mRecords.clear();
                mRecords.add(tmpWifiRecord);
                mRecords.addAll(new ArrayList<>(mRecords_hashMap.values()));
                mRecords_hashMap.put(SSID, tmpWifiRecord);
                mAdapter.notifyDataSetChanged();
            } else {
                mRecords.clear();
                mRecords.addAll(new ArrayList<>(mRecords_hashMap.values()));
                mAdapter.notifyDataSetChanged();
            }
        }
    };
}
