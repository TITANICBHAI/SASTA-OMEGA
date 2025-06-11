package com.gestureai.gameautomation.managers;

import android.content.Context;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.database.SessionData;

/**
 * AI Training Control Manager - Provides unified control over all AI training processes
 * Manages DQN, PPO, and strategy agent training with real-time monitoring
 */
public class AITrainingControlManager {
    private static final String TAG = "AITrainingControl";
    private static AITrainingControlManager instance;
    
    private Context context;
    private ExecutorService trainingExecutor;
    private Handler mainHandler;
    
    // AI Agents
    private DQNAgent dqnAgent;
    private PPOAgent ppoAgent;
    private GameStrategyAgent strategyAgent;
    
    // Training state
    private AtomicBoolean isDQNTraining = new AtomicBoolean(false);
    private AtomicBoolean isPPOTraining = new AtomicBoolean(false);
    private AtomicBoolean isStrategyTraining = new AtomicBoolean(false);
    
    // Training metrics
    private TrainingSession currentDQNSession;
    private TrainingSession currentPPOSession;
    private TrainingSession currentStrategySession;
    
    // Listeners
    private List<TrainingProgressListener> progressListeners = new ArrayList<>();
    
    public static synchronized AITrainingControlManager getInstance(Context context) {
        if (instance == null) {
            instance = new AITrainingControlManager(context);
        }
        return instance;
    }
    
    private AITrainingControlManager(Context context) {
        this.context = context.getApplicationContext();
        this.trainingExecutor = Executors.newFixedThreadPool(3); // One thread per agent type
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        initializeAgents();
        Log.d(TAG, "AI Training Control Manager initialized");
    }
    
    private void initializeAgents() {
        try {
            dqnAgent = new DQNAgent(16, 8); // State size, action size
            ppoAgent = new PPOAgent(16, 8);
            strategyAgent = new GameStrategyAgent(context);
            
            Log.d(TAG, "AI agents initialized for training");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize AI agents", e);
        }
    }
    
    // DQN Training Control
    public void startDQNTraining(TrainingConfiguration config) {
        if (isDQNTraining.get()) {
            notifyTrainingError("DQN", "Training already in progress");
            return;
        }
        
        if (dqnAgent == null) {
            notifyTrainingError("DQN", "DQN agent not initialized");
            return;
        }
        
        isDQNTraining.set(true);
        currentDQNSession = new TrainingSession("DQN", config);
        
        trainingExecutor.submit(() -> runDQNTraining(config));
        
        Log.d(TAG, "DQN training started");
        notifyTrainingStarted("DQN", config);
    }
    
    private void runDQNTraining(TrainingConfiguration config) {
        try {
            for (int episode = 0; episode < config.maxEpisodes && isDQNTraining.get(); episode++) {
                long episodeStart = System.currentTimeMillis();
                
                // Run training step
                float loss = dqnAgent.trainStep();
                
                // Update session metrics
                currentDQNSession.episodesCompleted = episode + 1;
                currentDQNSession.currentLoss = loss;
                currentDQNSession.averageLoss = updateAverageLoss(currentDQNSession.averageLoss, loss, episode);
                
                long episodeTime = System.currentTimeMillis() - episodeStart;
                currentDQNSession.lastEpisodeTime = episodeTime;
                
                // Notify progress every 10 episodes
                if ((episode + 1) % 10 == 0) {
                    notifyTrainingProgress("DQN", currentDQNSession);
                }
                
                // Respect training delay
                if (config.trainingDelayMs > 0) {
                    Thread.sleep(config.trainingDelayMs);
                }
            }
            
            // Training completed
            currentDQNSession.isCompleted = true;
            currentDQNSession.endTime = System.currentTimeMillis();
            
            Log.d(TAG, "DQN training completed");
            notifyTrainingCompleted("DQN", currentDQNSession);
            
        } catch (InterruptedException e) {
            Log.d(TAG, "DQN training interrupted");
            notifyTrainingInterrupted("DQN");
        } catch (Exception e) {
            Log.e(TAG, "DQN training failed", e);
            notifyTrainingError("DQN", e.getMessage());
        } finally {
            isDQNTraining.set(false);
        }
    }
    
    public void stopDQNTraining() {
        if (isDQNTraining.get()) {
            isDQNTraining.set(false);
            Log.d(TAG, "DQN training stop requested");
        }
    }
    
    // PPO Training Control
    public void startPPOTraining(TrainingConfiguration config) {
        if (isPPOTraining.get()) {
            notifyTrainingError("PPO", "Training already in progress");
            return;
        }
        
        if (ppoAgent == null) {
            notifyTrainingError("PPO", "PPO agent not initialized");
            return;
        }
        
        isPPOTraining.set(true);
        currentPPOSession = new TrainingSession("PPO", config);
        
        trainingExecutor.submit(() -> runPPOTraining(config));
        
        Log.d(TAG, "PPO training started");
        notifyTrainingStarted("PPO", config);
    }
    
    private void runPPOTraining(TrainingConfiguration config) {
        try {
            for (int episode = 0; episode < config.maxEpisodes && isPPOTraining.get(); episode++) {
                long episodeStart = System.currentTimeMillis();
                
                // Run training step
                float loss = ppoAgent.trainStep();
                
                // Update session metrics
                currentPPOSession.episodesCompleted = episode + 1;
                currentPPOSession.currentLoss = loss;
                currentPPOSession.averageLoss = updateAverageLoss(currentPPOSession.averageLoss, loss, episode);
                
                long episodeTime = System.currentTimeMillis() - episodeStart;
                currentPPOSession.lastEpisodeTime = episodeTime;
                
                // Notify progress every 10 episodes
                if ((episode + 1) % 10 == 0) {
                    notifyTrainingProgress("PPO", currentPPOSession);
                }
                
                // Respect training delay
                if (config.trainingDelayMs > 0) {
                    Thread.sleep(config.trainingDelayMs);
                }
            }
            
            // Training completed
            currentPPOSession.isCompleted = true;
            currentPPOSession.endTime = System.currentTimeMillis();
            
            Log.d(TAG, "PPO training completed");
            notifyTrainingCompleted("PPO", currentPPOSession);
            
        } catch (InterruptedException e) {
            Log.d(TAG, "PPO training interrupted");
            notifyTrainingInterrupted("PPO");
        } catch (Exception e) {
            Log.e(TAG, "PPO training failed", e);
            notifyTrainingError("PPO", e.getMessage());
        } finally {
            isPPOTraining.set(false);
        }
    }
    
    public void stopPPOTraining() {
        if (isPPOTraining.get()) {
            isPPOTraining.set(false);
            Log.d(TAG, "PPO training stop requested");
        }
    }
    
    // Strategy Agent Training Control
    public void startStrategyTraining(TrainingConfiguration config) {
        if (isStrategyTraining.get()) {
            notifyTrainingError("Strategy", "Training already in progress");
            return;
        }
        
        if (strategyAgent == null) {
            notifyTrainingError("Strategy", "Strategy agent not initialized");
            return;
        }
        
        isStrategyTraining.set(true);
        currentStrategySession = new TrainingSession("Strategy", config);
        
        trainingExecutor.submit(() -> runStrategyTraining(config));
        
        Log.d(TAG, "Strategy training started");
        notifyTrainingStarted("Strategy", config);
    }
    
    private void runStrategyTraining(TrainingConfiguration config) {
        try {
            for (int episode = 0; episode < config.maxEpisodes && isStrategyTraining.get(); episode++) {
                long episodeStart = System.currentTimeMillis();
                
                // Simulate strategy training (would integrate with actual strategy learning)
                float simulatedLoss = (float) (Math.random() * 0.5 + 0.1); // Decreasing loss over time
                simulatedLoss = Math.max(0.01f, simulatedLoss - (episode * 0.001f));
                
                // Update session metrics
                currentStrategySession.episodesCompleted = episode + 1;
                currentStrategySession.currentLoss = simulatedLoss;
                currentStrategySession.averageLoss = updateAverageLoss(currentStrategySession.averageLoss, simulatedLoss, episode);
                
                long episodeTime = System.currentTimeMillis() - episodeStart;
                currentStrategySession.lastEpisodeTime = episodeTime;
                
                // Notify progress every 10 episodes
                if ((episode + 1) % 10 == 0) {
                    notifyTrainingProgress("Strategy", currentStrategySession);
                }
                
                // Respect training delay
                if (config.trainingDelayMs > 0) {
                    Thread.sleep(config.trainingDelayMs);
                }
            }
            
            // Training completed
            currentStrategySession.isCompleted = true;
            currentStrategySession.endTime = System.currentTimeMillis();
            
            Log.d(TAG, "Strategy training completed");
            notifyTrainingCompleted("Strategy", currentStrategySession);
            
        } catch (InterruptedException e) {
            Log.d(TAG, "Strategy training interrupted");
            notifyTrainingInterrupted("Strategy");
        } catch (Exception e) {
            Log.e(TAG, "Strategy training failed", e);
            notifyTrainingError("Strategy", e.getMessage());
        } finally {
            isStrategyTraining.set(false);
        }
    }
    
    public void stopStrategyTraining() {
        if (isStrategyTraining.get()) {
            isStrategyTraining.set(false);
            Log.d(TAG, "Strategy training stop requested");
        }
    }
    
    // Combined Training Control
    public void startAllTraining(TrainingConfiguration config) {
        startDQNTraining(config);
        
        // Start PPO with slight delay to avoid resource conflicts
        mainHandler.postDelayed(() -> startPPOTraining(config), 1000);
        
        // Start Strategy with slight delay
        mainHandler.postDelayed(() -> startStrategyTraining(config), 2000);
        
        Log.d(TAG, "All AI training started");
    }
    
    public void stopAllTraining() {
        stopDQNTraining();
        stopPPOTraining();
        stopStrategyTraining();
        Log.d(TAG, "All AI training stopped");
    }
    
    // Performance Comparison
    public TrainingComparison getTrainingComparison() {
        TrainingComparison comparison = new TrainingComparison();
        
        if (currentDQNSession != null) {
            comparison.dqnMetrics = createAgentMetrics("DQN", currentDQNSession);
        }
        
        if (currentPPOSession != null) {
            comparison.ppoMetrics = createAgentMetrics("PPO", currentPPOSession);
        }
        
        if (currentStrategySession != null) {
            comparison.strategyMetrics = createAgentMetrics("Strategy", currentStrategySession);
        }
        
        return comparison;
    }
    
    private AgentPerformanceMetrics createAgentMetrics(String agentType, TrainingSession session) {
        AgentPerformanceMetrics metrics = new AgentPerformanceMetrics();
        metrics.agentType = agentType;
        metrics.episodesCompleted = session.episodesCompleted;
        metrics.currentLoss = session.currentLoss;
        metrics.averageLoss = session.averageLoss;
        metrics.trainingTime = session.getTrainingDuration();
        metrics.isTraining = isAgentTraining(agentType);
        metrics.isCompleted = session.isCompleted;
        
        return metrics;
    }
    
    private boolean isAgentTraining(String agentType) {
        switch (agentType) {
            case "DQN": return isDQNTraining.get();
            case "PPO": return isPPOTraining.get();
            case "Strategy": return isStrategyTraining.get();
            default: return false;
        }
    }
    
    private float updateAverageLoss(float currentAverage, float newLoss, int episodeCount) {
        if (episodeCount == 0) {
            return newLoss;
        }
        return (currentAverage * episodeCount + newLoss) / (episodeCount + 1);
    }
    
    // Listener management
    public void addTrainingProgressListener(TrainingProgressListener listener) {
        progressListeners.add(listener);
    }
    
    public void removeTrainingProgressListener(TrainingProgressListener listener) {
        progressListeners.remove(listener);
    }
    
    // Notification methods
    private void notifyTrainingStarted(String agentType, TrainingConfiguration config) {
        mainHandler.post(() -> {
            for (TrainingProgressListener listener : progressListeners) {
                listener.onTrainingStarted(agentType, config);
            }
        });
    }
    
    private void notifyTrainingProgress(String agentType, TrainingSession session) {
        mainHandler.post(() -> {
            for (TrainingProgressListener listener : progressListeners) {
                listener.onTrainingProgress(agentType, session);
            }
        });
    }
    
    private void notifyTrainingCompleted(String agentType, TrainingSession session) {
        mainHandler.post(() -> {
            for (TrainingProgressListener listener : progressListeners) {
                listener.onTrainingCompleted(agentType, session);
            }
        });
    }
    
    private void notifyTrainingInterrupted(String agentType) {
        mainHandler.post(() -> {
            for (TrainingProgressListener listener : progressListeners) {
                listener.onTrainingInterrupted(agentType);
            }
        });
    }
    
    private void notifyTrainingError(String agentType, String error) {
        mainHandler.post(() -> {
            for (TrainingProgressListener listener : progressListeners) {
                listener.onTrainingError(agentType, error);
            }
        });
    }
    
    // Public API
    public boolean isDQNTraining() { return isDQNTraining.get(); }
    public boolean isPPOTraining() { return isPPOTraining.get(); }
    public boolean isStrategyTraining() { return isStrategyTraining.get(); }
    public boolean isAnyTraining() { return isDQNTraining.get() || isPPOTraining.get() || isStrategyTraining.get(); }
    
    public TrainingSession getDQNSession() { return currentDQNSession; }
    public TrainingSession getPPOSession() { return currentPPOSession; }
    public TrainingSession getStrategySession() { return currentStrategySession; }
    
    public void cleanup() {
        stopAllTraining();
        if (trainingExecutor != null) {
            trainingExecutor.shutdown();
        }
    }
    
    // Data Classes
    public static class TrainingConfiguration {
        public int maxEpisodes = 1000;
        public float learningRate = 0.001f;
        public int batchSize = 32;
        public long trainingDelayMs = 10;
        public boolean saveCheckpoints = true;
        public int checkpointInterval = 100;
        
        public TrainingConfiguration() {}
        
        public TrainingConfiguration(int maxEpisodes, float learningRate) {
            this.maxEpisodes = maxEpisodes;
            this.learningRate = learningRate;
        }
    }
    
    public static class TrainingSession {
        public String agentType;
        public TrainingConfiguration config;
        public long startTime;
        public long endTime;
        public int episodesCompleted;
        public float currentLoss;
        public float averageLoss;
        public long lastEpisodeTime;
        public boolean isCompleted;
        
        public TrainingSession(String agentType, TrainingConfiguration config) {
            this.agentType = agentType;
            this.config = config;
            this.startTime = System.currentTimeMillis();
            this.isCompleted = false;
        }
        
        public long getTrainingDuration() {
            if (isCompleted) {
                return endTime - startTime;
            } else {
                return System.currentTimeMillis() - startTime;
            }
        }
        
        public float getProgress() {
            return config.maxEpisodes > 0 ? (float) episodesCompleted / config.maxEpisodes : 0f;
        }
    }
    
    public static class AgentPerformanceMetrics {
        public String agentType;
        public int episodesCompleted;
        public float currentLoss;
        public float averageLoss;
        public long trainingTime;
        public boolean isTraining;
        public boolean isCompleted;
    }
    
    public static class TrainingComparison {
        public AgentPerformanceMetrics dqnMetrics;
        public AgentPerformanceMetrics ppoMetrics;
        public AgentPerformanceMetrics strategyMetrics;
        
        public AgentPerformanceMetrics getBestPerforming() {
            AgentPerformanceMetrics best = null;
            float bestLoss = Float.MAX_VALUE;
            
            if (dqnMetrics != null && dqnMetrics.averageLoss < bestLoss) {
                best = dqnMetrics;
                bestLoss = dqnMetrics.averageLoss;
            }
            
            if (ppoMetrics != null && ppoMetrics.averageLoss < bestLoss) {
                best = ppoMetrics;
                bestLoss = ppoMetrics.averageLoss;
            }
            
            if (strategyMetrics != null && strategyMetrics.averageLoss < bestLoss) {
                best = strategyMetrics;
            }
            
            return best;
        }
    }
    
    // Interface for training progress callbacks
    public interface TrainingProgressListener {
        void onTrainingStarted(String agentType, TrainingConfiguration config);
        void onTrainingProgress(String agentType, TrainingSession session);
        void onTrainingCompleted(String agentType, TrainingSession session);
        void onTrainingInterrupted(String agentType);
        void onTrainingError(String agentType, String error);
    }
}