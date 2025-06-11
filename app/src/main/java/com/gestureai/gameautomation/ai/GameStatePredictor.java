package com.gestureai.gameautomation.ai;

import android.util.Log;
import com.gestureai.gameautomation.DetectedObject;

import org.deeplearning4j.nn.conf.RNNFormat;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.weights.WeightInit;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import java.util.List;
import java.util.ArrayList;

public class GameStatePredictor {
    private static final String TAG = "GameStatePredictor";
    private MultiLayerNetwork predictionNetwork;
    private List<GameStrategyAgent.UniversalGameState> stateHistory;
    private boolean isInitialized = false;
    private static final int SEQUENCE_LENGTH = 5;
    private static final int STATE_FEATURES = 8;

    public GameStatePredictor() {
        this.stateHistory = new ArrayList<>();
        initializeNetwork();
    }

    private void initializeNetwork() {
        try {
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(123)
                    .weightInit(WeightInit.XAVIER)
                    .updater(new org.nd4j.linalg.learning.config.Adam(0.001))
                    .list()
                    .layer(new LSTM.Builder().nIn(STATE_FEATURES).nOut(64)
                            .activation(Activation.TANH).build())
                    .layer(new LSTM.Builder().nIn(64).nOut(32)
                            .activation(Activation.TANH).build())
                    .layer(new RnnOutputLayer.Builder(LossFunctions.LossFunction.MSE)
                            .nIn(32).nOut(STATE_FEATURES)
                            .activation(Activation.IDENTITY)
                            .dataFormat(RNNFormat.NCW)
                            .build())
                    .build();

            predictionNetwork = new MultiLayerNetwork(conf);
            if (predictionNetwork != null) {
                try {
                    predictionNetwork.init();
                } catch (Exception e) {
                    Log.w(TAG, "Failed to initialize prediction network", e);
                    predictionNetwork = null;
                }
            }
            isInitialized = true;

            Log.d(TAG, "Game State Predictor initialized with LSTM network");
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize prediction network", e);
        }
    }

    public GameStrategyAgent.UniversalGameState predictNextState(
            GameStrategyAgent.UniversalGameState currentState,
            List<DetectedObject> detectedObjects) {

        if (!isInitialized) {
            return createFallbackPrediction(currentState, detectedObjects);
        }

        try {
            // Add current state to history
            stateHistory.add(copyState(currentState));

            // Keep only recent history
            if (stateHistory.size() > SEQUENCE_LENGTH) {
                stateHistory.remove(0);
            }

            // Need at least 3 states for meaningful prediction
            if (stateHistory.size() < 3) {
                return createSimplePrediction(currentState, detectedObjects);
            }

            // Create sequence input for LSTM
            INDArray sequenceInput = createSequenceInput();
//            predictionNetwork.setOutputLayerMaskArray(null);
            INDArray prediction = predictionNetwork.rnnTimeStep(sequenceInput);

            // Convert prediction back to game state
            return predictionToGameState(prediction, currentState);

        } catch (Exception e) {
            Log.e(TAG, "Error in state prediction", e);
            return createFallbackPrediction(currentState, detectedObjects);
        }
    }

    private INDArray createSequenceInput() {
        INDArray input = Nd4j.zeros(1, STATE_FEATURES, stateHistory.size());

        for (int i = 0; i < stateHistory.size(); i++) {
            GameStrategyAgent.UniversalGameState state = stateHistory.get(i);
            float[] stateFeatures = stateToFeatures(state);

            for (int j = 0; j < STATE_FEATURES; j++) {
                input.putScalar(new int[]{0, j, i}, stateFeatures[j]);
            }
        }

        return input;
    }

    private float[] stateToFeatures(GameStrategyAgent.UniversalGameState state) {
        return new float[]{
                state.playerX / 1080f,
                state.playerY / 1920f,
                state.gameSpeed / 10f,
                state.threatLevel,
                state.opportunityLevel,
                state.objectCount / 20f,
                state.gameScore / 10000f,
                state.healthLevel
        };
    }

    private GameStrategyAgent.UniversalGameState predictionToGameState(INDArray prediction,
                                                                       GameStrategyAgent.UniversalGameState baseState) {
        GameStrategyAgent.UniversalGameState predictedState = copyState(baseState);

        // Check if prediction has time dimension
        if (prediction.rank() == 3) {
            // 3D output: [batch, features, time]
            int lastTimeStep = (int) (prediction.size(2) - 1);
            predictedState.playerX = (int) (prediction.getDouble(0, 0, lastTimeStep) * 1080);
            predictedState.playerY = (int) (prediction.getDouble(0, 1, lastTimeStep) * 1920);
            predictedState.gameSpeed = (float) (prediction.getDouble(0, 2, lastTimeStep) * 10);
            predictedState.threatLevel = (float) prediction.getDouble(0, 3, lastTimeStep);
            predictedState.opportunityLevel = (float) prediction.getDouble(0, 4, lastTimeStep);
            predictedState.objectCount = (int) (prediction.getDouble(0, 5, lastTimeStep) * 20);
            predictedState.gameScore = (int) (prediction.getDouble(0, 6, lastTimeStep) * 10000);
            predictedState.healthLevel = (float) prediction.getDouble(0, 7, lastTimeStep);
        } else {
            // 2D output: [batch, features]
            predictedState.playerX = (int) (prediction.getDouble(0, 0) * 1080);
            predictedState.playerY = (int) (prediction.getDouble(0, 1) * 1920);
            predictedState.gameSpeed = (float) (prediction.getDouble(0, 2) * 10);
            predictedState.threatLevel = (float) prediction.getDouble(0, 3);
            predictedState.opportunityLevel = (float) prediction.getDouble(0, 4);
            predictedState.objectCount = (int) (prediction.getDouble(0, 5) * 20);
            predictedState.gameScore = (int) (prediction.getDouble(0, 6) * 10000);
            predictedState.healthLevel = (float) prediction.getDouble(0, 7);
        }

        Log.d(TAG, "Predicted next state - ThreatLevel: " + predictedState.threatLevel +
                ", OpportunityLevel: " + predictedState.opportunityLevel);

        return predictedState;
    }

    private GameStrategyAgent.UniversalGameState createSimplePrediction(
            GameStrategyAgent.UniversalGameState currentState, List<DetectedObject> objects) {

        GameStrategyAgent.UniversalGameState prediction = copyState(currentState);

        // Simple linear prediction based on current trends
        if (stateHistory.size() >= 2) {
            GameStrategyAgent.UniversalGameState prevState = stateHistory.get(stateHistory.size() - 2);

            // Predict player movement
            int deltaX = currentState.playerX - prevState.playerX;
            int deltaY = currentState.playerY - prevState.playerY;
            prediction.playerX = currentState.playerX + deltaX;
            prediction.playerY = currentState.playerY + deltaY;

            // Predict threat level based on object proximity
            float threatIncrease = 0f;
            for (DetectedObject obj : objects) {
                if (obj.action.equals("AVOID")) {
                    float distance = Math.abs(obj.boundingRect.centerX() - prediction.playerX) +
                            Math.abs(obj.boundingRect.centerY() - prediction.playerY);
                    if (distance < 300) {
                        threatIncrease += 0.2f;
                    }
                }
            }
            prediction.threatLevel = Math.min(1.0f, currentState.threatLevel + threatIncrease);
        }

        return prediction;
    }

    private GameStrategyAgent.UniversalGameState createFallbackPrediction(
            GameStrategyAgent.UniversalGameState currentState, List<DetectedObject> objects) {

        GameStrategyAgent.UniversalGameState prediction = copyState(currentState);

        // Basic prediction: increase threat if obstacles detected
        float threatIncrease = 0f;
        float opportunityIncrease = 0f;

        for (DetectedObject obj : objects) {
            if (obj.action.equals("AVOID")) {
                threatIncrease += 0.1f;
            } else if (obj.action.equals("COLLECT")) {
                opportunityIncrease += 0.1f;
            }
        }

        prediction.threatLevel = Math.min(1.0f, currentState.threatLevel + threatIncrease);
        prediction.opportunityLevel = Math.min(1.0f, currentState.opportunityLevel + opportunityIncrease);
        prediction.gameSpeed = Math.min(10f, currentState.gameSpeed + 0.1f); // Game gradually speeds up

        return prediction;
    }

    private GameStrategyAgent.UniversalGameState copyState(GameStrategyAgent.UniversalGameState original) {
        GameStrategyAgent.UniversalGameState copy = new GameStrategyAgent.UniversalGameState();
        copy.playerX = original.playerX;
        copy.playerY = original.playerY;
        copy.screenWidth = original.screenWidth;
        copy.screenHeight = original.screenHeight;
        copy.gameScore = original.gameScore;
        copy.gameSpeed = original.gameSpeed;
        copy.objectCount = original.objectCount;
        copy.threatLevel = original.threatLevel;
        copy.opportunityLevel = original.opportunityLevel;
        copy.timeInGame = original.timeInGame;
        copy.averageReward = original.averageReward;
        copy.consecutiveSuccess = original.consecutiveSuccess;
        copy.gameType = original.gameType;
        copy.difficultyLevel = original.difficultyLevel;
        copy.powerUpActive = original.powerUpActive;
        copy.healthLevel = original.healthLevel;
        copy.nearestObjectX = original.nearestObjectX;
        copy.nearestObjectY = original.nearestObjectY;
        return copy;
    }

    public void learnFromActualOutcome(GameStrategyAgent.UniversalGameState predicted,
                                       GameStrategyAgent.UniversalGameState actual) {
        if (!isInitialized) return;

        try {
            // Create training data from prediction vs reality
            INDArray predictedFeatures = Nd4j.create(stateToFeatures(predicted)).reshape(1, STATE_FEATURES, 1);
            INDArray actualFeatures = Nd4j.create(stateToFeatures(actual)).reshape(1, STATE_FEATURES, 1);

            // Train network to improve predictions
            predictionNetwork.fit(predictedFeatures, actualFeatures);

            Log.d(TAG, "Learned from prediction accuracy");

        } catch (Exception e) {
            Log.e(TAG, "Error learning from prediction outcome", e);
        }
    }
    public boolean isInitialized() {
        return isInitialized;
    }

    public void cleanup() {
        try {
            if (predictionNetwork != null) {
                predictionNetwork.clear();
                predictionNetwork = null;
            }
            if (stateHistory != null) {
                stateHistory.clear();
            }
            isInitialized = false;
            Log.d(TAG, "GameStatePredictor cleaned up");
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup", e);
        }
    }

    public float[] predictFutureState(float[] currentState, int stepsAhead) {
        try {
            if (!isInitialized || predictionNetwork == null) {
                // Simple linear extrapolation fallback
                float[] prediction = new float[currentState.length];
                for (int i = 0; i < currentState.length; i++) {
                    prediction[i] = currentState[i] * (1.0f + 0.1f * stepsAhead);
                }
                return prediction;
            }

            INDArray input = Nd4j.create(currentState).reshape(1, currentState.length, 1);
            INDArray prediction = predictionNetwork.rnnTimeStep(input);
            return prediction.toFloatVector();

        } catch (Exception e) {
            Log.e(TAG, "Error predicting future state", e);
            return currentState.clone();
        }
    }
}