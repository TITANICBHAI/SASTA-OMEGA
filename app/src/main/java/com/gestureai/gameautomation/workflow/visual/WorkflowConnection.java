package com.gestureai.gameautomation.workflow.visual;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/**
 * Visual connection between workflow nodes
 */
public class WorkflowConnection {
    private WorkflowNode source;
    private WorkflowNode target;
    private String connectionType;
    private boolean isSelected;
    
    public WorkflowConnection(WorkflowNode source, WorkflowNode target) {
        this.source = source;
        this.target = target;
        this.connectionType = "SEQUENTIAL";
    }
    
    public WorkflowConnection(WorkflowNode source, WorkflowNode target, String connectionType) {
        this.source = source;
        this.target = target;
        this.connectionType = connectionType;
    }
    
    public void draw(Canvas canvas, Paint paint) {
        if (source == null || target == null) return;
        
        // Get connection points
        RectF sourcePoint = source.getOutputConnectionPoint();
        RectF targetPoint = target.getInputConnectionPoint();
        
        float startX = sourcePoint.centerX();
        float startY = sourcePoint.centerY();
        float endX = targetPoint.centerX();
        float endY = targetPoint.centerY();
        
        // Draw connection based on type
        switch (connectionType) {
            case "SEQUENTIAL":
                drawSequentialConnection(canvas, paint, startX, startY, endX, endY);
                break;
            case "CONDITIONAL":
                drawConditionalConnection(canvas, paint, startX, startY, endX, endY);
                break;
            case "LOOP":
                drawLoopConnection(canvas, paint, startX, startY, endX, endY);
                break;
            default:
                drawSequentialConnection(canvas, paint, startX, startY, endX, endY);
                break;
        }
        
        // Draw arrow
        drawArrow(canvas, paint, endX, endY, startX, startY);
    }
    
    private void drawSequentialConnection(Canvas canvas, Paint paint, float startX, float startY, float endX, float endY) {
        // Draw curved line for better visual appeal
        Path path = new Path();
        path.moveTo(startX, startY);
        
        float controlPointOffset = Math.abs(endY - startY) * 0.5f;
        path.cubicTo(
            startX, startY + controlPointOffset,  // Control point 1
            endX, endY - controlPointOffset,      // Control point 2
            endX, endY                            // End point
        );
        
        Paint connectionPaint = new Paint(paint);
        connectionPaint.setStyle(Paint.Style.STROKE);
        connectionPaint.setStrokeWidth(isSelected ? 4f : 3f);
        connectionPaint.setColor(isSelected ? 0xFFFF9800 : 0xFF2196F3);
        
        canvas.drawPath(path, connectionPaint);
    }
    
    private void drawConditionalConnection(Canvas canvas, Paint paint, float startX, float startY, float endX, float endY) {
        Paint conditionalPaint = new Paint(paint);
        conditionalPaint.setStyle(Paint.Style.STROKE);
        conditionalPaint.setStrokeWidth(3f);
        conditionalPaint.setColor(0xFF4CAF50);
        conditionalPaint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10f, 5f}, 0));
        
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        
        canvas.drawPath(path, conditionalPaint);
    }
    
    private void drawLoopConnection(Canvas canvas, Paint paint, float startX, float startY, float endX, float endY) {
        Paint loopPaint = new Paint(paint);
        loopPaint.setStyle(Paint.Style.STROKE);
        loopPaint.setStrokeWidth(3f);
        loopPaint.setColor(0xFFFF5722);
        
        // Draw curved loop back
        Path path = new Path();
        path.moveTo(startX, startY);
        
        float midX = (startX + endX) / 2;
        float midY = Math.min(startY, endY) - 50f; // Arc above
        
        path.quadTo(midX, midY, endX, endY);
        
        canvas.drawPath(path, loopPaint);
    }
    
    private void drawArrow(Canvas canvas, Paint paint, float endX, float endY, float startX, float startY) {
        // Calculate arrow direction
        float dx = endX - startX;
        float dy = endY - startY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (length == 0) return;
        
        // Normalize direction
        dx /= length;
        dy /= length;
        
        // Arrow properties
        float arrowLength = 12f;
        float arrowAngle = 0.5f;
        
        // Calculate arrow points
        float arrowX1 = endX - arrowLength * (float) Math.cos(Math.atan2(dy, dx) - arrowAngle);
        float arrowY1 = endY - arrowLength * (float) Math.sin(Math.atan2(dy, dx) - arrowAngle);
        
        float arrowX2 = endX - arrowLength * (float) Math.cos(Math.atan2(dy, dx) + arrowAngle);
        float arrowY2 = endY - arrowLength * (float) Math.sin(Math.atan2(dy, dx) + arrowAngle);
        
        // Draw arrow
        Path arrowPath = new Path();
        arrowPath.moveTo(endX, endY);
        arrowPath.lineTo(arrowX1, arrowY1);
        arrowPath.moveTo(endX, endY);
        arrowPath.lineTo(arrowX2, arrowY2);
        
        Paint arrowPaint = new Paint(paint);
        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeWidth(3f);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        
        canvas.drawPath(arrowPath, arrowPaint);
    }
    
    public boolean contains(float x, float y) {
        // Check if point is near the connection line
        if (source == null || target == null) return false;
        
        RectF sourcePoint = source.getOutputConnectionPoint();
        RectF targetPoint = target.getInputConnectionPoint();
        
        float startX = sourcePoint.centerX();
        float startY = sourcePoint.centerY();
        float endX = targetPoint.centerX();
        float endY = targetPoint.centerY();
        
        // Distance from point to line
        float distance = distanceFromPointToLine(x, y, startX, startY, endX, endY);
        return distance < 10f; // Tolerance for selection
    }
    
    private float distanceFromPointToLine(float px, float py, float x1, float y1, float x2, float y2) {
        float A = px - x1;
        float B = py - y1;
        float C = x2 - x1;
        float D = y2 - y1;
        
        float dot = A * C + B * D;
        float lenSq = C * C + D * D;
        
        if (lenSq == 0) return (float) Math.sqrt(A * A + B * B);
        
        float param = dot / lenSq;
        
        float xx, yy;
        if (param < 0) {
            xx = x1;
            yy = y1;
        } else if (param > 1) {
            xx = x2;
            yy = y2;
        } else {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }
        
        float dx = px - xx;
        float dy = py - yy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    // Getters and setters
    public WorkflowNode getSource() { return source; }
    public void setSource(WorkflowNode source) { this.source = source; }
    
    public WorkflowNode getTarget() { return target; }
    public void setTarget(WorkflowNode target) { this.target = target; }
    
    public String getConnectionType() { return connectionType; }
    public void setConnectionType(String connectionType) { this.connectionType = connectionType; }
    
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { this.isSelected = selected; }
}