package com.gestureai.gameautomation.models;

import android.graphics.Bitmap;
import com.gestureai.gameautomation.utils.NLPProcessor;
import java.util.*;

/**
 * Comprehensive game frame data structure
 * Contains all multimodal information captured at a specific moment
 */
public class GameFrame {
    public long timestamp;
    public int frameIndex;
    
    // Visual data
    public Bitmap screenshot;
    public List<Object> detectedObjects;
    
    // Textual data
    public List<String> ocrTexts;
    public NLPProcessor.GameTextAnalysis nlpAnalysis;
    
    // Game state data
    public Object gameState; // Will be cast to actual GameState type
    public List<Object> playerActions;
    
    // Additional metadata
    public Map<String, Object> metadata;
    
    // User explanation fields - NEW for IRL integration
    public String userExplanation;
    public String objectLabel;
    public android.graphics.RectF boundingBox;
    public String playerAction;
    public float playerConfidence;
    
    public GameFrame() {
        this.detectedObjects = new ArrayList<>();
        this.ocrTexts = new ArrayList<>();
        this.playerActions = new ArrayList<>();
        this.metadata = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public boolean hasVisualData() {
        return screenshot != null && detectedObjects != null && !detectedObjects.isEmpty();
    }
    
    public boolean hasTextualData() {
        return ocrTexts != null && !ocrTexts.isEmpty() && nlpAnalysis != null;
    }
    
    public boolean hasGameStateData() {
        return gameState != null;
    }
    
    public int getDataCompleteness() {
        int score = 0;
        if (hasVisualData()) score += 33;
        if (hasTextualData()) score += 33;
        if (hasGameStateData()) score += 34;
        return score;
    }
}