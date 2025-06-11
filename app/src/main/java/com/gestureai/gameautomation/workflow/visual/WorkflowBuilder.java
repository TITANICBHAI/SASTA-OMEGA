package com.gestureai.gameautomation.workflow.visual;

import android.content.Context;
import android.graphics.*;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.gestureai.gameautomation.workflow.WorkflowDefinition;
import com.gestureai.gameautomation.workflow.WorkflowStep;
import com.gestureai.gameautomation.workflow.actions.*;
import com.gestureai.gameautomation.workflow.conditions.*;
import com.gestureai.gameautomation.utils.NLPProcessor;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Visual drag-and-drop workflow builder with real-time preview
 */
public class WorkflowBuilder extends View {
    private static final String TAG = "WorkflowBuilder";
    
    // Visual components
    private Paint actionPaint, conditionPaint, connectorPaint, backgroundPaint;
    private Paint selectedPaint, textPaint, gridPaint;
    private Rect canvasRect;
    private Matrix transformMatrix;
    private float scaleFactor = 1.0f;
    private float panX = 0f, panY = 0f;
    
    // Workflow building state
    private WorkflowDefinition currentWorkflow;
    private List<VisualWorkflowNode> workflowNodes;
    private List<VisualConnection> connections;
    private Map<String, NodeTemplate> nodeTemplates;
    
    // Interaction state
    private VisualWorkflowNode selectedNode;
    private VisualWorkflowNode draggingNode;
    private boolean isConnecting = false;
    private VisualWorkflowNode connectionSource;
    private PointF lastTouchPoint;
    private WorkflowBuilderListener listener;
    
    // Grid and snap settings
    private static final int GRID_SIZE = 50;
    private static final int SNAP_THRESHOLD = 25;
    private boolean showGrid = true;
    private boolean snapToGrid = true;
    
    // Node dimensions
    private static final int NODE_WIDTH = 150;
    private static final int NODE_HEIGHT = 100;
    private static final int NODE_PADDING = 20;
    private static final int CONNECTION_RADIUS = 15;
    
    public interface WorkflowBuilderListener {
        void onWorkflowChanged(WorkflowDefinition workflow);
        void onNodeSelected(VisualWorkflowNode node);
        void onNodePropertiesRequested(VisualWorkflowNode node);
        void onWorkflowValidationResult(boolean isValid, List<String> errors);
    }
    
    public WorkflowBuilder(Context context) {
        super(context);
        initializeBuilder();
    }
    
    private void initializeBuilder() {
        // Initialize paint objects
        actionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        actionPaint.setColor(Color.parseColor("#4CAF50"));
        actionPaint.setStyle(Paint.Style.FILL);
        
        conditionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        conditionPaint.setColor(Color.parseColor("#FF9800"));
        conditionPaint.setStyle(Paint.Style.FILL);
        
        connectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        connectorPaint.setColor(Color.parseColor("#2196F3"));
        connectorPaint.setStyle(Paint.Style.STROKE);
        connectorPaint.setStrokeWidth(4);
        
        selectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        selectedPaint.setColor(Color.parseColor("#E91E63"));
        selectedPaint.setStyle(Paint.Style.STROKE);
        selectedPaint.setStrokeWidth(6);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(24);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        gridPaint.setColor(Color.parseColor("#E0E0E0"));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1);
        
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.parseColor("#FAFAFA"));
        
        // Initialize collections
        workflowNodes = new ArrayList<>();
        connections = new ArrayList<>();
        nodeTemplates = new ConcurrentHashMap<>();
        
        transformMatrix = new Matrix();
        lastTouchPoint = new PointF();
        
        // Load node templates
        loadNodeTemplates();
        
        Log.d(TAG, "Visual workflow builder initialized");
    }
    
    private void loadNodeTemplates() {
        // Action templates
        nodeTemplates.put("TAP", new NodeTemplate("Tap", "Touch screen at position", NodeType.ACTION, actionPaint));
        nodeTemplates.put("SWIPE", new NodeTemplate("Swipe", "Swipe in direction", NodeType.ACTION, actionPaint));
        nodeTemplates.put("TYPE", new NodeTemplate("Type", "Enter text", NodeType.ACTION, actionPaint));
        nodeTemplates.put("WAIT", new NodeTemplate("Wait", "Pause execution", NodeType.ACTION, actionPaint));
        nodeTemplates.put("LONG_PRESS", new NodeTemplate("Long Press", "Hold touch", NodeType.ACTION, actionPaint));
        nodeTemplates.put("DOUBLE_TAP", new NodeTemplate("Double Tap", "Quick double touch", NodeType.ACTION, actionPaint));
        nodeTemplates.put("COLLECT", new NodeTemplate("Collect", "Gather items", NodeType.ACTION, actionPaint));
        
        // Condition templates
        nodeTemplates.put("TEXT_VISIBLE", new NodeTemplate("Text Visible", "Check if text appears", NodeType.CONDITION, conditionPaint));
        nodeTemplates.put("ELEMENT_EXISTS", new NodeTemplate("Element Exists", "Check if element present", NodeType.CONDITION, conditionPaint));
        nodeTemplates.put("TIME_ELAPSED", new NodeTemplate("Time Elapsed", "Wait for duration", NodeType.CONDITION, conditionPaint));
        nodeTemplates.put("REPEAT", new NodeTemplate("Repeat", "Loop actions", NodeType.CONDITION, conditionPaint));
        
        // Control flow templates
        nodeTemplates.put("START", new NodeTemplate("Start", "Workflow entry point", NodeType.CONTROL, selectedPaint));
        nodeTemplates.put("END", new NodeTemplate("End", "Workflow exit point", NodeType.CONTROL, selectedPaint));
        nodeTemplates.put("BRANCH", new NodeTemplate("Branch", "Conditional path", NodeType.CONTROL, selectedPaint));
    }
    
    /**
     * Create new workflow
     */
    public void createNewWorkflow(String name, String description) {
        currentWorkflow = new WorkflowDefinition(name, description);
        workflowNodes.clear();
        connections.clear();
        
        // Add start node
        VisualWorkflowNode startNode = new VisualWorkflowNode("START", "Start", 100, 100);
        startNode.setNodeType(NodeType.CONTROL);
        workflowNodes.add(startNode);
        
        invalidate(); // Trigger redraw
        notifyWorkflowChanged();
        
        Log.d(TAG, "Created new workflow: " + name);
    }
    
    /**
     * Load existing workflow for editing
     */
    public void loadWorkflow(WorkflowDefinition workflow) {
        this.currentWorkflow = workflow;
        workflowNodes.clear();
        connections.clear();
        
        // Convert workflow steps to visual nodes
        float x = 100, y = 100;
        VisualWorkflowNode previousNode = null;
        
        // Add start node
        VisualWorkflowNode startNode = new VisualWorkflowNode("START", "Start", x, y);
        startNode.setNodeType(NodeType.CONTROL);
        workflowNodes.add(startNode);
        previousNode = startNode;
        y += NODE_HEIGHT + NODE_PADDING;
        
        // Add step nodes
        for (WorkflowStep step : workflow.getSteps()) {
            VisualWorkflowNode stepNode = createNodeFromStep(step, x, y);
            workflowNodes.add(stepNode);
            
            // Connect to previous node
            if (previousNode != null) {
                connections.add(new VisualConnection(previousNode, stepNode));
            }
            
            previousNode = stepNode;
            y += NODE_HEIGHT + NODE_PADDING;
        }
        
        // Add end node
        VisualWorkflowNode endNode = new VisualWorkflowNode("END", "End", x, y);
        endNode.setNodeType(NodeType.CONTROL);
        workflowNodes.add(endNode);
        
        if (previousNode != null) {
            connections.add(new VisualConnection(previousNode, endNode));
        }
        
        invalidate();
        Log.d(TAG, "Loaded workflow for editing: " + workflow.getName());
    }
    
    /**
     * Add new node from template
     */
    public void addNodeFromTemplate(String templateId, float x, float y) {
        NodeTemplate template = nodeTemplates.get(templateId);
        if (template == null) {
            Log.w(TAG, "Unknown node template: " + templateId);
            return;
        }
        
        // Snap to grid if enabled
        if (snapToGrid) {
            x = Math.round(x / GRID_SIZE) * GRID_SIZE;
            y = Math.round(y / GRID_SIZE) * GRID_SIZE;
        }
        
        VisualWorkflowNode newNode = new VisualWorkflowNode(templateId, template.getName(), x, y);
        newNode.setNodeType(template.getType());
        newNode.setDescription(template.getDescription());
        
        workflowNodes.add(newNode);
        selectedNode = newNode;
        
        invalidate();
        notifyWorkflowChanged();
        
        Log.d(TAG, "Added node: " + template.getName() + " at (" + x + ", " + y + ")");
    }
    
    /**
     * Connect two nodes
     */
    public boolean connectNodes(VisualWorkflowNode source, VisualWorkflowNode target) {
        if (source == target) return false;
        
        // Check if connection already exists
        for (VisualConnection conn : connections) {
            if (conn.getSource() == source && conn.getTarget() == target) {
                return false; // Already connected
            }
        }
        
        // Validate connection logic
        if (!isValidConnection(source, target)) {
            return false;
        }
        
        connections.add(new VisualConnection(source, target));
        invalidate();
        notifyWorkflowChanged();
        
        return true;
    }
    
    /**
     * Generate workflow from visual representation
     */
    public WorkflowDefinition generateWorkflow() {
        if (currentWorkflow == null) {
            Log.w(TAG, "No current workflow to generate");
            return null;
        }
        
        WorkflowDefinition generated = new WorkflowDefinition(
            currentWorkflow.getName(),
            currentWorkflow.getDescription()
        );
        
        // Find start node and traverse connections
        VisualWorkflowNode startNode = findNodeByType("START");
        if (startNode == null) {
            Log.w(TAG, "No start node found");
            return null;
        }
        
        List<VisualWorkflowNode> orderedNodes = traverseWorkflow(startNode);
        
        // Convert nodes to workflow steps
        for (VisualWorkflowNode node : orderedNodes) {
            if (node.getNodeType() == NodeType.ACTION) {
                WorkflowStep step = createStepFromNode(node);
                if (step != null) {
                    generated.addStep(step);
                }
            }
        }
        
        Log.d(TAG, "Generated workflow with " + generated.getSteps().size() + " steps");
        return generated;
    }
    
    /**
     * Validate current workflow
     */
    public ValidationResult validateWorkflow() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Check for start node
        if (findNodeByType("START") == null) {
            errors.add("Workflow must have a start node");
        }
        
        // Check for end node
        if (findNodeByType("END") == null) {
            warnings.add("Workflow should have an end node");
        }
        
        // Check for disconnected nodes
        for (VisualWorkflowNode node : workflowNodes) {
            if (!isNodeConnected(node) && !node.getId().equals("START")) {
                warnings.add("Node '" + node.getDisplayName() + "' is not connected");
            }
        }
        
        // Check for circular dependencies
        if (hasCircularDependencies()) {
            errors.add("Workflow contains circular dependencies");
        }
        
        // Validate node properties
        for (VisualWorkflowNode node : workflowNodes) {
            List<String> nodeErrors = validateNodeProperties(node);
            errors.addAll(nodeErrors);
        }
        
        boolean isValid = errors.isEmpty();
        
        if (listener != null) {
            listener.onWorkflowValidationResult(isValid, errors);
        }
        
        return new ValidationResult(isValid, errors, warnings);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // Apply transformations
        canvas.save();
        canvas.translate(panX, panY);
        canvas.scale(scaleFactor, scaleFactor);
        
        // Draw background
        canvas.drawRect(0, 0, getWidth() / scaleFactor, getHeight() / scaleFactor, backgroundPaint);
        
        // Draw grid
        if (showGrid) {
            drawGrid(canvas);
        }
        
        // Draw connections
        for (VisualConnection connection : connections) {
            drawConnection(canvas, connection);
        }
        
        // Draw nodes
        for (VisualWorkflowNode node : workflowNodes) {
            drawNode(canvas, node);
        }
        
        // Draw connection preview if connecting
        if (isConnecting && connectionSource != null) {
            drawConnectionPreview(canvas);
        }
        
        canvas.restore();
    }
    
    private void drawGrid(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        
        // Vertical lines
        for (int x = 0; x < width; x += GRID_SIZE) {
            canvas.drawLine(x, 0, x, height, gridPaint);
        }
        
        // Horizontal lines
        for (int y = 0; y < height; y += GRID_SIZE) {
            canvas.drawLine(0, y, width, y, gridPaint);
        }
    }
    
    private void drawNode(Canvas canvas, VisualWorkflowNode node) {
        Rect nodeRect = new Rect(
            (int)node.getX(),
            (int)node.getY(),
            (int)(node.getX() + NODE_WIDTH),
            (int)(node.getY() + NODE_HEIGHT)
        );
        
        // Choose paint based on node type
        Paint nodePaint = getNodePaint(node.getNodeType());
        
        // Draw node background
        canvas.drawRoundRect(new RectF(nodeRect), 10, 10, nodePaint);
        
        // Draw selection highlight
        if (node == selectedNode) {
            canvas.drawRoundRect(new RectF(nodeRect), 10, 10, selectedPaint);
        }
        
        // Draw node text
        String displayText = node.getDisplayName();
        float textX = nodeRect.centerX();
        float textY = nodeRect.centerY() + (textPaint.getTextSize() / 3);
        canvas.drawText(displayText, textX, textY, textPaint);
        
        // Draw connection points
        drawConnectionPoints(canvas, node);
        
        // Draw node status indicators
        if (node.hasErrors()) {
            drawErrorIndicator(canvas, nodeRect);
        }
    }
    
    private void drawConnection(Canvas canvas, VisualConnection connection) {
        VisualWorkflowNode source = connection.getSource();
        VisualWorkflowNode target = connection.getTarget();
        
        PointF sourcePoint = getNodeConnectionPoint(source, true);
        PointF targetPoint = getNodeConnectionPoint(target, false);
        
        // Draw curved connection line
        Path connectionPath = new Path();
        connectionPath.moveTo(sourcePoint.x, sourcePoint.y);
        
        float controlOffset = Math.abs(targetPoint.y - sourcePoint.y) * 0.5f;
        connectionPath.cubicTo(
            sourcePoint.x, sourcePoint.y + controlOffset,
            targetPoint.x, targetPoint.y - controlOffset,
            targetPoint.x, targetPoint.y
        );
        
        canvas.drawPath(connectionPath, connectorPaint);
        
        // Draw arrow at target
        drawArrow(canvas, targetPoint, getConnectionAngle(sourcePoint, targetPoint));
    }
    
    private void drawConnectionPreview(Canvas canvas) {
        PointF sourcePoint = getNodeConnectionPoint(connectionSource, true);
        
        Paint previewPaint = new Paint(connectorPaint);
        previewPaint.setAlpha(128);
        
        canvas.drawLine(sourcePoint.x, sourcePoint.y, lastTouchPoint.x, lastTouchPoint.y, previewPaint);
    }
    
    private void drawConnectionPoints(Canvas canvas, VisualWorkflowNode node) {
        Paint pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setColor(Color.WHITE);
        pointPaint.setStyle(Paint.Style.FILL);
        
        // Input point (top)
        PointF inputPoint = getNodeConnectionPoint(node, false);
        canvas.drawCircle(inputPoint.x, inputPoint.y, CONNECTION_RADIUS / 2, pointPaint);
        
        // Output point (bottom)
        PointF outputPoint = getNodeConnectionPoint(node, true);
        canvas.drawCircle(outputPoint.x, outputPoint.y, CONNECTION_RADIUS / 2, pointPaint);
    }
    
    private void drawArrow(Canvas canvas, PointF point, float angle) {
        canvas.save();
        canvas.translate(point.x, point.y);
        canvas.rotate((float)Math.toDegrees(angle));
        
        Path arrowPath = new Path();
        arrowPath.moveTo(0, 0);
        arrowPath.lineTo(-15, -8);
        arrowPath.lineTo(-15, 8);
        arrowPath.close();
        
        canvas.drawPath(arrowPath, connectorPaint);
        canvas.restore();
    }
    
    private void drawErrorIndicator(Canvas canvas, Rect nodeRect) {
        Paint errorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        errorPaint.setColor(Color.RED);
        
        float x = nodeRect.right - 15;
        float y = nodeRect.top + 15;
        canvas.drawCircle(x, y, 8, errorPaint);
        
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(12);
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("!", x, y + 4, textPaint);
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Convert touch coordinates to canvas coordinates
        float[] point = {event.getX(), event.getY()};
        Matrix inverse = new Matrix();
        transformMatrix.invert(inverse);
        inverse.mapPoints(point);
        
        float canvasX = point[0] - panX;
        float canvasY = point[1] - panY;
        
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                return handleTouchDown(canvasX, canvasY);
                
            case MotionEvent.ACTION_MOVE:
                return handleTouchMove(canvasX, canvasY);
                
            case MotionEvent.ACTION_UP:
                return handleTouchUp(canvasX, canvasY);
        }
        
        return true;
    }
    
    private boolean handleTouchDown(float x, float y) {
        lastTouchPoint.set(x, y);
        
        // Check if touching a node
        VisualWorkflowNode touchedNode = findNodeAt(x, y);
        if (touchedNode != null) {
            selectedNode = touchedNode;
            draggingNode = touchedNode;
            
            if (listener != null) {
                listener.onNodeSelected(touchedNode);
            }
            
            invalidate();
            return true;
        }
        
        // Check if touching a connection point for connecting
        VisualWorkflowNode connectionNode = findConnectionPointAt(x, y);
        if (connectionNode != null && !isConnecting) {
            isConnecting = true;
            connectionSource = connectionNode;
            return true;
        }
        
        // Clear selection if touching empty space
        selectedNode = null;
        invalidate();
        return true;
    }
    
    private boolean handleTouchMove(float x, float y) {
        float deltaX = x - lastTouchPoint.x;
        float deltaY = y - lastTouchPoint.y;
        
        if (draggingNode != null) {
            // Move the selected node
            draggingNode.setPosition(draggingNode.getX() + deltaX, draggingNode.getY() + deltaY);
            
            // Snap to grid if enabled
            if (snapToGrid) {
                float snappedX = Math.round(draggingNode.getX() / GRID_SIZE) * GRID_SIZE;
                float snappedY = Math.round(draggingNode.getY() / GRID_SIZE) * GRID_SIZE;
                draggingNode.setPosition(snappedX, snappedY);
            }
            
            invalidate();
        } else if (isConnecting) {
            // Update connection preview
            lastTouchPoint.set(x, y);
            invalidate();
        } else {
            // Pan the canvas
            panX += deltaX;
            panY += deltaY;
            invalidate();
        }
        
        lastTouchPoint.set(x, y);
        return true;
    }
    
    private boolean handleTouchUp(float x, float y) {
        if (isConnecting) {
            // Try to complete connection
            VisualWorkflowNode targetNode = findNodeAt(x, y);
            if (targetNode != null && targetNode != connectionSource) {
                connectNodes(connectionSource, targetNode);
            }
            
            isConnecting = false;
            connectionSource = null;
            invalidate();
        }
        
        draggingNode = null;
        return true;
    }
    
    // Helper methods
    
    private VisualWorkflowNode findNodeAt(float x, float y) {
        for (VisualWorkflowNode node : workflowNodes) {
            if (x >= node.getX() && x <= node.getX() + NODE_WIDTH &&
                y >= node.getY() && y <= node.getY() + NODE_HEIGHT) {
                return node;
            }
        }
        return null;
    }
    
    private VisualWorkflowNode findConnectionPointAt(float x, float y) {
        for (VisualWorkflowNode node : workflowNodes) {
            PointF outputPoint = getNodeConnectionPoint(node, true);
            float distance = (float)Math.sqrt(
                Math.pow(x - outputPoint.x, 2) + Math.pow(y - outputPoint.y, 2)
            );
            
            if (distance <= CONNECTION_RADIUS) {
                return node;
            }
        }
        return null;
    }
    
    private VisualWorkflowNode findNodeByType(String type) {
        for (VisualWorkflowNode node : workflowNodes) {
            if (type.equals(node.getId())) {
                return node;
            }
        }
        return null;
    }
    
    private PointF getNodeConnectionPoint(VisualWorkflowNode node, boolean isOutput) {
        float x = node.getX() + NODE_WIDTH / 2;
        float y = isOutput ? node.getY() + NODE_HEIGHT : node.getY();
        return new PointF(x, y);
    }
    
    private float getConnectionAngle(PointF from, PointF to) {
        return (float)Math.atan2(to.y - from.y, to.x - from.x);
    }
    
    private Paint getNodePaint(NodeType type) {
        switch (type) {
            case ACTION: return actionPaint;
            case CONDITION: return conditionPaint;
            case CONTROL: return selectedPaint;
            default: return actionPaint;
        }
    }
    
    private VisualWorkflowNode createNodeFromStep(WorkflowStep step, float x, float y) {
        String stepType = step.getName(); // Assuming step name matches node type
        VisualWorkflowNode node = new VisualWorkflowNode(stepType, stepType, x, y);
        node.setNodeType(NodeType.ACTION);
        
        // Set node properties from step
        if (step.getAction() != null) {
            node.setProperties(extractActionProperties(step.getAction()));
        }
        
        return node;
    }
    
    private WorkflowStep createStepFromNode(VisualWorkflowNode node) {
        WorkflowStep step = new WorkflowStep(node.getId());
        
        // Create appropriate action based on node type
        WorkflowAction action = createActionFromNode(node);
        if (action != null) {
            step.setAction(action);
        }
        
        return step;
    }
    
    private WorkflowAction createActionFromNode(VisualWorkflowNode node) {
        Map<String, Object> props = node.getProperties();
        
        switch (node.getId()) {
            case "TAP":
                int x = (int)props.getOrDefault("x", 500);
                int y = (int)props.getOrDefault("y", 500);
                return new TapAction(x, y);
                
            case "SWIPE":
                String direction = (String)props.getOrDefault("direction", "up");
                return new SwipeAction(direction);
                
            case "TYPE":
                String text = (String)props.getOrDefault("text", "");
                return new TypeTextAction(text);
                
            case "WAIT":
                long duration = (long)props.getOrDefault("duration", 1000L);
                return new WaitAction(duration);
                
            case "LONG_PRESS":
                int lpX = (int)props.getOrDefault("x", 500);
                int lpY = (int)props.getOrDefault("y", 500);
                return new LongPressAction(lpX, lpY);
                
            case "DOUBLE_TAP":
                int dtX = (int)props.getOrDefault("x", 500);
                int dtY = (int)props.getOrDefault("y", 500);
                return new DoubleTapAction(dtX, dtY);
                
            default:
                return null;
        }
    }
    
    private Map<String, Object> extractActionProperties(WorkflowAction action) {
        Map<String, Object> props = new HashMap<>();
        
        // Extract properties based on action type
        // This would be implemented based on your specific action classes
        
        return props;
    }
    
    private boolean isValidConnection(VisualWorkflowNode source, VisualWorkflowNode target) {
        // Basic validation rules
        if (source.getNodeType() == NodeType.CONTROL && source.getId().equals("END")) {
            return false; // Can't connect from end node
        }
        
        if (target.getNodeType() == NodeType.CONTROL && target.getId().equals("START")) {
            return false; // Can't connect to start node
        }
        
        return true;
    }
    
    private boolean isNodeConnected(VisualWorkflowNode node) {
        for (VisualConnection conn : connections) {
            if (conn.getSource() == node || conn.getTarget() == node) {
                return true;
            }
        }
        return false;
    }
    
    private boolean hasCircularDependencies() {
        // Simple cycle detection using DFS
        Set<VisualWorkflowNode> visited = new HashSet<>();
        Set<VisualWorkflowNode> recursionStack = new HashSet<>();
        
        for (VisualWorkflowNode node : workflowNodes) {
            if (hasCycleDFS(node, visited, recursionStack)) {
                return true;
            }
        }
        
        return false;
    }
    
    private boolean hasCycleDFS(VisualWorkflowNode node, Set<VisualWorkflowNode> visited, Set<VisualWorkflowNode> recursionStack) {
        if (recursionStack.contains(node)) {
            return true; // Back edge found
        }
        
        if (visited.contains(node)) {
            return false; // Already processed
        }
        
        visited.add(node);
        recursionStack.add(node);
        
        // Check all connected nodes
        for (VisualConnection conn : connections) {
            if (conn.getSource() == node) {
                if (hasCycleDFS(conn.getTarget(), visited, recursionStack)) {
                    return true;
                }
            }
        }
        
        recursionStack.remove(node);
        return false;
    }
    
    private List<VisualWorkflowNode> traverseWorkflow(VisualWorkflowNode startNode) {
        List<VisualWorkflowNode> result = new ArrayList<>();
        Set<VisualWorkflowNode> visited = new HashSet<>();
        
        traverseDFS(startNode, visited, result);
        
        return result;
    }
    
    private void traverseDFS(VisualWorkflowNode node, Set<VisualWorkflowNode> visited, List<VisualWorkflowNode> result) {
        if (visited.contains(node)) return;
        
        visited.add(node);
        result.add(node);
        
        // Find connected nodes
        for (VisualConnection conn : connections) {
            if (conn.getSource() == node) {
                traverseDFS(conn.getTarget(), visited, result);
            }
        }
    }
    
    private List<String> validateNodeProperties(VisualWorkflowNode node) {
        List<String> errors = new ArrayList<>();
        
        // Validate based on node type
        switch (node.getId()) {
            case "TAP":
            case "LONG_PRESS":
            case "DOUBLE_TAP":
                if (!node.getProperties().containsKey("x") || !node.getProperties().containsKey("y")) {
                    errors.add("Node '" + node.getDisplayName() + "' missing coordinates");
                }
                break;
                
            case "TYPE":
                if (!node.getProperties().containsKey("text") || 
                    ((String)node.getProperties().get("text")).isEmpty()) {
                    errors.add("Node '" + node.getDisplayName() + "' missing text");
                }
                break;
                
            case "WAIT":
                if (!node.getProperties().containsKey("duration")) {
                    errors.add("Node '" + node.getDisplayName() + "' missing duration");
                }
                break;
        }
        
        return errors;
    }
    
    private void notifyWorkflowChanged() {
        if (listener != null && currentWorkflow != null) {
            WorkflowDefinition updated = generateWorkflow();
            if (updated != null) {
                listener.onWorkflowChanged(updated);
            }
        }
    }
    
    // Public interface methods
    
    public void setListener(WorkflowBuilderListener listener) {
        this.listener = listener;
    }
    
    public void setShowGrid(boolean show) {
        this.showGrid = show;
        invalidate();
    }
    
    public void setSnapToGrid(boolean snap) {
        this.snapToGrid = snap;
    }
    
    public void setZoom(float scale) {
        this.scaleFactor = Math.max(0.1f, Math.min(3.0f, scale));
        invalidate();
    }
    
    public void centerWorkflow() {
        if (workflowNodes.isEmpty()) return;
        
        // Calculate workflow bounds
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE;
        
        for (VisualWorkflowNode node : workflowNodes) {
            minX = Math.min(minX, node.getX());
            minY = Math.min(minY, node.getY());
            maxX = Math.max(maxX, node.getX() + NODE_WIDTH);
            maxY = Math.max(maxY, node.getY() + NODE_HEIGHT);
        }
        
        float workflowWidth = maxX - minX;
        float workflowHeight = maxY - minY;
        
        panX = (getWidth() - workflowWidth * scaleFactor) / 2 - minX * scaleFactor;
        panY = (getHeight() - workflowHeight * scaleFactor) / 2 - minY * scaleFactor;
        
        invalidate();
    }
    
    public List<String> getAvailableTemplates() {
        return new ArrayList<>(nodeTemplates.keySet());
    }
    
    public NodeTemplate getTemplate(String templateId) {
        return nodeTemplates.get(templateId);
    }
    
    public VisualWorkflowNode getSelectedNode() {
        return selectedNode;
    }
    
    public void deleteSelectedNode() {
        if (selectedNode != null) {
            workflowNodes.remove(selectedNode);
            
            // Remove associated connections
            connections.removeIf(conn -> 
                conn.getSource() == selectedNode || conn.getTarget() == selectedNode);
            
            selectedNode = null;
            invalidate();
            notifyWorkflowChanged();
        }
    }
    
    public void exportWorkflow() {
        try {
            JSONObject export = new JSONObject();
            export.put("name", currentWorkflow.getName());
            export.put("description", currentWorkflow.getDescription());
            
            // Export nodes
            JSONArray nodesArray = new JSONArray();
            for (VisualWorkflowNode node : workflowNodes) {
                JSONObject nodeJson = new JSONObject();
                nodeJson.put("id", node.getId());
                nodeJson.put("name", node.getDisplayName());
                nodeJson.put("x", node.getX());
                nodeJson.put("y", node.getY());
                nodeJson.put("type", node.getNodeType().toString());
                
                JSONObject propsJson = new JSONObject();
                for (Map.Entry<String, Object> prop : node.getProperties().entrySet()) {
                    propsJson.put(prop.getKey(), prop.getValue());
                }
                nodeJson.put("properties", propsJson);
                
                nodesArray.put(nodeJson);
            }
            export.put("nodes", nodesArray);
            
            // Export connections
            JSONArray connectionsArray = new JSONArray();
            for (VisualConnection conn : connections) {
                JSONObject connJson = new JSONObject();
                connJson.put("source", workflowNodes.indexOf(conn.getSource()));
                connJson.put("target", workflowNodes.indexOf(conn.getTarget()));
                connectionsArray.put(connJson);
            }
            export.put("connections", connectionsArray);
            
            Log.d(TAG, "Exported workflow: " + export.toString());
            
        } catch (Exception e) {
            Log.e(TAG, "Error exporting workflow", e);
        }
    }
    
    // Inner classes and enums
    
    public enum NodeType {
        ACTION, CONDITION, CONTROL
    }
    
    public static class NodeTemplate {
        private String name;
        private String description;
        private NodeType type;
        private Paint paint;
        
        public NodeTemplate(String name, String description, NodeType type, Paint paint) {
            this.name = name;
            this.description = description;
            this.type = type;
            this.paint = paint;
        }
        
        public String getName() { return name; }
        public String getDescription() { return description; }
        public NodeType getType() { return type; }
        public Paint getPaint() { return paint; }
    }
    
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;
        
        public ValidationResult(boolean valid, List<String> errors, List<String> warnings) {
            this.valid = valid;
            this.errors = errors;
            this.warnings = warnings;
        }
        
        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
        public List<String> getWarnings() { return warnings; }
    }
}