package com.mmlab.performance.wifi;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WiFiManager {

    private static final String TAG = WifiManager.class.getName();

    private Context mContext;
    private WifiManager mWifiManager;
    private static final Map<String, Method> methodMap = new HashMap<String, Method>();
    boolean isHtc = false;

    public WiFiManager(Context context) {
        mContext = context;
        mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);

        try {
            Field field = WifiConfiguration.class.getDeclaredField("mWifiApProfile");
            isHtc = field != null;
        } catch (Exception e) {
            Log.d(TAG, e.toString(), e);
        }

        try {
            Method method = WifiManager.class.getMethod("getWifiApState");
            methodMap.put("getWifiApState", method);
        } catch (SecurityException | NoSuchMethodException e) {
            Log.d(TAG, e.toString(), e);
        }

        try {
            Method method = WifiManager.class.getMethod("getWifiApConfiguration");
            methodMap.put("getWifiApConfiguration", method);
        } catch (SecurityException | NoSuchMethodException e) {
            Log.d(TAG, e.toString(), e);
        }

        try {
            Method method = WifiManager.class.getMethod(getSetWifiApConfigName(), WifiConfiguration.class);
            methodMap.put("setWifiApConfiguration", method);
        } catch (SecurityException | NoSuchMethodException e) {
            Log.d(TAG, e.toString(), e);
        }

        try {
            Method method = WifiManager.class.getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            methodMap.put("setWifiApEnabled", method);
        } catch (SecurityException | NoSuchMethodException e) {
            Log.d(TAG, e.toString(), e);
        }
    }

    public boolean isWifiEnabled() {
        return mWifiManager.isWifiEnabled();
    }

    public void startScan() {
        mWifiManager.startScan();
    }

    public List<ScanResult> getScanResults() {
        return mWifiManager.getScanResults();
    }

    public List<WifiConfiguration> getConfiguredNetworks() {
        return mWifiManager.getConfiguredNetworks();
    }

    public WifiInfo getConnectionInfo() {
        return mWifiManager.getConnectionInfo();
    }

    /**
     * 取得當前所連無線網路SSID
     */
    public String getActivedSSID() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        String ssid = wifiInfo.getSSID();
        return WifiRecord.normalizedSSID(ssid);
    }

    /**
     * 計算訊號強度
     *
     * @param level ScanResult object field : level
     * @return 強度1-5
     */
    public static int calculateSignalStength(int level) {
        return WifiManager.calculateSignalLevel(level, 5);
    }

    /**
     * 查看以前是否配置過這個網絡
     *
     * @param SSID 服務設定識別碼
     * @return 回傳WifiConfiguration，可以為null
     */
    public WifiConfiguration getConfiguredNetwork(String SSID) {
        List<WifiConfiguration> configurations = mWifiManager.getConfiguredNetworks();
        if (configurations != null) {
            for (WifiConfiguration wifiConfiguration : configurations) {
                if (wifiConfiguration.SSID != null && wifiConfiguration.SSID.equals("\"" + SSID + "\"")) {
                    return wifiConfiguration;
                }
            }
        }
        return null;
    }

    /**
     * 提供一個外部接口，傳入已經認證的無線網絡
     * enableNetwork()方法回傳值為真，只能說明密碼沒有輸錯，並且網路可用，但不一定連接上
     * 最後需要在呼叫reconnect()連接上次關聯的無線網絡
     *
     * @param wifiRecord WifiRecord
     * @return 回傳認證是否成功
     */
    public boolean connect(WifiRecord wifiRecord) {
        WifiConfiguration wifiConfiguration = getConfiguredNetwork(wifiRecord.SSID);
        disableNetwork();
        if (wifiConfiguration != null) {
            Log.d(TAG, "connect()...success");
            return connectConfigured(wifiRecord.SSID);
        } else {
            Log.d(TAG, "connect()...fail");
            return connectUnconfigured(wifiRecord);
        }
    }

    /**
     * 提供一個外部接口，傳入要連接的尚未認證或想更改認證的無線網絡
     * 回傳值為真，只能說明密碼沒有輸錯，並且網路可用，但不一定連接上
     */
    public boolean connectUnconfigured(WifiRecord wifiRecord) {
        // 更新認證的狀態
        WifiConfiguration wifiConfiguration = getConfiguredNetwork(wifiRecord.SSID);
        if (wifiConfiguration != null) {
            mWifiManager.removeNetwork(wifiConfiguration.networkId);
        }

        WifiConfiguration wifiConfig = this.createWifiInfo(wifiRecord.SSID, wifiRecord.SSIDpwd, WifiRecord.getType(wifiRecord.capabilities));
        if (wifiConfig == null) {
            Log.i(TAG, "wifiConfig is null");
            return false;
        }
        int netID = mWifiManager.addNetwork(wifiConfig);

        return mWifiManager.enableNetwork(netID, true) && mWifiManager.saveConfiguration() && mWifiManager.reconnect();
    }

    /**
     * 提供一個外部接口，傳入已經認證的無線網絡
     * enableNetwork()方法回傳值為真，只能說明密碼沒有輸錯，並且網路可用，但不一定連接上
     * 最後需要在呼叫reconnect()連接上次關聯的無線網絡
     */
    public boolean connectConfigured(String SSID) {
        WifiConfiguration wifiConfiguration = getConfiguredNetwork(SSID);
        return (mWifiManager.enableNetwork(wifiConfiguration.networkId, true) && mWifiManager.reconnect());
    }

    public boolean disableNetwork() {
        int netId = mWifiManager.getConnectionInfo().getNetworkId();
        return netId < 0 || mWifiManager.disableNetwork(netId);
    }

    public boolean disconnectNetwork(int netId) {
        return netId < 0 || mWifiManager.removeNetwork(netId);
    }

    public boolean disconnect() {
        return mWifiManager.disconnect();
    }

    public LinkedHashMap<String, WifiRecord> getWifiRecord() {

        List<ScanResult> scanResults = getScanResults();
        LinkedHashMap<String, WifiRecord> hashMap = new LinkedHashMap<>();

        for (ScanResult scanResult : scanResults) {
            WifiRecord wifiRecord;
            if (scanResult.SSID != null && !scanResult.SSID.equals("")) {
                if (hashMap.containsKey(scanResult.SSID)) {
                    wifiRecord = hashMap.get(scanResult.SSID);
                    if (calculateSignalStength(scanResult.level) >
                            calculateSignalStength(wifiRecord.level)) {
                        wifiRecord.updateScanResult(scanResult);
                    }
                } else {
                    wifiRecord = new WifiRecord();
                    wifiRecord.updateScanResult(scanResult);
                }
                hashMap.put(scanResult.SSID, wifiRecord);
            }
        }

        return hashMap;
    }

    private WifiConfiguration createWifiInfo(String SSID, String SSIDpwd,
                                             int type) {

        WifiConfiguration config = new WifiConfiguration();

        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();

        config.SSID = "\"" + SSID + "\"";
        config.status = WifiConfiguration.Status.DISABLED;
        config.priority = 40;

        if (type == WifiRecord.NOPASS) {
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
            return config;
        } else if (type == WifiRecord.WEP) {
            config.wepKeys[0] = "\"" + SSIDpwd + "\"";
            config.hiddenSSID = true;
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.wepTxKeyIndex = 0;

            return config;
        } else if (type == WifiRecord.WPA) {

            config.preSharedKey = "\"" + SSIDpwd + "\"";
            config.hiddenSSID = true;
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
            config.status = WifiConfiguration.Status.ENABLED;
            return config;
        } else {
            return null;
        }
    }

    public int getWifiApState() {
        try {
            Method method = methodMap.get("getWifiApState");
            return (Integer) method.invoke(mWifiManager);
        } catch (IllegalAccessException e) {
            Log.d(TAG, e.toString(), e);
        } catch (InvocationTargetException e) {
            Log.d(TAG, e.toString(), e);
        }
        return -1;
    }

    private WifiConfiguration getHtcWifiApConfiguration(WifiConfiguration wifiConfiguration) {
        try {
            Object mWifiApProfileValue = getFieldValue(wifiConfiguration, "mWifiApProfile");
            if (mWifiApProfileValue != null)
                wifiConfiguration.SSID = (String) getFieldValue(mWifiApProfileValue, "SSID");
        } catch (Exception e) {
            Log.d(TAG, e.toString(), e);
        }
        return wifiConfiguration;
    }

    public WifiConfiguration getWifiApConfiguration() {
        WifiConfiguration wifiConfiguration = null;
        try {
            Method method = methodMap.get("getWifiApConfiguration");
            wifiConfiguration = (WifiConfiguration) method.invoke(mWifiManager);
            if (isHtc)
                wifiConfiguration = getHtcWifiApConfiguration(wifiConfiguration);
        } catch (Exception e) {
            Log.d(TAG, e.toString(), e);
        }
        return wifiConfiguration;
    }

    private void setupHtcWifiConfiguration(WifiConfiguration wifiConfiguration) {
        try {
            Object mWifiApProfileValue = getFieldValue(wifiConfiguration, "mWifiApProfile");

            if (mWifiApProfileValue != null) {
                setFieldValue(mWifiApProfileValue, "SSID", wifiConfiguration.SSID);
                setFieldValue(mWifiApProfileValue, "BSSID", wifiConfiguration.BSSID);
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString(), e);
        }
    }

    public boolean setWifiApConfiguration(WifiConfiguration wifiConfiguration) {
        boolean result = false;
        try {
            if (isHtc)
                setupHtcWifiConfiguration(wifiConfiguration);

            Method method = methodMap.get("setWifiApConfiguration");

            if (isHtc) {
                int value = (Integer) method.invoke(mWifiManager, wifiConfiguration);
                result = value > 0;
            } else {
                result = (Boolean) method.invoke(mWifiManager, wifiConfiguration);
            }
        } catch (Exception e) {
            Log.e(TAG, "", e);
        }
        return result;
    }


    private String getSetWifiApConfigName() {
        return isHtc ? "setWifiApConfig" : "setWifiApConfiguration";
    }

    public WifiManager wifiManager() {
        return mWifiManager;
    }

    private Object getFieldValue(Object object, String propertyName)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = object.getClass().getDeclaredField(propertyName);
        field.setAccessible(true);
        return field.get(object);
    }

    private void setFieldValue(Object object, String propertyName, Object value)
            throws IllegalAccessException, NoSuchFieldException {
        Field field = object.getClass().getDeclaredField(propertyName);
        field.setAccessible(true);
        field.set(object, value);
    }
}
