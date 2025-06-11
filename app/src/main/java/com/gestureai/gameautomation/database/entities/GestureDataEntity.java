package com.gestureai.gameautomation.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.gestureai.gameautomation.data.SessionData;

@Entity(
    tableName = "gesture_data",
    foreignKeys = @ForeignKey(
        entity = SessionData.class,
        parentColumns = "id",
        childColumns = "session_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("session_id")}
)
public class GestureDataEntity {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    @ColumnInfo(name = "session_id")
    public long sessionId;
    
    @ColumnInfo(name = "gesture_type")
    public String gestureType;
    
    @ColumnInfo(name = "start_x")
    public float startX;
    
    @ColumnInfo(name = "start_y")
    public float startY;
    
    @ColumnInfo(name = "end_x")
    public float endX;
    
    @ColumnInfo(name = "end_y")
    public float endY;
    
    @ColumnInfo(name = "duration")
    public long duration;
    
    @ColumnInfo(name = "velocity")
    public float velocity;
    
    @ColumnInfo(name = "accuracy")
    public float accuracy;
    
    @ColumnInfo(name = "timestamp")
    public long timestamp;
    
    @ColumnInfo(name = "recognition_confidence")
    public float recognitionConfidence;
    
    @ColumnInfo(name = "landmark_data")
    public String landmarkData; // JSON string for MediaPipe landmarks
    
    @ColumnInfo(name = "training_label")
    public String trainingLabel;
    
    @ColumnInfo(name = "is_training_data")
    public boolean isTrainingData;
    
    @ColumnInfo(name = "user_feedback")
    public String userFeedback;
    
    public GestureDataEntity() {
        this.timestamp = System.currentTimeMillis();
        this.recognitionConfidence = 0.0f;
        this.accuracy = 0.0f;
        this.velocity = 0.0f;
        this.duration = 0L;
        this.isTrainingData = false;
    }
    
    public GestureDataEntity(String gestureType, float startX, float startY, float endX, float endY) {
        this();
        this.gestureType = gestureType;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.duration = calculateDuration();
        this.velocity = calculateVelocity();
    }
    
    private long calculateDuration() {
        // Placeholder - would be calculated from actual gesture timing
        return 200L;
    }
    
    private float calculateVelocity() {
        float distance = (float) Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
        return duration > 0 ? distance / duration : 0.0f;
    }
    
    // Getters and Setters for Room compatibility
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }
    
    public String getGestureType() { return gestureType; }
    public void setGestureType(String gestureType) { this.gestureType = gestureType; }
    
    public float getStartX() { return startX; }
    public void setStartX(float startX) { this.startX = startX; }
    
    public float getStartY() { return startY; }
    public void setStartY(float startY) { this.startY = startY; }
    
    public float getEndX() { return endX; }
    public void setEndX(float endX) { this.endX = endX; }
    
    public float getEndY() { return endY; }
    public void setEndY(float endY) { this.endY = endY; }
    
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }
    
    public float getVelocity() { return velocity; }
    public void setVelocity(float velocity) { this.velocity = velocity; }
    
    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public float getRecognitionConfidence() { return recognitionConfidence; }
    public void setRecognitionConfidence(float recognitionConfidence) { this.recognitionConfidence = recognitionConfidence; }
    
    public String getLandmarkData() { return landmarkData; }
    public void setLandmarkData(String landmarkData) { this.landmarkData = landmarkData; }
    
    public String getTrainingLabel() { return trainingLabel; }
    public void setTrainingLabel(String trainingLabel) { this.trainingLabel = trainingLabel; }
    
    public boolean isTrainingData() { return isTrainingData; }
    public void setTrainingData(boolean trainingData) { isTrainingData = trainingData; }
    
    public String getUserFeedback() { return userFeedback; }
    public void setUserFeedback(String userFeedback) { this.userFeedback = userFeedback; }
}