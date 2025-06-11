package com.gestureai.gameautomation.systems;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.gestureai.gameautomation.ai.InverseReinforcementLearner;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import com.gestureai.gameautomation.ai.DQNAgent;
import com.gestureai.gameautomation.ai.PPOAgent;
import com.gestureai.gameautomation.ai.NeuralNetworkTrainer;
import com.gestureai.gameautomation.ObjectDetectionEngine;
import com.gestureai.gameautomation.ObjectLabelerEngine;
import com.gestureai.gameautomation.data.UniversalGameState;
import com.gestureai.gameautomation.workflow.WorkflowDefinition;
import com.gestureai.gameautomation.workflow.WorkflowStep;
import com.gestureai.gameautomation.workflow.actions.*;
import com.gestureai.gameautomation.workflow.conditions.*;
import com.gestureai.gameautomation.utils.NLPProcessor;
import com.gestureai.gameautomation.utils.TensorFlowLiteHelper;
import com.gestureai.gameautomation.DetectedObject;
import org.json.JSONObject;
import org.json.JSONArray;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced Expert Demonstration Learning System
 * Multi-frame selection with sophisticated reasoning analysis, semantic understanding,
 * and deep integration with BERT, neural networks, RL/IRL, and object detection systems.
 */
public class ExpertDemonstrationSystem {
    private static final String TAG = "EnhancedExpertDemo";
    
    private Context context;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // Core AI Integration
    private NLPProcessor nlpProcessor;
    private InverseReinforcementLearner irlLearner;
    private GameStrategyAgent strategyAgent;
    private DQNAgent dqnAgent;
    private PPOAgent ppoAgent;
    private NeuralNetworkTrainer neuralTrainer;
    
    // Computer Vision Integration
    private ObjectDetectionEngine detectionEngine;
    private ObjectLabelerEngine labelerEngine;
    private TensorFlowLiteHelper tfHelper;
    
    // Session Management
    private DemonstrationSession currentSession;
    private Map<String, DemonstrationSession> activeSessions;
    private List<CompletedDemonstration> demonstrationLibrary;
    private SemanticLearningEngine semanticEngine;
    
    // Multi-Frame Analysis
    private FrameSequenceAnalyzer sequenceAnalyzer;
    private ReasoningExtractor reasoningExtractor;
    private ActionPatternMiner patternMiner;
    private ContextualDecisionTree decisionTree;
    
    // Learning Configuration
    private boolean realTimeLearning = true;
    private boolean semanticAnalysisEnabled = true;
    private boolean contextualReasoningEnabled = true;
    private DemonstrationListener listener;
    
    public interface DemonstrationListener {
        void onSessionStarted(String sessionName);
        void onFrameAdded(GameFrame frame, int frameCount);
        void onFrameAnalyzed(GameFrame frame, FrameAnalysis analysis);
        void onSequenceCompleted(DemonstrationSequence sequence);
        void onLearningProgress(LearningMetrics metrics);
        void onSessionCompleted(DemonstrationSession session);
        void onError(String error);
    }
    
    public ExpertDemonstrationSystem(Context context) {
        this.context = context;
        this.executorService = Executors.newFixedThreadPool(4);
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        initializeEnhancedSystem();
    }
    
    private void initializeEnhancedSystem() {
        try {
            // Initialize Core AI Components
            nlpProcessor = new NLPProcessor(context);
            irlLearner = new InverseReinforcementLearner(context);
            strategyAgent = new GameStrategyAgent(context);
            dqnAgent = new DQNAgent(context);
            ppoAgent = new PPOAgent(context);
            neuralTrainer = new NeuralNetworkTrainer(context);
            
            // Initialize Computer Vision
            detectionEngine = new ObjectDetectionEngine(context);
            labelerEngine = new ObjectLabelerEngine(context);
            tfHelper = new TensorFlowLiteHelper();
            
            // Initialize Session Management
            activeSessions = new ConcurrentHashMap<>();
            demonstrationLibrary = new ArrayList<>();
            semanticEngine = new SemanticLearningEngine(context);
            
            // Initialize Analysis Components
            sequenceAnalyzer = new FrameSequenceAnalyzer(nlpProcessor);
            reasoningExtractor = new ReasoningExtractor(nlpProcessor);
            patternMiner = new ActionPatternMiner(strategyAgent);
            decisionTree = new ContextualDecisionTree(context);
            
            Log.d(TAG, "Enhanced Expert Demonstration System initialized with full AI integration");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize Enhanced Expert Demonstration System", e);
        }
    }
    
    /**
     * Start new multi-frame demonstration session
     */
    public void startDemonstrationSession(String sessionName, String gameContext) {
        try {
            if (currentSession != null) {
                Log.w(TAG, "Session already in progress, completing previous session");
                completeDemonstrationSession();
            }
            
            currentSession = new DemonstrationSession(sessionName, gameContext);
            currentSession.setStartTime(System.currentTimeMillis());
            
            if (listener != null) {
                listener.onSessionStarted(sessionName);
            }
            
            Log.d(TAG, "Started demonstration session: " + sessionName);
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting demonstration session", e);
            if (listener != null) {
                listener.onError("Failed to start session: " + e.getMessage());
            }
        }
    }
    
    /**
     * Add multiple frames with actions and detailed reasoning
     */
    public void addMultiFrameDemonstration(List<Bitmap> screenshots, List<String> actions, 
                                         List<String> reasons, List<Float> actionXs, 
                                         List<Float> actionYs, String overallStrategy) {
        if (currentSession == null) {
            Log.e(TAG, "No active session. Start a session first.");
            return;
        }
        
        executorService.execute(() -> {
            try {
                List<GameFrame> demonstrationFrames = new ArrayList<>();
                
                // Process each frame in the demonstration
                for (int i = 0; i < screenshots.size(); i++) {
                    GameFrame frame = createEnhancedGameFrame(
                        screenshots.get(i), actions.get(i), reasons.get(i),
                        actionXs.get(i), actionYs.get(i), i, screenshots.size()
                    );
                    
                    // Perform comprehensive analysis
                    FrameAnalysis analysis = analyzeFrameComprehensively(frame);
                    
                    demonstrationFrames.add(frame);
                    currentSession.addFrame(frame, analysis);
                    
                    // Process through AI systems
                    processFrameThroughAI(frame, analysis);
                    
                    mainHandler.post(() -> {
                        if (listener != null) {
                            listener.onFrameAdded(frame, currentSession.getFrameCount());
                            listener.onFrameAnalyzed(frame, analysis);
                        }
                    });
                }
                
                // Analyze the sequence as a whole
                DemonstrationSequence sequence = new DemonstrationSequence(
                    demonstrationFrames, overallStrategy);
                
                // Sequence-level AI processing
                processSequenceThroughAI(sequence);
                currentSession.addSequence(sequence);
                
                // Update learning metrics
                LearningMetrics metrics = progressTracker.updateWithSequence(sequence);
                
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onSequenceCompleted(sequence);
                        listener.onLearningProgress(metrics);
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error adding multi-frame demonstration", e);
                mainHandler.post(() -> {
                    if (listener != null) {
                        listener.onError("Failed to add demonstration: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * Complete current demonstration session and trigger comprehensive learning
     */
    public DemonstrationResult completeDemonstrationSession() {
        if (currentSession == null) {
            Log.w(TAG, "No active session to complete");
            return null;
        }
        
        try {
            currentSession.setEndTime(System.currentTimeMillis());
            
            // Perform comprehensive session analysis
            SessionAnalysisResult sessionAnalysis = analyzeSession(currentSession);
            
            // Trigger advanced learning processes
            ComprehensiveLearningResult learningResult = performComprehensiveLearning(currentSession);
            
            // Create demonstration result
            DemonstrationResult result = new DemonstrationResult(
                currentSession, sessionAnalysis, learningResult);
            
            // Store completed session
            completedSessions.add(currentSession);
            
            // Update global learning metrics
            LearningMetrics finalMetrics = progressTracker.completeSession(currentSession);
            
            if (listener != null) {
                listener.onSessionCompleted(currentSession);
                listener.onLearningProgress(finalMetrics);
            }
            
            currentSession = null;
            
            Log.d(TAG, "Demonstration session completed. Learned from " + 
                  sessionAnalysis.getTotalFrames() + " frames");
            
            return result;
            
        } catch (Exception e) {
            Log.e(TAG, "Error completing demonstration session", e);
            if (listener != null) {
                listener.onError("Failed to complete session: " + e.getMessage());
            }
            return null;
        }
    }
    
    /**
     * Get learning insights and recommendations
     */
    public LearningInsights getLearningInsights() {
        try {
            LearningInsights insights = new LearningInsights();
            
            // Analyze all completed sessions
            for (DemonstrationSession session : completedSessions) {
                SessionInsights sessionInsights = analyzeSessionInsights(session);
                insights.addSessionInsights(sessionInsights);
            }
            
            // Generate recommendations
            List<LearningRecommendation> recommendations = generateLearningRecommendations();
            insights.setRecommendations(recommendations);
            
            // Calculate learning effectiveness
            float effectiveness = calculateLearningEffectiveness();
            insights.setOverallEffectiveness(effectiveness);
            
            return insights;
            
        } catch (Exception e) {
            Log.e(TAG, "Error generating learning insights", e);
            return new LearningInsights();
        }
    }
    
    private GameFrame createEnhancedGameFrame(Bitmap screenshot, String action, String reasoning,
                                            float actionX, float actionY, int sequenceIndex, int sequenceTotal) {
        GameFrame frame = new GameFrame();
        frame.screenshot = screenshot;
        frame.timestamp = System.currentTimeMillis();
        frame.frameIndex = currentSession.getFrameCount();
        
        // Action information
        frame.userAction = action;
        frame.userExplanation = reasoning;
        frame.actionX = actionX;
        frame.actionY = actionY;
        
        // Sequence information
        frame.sequenceIndex = sequenceIndex;
        frame.sequenceTotal = sequenceTotal;
        
        // Enhanced context
        frame.sessionName = currentSession.getName();
        frame.gameContext = currentSession.getGameContext();
        
        return frame;
    }
    
    private FrameAnalysis analyzeFrameComprehensively(GameFrame frame) {
        FrameAnalysis analysis = new FrameAnalysis();
        
        try {
            // NLP analysis of reasoning
            NLPProcessor.GameTextAnalysis nlpAnalysis = 
                nlpProcessor.analyzeGameTextWithBERT(Arrays.asList(frame.userExplanation));
            analysis.setNlpAnalysis(nlpAnalysis);
            
            // Intent classification
            ActionIntent intent = classifyActionIntent(frame);
            analysis.setActionIntent(intent);
            
            // Confidence scoring
            float confidence = calculateFrameConfidence(frame);
            analysis.setConfidence(confidence);
            
            // Visual-textual correlation
            float visualTextMatch = analyzeVisualTextCorrelation(frame);
            analysis.setVisualTextMatch(visualTextMatch);
            
        } catch (Exception e) {
            Log.e(TAG, "Error in comprehensive frame analysis", e);
            analysis.setConfidence(0.5f);
        }
        
        return analysis;
    }
    
    private void processFrameThroughAI(GameFrame frame, FrameAnalysis analysis) {
        try {
            // IRL learning from expert demonstration
            List<GameFrame> trajectory = Arrays.asList(frame);
            irlLearner.learnFromUserExplanation(trajectory, frame.userExplanation, analysis.getActionIntent());
            
            // Strategy agent learning
            strategyAgent.updateStrategyFromUserInput(frame, analysis.getActionIntent(), frame.userExplanation);
            
            // Generate AI explanation for comparison
            String aiExplanation = explanationEngine.explainUserAction(frame, analysis);
            analysis.setAiExplanation(aiExplanation);
            
            // Calculate explanation similarity
            float similarity = calculateExplanationSimilarity(frame.userExplanation, aiExplanation);
            analysis.setExplanationSimilarity(similarity);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing frame through AI", e);
        }
    }
    
    private void processSequenceThroughAI(DemonstrationSequence sequence) {
        try {
            // Process sequence through IRL for pattern learning
            List<GameFrame> frames = sequence.getFrames();
            irlLearner.learnFromSequence(frames, sequence.getOverallStrategy());
            
            // Update strategy with sequence patterns
            strategyAgent.updateWithSequencePattern(sequence);
            
            // Analyze sequence coherence
            float coherence = analyzeSequenceCoherence(sequence);
            sequence.setCoherenceScore(coherence);
            
            Log.d(TAG, "Processed sequence through AI systems, coherence: " + coherence);
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing sequence through AI", e);
        }
    }
    
    private SessionAnalysisResult analyzeSession(DemonstrationSession session) {
        SessionAnalysisResult result = new SessionAnalysisResult();
        
        try {
            result.setTotalFrames(session.getAllFrames().size());
            result.setTotalSequences(session.getSequences().size());
            result.setSessionDuration(session.getDuration());
            
            // Analyze action distribution
            Map<String, Integer> actionDistribution = calculateActionDistribution(session);
            result.setActionDistribution(actionDistribution);
            
            // Analyze reasoning quality
            float avgReasoningQuality = calculateAverageReasoningQuality(session);
            result.setAverageReasoningQuality(avgReasoningQuality);
            
            // Analyze learning effectiveness
            float learningEffectiveness = calculateSessionLearningEffectiveness(session);
            result.setLearningEffectiveness(learningEffectiveness);
            
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing session", e);
        }
        
        return result;
    }
    
    private ComprehensiveLearningResult performComprehensiveLearning(DemonstrationSession session) {
        ComprehensiveLearningResult result = new ComprehensiveLearningResult();
        
        try {
            // Strategy refinement
            StrategyRefinement refinement = strategyAgent.refineStrategyFromSession(session);
            result.setStrategyRefinement(refinement);
            
            // Reward function updates
            RewardFunctionUpdate rewardUpdate = irlLearner.updateFromSession(session);
            result.setRewardUpdate(rewardUpdate);
            
            // Learning effectiveness metrics
            LearningEffectivenessMetrics metrics = calculateDetailedLearningMetrics(session);
            result.setEffectivenessMetrics(metrics);
            
            Log.d(TAG, "Comprehensive learning completed for session: " + session.getName());
            
        } catch (Exception e) {
            Log.e(TAG, "Error in comprehensive learning", e);
        }
        
        return result;
    }
    
    private ActionIntent classifyActionIntent(GameFrame frame) {
        try {
            String reasoning = frame.userExplanation.toLowerCase();
            String action = frame.userAction.toLowerCase();
            
            // Intent classification based on keywords
            if (reasoning.contains("attack") || reasoning.contains("fight") || reasoning.contains("kill") ||
                action.contains("attack") || action.contains("fight")) {
                return ActionIntent.ATTACK;
            } else if (reasoning.contains("defend") || reasoning.contains("protect") || reasoning.contains("block") ||
                      action.contains("defend") || action.contains("shield")) {
                return ActionIntent.DEFEND;
            } else if (reasoning.contains("collect") || reasoning.contains("gather") || reasoning.contains("loot") ||
                      action.contains("collect") || action.contains("pickup")) {
                return ActionIntent.COLLECT;
            } else if (reasoning.contains("move") || reasoning.contains("navigate") || reasoning.contains("go") ||
                      action.contains("move") || action.contains("walk")) {
                return ActionIntent.NAVIGATE;
            } else if (reasoning.contains("use") || reasoning.contains("activate") || reasoning.contains("interact") ||
                      action.contains("use") || action.contains("press")) {
                return ActionIntent.INTERACT;
            }
            
            return ActionIntent.UNKNOWN;
            
        } catch (Exception e) {
            Log.e(TAG, "Error classifying action intent", e);
            return ActionIntent.UNKNOWN;
        }
    }
    
    private float calculateFrameConfidence(GameFrame frame) {
        try {
            float reasoningLength = Math.min(1.0f, frame.userExplanation.length() / 50.0f);
            float actionSpecificity = frame.userAction.isEmpty() ? 0.3f : 0.8f;
            
            return (reasoningLength * 0.6f + actionSpecificity * 0.4f);
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating frame confidence", e);
            return 0.5f;
        }
    }
    
    private float analyzeVisualTextCorrelation(GameFrame frame) {
        try {
            // Simple correlation analysis based on action coordinates and reasoning
            boolean hasLocationReference = frame.userExplanation.toLowerCase().contains("here") ||
                                         frame.userExplanation.toLowerCase().contains("there") ||
                                         frame.userExplanation.toLowerCase().contains("position");
            
            boolean hasValidCoordinates = frame.actionX > 0 && frame.actionY > 0;
            
            if (hasLocationReference && hasValidCoordinates) {
                return 0.9f;
            } else if (hasValidCoordinates) {
                return 0.7f;
            } else {
                return 0.5f;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing visual-text correlation", e);
            return 0.5f;
        }
    }
    
    private float calculateExplanationSimilarity(String userExplanation, String aiExplanation) {
        try {
            if (nlpProcessor != null) {
                return nlpProcessor.calculateTextSimilarity(userExplanation, aiExplanation);
            }
            
            // Fallback simple similarity
            String[] userWords = userExplanation.toLowerCase().split("\\s+");
            String[] aiWords = aiExplanation.toLowerCase().split("\\s+");
            
            Set<String> userSet = new HashSet<>(Arrays.asList(userWords));
            Set<String> aiSet = new HashSet<>(Arrays.asList(aiWords));
            
            Set<String> intersection = new HashSet<>(userSet);
            intersection.retainAll(aiSet);
            
            Set<String> union = new HashSet<>(userSet);
            union.addAll(aiSet);
            
            return union.isEmpty() ? 0f : (float) intersection.size() / union.size();
            
        } catch (Exception e) {
            Log.e(TAG, "Error calculating explanation similarity", e);
            return 0.5f;
        }
    }
    
    private float analyzeSequenceCoherence(DemonstrationSequence sequence) {
        try {
            List<GameFrame> frames = sequence.getFrames();
            if (frames.size() < 2) return 1.0f;
            
            float coherenceSum = 0f;
            int comparisons = 0;
            
            for (int i = 1; i < frames.size(); i++) {
                GameFrame prev = frames.get(i-1);
                GameFrame curr = frames.get(i);
                
                // Temporal coherence
                long timeDiff = curr.timestamp - prev.timestamp;
                float temporalScore = timeDiff < 10000 ? 1.0f : 0.5f; // 10 seconds
                
                // Action coherence
                float actionScore = analyzeActionCoherence(prev.userAction, curr.userAction);
                
                coherenceSum += (temporalScore * 0.3f + actionScore * 0.7f);
                comparisons++;
            }
            
            return comparisons > 0 ? coherenceSum / comparisons : 1.0f;
            
        } catch (Exception e) {
            Log.e(TAG, "Error analyzing sequence coherence", e);
            return 0.5f;
        }
    }
    
    private float analyzeActionCoherence(String action1, String action2) {
        // Simple action coherence based on logical sequences
        Map<String, List<String>> logicalSequences = new HashMap<>();
        logicalSequences.put("move", Arrays.asList("attack", "collect", "interact"));
        logicalSequences.put("attack", Arrays.asList("move", "defend", "collect"));
        logicalSequences.put("collect", Arrays.asList("move", "use"));
        
        List<String> validNext = logicalSequences.get(action1.toLowerCase());
        if (validNext != null && validNext.contains(action2.toLowerCase())) {
            return 0.9f;
        }
        
        return 0.6f; // Default coherence
    }
    
    // Additional helper methods
    private SessionInsights analyzeSessionInsights(DemonstrationSession session) {
        return new SessionInsights();
    }
    
    private List<LearningRecommendation> generateLearningRecommendations() {
        return new ArrayList<>();
    }
    
    private float calculateLearningEffectiveness() {
        return completedSessions.isEmpty() ? 0f : 0.8f;
    }
    
    private Map<String, Integer> calculateActionDistribution(DemonstrationSession session) {
        Map<String, Integer> distribution = new HashMap<>();
        for (GameFrame frame : session.getAllFrames()) {
            distribution.merge(frame.userAction, 1, Integer::sum);
        }
        return distribution;
    }
    
    private float calculateAverageReasoningQuality(DemonstrationSession session) {
        if (session.getAllFrames().isEmpty()) return 0f;
        
        float total = 0f;
        for (GameFrame frame : session.getAllFrames()) {
            total += assessReasoningQuality(frame.userExplanation);
        }
        
        return total / session.getAllFrames().size();
    }
    
    private float assessReasoningQuality(String reasoning) {
        int length = reasoning.length();
        boolean hasDetail = reasoning.toLowerCase().contains("because") || 
                           reasoning.toLowerCase().contains("since") ||
                           reasoning.toLowerCase().contains("to");
        
        float score = 0.3f;
        if (length > 20) score += 0.3f;
        if (length > 50) score += 0.2f;
        if (hasDetail) score += 0.2f;
        
        return Math.min(1.0f, score);
    }
    
    private float calculateSessionLearningEffectiveness(DemonstrationSession session) {
        return 0.8f; // Placeholder implementation
    }
    
    private LearningEffectivenessMetrics calculateDetailedLearningMetrics(DemonstrationSession session) {
        return new LearningEffectivenessMetrics();
    }
    
    // Public interface methods
    public void setListener(DemonstrationListener listener) {
        this.listener = listener;
    }
    
    public DemonstrationSession getCurrentSession() {
        return currentSession;
    }
    
    public List<DemonstrationSession> getCompletedSessions() {
        return new ArrayList<>(completedSessions);
    }
    
    public boolean hasActiveSession() {
        return currentSession != null;
    }
    
    public void clearCompletedSessions() {
        completedSessions.clear();
    }
    
    public void shutdown() {
        if (currentSession != null) {
            completeDemonstrationSession();
        }
        
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        
        Log.d(TAG, "Expert Demonstration System shutdown complete");
    }
    
    // Supporting classes
    public static class DemonstrationSession {
        private String name;
        private String gameContext;
        private long startTime;
        private long endTime;
        private List<GameFrame> frames;
        private List<DemonstrationSequence> sequences;
        
        public DemonstrationSession(String name, String gameContext) {
            this.name = name;
            this.gameContext = gameContext;
            this.frames = new ArrayList<>();
            this.sequences = new ArrayList<>();
        }
        
        public void addFrame(GameFrame frame, FrameAnalysis analysis) {
            frames.add(frame);
        }
        
        public void addSequence(DemonstrationSequence sequence) {
            sequences.add(sequence);
        }
        
        public int getFrameCount() { return frames.size(); }
        public List<GameFrame> getAllFrames() { return new ArrayList<>(frames); }
        public List<DemonstrationSequence> getSequences() { return new ArrayList<>(sequences); }
        public String getName() { return name; }
        public String getGameContext() { return gameContext; }
        public long getDuration() { return endTime - startTime; }
        
        public void setStartTime(long startTime) { this.startTime = startTime; }
        public void setEndTime(long endTime) { this.endTime = endTime; }
    }
    
    public static class DemonstrationSequence {
        private List<GameFrame> frames;
        private String overallStrategy;
        private float coherenceScore;
        
        public DemonstrationSequence(List<GameFrame> frames, String overallStrategy) {
            this.frames = new ArrayList<>(frames);
            this.overallStrategy = overallStrategy;
        }
        
        public List<GameFrame> getFrames() { return new ArrayList<>(frames); }
        public String getOverallStrategy() { return overallStrategy; }
        public float getCoherenceScore() { return coherenceScore; }
        public void setCoherenceScore(float coherenceScore) { this.coherenceScore = coherenceScore; }
    }
    
    public static class FrameAnalysis {
        private NLPProcessor.GameTextAnalysis nlpAnalysis;
        private ActionIntent actionIntent;
        private float confidence;
        private float visualTextMatch;
        private String aiExplanation;
        private float explanationSimilarity;
        
        // Getters and setters
        public NLPProcessor.GameTextAnalysis getNlpAnalysis() { return nlpAnalysis; }
        public void setNlpAnalysis(NLPProcessor.GameTextAnalysis nlpAnalysis) { this.nlpAnalysis = nlpAnalysis; }
        
        public ActionIntent getActionIntent() { return actionIntent; }
        public void setActionIntent(ActionIntent actionIntent) { this.actionIntent = actionIntent; }
        
        public float getConfidence() { return confidence; }
        public void setConfidence(float confidence) { this.confidence = confidence; }
        
        public float getVisualTextMatch() { return visualTextMatch; }
        public void setVisualTextMatch(float visualTextMatch) { this.visualTextMatch = visualTextMatch; }
        
        public String getAiExplanation() { return aiExplanation; }
        public void setAiExplanation(String aiExplanation) { this.aiExplanation = aiExplanation; }
        
        public float getExplanationSimilarity() { return explanationSimilarity; }
        public void setExplanationSimilarity(float explanationSimilarity) { this.explanationSimilarity = explanationSimilarity; }
    }
    
    // Placeholder classes for complex types
    public static class LearningMetrics { }
    public static class DemonstrationResult { 
        private DemonstrationSession session;
        private SessionAnalysisResult sessionAnalysis;
        private ComprehensiveLearningResult learningResult;
        
        public DemonstrationResult(DemonstrationSession session, SessionAnalysisResult sessionAnalysis, ComprehensiveLearningResult learningResult) {
            this.session = session;
            this.sessionAnalysis = sessionAnalysis;
            this.learningResult = learningResult;
        }
        
        public SessionAnalysisResult getSessionAnalysis() { return sessionAnalysis; }
    }
    public static class SessionAnalysisResult { 
        private int totalFrames;
        private int totalSequences;
        private long sessionDuration;
        private Map<String, Integer> actionDistribution;
        private float averageReasoningQuality;
        private float learningEffectiveness;
        
        public int getTotalFrames() { return totalFrames; }
        public void setTotalFrames(int totalFrames) { this.totalFrames = totalFrames; }
        public void setTotalSequences(int totalSequences) { this.totalSequences = totalSequences; }
        public void setSessionDuration(long sessionDuration) { this.sessionDuration = sessionDuration; }
        public void setActionDistribution(Map<String, Integer> actionDistribution) { this.actionDistribution = actionDistribution; }
        public void setAverageReasoningQuality(float averageReasoningQuality) { this.averageReasoningQuality = averageReasoningQuality; }
        public void setLearningEffectiveness(float learningEffectiveness) { this.learningEffectiveness = learningEffectiveness; }
    }
    public static class ComprehensiveLearningResult { 
        private StrategyRefinement strategyRefinement;
        private RewardFunctionUpdate rewardUpdate;
        private LearningEffectivenessMetrics effectivenessMetrics;
        
        public void setStrategyRefinement(StrategyRefinement strategyRefinement) { this.strategyRefinement = strategyRefinement; }
        public void setRewardUpdate(RewardFunctionUpdate rewardUpdate) { this.rewardUpdate = rewardUpdate; }
        public void setEffectivenessMetrics(LearningEffectivenessMetrics effectivenessMetrics) { this.effectivenessMetrics = effectivenessMetrics; }
    }
    public static class LearningInsights { 
        private List<SessionInsights> sessionInsights = new ArrayList<>();
        private List<LearningRecommendation> recommendations = new ArrayList<>();
        private float overallEffectiveness;
        
        public void addSessionInsights(SessionInsights insights) { sessionInsights.add(insights); }
        public void setRecommendations(List<LearningRecommendation> recommendations) { this.recommendations = recommendations; }
        public void setOverallEffectiveness(float effectiveness) { this.overallEffectiveness = effectiveness; }
    }
    public static class SessionInsights { }
    public static class LearningRecommendation { }
    public static class LearningProgressTracker { 
        public LearningMetrics updateWithSequence(DemonstrationSequence sequence) { return new LearningMetrics(); }
        public LearningMetrics completeSession(DemonstrationSession session) { return new LearningMetrics(); }
    }
    public static class StrategyRefinement { }
    public static class RewardFunctionUpdate { }
    public static class LearningEffectivenessMetrics { }
}