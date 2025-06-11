package com.gestureai.gameautomation.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "gesture_data")
public class GestureData {
    @PrimaryKey(autoGenerate = true)
    public int id;
    
    public String gestureType;
    public float confidence;
    public long timestamp;
    public boolean actionSuccessful;
    public int gameX;
    public int gameY;
    public String gameContext;

    public GestureData(String gestureType, float confidence, long timestamp, 
                      boolean actionSuccessful, int gameX, int gameY, String gameContext) {
        this.gestureType = gestureType;
        this.confidence = confidence;
        this.timestamp = timestamp;
        this.actionSuccessful = actionSuccessful;
        this.gameX = gameX;
        this.gameY = gameY;
        this.gameContext = gameContext;
    }
}