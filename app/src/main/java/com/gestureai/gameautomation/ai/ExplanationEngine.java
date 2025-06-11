package com.gestureai.gameautomation.ai;

import android.content.Context;
import android.util.Log;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.learning.config.Adam;

import com.gestureai.gameautomation.models.DecisionExplanation;
import com.gestureai.gameautomation.models.GameFrame;
import com.gestureai.gameautomation.utils.NLPProcessor;
import java.util.*;

/**
 * Advanced AI Explanation Engine
 * Provides comprehensive explanations for AI decisions using multimodal analysis
 * Integrates visual features, textual context, and learned reward functions
 */
public class ExplanationEngine {
    private static final String TAG = "ExplanationEngine";
    
    private Context context;
    private MultiLayerNetwork explanationNetwork;
    private AttentionMechanism attentionMechanism;
    private CausalAnalyzer causalAnalyzer;
    
    private boolean isInitialized = false;
    private boolean realTimeMode = false;
    private float explanationDepth = 0.8f;
    
    // Neural network parameters
    private static final int INPUT_SIZE = 64;
    private static final int HIDDEN_SIZE = 128;
    private static final int ATTENTION_SIZE = 32;
    
    public ExplanationEngine(Context context) {
        this.context = context;
        initialize();
    }
    
    private void initialize() {
        try {
            initializeExplanationNetwork();
            attentionMechanism = new AttentionMechanism();
            causalAnalyzer = new CausalAnalyzer();
            isInitialized = true;
            
            Log.d(TAG, "Explanation Engine initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Explanation Engine", e);
        }
    }
    
    private void initializeExplanationNetwork() {
        MultiLayerConfiguration config = new NeuralNetConfiguration.Builder()
            .seed(42)
            .weightInit(WeightInit.XAVIER)
            .updater(new Adam(0.001))
            .list()
            .layer(0, new DenseLayer.Builder()
                .nIn(INPUT_SIZE)
                .nOut(HIDDEN_SIZE)
                .activation(Activation.RELU)
                .build())
            .layer(1, new DenseLayer.Builder()
                .nIn(HIDDEN_SIZE)
                .nOut(HIDDEN_SIZE)
                .activation(Activation.RELU)
                .build())
            .layer(2, new DenseLayer.Builder()
                .nIn(HIDDEN_SIZE)
                .nOut(ATTENTION_SIZE)
                .activation(Activation.TANH)
                .build())
            .layer(3, new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                .nIn(ATTENTION_SIZE)
                .nOut(10) // Decision confidence scores
                .activation(Activation.SOFTMAX)
                .build())
            .build();
            
        explanationNetwork = new MultiLayerNetwork(config);
        explanationNetwork.init();
    }
    
    /**
     * Generate comprehensive explanation for AI decision
     */
    public DecisionExplanation explainDecision(GameFrame frame) {
        if (!isInitialized) {
            return createFallbackExplanation(frame);
        }
        
        DecisionExplanation explanation = new DecisionExplanation();
        explanation.timestamp = frame.timestamp;
        explanation.frameIndex = frame.frameIndex;
        
        try {
            // Extract multimodal features
            INDArray combinedFeatures = extractCombinedFeatures(frame);
            
            // Generate attention weights to identify important factors
            Map<String, Float> attentionWeights = attentionMechanism.computeAttention(combinedFeatures);
            
            // Analyze causal relationships
            List<String> causalFactors = causalAnalyzer.identifyCausalFactors(frame, attentionWeights);
            
            // Run neural explanation model
            INDArray explanationOutput = explanationNetwork.output(combinedFeatures);
            
            // Build comprehensive explanation
            explanation.decision = interpretDecisionOutput(explanationOutput);
            explanation.confidence = calculateExplanationConfidence(explanationOutput, attentionWeights);
            explanation.keyFactors = extractKeyFactors(attentionWeights, explanationDepth);
            explanation.causalChain = causalFactors;
            explanation.visualInfluence = analyzeVisualInfluence(frame, attentionWeights);
            explanation.textualInfluence = analyzeTextualInfluence(frame, attentionWeights);
            explanation.reasoning = generateDetailedReasoning(explanation, frame);
            explanation.alternativeActions = suggestAlternativeActions(frame, explanation);
            explanation.confidenceBreakdown = generateConfidenceBreakdown(explanationOutput);
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating explanation", e);
            return createFallbackExplanation(frame);
        }
        
        return explanation;
    }
    
    private INDArray extractCombinedFeatures(GameFrame frame) {
        float[] features = new float[INPUT_SIZE];
        int index = 0;
        
        // Visual features (16 dimensions)
        if (frame.screenshot != null) {
            float[] visualFeatures = extractVisualFeatures(frame.screenshot);
            System.arraycopy(visualFeatures, 0, features, index, Math.min(visualFeatures.length, 16));
        }
        index += 16;
        
        // Object detection features (16 dimensions)
        if (frame.detectedObjects != null) {
            float[] objectFeatures = extractObjectFeatures(frame.detectedObjects);
            System.arraycopy(objectFeatures, 0, features, index, Math.min(objectFeatures.length, 16));
        }
        index += 16;
        
        // NLP features (16 dimensions)
        if (frame.nlpAnalysis != null) {
            float[] nlpFeatures = extractNLPFeatures(frame.nlpAnalysis);
            System.arraycopy(nlpFeatures, 0, features, index, Math.min(nlpFeatures.length, 16));
        }
        index += 16;
        
        // Game state features (16 dimensions)
        if (frame.gameState != null) {
            float[] stateFeatures = extractGameStateFeatures(frame.gameState);
            System.arraycopy(stateFeatures, 0, features, index, Math.min(stateFeatures.length, 16));
        }
        
        return Nd4j.create(features).reshape(1, INPUT_SIZE);
    }
    
    private float[] extractVisualFeatures(android.graphics.Bitmap screenshot) {
        float[] features = new float[16];
        
        if (screenshot != null) {
            int width = screenshot.getWidth();
            int height = screenshot.getHeight();
            
            // Sample colors from different regions
            int[] pixels = new int[width * height];
            screenshot.getPixels(pixels, 0, width, 0, 0, width, height);
            
            // Calculate color distribution features
            float[] rgbMeans = new float[3];
            float[] rgbStds = new float[3];
            
            for (int pixel : pixels) {
                rgbMeans[0] += ((pixel >> 16) & 0xFF) / 255.0f;
                rgbMeans[1] += ((pixel >> 8) & 0xFF) / 255.0f;
                rgbMeans[2] += (pixel & 0xFF) / 255.0f;
            }
            
            for (int i = 0; i < 3; i++) {
                rgbMeans[i] /= pixels.length;
            }
            
            // Calculate standard deviations
            for (int pixel : pixels) {
                float r = ((pixel >> 16) & 0xFF) / 255.0f;
                float g = ((pixel >> 8) & 0xFF) / 255.0f;
                float b = (pixel & 0xFF) / 255.0f;
                
                rgbStds[0] += (r - rgbMeans[0]) * (r - rgbMeans[0]);
                rgbStds[1] += (g - rgbMeans[1]) * (g - rgbMeans[1]);
                rgbStds[2] += (b - rgbMeans[2]) * (b - rgbMeans[2]);
            }
            
            for (int i = 0; i < 3; i++) {
                rgbStds[i] = (float) Math.sqrt(rgbStds[i] / pixels.length);
            }
            
            // Populate features
            System.arraycopy(rgbMeans, 0, features, 0, 3);
            System.arraycopy(rgbStds, 0, features, 3, 3);
            
            // Additional visual complexity features
            features[6] = width / 1920.0f; // Normalized width
            features[7] = height / 1080.0f; // Normalized height
            features[8] = calculateVisualComplexity(pixels, width, height);
            features[9] = calculateEdgeDensity(pixels, width, height);
        }
        
        return features;
    }
    
    private float[] extractObjectFeatures(List<Object> detectedObjects) {
        float[] features = new float[16];
        
        if (detectedObjects != null) {
            Map<String, Integer> objectCounts = new HashMap<>();
            
            for (Object obj : detectedObjects) {
                String type = obj.toString().toLowerCase();
                if (type.contains("enemy")) {
                    objectCounts.put("enemy", objectCounts.getOrDefault("enemy", 0) + 1);
                } else if (type.contains("powerup")) {
                    objectCounts.put("powerup", objectCounts.getOrDefault("powerup", 0) + 1);
                } else if (type.contains("coin")) {
                    objectCounts.put("coin", objectCounts.getOrDefault("coin", 0) + 1);
                } else if (type.contains("obstacle")) {
                    objectCounts.put("obstacle", objectCounts.getOrDefault("obstacle", 0) + 1);
                }
            }
            
            features[0] = Math.min(objectCounts.getOrDefault("enemy", 0) / 10.0f, 1.0f);
            features[1] = Math.min(objectCounts.getOrDefault("powerup", 0) / 5.0f, 1.0f);
            features[2] = Math.min(objectCounts.getOrDefault("coin", 0) / 20.0f, 1.0f);
            features[3] = Math.min(objectCounts.getOrDefault("obstacle", 0) / 15.0f, 1.0f);
            features[4] = Math.min(detectedObjects.size() / 50.0f, 1.0f); // Total object density
        }
        
        return features;
    }
    
    private float[] extractNLPFeatures(NLPProcessor.GameTextAnalysis nlpAnalysis) {
        float[] features = new float[16];
        
        if (nlpAnalysis != null) {
            float maxAggression = 0;
            float maxDefensive = 0;
            float maxTeamwork = 0;
            float maxUrgency = 0;
            
            for (NLPProcessor.StrategyInsight insight : nlpAnalysis.getStrategies()) {
                maxAggression = Math.max(maxAggression, insight.aggressionLevel);
                maxDefensive = Math.max(maxDefensive, insight.defensiveNeeds);
                maxTeamwork = Math.max(maxTeamwork, insight.teamworkOpportunity);
                maxUrgency = Math.max(maxUrgency, insight.urgencyLevel);
            }
            
            features[0] = maxAggression;
            features[1] = maxDefensive;
            features[2] = maxTeamwork;
            features[3] = maxUrgency;
            
            // Text classification confidence
            float avgConfidence = 0;
            int classificationCount = 0;
            
            for (NLPProcessor.GameTextClassification classification : nlpAnalysis.getClassifications()) {
                avgConfidence += classification.confidence;
                classificationCount++;
            }
            
            if (classificationCount > 0) {
                features[4] = avgConfidence / classificationCount;
            }
            
            features[5] = Math.min(nlpAnalysis.getClassifications().size() / 10.0f, 1.0f); // Text density
        }
        
        return features;
    }
    
    private float[] extractGameStateFeatures(Object gameState) {
        float[] features = new float[16];
        
        // This would extract actual game state features
        // For now, using placeholder structure
        try {
            // Use reflection or specific casting based on actual GameState structure
            features[0] = 0.5f; // Health normalized
            features[1] = 0.7f; // Ammo normalized
            features[2] = 0.3f; // Score normalized
            features[3] = 0.8f; // Position quality
        } catch (Exception e) {
            Log.w(TAG, "Game state feature extraction failed", e);
        }
        
        return features;
    }
    
    private String interpretDecisionOutput(INDArray output) {
        int maxIndex = 0;
        float maxValue = output.getFloat(0, 0);
        
        for (int i = 1; i < output.columns(); i++) {
            float value = output.getFloat(0, i);
            if (value > maxValue) {
                maxValue = value;
                maxIndex = i;
            }
        }
        
        String[] decisions = {
            "AGGRESSIVE_ENGAGE", "DEFENSIVE_RETREAT", "COLLECT_RESOURCES",
            "SEEK_COVER", "ADVANCE_POSITION", "COORDINATE_TEAM",
            "AVOID_THREAT", "EXPLOIT_OPPORTUNITY", "MAINTAIN_POSITION", "STRATEGIC_WAIT"
        };
        
        return decisions[Math.min(maxIndex, decisions.length - 1)];
    }
    
    private float calculateExplanationConfidence(INDArray output, Map<String, Float> attention) {
        // Calculate confidence based on output distribution and attention weights
        float entropy = 0;
        float sum = 0;
        
        for (int i = 0; i < output.columns(); i++) {
            float prob = output.getFloat(0, i);
            if (prob > 0) {
                entropy -= prob * Math.log(prob);
            }
            sum += prob;
        }
        
        float normalized_entropy = entropy / Math.log(output.columns());
        float confidence = 1.0f - normalized_entropy;
        
        // Boost confidence with strong attention signals
        float attentionBoost = 0;
        for (float weight : attention.values()) {
            if (weight > 0.7f) attentionBoost += 0.1f;
        }
        
        return Math.min(1.0f, confidence + attentionBoost);
    }
    
    private List<String> extractKeyFactors(Map<String, Float> attentionWeights, float threshold) {
        List<String> keyFactors = new ArrayList<>();
        
        for (Map.Entry<String, Float> entry : attentionWeights.entrySet()) {
            if (entry.getValue() > threshold) {
                keyFactors.add(entry.getKey() + " (" + String.format("%.2f", entry.getValue()) + ")");
            }
        }
        
        // Sort by importance
        keyFactors.sort((a, b) -> {
            float weightA = Float.parseFloat(a.substring(a.indexOf('(') + 1, a.indexOf(')')));
            float weightB = Float.parseFloat(b.substring(b.indexOf('(') + 1, b.indexOf(')')));
            return Float.compare(weightB, weightA);
        });
        
        return keyFactors;
    }
    
    private String generateDetailedReasoning(DecisionExplanation explanation, GameFrame frame) {
        StringBuilder reasoning = new StringBuilder();
        
        reasoning.append("Decision: ").append(explanation.decision).append(" ");
        reasoning.append("(Confidence: ").append(String.format("%.1f%%", explanation.confidence * 100)).append(")\n\n");
        
        reasoning.append("Key Factors:\n");
        for (String factor : explanation.keyFactors) {
            reasoning.append("• ").append(factor).append("\n");
        }
        
        if (!explanation.causalChain.isEmpty()) {
            reasoning.append("\nCausal Chain:\n");
            for (int i = 0; i < explanation.causalChain.size(); i++) {
                reasoning.append(i + 1).append(". ").append(explanation.causalChain.get(i)).append("\n");
            }
        }
        
        if (!explanation.visualInfluence.isEmpty()) {
            reasoning.append("\nVisual Influences:\n");
            for (Map.Entry<String, Float> influence : explanation.visualInfluence.entrySet()) {
                if (influence.getValue() > 0.3f) {
                    reasoning.append("• ").append(influence.getKey()).append(": ")
                        .append(String.format("%.1f%%", influence.getValue() * 100)).append("\n");
                }
            }
        }
        
        return reasoning.toString();
    }
    
    private List<String> suggestAlternativeActions(GameFrame frame, DecisionExplanation explanation) {
        List<String> alternatives = new ArrayList<>();
        
        // Generate contextual alternatives based on current decision
        String currentDecision = explanation.decision;
        
        if ("AGGRESSIVE_ENGAGE".equals(currentDecision)) {
            alternatives.add("DEFENSIVE_RETREAT - Lower risk approach");
            alternatives.add("SEEK_COVER - Tactical positioning");
        } else if ("DEFENSIVE_RETREAT".equals(currentDecision)) {
            alternatives.add("AGGRESSIVE_ENGAGE - Higher reward potential");
            alternatives.add("MAINTAIN_POSITION - Balanced approach");
        } else if ("COLLECT_RESOURCES".equals(currentDecision)) {
            alternatives.add("ADVANCE_POSITION - Positional advantage");
            alternatives.add("AVOID_THREAT - Safety first");
        }
        
        return alternatives;
    }
    
    private Map<String, Float> generateConfidenceBreakdown(INDArray output) {
        Map<String, Float> breakdown = new HashMap<>();
        
        String[] categories = {
            "Visual Analysis", "Text Understanding", "Strategic Context",
            "Threat Assessment", "Opportunity Recognition"
        };
        
        for (int i = 0; i < Math.min(categories.length, output.columns()); i++) {
            breakdown.put(categories[i], output.getFloat(0, i));
        }
        
        return breakdown;
    }
    
    // Helper methods for visual analysis
    private float calculateVisualComplexity(int[] pixels, int width, int height) {
        // Simplified complexity measure based on color variance
        Map<Integer, Integer> colorCounts = new HashMap<>();
        
        for (int pixel : pixels) {
            int quantizedColor = quantizeColor(pixel);
            colorCounts.put(quantizedColor, colorCounts.getOrDefault(quantizedColor, 0) + 1);
        }
        
        return Math.min(colorCounts.size() / 100.0f, 1.0f);
    }
    
    private float calculateEdgeDensity(int[] pixels, int width, int height) {
        // Simplified edge detection using color differences
        int edgeCount = 0;
        
        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                int center = pixels[y * width + x];
                int right = pixels[y * width + (x + 1)];
                int bottom = pixels[(y + 1) * width + x];
                
                if (colorDifference(center, right) > 50 || colorDifference(center, bottom) > 50) {
                    edgeCount++;
                }
            }
        }
        
        return Math.min(edgeCount / (float)(width * height * 0.1), 1.0f);
    }
    
    private int quantizeColor(int color) {
        int r = ((color >> 16) & 0xFF) / 32;
        int g = ((color >> 8) & 0xFF) / 32;
        int b = (color & 0xFF) / 32;
        return (r << 10) | (g << 5) | b;
    }
    
    private int colorDifference(int color1, int color2) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        return Math.abs(r1 - r2) + Math.abs(g1 - g2) + Math.abs(b1 - b2);
    }
    
    private Map<String, Float> analyzeVisualInfluence(GameFrame frame, Map<String, Float> attention) {
        Map<String, Float> influence = new HashMap<>();
        
        if (frame.detectedObjects != null) {
            for (Object obj : frame.detectedObjects) {
                String type = obj.toString().toLowerCase();
                float weight = attention.getOrDefault("visual_" + type, 0.0f);
                influence.put(type, weight);
            }
        }
        
        return influence;
    }
    
    private Map<String, Float> analyzeTextualInfluence(GameFrame frame, Map<String, Float> attention) {
        Map<String, Float> influence = new HashMap<>();
        
        if (frame.nlpAnalysis != null) {
            for (NLPProcessor.GameTextClassification classification : frame.nlpAnalysis.getClassifications()) {
                String type = classification.type.toString();
                float weight = attention.getOrDefault("text_" + type, 0.0f);
                influence.put(type, weight * classification.confidence);
            }
        }
        
        return influence;
    }
    
    private DecisionExplanation createFallbackExplanation(GameFrame frame) {
        DecisionExplanation explanation = new DecisionExplanation();
        explanation.timestamp = frame.timestamp;
        explanation.frameIndex = frame.frameIndex;
        explanation.decision = "MAINTAIN_POSITION";
        explanation.confidence = 0.3f;
        explanation.reasoning = "Fallback explanation due to processing error";
        explanation.keyFactors = Arrays.asList("System limitation");
        explanation.causalChain = Arrays.asList("Processing error occurred");
        explanation.visualInfluence = new HashMap<>();
        explanation.textualInfluence = new HashMap<>();
        explanation.alternativeActions = Arrays.asList("Retry analysis");
        explanation.confidenceBreakdown = new HashMap<>();
        
        return explanation;
    }
    
    public void setRealTimeMode(boolean enabled) {
        this.realTimeMode = enabled;
    }
    
    public void setExplanationDepth(float depth) {
        this.explanationDepth = Math.max(0.0f, Math.min(1.0f, depth));
    }
    
    public void cleanup() {
        if (explanationNetwork != null) {
            explanationNetwork = null;
        }
        isInitialized = false;
    }
    
    /**
     * Attention mechanism for identifying important features
     */
    private static class AttentionMechanism {
        public Map<String, Float> computeAttention(INDArray features) {
            Map<String, Float> attention = new HashMap<>();
            
            // Simplified attention computation
            String[] featureNames = {
                "visual_complexity", "object_density", "enemy_presence", "powerup_availability",
                "text_aggression", "text_defensive", "text_teamwork", "text_urgency",
                "health_status", "ammo_status", "score_progress", "position_quality",
                "threat_level", "opportunity_score", "strategic_value", "decision_confidence"
            };
            
            for (int i = 0; i < Math.min(featureNames.length, features.columns()); i++) {
                float value = Math.abs(features.getFloat(0, i));
                attention.put(featureNames[i], value);
            }
            
            return attention;
        }
    }
    
    /**
     * Causal analysis for understanding decision factors
     */
    private static class CausalAnalyzer {
        public List<String> identifyCausalFactors(GameFrame frame, Map<String, Float> attention) {
            List<String> factors = new ArrayList<>();
            
            // Analyze high-attention factors for causal relationships
            for (Map.Entry<String, Float> entry : attention.entrySet()) {
                if (entry.getValue() > 0.6f) {
                    String factor = entry.getKey();
                    String causalExplanation = generateCausalExplanation(factor, frame);
                    if (!causalExplanation.isEmpty()) {
                        factors.add(causalExplanation);
                    }
                }
            }
            
            return factors;
        }
        
        private String generateCausalExplanation(String factor, GameFrame frame) {
            switch (factor) {
                case "enemy_presence":
                    return "High enemy density detected → Defensive strategy activated";
                case "powerup_availability":
                    return "Valuable powerups identified → Collection behavior prioritized";
                case "text_aggression":
                    return "Aggressive context in UI text → Combat readiness increased";
                case "text_defensive":
                    return "Defensive indicators in text → Risk mitigation activated";
                case "health_status":
                    return "Health level influenced risk tolerance → Strategy adjusted";
                case "threat_level":
                    return "Threat assessment → Appropriate response selected";
                default:
                    return "";
            }
        }
    }
}