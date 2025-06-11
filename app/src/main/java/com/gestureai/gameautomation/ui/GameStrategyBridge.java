package com.gestureai.gameautomation.ui;

import android.content.Context;
import android.util.Log;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import java.util.Map;
import java.util.HashMap;

/**
 * Bridge connecting game strategy configuration UI to AI backend
 * Enables users to configure AI behavior for different game types
 */
public class GameStrategyBridge {
    private static final String TAG = "GameStrategyBridge";
    
    private Context context;
    private GameStrategyAgent strategyAgent;
    
    public interface StrategyUpdateListener {
        void onStrategyConfigured(String gameType, Map<String, Float> config);
        void onStrategyEffectivenessChanged(double effectiveness);
    }
    
    private StrategyUpdateListener strategyListener;
    
    public GameStrategyBridge(Context context) {
        this.context = context;
        this.strategyAgent = new GameStrategyAgent(context);
        Log.d(TAG, "Game Strategy Bridge initialized");
    }
    
    public void configureGameStrategy(String gameType, float aggression, float reactionTime, float accuracy) {
        GameStrategyAgent.GameProfile profile = new GameStrategyAgent.GameProfile();
        profile.gameType = gameType;
        profile.aggressiveness = aggression;
        profile.reactionTime = reactionTime;
        profile.accuracy = accuracy;
        
        strategyAgent.setGameProfile(profile);
        
        Map<String, Float> config = new HashMap<>();
        config.put("aggression", aggression);
        config.put("reaction_time", reactionTime);
        config.put("accuracy", accuracy);
        
        if (strategyListener != null) {
            strategyListener.onStrategyConfigured(gameType, config);
        }
        
        Log.d(TAG, "Strategy configured for " + gameType + 
            " - Aggression: " + aggression + 
            ", Reaction: " + reactionTime + 
            ", Accuracy: " + accuracy);
    }
    
    public void setStrategyUpdateListener(StrategyUpdateListener listener) {
        this.strategyListener = listener;
    }
    
    public void cleanup() {
        strategyListener = null;
        Log.d(TAG, "Game Strategy Bridge cleaned up");
    }
}