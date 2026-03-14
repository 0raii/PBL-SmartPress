package com.example.itproyek2;

public class HistoryItem {
    private String title;
    private String time;
    private int iconRes;

    public HistoryItem(String title, String time, int iconRes) {
        this.title = title;
        this.time = time;
        this.iconRes = iconRes;
    }

    public String getTitle() { return title; }
    public String getTime() { return time; }
    public int getIconRes() { return iconRes; }
}
