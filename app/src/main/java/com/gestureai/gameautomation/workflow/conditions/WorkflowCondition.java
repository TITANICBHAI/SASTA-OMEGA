package com.gestureai.gameautomation.workflow.conditions;

import android.content.Context;
import java.util.Map;

/**
 * Base interface for workflow conditions
 */
public interface WorkflowCondition {
    /**
     * Evaluate the condition
     * @param context Android context
     * @param variables Global workflow variables
     * @return true if condition is met, false otherwise
     */
    boolean evaluate(Context context, Map<String, Object> variables);
    
    /**
     * Get condition description
     */
    String getDescription();
    
    /**
     * Validate condition parameters
     */
    boolean isValid();
}