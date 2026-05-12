package com.example.itproyek2;

public class AutoLogModel {
    private long timestamp;
    private String action;
    private int lumen;
    private double watt;

    public AutoLogModel(long timestamp, String action, int lumen, double watt) {
        this.timestamp = timestamp;
        this.action = action;
        this.lumen = lumen;
        this.watt = watt;
    }

    public long getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public int getLumen() { return lumen; }
    public double getWatt() { return watt; }
}