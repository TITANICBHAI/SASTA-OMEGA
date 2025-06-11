package com.gestureai.gameautomation.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Query;
import java.util.List;

@Dao
public interface SessionDao {
    @Insert
    void insertSession(SessionData sessionData);
    
    @Update
    void updateSession(SessionData sessionData);
    
    @Delete
    void deleteSession(SessionData sessionData);
    
    @Query("DELETE FROM session_data WHERE sessionId = :sessionId")
    void deleteSession(long sessionId);

    @Query("SELECT * FROM session_data ORDER BY startTime DESC")
    List<SessionData> getAllSessions();
    
    @Query("SELECT * FROM session_data ORDER BY startTime DESC LIMIT :limit")
    List<SessionData> getRecentSessions(int limit);
    
    @Query("SELECT * FROM session_data WHERE sessionId = :sessionId")
    SessionData getSessionById(long sessionId);
    
    @Query("SELECT * FROM session_data WHERE startTime >= :startTime AND startTime <= :endTime ORDER BY startTime DESC")
    List<SessionData> getSessionsByDateRange(long startTime, long endTime);
    
    @Query("SELECT * FROM session_data WHERE gamePackage = :gamePackage ORDER BY startTime DESC")
    List<SessionData> getSessionsByGame(String gamePackage);
    
    @Query("SELECT * FROM session_data WHERE automationMode = :mode ORDER BY startTime DESC")
    List<SessionData> getSessionsByMode(String mode);

    @Query("SELECT AVG(CAST(successfulActions AS FLOAT) / totalActions) FROM session_data WHERE totalActions > 0")
    float getAverageSuccessRate();
    
    @Query("SELECT AVG(averageLatency) FROM session_data WHERE averageLatency > 0")
    float getAverageLatency();
    
    @Query("SELECT SUM(totalActions) FROM session_data")
    int getTotalActions();
    
    @Query("SELECT SUM(successfulActions) FROM session_data")
    int getTotalSuccessfulActions();
    
    @Query("SELECT COUNT(*) FROM session_data")
    int getSessionCount();
    
    @Query("SELECT SUM(sessionDuration) FROM session_data")
    long getTotalPlayTime();
    
    @Query("SELECT AVG(sessionDuration) FROM session_data WHERE sessionDuration > 0")
    long getAverageSessionDuration();
    
    @Query("SELECT gamePackage, COUNT(*) as count FROM session_data GROUP BY gamePackage ORDER BY count DESC")
    List<GamePlayStats> getGamePlayStatistics();
    
    @Query("SELECT automationMode, AVG(CAST(successfulActions AS FLOAT) / totalActions) as successRate FROM session_data WHERE totalActions > 0 GROUP BY automationMode")
    List<ModePerformanceStats> getModePerformanceStats();
    
    // Performance analytics queries
    @Query("SELECT * FROM session_data WHERE startTime >= :timestamp ORDER BY startTime DESC")
    List<SessionData> getSessionsSince(long timestamp);
    
    @Query("SELECT MAX(gameScore) FROM session_data WHERE gamePackage = :gamePackage")
    int getHighScore(String gamePackage);
    
    @Query("SELECT * FROM session_data WHERE gameScore = (SELECT MAX(gameScore) FROM session_data WHERE gamePackage = :gamePackage) AND gamePackage = :gamePackage LIMIT 1")
    SessionData getBestSession(String gamePackage);
    
    // Data classes for complex query results
    public static class GamePlayStats {
        public String gamePackage;
        public int count;
    }
    
    public static class ModePerformanceStats {
        public String automationMode;
        public float successRate;
    }
}