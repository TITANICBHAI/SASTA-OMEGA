package com.gestureai.gameautomation.workflow.actions;

import android.content.Context;
import java.util.Map;

/**
 * Base interface for workflow actions
 */
public interface WorkflowAction {
    /**
     * Execute the action
     * @param context Android context
     * @param variables Global workflow variables
     * @throws Exception if action fails
     */
    void execute(Context context, Map<String, Object> variables) throws Exception;
    
    /**
     * Get action description
     */
    String getDescription();
    
    /**
     * Validate action parameters
     */
    boolean isValid();
}