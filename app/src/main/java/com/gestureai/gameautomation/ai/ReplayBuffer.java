package com.gestureai.gameautomation.ai;

import com.gestureai.gameautomation.GameAction;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ReplayBuffer {
    private List<Experience> buffer;
    private int maxSize;
    private Random random;

    public ReplayBuffer(int maxSize) {
        this.maxSize = maxSize;
        this.buffer = new ArrayList<>();
        this.random = new Random();
    }

    public void addExperience(GameStrategyAgent.UniversalGameState state, GameAction action, 
                             float reward, GameStrategyAgent.UniversalGameState nextState, boolean gameOver) {
        Experience experience = new Experience(state, action, reward, nextState, gameOver);
        
        if (buffer.size() >= maxSize) {
            buffer.remove(0); // Remove oldest experience
        }
        
        buffer.add(experience);
    }

    public List<Experience> sampleBatch(int batchSize) {
        List<Experience> batch = new ArrayList<>();
        
        if (buffer.size() < batchSize) {
            return new ArrayList<>(buffer); // Return all if not enough samples
        }
        
        for (int i = 0; i < batchSize; i++) {
            int randomIndex = random.nextInt(buffer.size());
            batch.add(buffer.get(randomIndex));
        }
        
        return batch;
    }

    public int size() {
        return buffer.size();
    }

    public void clear() {
        buffer.clear();
    }
}