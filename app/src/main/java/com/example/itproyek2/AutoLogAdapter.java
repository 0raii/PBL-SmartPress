package com.example.itproyek2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AutoLogAdapter extends RecyclerView.Adapter<AutoLogAdapter.ViewHolder> {

    private List<AutoLogModel> logList;
    private SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault());

    public AutoLogAdapter(List<AutoLogModel> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_auto_log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AutoLogModel log = logList.get(position);
        
        holder.tvActionTitle.setText(log.getAction());
        holder.tvActionTime.setText(sdf.format(new Date(log.getTimestamp())));
        holder.tvLumenValue.setText(log.getLumen() + " Lm");
        holder.tvWattValue.setText(String.format("%.2f W", log.getWatt()));
        
        if ("HIDUP".equalsIgnoreCase(log.getAction())) {
            holder.tvActionTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.accent_yellow));
        } else {
            holder.tvActionTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_sub));
        }
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvActionTitle, tvActionTime, tvLumenValue, tvWattValue;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvActionTitle = itemView.findViewById(R.id.tvActionTitle);
            tvActionTime = itemView.findViewById(R.id.tvActionTime);
            tvLumenValue = itemView.findViewById(R.id.tvLumenValue);
            tvWattValue = itemView.findViewById(R.id.tvWattValue);
        }
    }
}