package com.gestureai.gameautomation.database.dao;

import androidx.room.*;
import com.gestureai.gameautomation.database.entities.GestureDataEntity;
import java.util.List;

@Dao
public interface GestureDataDao {
    
    @Query("SELECT * FROM gesture_data ORDER BY timestamp DESC")
    List<GestureDataEntity> getAllGestures();
    
    @Query("SELECT * FROM gesture_data WHERE session_id = :sessionId ORDER BY timestamp DESC")
    List<GestureDataEntity> getGesturesBySession(long sessionId);
    
    @Query("SELECT * FROM gesture_data WHERE gesture_type = :gestureType ORDER BY timestamp DESC")
    List<GestureDataEntity> getGesturesByType(String gestureType);
    
    @Query("SELECT * FROM gesture_data WHERE is_training_data = 1 ORDER BY timestamp DESC")
    List<GestureDataEntity> getTrainingData();
    
    @Query("SELECT * FROM gesture_data WHERE recognition_confidence >= :minConfidence ORDER BY recognition_confidence DESC")
    List<GestureDataEntity> getGesturesWithMinConfidence(float minConfidence);
    
    @Query("SELECT * FROM gesture_data WHERE accuracy >= :minAccuracy ORDER BY accuracy DESC")
    List<GestureDataEntity> getGesturesWithMinAccuracy(float minAccuracy);
    
    @Query("SELECT * FROM gesture_data WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<GestureDataEntity> getGesturesByTimeRange(long startTime, long endTime);
    
    @Query("SELECT * FROM gesture_data WHERE training_label = :label ORDER BY timestamp DESC")
    List<GestureDataEntity> getGesturesByLabel(String label);
    
    @Query("SELECT AVG(recognition_confidence) FROM gesture_data WHERE gesture_type = :gestureType")
    Float getAverageConfidenceForType(String gestureType);
    
    @Query("SELECT AVG(accuracy) FROM gesture_data WHERE gesture_type = :gestureType")
    Float getAverageAccuracyForType(String gestureType);
    
    @Query("SELECT COUNT(*) FROM gesture_data WHERE gesture_type = :gestureType")
    int getCountForGestureType(String gestureType);
    
    @Query("SELECT COUNT(*) FROM gesture_data WHERE session_id = :sessionId")
    int getGestureCountForSession(long sessionId);
    
    @Query("SELECT DISTINCT gesture_type FROM gesture_data ORDER BY gesture_type")
    List<String> getAllGestureTypes();
    
    @Query("SELECT DISTINCT training_label FROM gesture_data WHERE training_label IS NOT NULL ORDER BY training_label")
    List<String> getAllTrainingLabels();
    
    @Insert
    long insertGesture(GestureDataEntity gesture);
    
    @Insert
    void insertGestures(List<GestureDataEntity> gestures);
    
    @Update
    void updateGesture(GestureDataEntity gesture);
    
    @Delete
    void deleteGesture(GestureDataEntity gesture);
    
    @Query("DELETE FROM gesture_data WHERE session_id = :sessionId")
    void deleteGesturesBySession(long sessionId);
    
    @Query("DELETE FROM gesture_data WHERE timestamp < :cutoffTime")
    void deleteOldGestures(long cutoffTime);
    
    @Query("DELETE FROM gesture_data WHERE is_training_data = 0 AND timestamp < :cutoffTime")
    void deleteOldNonTrainingData(long cutoffTime);
}