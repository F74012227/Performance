package com.mmlab.performance;

import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;
import java.text.SimpleDateFormat;

public class BroadcastActivity extends AppCompatActivity {

    private static final String TAG = BroadcastActivity.class.getName();
    private GlobalVariable globalVariable;
    private BroadcastService broadcastService;

    private TextView textView_message;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_broadcast);

        globalVariable = (GlobalVariable) getApplication();
        broadcastService = globalVariable.getBroadcastService();
        broadcastService.start();
        textView_message = (TextView) findViewById(R.id.textView_message);
        broadcastService.setOnFinishedListener(new BroadcastService.OnFinishedListener() {
            public void onReceived() {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String date = simpleDateFormat.format(new java.util.Date());

                textView_message.setText(textView_message.getText() + "\n" + date + " onReceived()");
            }

            public void onTransmitted() {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String date = simpleDateFormat.format(new java.util.Date());

                textView_message.setText(textView_message.getText() + "\n" + date + "onTransmitted()");
            }

            @Override
            public void onFinished() {
                Toast.makeText(getApplicationContext(), "OnFinished()...", Toast.LENGTH_SHORT).show();
            }
        });

        Button button_send = (Button) findViewById(R.id.button_send);
        button_send.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // broadcastService.send("txt", "132456789");
                broadcastService.send("file.jpg", new File(Environment.getExternalStorageDirectory() + File.separator + "Test" + File.separator + "poi_image_1.jpg"));
            }
        });

        Button button_receive = (Button) findViewById(R.id.button_receive);
        button_receive.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                broadcastService.receive();
            }
        });

        ToggleButton toggleButton_fec = (ToggleButton) findViewById(R.id.toggleButton_fec);
        toggleButton_fec.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                broadcastService.setFECEnabled(isChecked);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_broadcast, menu);
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
}
