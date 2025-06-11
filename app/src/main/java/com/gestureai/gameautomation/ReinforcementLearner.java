package com.gestureai.gameautomation;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

public class ReinforcementLearner {
    private static final String TAG = "ReinforcementLearner";
    
    private Context context;
    private Map<String, Double> qTable;
    private double learningRate = 0.1;
    private double discountFactor = 0.95;
    private double explorationRate = 0.1;
    
    private List<LearningSession> sessions;
    
    public static class LearningSession {
        public String state;
        public String action;
        public double reward;
        public String nextState;
        public long timestamp;
        
        public LearningSession(String state, String action, double reward, String nextState) {
            this.state = state;
            this.action = action;
            this.reward = reward;
            this.nextState = nextState;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public ReinforcementLearner(Context context) {
        this.context = context;
        this.qTable = new HashMap<>();
        this.sessions = new ArrayList<>();
        Log.d(TAG, "ReinforcementLearner initialized");
    }
    
    public void learnFromAction(String state, String action, double reward, String nextState) {
        // Q-Learning update
        String stateAction = state + "_" + action;
        double currentQ = qTable.getOrDefault(stateAction, 0.0);
        double maxNextQ = getMaxQValue(nextState);
        
        double newQ = currentQ + learningRate * (reward + discountFactor * maxNextQ - currentQ);
        qTable.put(stateAction, newQ);
        
        // Store session
        sessions.add(new LearningSession(state, action, reward, nextState));
        
        Log.d(TAG, "Learned: " + stateAction + " -> " + newQ);
    }
    
    public String getBestAction(String state, List<String> possibleActions) {
        String bestAction = null;
        double bestQ = Double.NEGATIVE_INFINITY;
        
        // Epsilon-greedy exploration
        if (Math.random() < explorationRate) {
            return possibleActions.get((int)(Math.random() * possibleActions.size()));
        }
        
        for (String action : possibleActions) {
            String stateAction = state + "_" + action;
            double q = qTable.getOrDefault(stateAction, 0.0);
            if (q > bestQ) {
                bestQ = q;
                bestAction = action;
            }
        }
        
        return bestAction != null ? bestAction : possibleActions.get(0);
    }
    
    private double getMaxQValue(String state) {
        double maxQ = 0.0;
        for (Map.Entry<String, Double> entry : qTable.entrySet()) {
            if (entry.getKey().startsWith(state + "_")) {
                maxQ = Math.max(maxQ, entry.getValue());
            }
        }
        return maxQ;
    }
    
    public void updateExplorationRate(double rate) {
        this.explorationRate = Math.max(0.01, Math.min(1.0, rate));
    }
    
    public void updateLearningRate(double rate) {
        this.learningRate = Math.max(0.01, Math.min(1.0, rate));
    }
    
    public Map<String, Double> getQTable() {
        return new HashMap<>(qTable);
    }
    
    public List<LearningSession> getSessions() {
        return new ArrayList<>(sessions);
    }
}