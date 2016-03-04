package com.mmlab.performance.wifi;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.mmlab.performance.R;

public class ConnectionDialog {
    private static String TAG = "ConnectionDialog";

    // private static AlertDialog alertDialog = null;
    private static MaterialDialog materialDialog = null;

    /**
     * @param wiFiManager NetworkManager
     * @param activity    Activity
     * @param wifiRecord  WifiRecord
     * @return
     */
    public static MaterialDialog createDialog(final WiFiManager wiFiManager, final Activity activity, final WifiRecord wifiRecord) {

        // get prompts.xml view
        LayoutInflater layoutInflater = LayoutInflater.from(activity);
        View promptView = layoutInflater.inflate(R.layout.configured_dialog, null);
        final MaterialDialog.Builder materialDialogBuilder = new MaterialDialog.Builder(activity);
        materialDialogBuilder.customView(promptView, true);

        final TextView securityshowTextView = (TextView) promptView.findViewById(R.id.securityshow_textView);
        final TextView passwordTextView = (TextView) promptView.findViewById(R.id.password_textView);
        final EditText passwordshowEditView = (EditText) promptView.findViewById(R.id.passwordshow_editText);
        final CheckBox checkBox = (CheckBox) promptView.findViewById(R.id.checkBox);

        if (wifiRecord.getWifiEncrypt() == WifiRecord.NOPASS || wiFiManager.getConfiguredNetwork(wifiRecord.SSID) != null || !wifiRecord.SSIDpwd.isEmpty()) {
            passwordTextView.setVisibility(View.GONE);
            passwordshowEditView.setVisibility(View.GONE);
            checkBox.setVisibility(View.GONE);
        } else {
            passwordshowEditView.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                    Log.d(TAG, "beforeTextChanged()...");
                }

                public void onTextChanged(CharSequence s, int start, int before, int count) {
//                    Log.d(TAG, "onTextChanged()...");
                }

                public void afterTextChanged(Editable s) {
//                    Log.d(TAG, "afterTextChanged()...");
                    if (passwordshowEditView.getText() != null && !passwordshowEditView.getText().toString().isEmpty()) {
                        materialDialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
                    } else {
                        materialDialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
                    }
                }
            });

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        passwordshowEditView.setTransformationMethod(null);
                    } else {
                        passwordshowEditView.setTransformationMethod(new PasswordTransformationMethod());
                    }
                }
            });
        }
        // 設置安全性
        securityshowTextView.setText(wifiRecord.getSecurity());

        // 建立對話窗視窗
        materialDialogBuilder.title(wifiRecord.SSID);

        if (wiFiManager.getConfiguredNetwork(wifiRecord.SSID) != null) {

            materialDialogBuilder.neutralText(R.string.clear).onNeutral(new MaterialDialog.SingleButtonCallback() {
                public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                    wiFiManager.disconnectNetwork(wiFiManager.getConfiguredNetwork(wifiRecord.SSID).networkId);
                }
            });
        }

        if (!wiFiManager.getActivedSSID().equals(wifiRecord.SSID)) {
            materialDialogBuilder.positiveText(R.string.connect).onPositive(new MaterialDialog.SingleButtonCallback() {
                public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                    if (passwordshowEditView.getVisibility() == View.VISIBLE) {
                        wifiRecord.SSIDpwd = passwordshowEditView.getText().toString();
                    }
                    wiFiManager.connect(wifiRecord);
                }
            });
        }

        materialDialogBuilder.negativeText(R.string.cancel).onNegative(new MaterialDialog.SingleButtonCallback() {
            public void onClick(MaterialDialog materialDialog, DialogAction dialogAction) {
                materialDialog.dismiss();
            }
        });


        materialDialog = materialDialogBuilder.positiveColorRes(R.color.colorPrimary).neutralColorRes(R.color.colorPrimary).negativeColorRes(R.color.colorPrimary).build();
        materialDialog.show();
        if (wifiRecord.getWifiEncrypt() == WifiRecord.NOPASS || wiFiManager.getConfiguredNetwork(wifiRecord.SSID) != null || !wifiRecord.SSIDpwd.isEmpty()) {
            materialDialog.getActionButton(DialogAction.POSITIVE).setEnabled(true);
        } else {
            materialDialog.getActionButton(DialogAction.POSITIVE).setEnabled(false);
        }
        // 創建對話窗視窗
        return materialDialog;
    }
}
