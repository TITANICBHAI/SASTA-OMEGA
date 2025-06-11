package com.gestureai.gameautomation.database.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.ColumnInfo;
import androidx.room.ForeignKey;
import androidx.room.Index;
import com.gestureai.gameautomation.data.SessionData;

@Entity(
    tableName = "game_actions",
    foreignKeys = @ForeignKey(
        entity = SessionData.class,
        parentColumns = "id",
        childColumns = "session_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {@Index("session_id")}
)
public class GameActionEntity {
    
    @PrimaryKey(autoGenerate = true)
    public long id;
    
    @ColumnInfo(name = "session_id")
    public long sessionId;
    
    @ColumnInfo(name = "action_type")
    public String actionType;
    
    @ColumnInfo(name = "x_coordinate")
    public int xCoordinate;
    
    @ColumnInfo(name = "y_coordinate")
    public int yCoordinate;
    
    @ColumnInfo(name = "confidence")
    public float confidence;
    
    @ColumnInfo(name = "timestamp")
    public long timestamp;
    
    @ColumnInfo(name = "success")
    public boolean success;
    
    @ColumnInfo(name = "reaction_time")
    public long reactionTime;
    
    @ColumnInfo(name = "game_context")
    public String gameContext; // JSON string for complex context data
    
    @ColumnInfo(name = "ai_strategy")
    public String aiStrategy;
    
    @ColumnInfo(name = "priority")
    public int priority;
    
    @ColumnInfo(name = "source")
    public String source; // "user", "ai", "automation"
    
    public GameActionEntity() {
        this.timestamp = System.currentTimeMillis();
        this.success = false;
        this.confidence = 0.0f;
        this.priority = 0;
        this.source = "unknown";
        this.reactionTime = 0L;
    }
    
    public GameActionEntity(String actionType, int x, int y, float confidence, String source) {
        this();
        this.actionType = actionType;
        this.xCoordinate = x;
        this.yCoordinate = y;
        this.confidence = confidence;
        this.source = source;
    }
    
    // Getters and Setters for Room compatibility
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    
    public long getSessionId() { return sessionId; }
    public void setSessionId(long sessionId) { this.sessionId = sessionId; }
    
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    
    public int getXCoordinate() { return xCoordinate; }
    public void setXCoordinate(int xCoordinate) { this.xCoordinate = xCoordinate; }
    
    public int getYCoordinate() { return yCoordinate; }
    public void setYCoordinate(int yCoordinate) { this.yCoordinate = yCoordinate; }
    
    public float getConfidence() { return confidence; }
    public void setConfidence(float confidence) { this.confidence = confidence; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public long getReactionTime() { return reactionTime; }
    public void setReactionTime(long reactionTime) { this.reactionTime = reactionTime; }
    
    public String getGameContext() { return gameContext; }
    public void setGameContext(String gameContext) { this.gameContext = gameContext; }
    
    public String getAiStrategy() { return aiStrategy; }
    public void setAiStrategy(String aiStrategy) { this.aiStrategy = aiStrategy; }
    
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
}