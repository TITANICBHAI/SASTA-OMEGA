package com.gestureai.gameautomation.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import com.gestureai.gameautomation.data.SessionData;
import java.util.List;

@Dao
public interface SessionDataDao {
    
    @Insert
    long insertSession(SessionData session);
    
    @Update
    void updateSession(SessionData session);
    
    @Delete
    void deleteSession(SessionData session);
    
    @Query("SELECT * FROM sessions ORDER BY start_time DESC")
    List<SessionData> getAllSessions();
    
    @Query("SELECT * FROM sessions ORDER BY start_time DESC LIMIT :limit")
    List<SessionData> getLatestSessions(int limit);
    
    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    SessionData getSessionById(long sessionId);
    
    @Query("SELECT * FROM sessions WHERE game_type = :gameType ORDER BY start_time DESC")
    List<SessionData> getSessionsByGameType(String gameType);
    
    @Query("SELECT * FROM sessions WHERE ai_strategy = :strategy ORDER BY start_time DESC")
    List<SessionData> getSessionsByStrategy(String strategy);
    
    @Query("SELECT * FROM sessions WHERE start_time >= :startTime AND start_time <= :endTime ORDER BY start_time DESC")
    List<SessionData> getSessionsByTimeRange(long startTime, long endTime);
    
    @Query("SELECT AVG(performance_rating) FROM sessions WHERE game_type = :gameType")
    float getAveragePerformanceForGameType(String gameType);
    
    @Query("SELECT COUNT(*) FROM sessions")
    int getSessionCount();
    
    @Query("SELECT * FROM sessions ORDER BY performance_rating DESC LIMIT :limit")
    List<SessionData> getTopPerformingSessions(int limit);
    
    @Query("DELETE FROM sessions WHERE start_time < :cutoffTime")
    void deleteOldSessions(long cutoffTime);
    
    @Query("DELETE FROM sessions")
    void deleteAllSessions();
    
    @Query("UPDATE sessions SET end_time = :endTime, total_actions = :totalActions, successful_actions = :successfulActions, performance_rating = :rating WHERE id = :sessionId")
    void updateSessionStats(long sessionId, long endTime, int totalActions, int successfulActions, float rating);
    
    @Query("SELECT AVG(average_reaction_time) FROM sessions")
    float getOverallAverageReactionTime();
    
    @Query("SELECT SUM(total_actions) FROM sessions")
    int getTotalActionsPerformed();
}