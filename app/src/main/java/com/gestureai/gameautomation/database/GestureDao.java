package com.gestureai.gameautomation.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface GestureDao {
    @Insert
    void insertGesture(GestureData gestureData);

    @Query("SELECT * FROM gesture_data ORDER BY timestamp DESC LIMIT 100")
    List<GestureData> getRecentGestures();

    @Query("SELECT * FROM gesture_data WHERE timestamp > :startTime")
    List<GestureData> getGesturesSince(long startTime);

    @Query("SELECT COUNT(*) FROM gesture_data WHERE actionSuccessful = 1")
    int getSuccessfulGestureCount();

    @Query("SELECT COUNT(*) FROM gesture_data")
    int getTotalGestureCount();
}