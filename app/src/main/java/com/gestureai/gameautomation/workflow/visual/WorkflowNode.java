package com.gestureai.gameautomation.workflow.visual;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import java.util.HashMap;
import java.util.Map;

/**
 * Visual representation of a workflow step in the drag-and-drop builder
 */
public class WorkflowNode implements Cloneable {
    private String id;
    private String type;
    private String label;
    private float x, y;
    private float width, height;
    private int color;
    private Map<String, Object> properties;
    private boolean isSelected;
    private NodeCategory category;
    
    public enum NodeCategory {
        ACTION, CONDITION, CONTROL, START_END
    }
    
    public WorkflowNode(String type, String label, int color) {
        this.type = type;
        this.label = label;
        this.color = color;
        this.width = 120f;
        this.height = 80f;
        this.properties = new HashMap<>();
        this.category = determineCategory(type);
    }
    
    private NodeCategory determineCategory(String type) {
        switch (type) {
            case "START":
            case "END":
                return NodeCategory.START_END;
            case "CONDITION":
            case "LOOP":
                return NodeCategory.CONTROL;
            case "TEXT_VISIBLE":
            case "ELEMENT_EXISTS":
            case "TIME_ELAPSED":
                return NodeCategory.CONDITION;
            default:
                return NodeCategory.ACTION;
        }
    }
    
    public void draw(Canvas canvas, Paint paint, Paint textPaint) {
        // Set color based on category
        paint.setColor(getColorForCategory());
        
        // Draw node background
        RectF nodeRect = new RectF(x, y, x + width, y + height);
        canvas.drawRoundRect(nodeRect, 8f, 8f, paint);
        
        // Draw border if selected
        if (isSelected) {
            Paint borderPaint = new Paint(paint);
            borderPaint.setStyle(Paint.Style.STROKE);
            borderPaint.setStrokeWidth(3f);
            borderPaint.setColor(0xFFFF9800);
            canvas.drawRoundRect(nodeRect, 8f, 8f, borderPaint);
        }
        
        // Draw label
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(label, x + width / 2, y + height / 2 + textPaint.getTextSize() / 3, textPaint);
        
        // Draw connection points
        drawConnectionPoints(canvas, paint);
    }
    
    private void drawConnectionPoints(Canvas canvas, Paint paint) {
        Paint pointPaint = new Paint(paint);
        pointPaint.setColor(0xFF757575);
        pointPaint.setStyle(Paint.Style.FILL);
        
        float pointRadius = 6f;
        
        // Input point (top)
        if (category != NodeCategory.START_END || type.equals("END")) {
            canvas.drawCircle(x + width / 2, y, pointRadius, pointPaint);
        }
        
        // Output point (bottom)
        if (category != NodeCategory.START_END || type.equals("START")) {
            canvas.drawCircle(x + width / 2, y + height, pointRadius, pointPaint);
        }
    }
    
    private int getColorForCategory() {
        if (isSelected) {
            return lightenColor(color);
        }
        return color;
    }
    
    private int lightenColor(int color) {
        int alpha = (color >> 24) & 0xFF;
        int red = Math.min(255, ((color >> 16) & 0xFF) + 30);
        int green = Math.min(255, ((color >> 8) & 0xFF) + 30);
        int blue = Math.min(255, (color & 0xFF) + 30);
        
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    
    public boolean contains(float touchX, float touchY) {
        return touchX >= x && touchX <= x + width && 
               touchY >= y && touchY <= y + height;
    }
    
    public void move(float deltaX, float deltaY) {
        this.x += deltaX;
        this.y += deltaY;
    }
    
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    public RectF getInputConnectionPoint() {
        float pointSize = 12f;
        return new RectF(
            x + width / 2 - pointSize / 2, 
            y - pointSize / 2,
            x + width / 2 + pointSize / 2, 
            y + pointSize / 2
        );
    }
    
    public RectF getOutputConnectionPoint() {
        float pointSize = 12f;
        return new RectF(
            x + width / 2 - pointSize / 2, 
            y + height - pointSize / 2,
            x + width / 2 + pointSize / 2, 
            y + height + pointSize / 2
        );
    }
    
    public boolean canConnectTo(WorkflowNode target) {
        // Basic connection rules
        if (this.type.equals("END")) return false;
        if (target.type.equals("START")) return false;
        if (this == target) return false;
        
        // Category-specific rules
        switch (this.category) {
            case CONDITION:
                return target.category == NodeCategory.ACTION || 
                       target.category == NodeCategory.CONTROL ||
                       target.type.equals("END");
            case CONTROL:
                return true; // Control nodes can connect to anything
            default:
                return true;
        }
    }
    
    public boolean isActionNode() {
        return category == NodeCategory.ACTION;
    }
    
    public boolean hasCondition() {
        return properties.containsKey("condition_type");
    }
    
    public void addProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public Object getProperty(String key, Object defaultValue) {
        return properties.getOrDefault(key, defaultValue);
    }
    
    @Override
    public WorkflowNode clone() {
        try {
            WorkflowNode cloned = (WorkflowNode) super.clone();
            cloned.properties = new HashMap<>(this.properties);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
    
    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    
    public float getX() { return x; }
    public float getY() { return y; }
    
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { this.isSelected = selected; }
    
    public NodeCategory getCategory() { return category; }
    public void setCategory(NodeCategory category) { this.category = category; }
}