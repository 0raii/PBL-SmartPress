package com.example.itproyek2;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> historyList;

    public HistoryAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDate.setText(item.getTime());
        holder.tvTime.setText(item.getTime());
        
        int iconRes = R.drawable.ic_history; // Default
        int iconColor = Color.GRAY; // Default Color

        switch (item.getIconRes()) {
            case 1: // Lamp ON
                iconRes = R.drawable.ic_lamp_on;
                iconColor = Color.parseColor("#FFD600"); // Kuning Terang
                break;
            case 2: // Lamp OFF
                iconRes = R.drawable.ic_lamp_off;
                iconColor = Color.GRAY;
                break;
            case 3: // Power / Alert
                iconRes = R.drawable.ic_power;
                iconColor = Color.RED;
                break;
            case 4: // Settings Change
                iconRes = R.drawable.ic_settings;
                iconColor = Color.BLUE;
                break;
            default:
                iconRes = R.drawable.ic_history;
                iconColor = Color.GRAY;
                break;
        }
        
        holder.ivIcon.setImageResource(iconRes);
        holder.ivIcon.setColorFilter(iconColor);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvDate, tvTime;
        ImageView ivIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvHistoryTitle);
            tvDate = itemView.findViewById(R.id.tvHistoryDate);
            tvTime = itemView.findViewById(R.id.tvHistoryTime);
            ivIcon = itemView.findViewById(R.id.ivHistoryIcon);
        }
    }
}
