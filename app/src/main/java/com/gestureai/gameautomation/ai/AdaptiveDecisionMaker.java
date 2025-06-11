package com.gestureai.gameautomation.ai;

import android.util.Log;
import com.gestureai.gameautomation.GameAction;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import com.gestureai.gameautomation.data.UniversalGameState;
import com.gestureai.gameautomation.data.UniversalGameState;
import com.gestureai.gameautomation.ai.GameStrategyAgent;
import java.util.List;
import java.util.ArrayList;

public class AdaptiveDecisionMaker {
    private static final String TAG = "AdaptiveDecisionMaker";
    private MultiLayerNetwork decisionNetwork;
    private boolean isInitialized = false;

    public AdaptiveDecisionMaker() {
        initializeNetwork();
    }

    private void initializeNetwork() {
        try {
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(42)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new Adam(0.001))
                    .list()
                    .layer(new DenseLayer.Builder().nIn(10).nOut(64)
                            .activation(Activation.RELU).build())
                    .layer(new DenseLayer.Builder().nIn(64).nOut(32)
                            .activation(Activation.RELU).build())
                    .layer(new OutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(32).nOut(1)
                            .activation(Activation.SIGMOID).build())
                    .build();

            decisionNetwork = new MultiLayerNetwork(conf);
            if (decisionNetwork != null) {
                try {
                    decisionNetwork.init();
                    Log.d(TAG, "Decision network initialized successfully");
                } catch (Exception e) {
                    Log.w(TAG, "Failed to initialize decision network, using fallback", e);
                    decisionNetwork = null;
                }
            }
            isInitialized = true;

            Log.d(TAG, "Adaptive Decision Maker initialized");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize decision network", e);
        }
    }

    public GameAction selectOptimalAction(GameAction strategicAction, List<GameAction> labeledActions,
                                          GameStrategyAgent.UniversalGameState gameState) {
        if (!isInitialized) {
            return selectFallbackAction(strategicAction, labeledActions);
        }

        try {
            List<GameAction> allActions = new ArrayList<>();
            if (strategicAction != null) {
                allActions.add(strategicAction);
            }
            allActions.addAll(labeledActions);

            if (allActions.isEmpty()) {
                return null;
            }

            // Score each action using neural network
            GameAction bestAction = null;
            float bestScore = -1f;

            for (GameAction action : allActions) {
                float score = scoreAction(action, gameState);
                if (score > bestScore) {
                    bestScore = score;
                    bestAction = action;
                }
            }

            Log.d(TAG, "Selected optimal action: " + (bestAction != null ? bestAction.getActionType() : "none") +
                    " with score: " + bestScore);

            return bestAction;

        } catch (Exception e) {
            Log.e(TAG, "Error in adaptive decision making", e);
            return selectFallbackAction(strategicAction, labeledActions);
        }
    }

    private float scoreAction(GameAction action, GameStrategyAgent.UniversalGameState gameState) {
        try {
            // Convert action to neural network input
            INDArray actionFeatures = actionToFeatures(action, gameState);
            INDArray output = decisionNetwork.output(actionFeatures);

            return output.getFloat(0);

        } catch (Exception e) {
            Log.w(TAG, "Error scoring action, using fallback", e);
            return action.getConfidence() * action.getPriority();
        }
    }

    private INDArray actionToFeatures(GameAction action, GameStrategyAgent.UniversalGameState gameState) {
        float[] features = new float[10];

        // Action features
        features[0] = action.getConfidence();
        features[1] = action.getPriority();
        features[2] = normalizeActionType(action.getActionType());
        features[3] = action.getX() / 1080f; // Normalized screen position
        features[4] = action.getY() / 1920f;

        // Game state context
        features[5] = gameState != null ? gameState.threatLevel : 0f;
        features[6] = gameState != null ? gameState.opportunityLevel : 0f;
        features[7] = gameState != null ? gameState.gameSpeed / 10f : 0f;
        features[8] = gameState != null ? gameState.objectCount / 20f : 0f;
        features[9] = (System.currentTimeMillis() % 1000) / 1000f; // Time factor

        return Nd4j.create(features).reshape(1, 10);
    }

    private float normalizeActionType(String actionType) {
        switch (actionType.toUpperCase()) {
            case "TAP": return 0.1f;
            case "SWIPE_UP": return 0.2f;
            case "SWIPE_DOWN": return 0.3f;
            case "SWIPE_LEFT": return 0.4f;
            case "SWIPE_RIGHT": return 0.5f;
            case "LONG_PRESS": return 0.6f;
            case "DOUBLE_TAP": return 0.7f;
            case "COLLECT": return 0.8f;
            case "AVOID": return 0.9f;
            case "ACTIVATE_POWERUP": return 1.0f;
            default: return 0.5f;
        }
    }

    private GameAction selectFallbackAction(GameAction strategicAction, List<GameAction> labeledActions) {
        // Simple fallback: highest priority action
        GameAction bestAction = strategicAction;
        float bestPriority = strategicAction != null ? strategicAction.getPriority() : -1f;

        for (GameAction action : labeledActions) {
            if (action.getPriority() > bestPriority) {
                bestPriority = action.getPriority();
                bestAction = action;
            }
        }

        return bestAction;
    }

    public void learnFromOutcome(GameAction selectedAction, float outcome,
                                 GameStrategyAgent.UniversalGameState gameState) {
        if (!isInitialized || selectedAction == null) return;

        try {
            // Create training data from outcome
            INDArray input = actionToFeatures(selectedAction, gameState);
            INDArray target = Nd4j.create(new float[]{outcome}).reshape(1, 1);

            // Train the network
            decisionNetwork.fit(input, target);

            Log.d(TAG, "Learned from outcome: " + outcome + " for action: " + selectedAction.getActionType());

        } catch (Exception e) {
            Log.e(TAG, "Error learning from outcome", e);
        }
    }
    
    public void cleanup() {
        try {
            if (decisionNetwork != null) {
                decisionNetwork.clear();
                decisionNetwork = null;
            }
            isInitialized = false;
            Log.d(TAG, "AdaptiveDecisionMaker cleaned up successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error during AdaptiveDecisionMaker cleanup", e);
        }
    }
}
