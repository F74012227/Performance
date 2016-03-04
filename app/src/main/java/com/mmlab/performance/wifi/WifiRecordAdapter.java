package com.mmlab.performance.wifi;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mmlab.performance.R;

import java.util.List;

/**
 * Created by mmlab on 2015/9/16.
 */
public class WifiRecordAdapter extends RecyclerView.Adapter<WifiRecordAdapter.MyViewHolder> {

    private static final String TAG = "WifiRecordAdapter";
    private Context mContext = null;

    private List<WifiRecord> mRecords = null;

    public WifiRecordAdapter(Context context, List<WifiRecord> records) {
        this.mRecords = records;
        this.mContext = context;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wifirecord, parent, false));
    }

    public interface OnItemClickLitener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    private OnItemClickLitener mOnItemClickLitener;

    public void setOnItemClickLitener(OnItemClickLitener mOnItemClickLitener) {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, int position) {

        switch (WiFiManager.calculateSignalStength(mRecords.get(position).level)) {
            case 1:
                holder.wifirecord_imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_signal_wifi_1_bar_indigo_800_36dp));
                break;
            case 2:
                holder.wifirecord_imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_signal_wifi_2_bar_indigo_800_36dp));
                break;
            case 3:
                holder.wifirecord_imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_signal_wifi_3_bar_indigo_800_36dp));
                break;
            case 4:
                holder.wifirecord_imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_signal_wifi_4_bar_indigo_800_36dp));
                break;
            default:
                holder.wifirecord_imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_signal_wifi_0_bar_indigo_800_36dp));
        }

        if (mRecords.get(position).isHost) {
            holder.wifirecord_imageView.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_remove_red_eye_indigo_800_36dp));
        }

        holder.wifirecord_title.setText(mRecords.get(position).SSID);
        holder.wifirecord_content.setText(mRecords.get(position).getStatus());

        // 如果設置了回調，則設置點擊事件
        if (mOnItemClickLitener != null) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickLitener.onItemClick(holder.itemView, pos);
                }
            });

            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    int pos = holder.getLayoutPosition();
                    mOnItemClickLitener.onItemLongClick(holder.itemView, pos);
                    return false;
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mRecords.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        TextView wifirecord_title;
        TextView wifirecord_content;
        ImageView wifirecord_imageView;

        public MyViewHolder(View view) {
            super(view);
            wifirecord_title = (TextView) view.findViewById(R.id.wifirecord_title);
            wifirecord_content = (TextView) view.findViewById(R.id.wifirecord_content);
            wifirecord_imageView = (ImageView) view.findViewById(R.id.wifirecord_imageView);
            wifirecord_imageView.setClickable(false);
            wifirecord_imageView.setFocusable(false);
            wifirecord_imageView.setFocusableInTouchMode(false);
            // Log.d(TAG, "menu : " + wifirecord_menu.getParent().toString());
        }
    }
}
