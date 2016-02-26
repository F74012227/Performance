package com.mmlab.performance;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class POIActivity extends AppCompatActivity {

    private class ImagePagerAdapter extends PagerAdapter {
        private final int[] mImages = new int[]{
                R.drawable.poi_image_1,
                R.drawable.poi_image_2,
                R.drawable.poi_image_3,
                R.drawable.poi_image_4
        };

        @Override
        public void destroyItem(final ViewGroup container, final int position, final Object object) {
            ((ViewPager) container).removeView((ImageView) object);
        }

        @Override
        public int getCount() {
            return this.mImages.length;
        }

        @Override
        public Object instantiateItem(final ViewGroup container, final int position) {
            final Context context = POIActivity.this;
            final ImageView imageView = new ImageView(context);
            final int padding = context.getResources().getDimensionPixelSize(
                    R.dimen.padding_medium);
            imageView.setPadding(padding, padding, padding, padding);
            Glide.with(POIActivity.this).load(this.mImages[position]).centerCrop().into(imageView);
            ((ViewPager) container).addView(imageView, 0);
            return imageView;
        }

        @Override
        public boolean isViewFromObject(final View view, final Object object) {
            return view == ((ImageView) object);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_poi);

        final ViewPager viewPager = (ViewPager) findViewById(R.id.viewPager);
        final ImagePagerAdapter adapter = new ImagePagerAdapter();
        viewPager.setAdapter(adapter);

        final CirclePageIndicator circleIndicator = (CirclePageIndicator) findViewById(R.id.circlePageIndicator);
        circleIndicator.setViewPager(viewPager);

        final TextView tvText = (TextView) findViewById(R.id.textView);
        tvText.setText(getResources().getString(R.string.bodyText));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_poi, menu);
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
