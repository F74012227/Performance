package com.mmlab.performance.share;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.mmlab.performance.R;

public class ShareActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


//        FrameLayout frameLayout = new FrameLayout(this);
//        frameLayout.setId(R.id.layout1);
//        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
//        frameLayout.setLayoutParams(layoutParams);
//
//        View rootView = getLayoutInflater().inflate(R.layout.activity_share, frameLayout, false);
//
//        frameLayout.addView(rootView);

//        Button button = new Button(this);
//        FrameLayout.LayoutParams layoutParams1 = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
//        button.setLayoutParams(layoutParams1);
//
//        frameLayout.addView(button);



        setContentView(R.layout.test);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_share, menu);
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
