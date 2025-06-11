package com.gestureai.gameautomation.ai;

import android.util.Log;
import java.util.Random;

public class SimpleNeuralNetwork {
    private static final String TAG = "SimpleNeuralNetwork";
    
    private int[] layerSizes;
    private float[][][] weights;
    private float[][] biases;
    private float[][] activations;
    private Random random;
    
    public SimpleNeuralNetwork(int[] layerSizes) {
        this.layerSizes = layerSizes.clone();
        this.random = new Random();
        initializeNetwork();
        Log.d(TAG, "SimpleNeuralNetwork initialized with layers: " + java.util.Arrays.toString(layerSizes));
    }
    
    private void initializeNetwork() {
        int numLayers = layerSizes.length;
        
        // Initialize weights and biases
        weights = new float[numLayers - 1][][];
        biases = new float[numLayers - 1][];
        activations = new float[numLayers][];
        
        for (int layer = 0; layer < numLayers; layer++) {
            activations[layer] = new float[layerSizes[layer]];
        }
        
        for (int layer = 0; layer < numLayers - 1; layer++) {
            int inputSize = layerSizes[layer];
            int outputSize = layerSizes[layer + 1];
            
            weights[layer] = new float[outputSize][inputSize];
            biases[layer] = new float[outputSize];
            
            // Xavier initialization for weights
            float scale = (float) Math.sqrt(2.0 / (inputSize + outputSize));
            
            for (int i = 0; i < outputSize; i++) {
                biases[layer][i] = 0f;
                for (int j = 0; j < inputSize; j++) {
                    weights[layer][i][j] = (random.nextFloat() * 2f - 1f) * scale;
                }
            }
        }
    }
    
    public float[] predict(float[] inputs) {
        if (inputs.length != layerSizes[0]) {
            throw new IllegalArgumentException("Input size mismatch. Expected: " + layerSizes[0] + ", Got: " + inputs.length);
        }
        
        // Set input layer
        System.arraycopy(inputs, 0, activations[0], 0, inputs.length);
        
        // Forward propagation
        for (int layer = 0; layer < weights.length; layer++) {
            forwardLayer(layer);
        }
        
        // Return output layer
        float[] output = new float[activations[activations.length - 1].length];
        System.arraycopy(activations[activations.length - 1], 0, output, 0, output.length);
        
        return output;
    }
    
    private void forwardLayer(int layer) {
        float[] prevActivations = activations[layer];
        float[] currentActivations = activations[layer + 1];
        
        for (int i = 0; i < currentActivations.length; i++) {
            float sum = biases[layer][i];
            
            for (int j = 0; j < prevActivations.length; j++) {
                sum += weights[layer][i][j] * prevActivations[j];
            }
            
            currentActivations[i] = sigmoid(sum);
        }
    }
    
    public void updateWeights(float[] inputs, float[] targets, float learningRate) {
        // Perform forward pass
        float[] outputs = predict(inputs);
        
        // Calculate output layer errors
        float[] outputErrors = new float[targets.length];
        for (int i = 0; i < targets.length; i++) {
            outputErrors[i] = targets[i] - outputs[i];
        }
        
        // Backpropagate errors and update weights
        backpropagate(outputErrors, learningRate);
    }
    
    private void backpropagate(float[] outputErrors, float learningRate) {
        int numLayers = layerSizes.length;
        float[][] layerErrors = new float[numLayers][];
        
        // Initialize error arrays
        for (int layer = 0; layer < numLayers; layer++) {
            layerErrors[layer] = new float[layerSizes[layer]];
        }
        
        // Set output layer errors
        System.arraycopy(outputErrors, 0, layerErrors[numLayers - 1], 0, outputErrors.length);
        
        // Backpropagate errors
        for (int layer = numLayers - 2; layer >= 0; layer--) {
            for (int i = 0; i < layerSizes[layer]; i++) {
                float error = 0f;
                
                for (int j = 0; j < layerSizes[layer + 1]; j++) {
                    error += layerErrors[layer + 1][j] * weights[layer][j][i];
                }
                
                layerErrors[layer][i] = error * sigmoidDerivative(activations[layer][i]);
            }
        }
        
        // Update weights and biases
        for (int layer = 0; layer < weights.length; layer++) {
            for (int i = 0; i < weights[layer].length; i++) {
                // Update bias
                biases[layer][i] += learningRate * layerErrors[layer + 1][i];
                
                // Update weights
                for (int j = 0; j < weights[layer][i].length; j++) {
                    weights[layer][i][j] += learningRate * layerErrors[layer + 1][i] * activations[layer][j];
                }
            }
        }
    }
    
    private float sigmoid(float x) {
        return (float) (1.0 / (1.0 + Math.exp(-x)));
    }
    
    private float sigmoidDerivative(float x) {
        return x * (1f - x);
    }
    
    public void clear() {
        if (weights != null) {
            for (int i = 0; i < weights.length; i++) {
                weights[i] = null;
            }
            weights = null;
        }
        
        if (biases != null) {
            for (int i = 0; i < biases.length; i++) {
                biases[i] = null;
            }
            biases = null;
        }
        
        if (activations != null) {
            for (int i = 0; i < activations.length; i++) {
                activations[i] = null;
            }
            activations = null;
        }
        
        Log.d(TAG, "SimpleNeuralNetwork cleared");
    }
    
    public int[] getLayerSizes() {
        return layerSizes.clone();
    }
    
    public int getNumLayers() {
        return layerSizes.length;
    }
    
    public int getInputSize() {
        return layerSizes[0];
    }
    
    public int getOutputSize() {
        return layerSizes[layerSizes.length - 1];
    }
}