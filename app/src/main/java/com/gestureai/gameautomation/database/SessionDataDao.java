package com.gestureai.gameautomation.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.gestureai.gameautomation.data.SessionData;

import java.util.List;

@Dao
public interface SessionDataDao {
    
    @Query("SELECT * FROM session_data ORDER BY session_start_time DESC")
    List<SessionData> getAllSessions();

    @Query("SELECT * FROM session_data WHERE id = :sessionId")
    SessionData getSessionById(long sessionId);

    @Query("SELECT * FROM session_data WHERE game_type = :gameType ORDER BY session_start_time DESC")
    List<SessionData> getSessionsByGameType(String gameType);

    @Query("SELECT * FROM session_data WHERE automation_enabled = 1 ORDER BY session_start_time DESC")
    List<SessionData> getAutomatedSessions();

    @Query("SELECT * FROM session_data WHERE session_start_time >= :startTime AND session_start_time <= :endTime")
    List<SessionData> getSessionsInTimeRange(long startTime, long endTime);

    @Query("SELECT AVG(success_rate) FROM session_data WHERE game_type = :gameType AND automation_enabled = 1")
    Float getAverageSuccessRateForGameType(String gameType);

    @Query("SELECT COUNT(*) FROM session_data")
    int getTotalSessionCount();

    @Query("SELECT SUM(actions_performed) FROM session_data WHERE automation_enabled = 1")
    int getTotalAutomatedActions();

    @Query("SELECT * FROM session_data ORDER BY session_start_time DESC LIMIT :limit")
    List<SessionData> getRecentSessions(int limit);

    @Query("DELETE FROM session_data WHERE session_start_time < :cutoffTime")
    void deleteOldSessions(long cutoffTime);

    @Insert
    long insertSession(SessionData session);

    @Update
    void updateSession(SessionData session);

    @Delete
    void deleteSession(SessionData session);

    @Query("DELETE FROM session_data")
    void deleteAllSessions();
}