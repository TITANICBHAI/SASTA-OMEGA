package com.gestureai.gameautomation.database.dao;

import androidx.room.*;
import com.gestureai.gameautomation.data.SessionData;
import java.util.List;

@Dao
public interface SessionDataDao {
    
    @Query("SELECT * FROM session_data ORDER BY startTime DESC")
    List<SessionData> getAllSessions();
    
    @Query("SELECT * FROM session_data WHERE id = :sessionId")
    SessionData getSessionById(long sessionId);
    
    @Query("SELECT * FROM session_data WHERE gameType = :gameType ORDER BY startTime DESC")
    List<SessionData> getSessionsByGameType(String gameType);
    
    @Query("SELECT * FROM session_data WHERE startTime BETWEEN :startTime AND :endTime ORDER BY startTime DESC")
    List<SessionData> getSessionsByTimeRange(long startTime, long endTime);
    
    @Query("SELECT * FROM session_data WHERE aiEnabled = :aiEnabled ORDER BY startTime DESC")
    List<SessionData> getSessionsByAIStatus(boolean aiEnabled);
    
    @Query("SELECT AVG(successRate) FROM session_data WHERE gameType = :gameType")
    Float getAverageSuccessRateForGame(String gameType);
    
    @Query("SELECT COUNT(*) FROM session_data WHERE aiEnabled = 1")
    int getAIEnabledSessionCount();
    
    @Query("SELECT SUM(totalActions) FROM session_data WHERE gameType = :gameType")
    int getTotalActionsForGame(String gameType);
    
    @Query("SELECT * FROM session_data ORDER BY startTime DESC LIMIT :limit")
    List<SessionData> getRecentSessions(int limit);
    
    @Query("DELETE FROM session_data WHERE startTime < :cutoffTime")
    void deleteOldSessions(long cutoffTime);
    
    @Insert
    long insertSession(SessionData session);
    
    @Insert
    void insertSessions(List<SessionData> sessions);
    
    @Update
    void updateSession(SessionData session);
    
    @Delete
    void deleteSession(SessionData session);
    
    @Query("DELETE FROM session_data")
    void deleteAllSessions();
}