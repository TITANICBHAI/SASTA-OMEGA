package com.gestureai.gameautomation.workflow.conditions;

import android.content.Context;
import android.util.Log;
import java.util.Map;

/**
 * Number Compare Condition - Compares variable value with target number
 */
public class NumberCompareCondition implements WorkflowCondition {
    private static final String TAG = "NumberCompareCondition";
    
    private String variableName;
    private String operator; // "==", "!=", ">", "<", ">=", "<="
    private double value;
    
    public NumberCompareCondition(String variableName, String operator, double value) {
        this.variableName = variableName;
        this.operator = operator;
        this.value = value;
    }
    
    @Override
    public boolean evaluate(Context context, Map<String, Object> variables) {
        Log.d(TAG, "Comparing " + variableName + " " + operator + " " + value);
        
        Object variableValue = variables.get(variableName);
        if (variableValue == null) {
            Log.w(TAG, "Variable not found: " + variableName);
            return false;
        }
        
        double currentValue;
        try {
            if (variableValue instanceof Number) {
                currentValue = ((Number) variableValue).doubleValue();
            } else {
                currentValue = Double.parseDouble(String.valueOf(variableValue));
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "Cannot convert variable to number: " + variableValue);
            return false;
        }
        
        switch (operator) {
            case "==":
                return Math.abs(currentValue - value) < 0.0001; // Handle floating point comparison
            case "!=":
                return Math.abs(currentValue - value) >= 0.0001;
            case ">":
                return currentValue > value;
            case "<":
                return currentValue < value;
            case ">=":
                return currentValue >= value;
            case "<=":
                return currentValue <= value;
            default:
                Log.e(TAG, "Unknown operator: " + operator);
                return false;
        }
    }
    
    @Override
    public String getDescription() {
        return "Variable " + variableName + " " + operator + " " + value;
    }
    
    @Override
    public boolean isValid() {
        return variableName != null && !variableName.isEmpty() && 
               operator != null && isValidOperator(operator);
    }
    
    private boolean isValidOperator(String op) {
        return "==".equals(op) || "!=".equals(op) || ">".equals(op) || 
               "<".equals(op) || ">=".equals(op) || "<=".equals(op);
    }
    
    // Getters and setters
    public String getVariableName() { return variableName; }
    public void setVariableName(String variableName) { this.variableName = variableName; }
    
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
}