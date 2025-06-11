package com.gestureai.gameautomation.database.dao;

import androidx.room.*;
import com.gestureai.gameautomation.database.entities.GameActionEntity;
import java.util.List;

@Dao
public interface GameActionDao {
    
    @Query("SELECT * FROM game_actions ORDER BY timestamp DESC")
    List<GameActionEntity> getAllActions();
    
    @Query("SELECT * FROM game_actions WHERE session_id = :sessionId ORDER BY timestamp DESC")
    List<GameActionEntity> getActionsBySession(long sessionId);
    
    @Query("SELECT * FROM game_actions WHERE action_type = :actionType ORDER BY timestamp DESC")
    List<GameActionEntity> getActionsByType(String actionType);
    
    @Query("SELECT * FROM game_actions WHERE success = :success ORDER BY timestamp DESC")
    List<GameActionEntity> getActionsBySuccess(boolean success);
    
    @Query("SELECT * FROM game_actions WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<GameActionEntity> getActionsByTimeRange(long startTime, long endTime);
    
    @Query("SELECT * FROM game_actions WHERE confidence >= :minConfidence ORDER BY confidence DESC")
    List<GameActionEntity> getActionsWithMinConfidence(float minConfidence);
    
    @Query("SELECT * FROM game_actions WHERE source = :source ORDER BY timestamp DESC")
    List<GameActionEntity> getActionsBySource(String source);
    
    @Query("SELECT AVG(confidence) FROM game_actions WHERE action_type = :actionType AND success = 1")
    Float getAverageSuccessConfidence(String actionType);
    
    @Query("SELECT COUNT(*) FROM game_actions WHERE action_type = :actionType AND success = 1")
    int getSuccessCountForAction(String actionType);
    
    @Query("SELECT COUNT(*) FROM game_actions WHERE session_id = :sessionId")
    int getActionCountForSession(long sessionId);
    
    @Insert
    long insertAction(GameActionEntity action);
    
    @Insert
    void insertActions(List<GameActionEntity> actions);
    
    @Update
    void updateAction(GameActionEntity action);
    
    @Delete
    void deleteAction(GameActionEntity action);
    
    @Query("DELETE FROM game_actions WHERE session_id = :sessionId")
    void deleteActionsBySession(long sessionId);
    
    @Query("DELETE FROM game_actions WHERE timestamp < :cutoffTime")
    void deleteOldActions(long cutoffTime);
}