package com.gestureai.gameautomation;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public class OCREngine {
    private static final String TAG = "OCREngine";
    
    private Context context;
    private TextRecognizer textRecognizer;
    private List<DetectedText> lastDetectedTexts;
    
    public static class DetectedText {
        public String text;
        public Rect boundingBox;
        public float confidence;
        public String category; // health, score, ammo, etc.
        
        public DetectedText(String text, Rect boundingBox, float confidence) {
            this.text = text;
            this.boundingBox = boundingBox;
            this.confidence = confidence;
            this.category = categorizeText(text);
        }
        
        private static String categorizeText(String text) {
            text = text.toLowerCase();
            if (text.matches(".*\\d+.*")) {
                if (text.contains("hp") || text.contains("health")) return "health";
                if (text.contains("score") || text.matches("\\d+")) return "score";
                if (text.contains("ammo") || text.contains("/")) return "ammo";
                if (text.contains("level") || text.contains("lvl")) return "level";
                return "number";
            }
            if (text.contains("game") || text.contains("over")) return "game_state";
            if (text.contains("pause") || text.contains("menu")) return "ui_state";
            return "text";
        }
    }
    
    public interface OCRCallback {
        void onTextDetected(List<DetectedText> texts);
        void onError(String error);
    }
    
    public OCREngine(Context context) {
        this.context = context;
        this.textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.lastDetectedTexts = new ArrayList<>();
        Log.d(TAG, "OCREngine initialized with ML Kit");
    }
    
    public void extractText(Bitmap bitmap, OCRCallback callback) {
        if (bitmap == null || callback == null) {
            if (callback != null) callback.onError("Invalid input");
            return;
        }
        
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        
        textRecognizer.process(image)
            .addOnSuccessListener(visionText -> {
                List<DetectedText> detectedTexts = new ArrayList<>();
                
                for (Text.TextBlock block : visionText.getTextBlocks()) {
                    for (Text.Line line : block.getLines()) {
                        String text = line.getText();
                        Rect boundingBox = line.getBoundingBox();
                        float confidence = line.getConfidence() != null ? line.getConfidence() : 0.5f;
                        
                        detectedTexts.add(new DetectedText(text, boundingBox, confidence));
                    }
                }
                
                lastDetectedTexts = detectedTexts;
                callback.onTextDetected(detectedTexts);
                Log.d(TAG, "Extracted " + detectedTexts.size() + " text elements");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Text recognition failed", e);
                callback.onError(e.getMessage());
            });
    }
    
    public List<DetectedText> getTextsByCategory(String category) {
        List<DetectedText> filtered = new ArrayList<>();
        for (DetectedText text : lastDetectedTexts) {
            if (category.equals(text.category)) {
                filtered.add(text);
            }
        }
        return filtered;
    }
    
    public String getGameScore() {
        List<DetectedText> scoreTexts = getTextsByCategory("score");
        if (!scoreTexts.isEmpty()) {
            return scoreTexts.get(0).text.replaceAll("[^0-9]", "");
        }
        return "0";
    }
    
    public String getHealthValue() {
        List<DetectedText> healthTexts = getTextsByCategory("health");
        if (!healthTexts.isEmpty()) {
            return healthTexts.get(0).text.replaceAll("[^0-9]", "");
        }
        return "100";
    }
    
    public String getAmmoCount() {
        List<DetectedText> ammoTexts = getTextsByCategory("ammo");
        if (!ammoTexts.isEmpty()) {
            return ammoTexts.get(0).text;
        }
        return "30/90";
    }
    
    public boolean isGameOver() {
        for (DetectedText text : lastDetectedTexts) {
            String textLower = text.text.toLowerCase();
            if (textLower.contains("game over") || textLower.contains("you died") || 
                textLower.contains("defeat") || textLower.contains("eliminated")) {
                return true;
            }
        }
        return false;
    }
    
    public boolean isGamePaused() {
        for (DetectedText text : lastDetectedTexts) {
            String textLower = text.text.toLowerCase();
            if (textLower.contains("pause") || textLower.contains("paused")) {
                return true;
            }
        }
        return false;
    }
    
    public void cleanup() {
        if (textRecognizer != null) {
            textRecognizer.close();
            textRecognizer = null;
        }
        
        if (lastDetectedTexts != null) {
            lastDetectedTexts.clear();
        }
        
        Log.d(TAG, "OCREngine cleaned up");
    }
    
    public List<DetectedText> getLastDetectedTexts() {
        return new ArrayList<>(lastDetectedTexts);
    }
}