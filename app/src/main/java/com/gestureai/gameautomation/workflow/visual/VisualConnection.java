package com.gestureai.gameautomation.workflow.visual;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;

/**
 * Visual connection between workflow nodes in the builder
 */
public class VisualConnection {
    private VisualWorkflowNode source;
    private VisualWorkflowNode target;
    private ConnectionType type;
    private boolean isSelected;
    private String label;
    
    public enum ConnectionType {
        SEQUENTIAL, CONDITIONAL, LOOP_BACK, ERROR_HANDLER
    }
    
    public VisualConnection(VisualWorkflowNode source, VisualWorkflowNode target) {
        this.source = source;
        this.target = target;
        this.type = ConnectionType.SEQUENTIAL;
        this.isSelected = false;
    }
    
    public VisualConnection(VisualWorkflowNode source, VisualWorkflowNode target, ConnectionType type) {
        this.source = source;
        this.target = target;
        this.type = type;
        this.isSelected = false;
    }
    
    public void draw(Canvas canvas, Paint basePaint) {
        if (source == null || target == null) return;
        
        PointF startPoint = getSourceConnectionPoint();
        PointF endPoint = getTargetConnectionPoint();
        
        Paint connectionPaint = new Paint(basePaint);
        configureConnectionPaint(connectionPaint);
        
        switch (type) {
            case SEQUENTIAL:
                drawSequentialConnection(canvas, connectionPaint, startPoint, endPoint);
                break;
            case CONDITIONAL:
                drawConditionalConnection(canvas, connectionPaint, startPoint, endPoint);
                break;
            case LOOP_BACK:
                drawLoopConnection(canvas, connectionPaint, startPoint, endPoint);
                break;
            case ERROR_HANDLER:
                drawErrorConnection(canvas, connectionPaint, startPoint, endPoint);
                break;
        }
        
        drawConnectionArrow(canvas, connectionPaint, endPoint, startPoint);
        
        if (label != null && !label.isEmpty()) {
            drawConnectionLabel(canvas, connectionPaint, startPoint, endPoint);
        }
    }
    
    private void configureConnectionPaint(Paint paint) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(isSelected ? 5f : 3f);
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeJoin(Paint.Join.ROUND);
        
        switch (type) {
            case SEQUENTIAL:
                paint.setColor(isSelected ? 0xFF1976D2 : 0xFF2196F3);
                break;
            case CONDITIONAL:
                paint.setColor(isSelected ? 0xFF388E3C : 0xFF4CAF50);
                paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{10f, 5f}, 0));
                break;
            case LOOP_BACK:
                paint.setColor(isSelected ? 0xFFD32F2F : 0xFFF44336);
                break;
            case ERROR_HANDLER:
                paint.setColor(isSelected ? 0xFFE64A19 : 0xFFFF5722);
                paint.setPathEffect(new android.graphics.DashPathEffect(new float[]{5f, 3f}, 0));
                break;
        }
    }
    
    private void drawSequentialConnection(Canvas canvas, Paint paint, PointF start, PointF end) {
        Path path = new Path();
        path.moveTo(start.x, start.y);
        
        // Create smooth curve
        float controlOffset = Math.abs(end.y - start.y) * 0.4f;
        if (controlOffset < 20f) controlOffset = 20f;
        
        path.cubicTo(
            start.x, start.y + controlOffset,
            end.x, end.y - controlOffset,
            end.x, end.y
        );
        
        canvas.drawPath(path, paint);
    }
    
    private void drawConditionalConnection(Canvas canvas, Paint paint, PointF start, PointF end) {
        Path path = new Path();
        path.moveTo(start.x, start.y);
        
        // Conditional connections have a slight curve
        float midX = (start.x + end.x) / 2;
        float midY = (start.y + end.y) / 2;
        
        // Add slight offset for visual distinction
        midX += (start.x < end.x ? 20 : -20);
        
        path.quadTo(midX, midY, end.x, end.y);
        canvas.drawPath(path, paint);
    }
    
    private void drawLoopConnection(Canvas canvas, Paint paint, PointF start, PointF end) {
        Path path = new Path();
        path.moveTo(start.x, start.y);
        
        // Loop connections curve outward
        float offsetX = 60f;
        float offsetY = -40f;
        
        if (start.y > end.y) { // Looping back up
            float controlX1 = start.x + offsetX;
            float controlY1 = start.y + offsetY;
            float controlX2 = end.x + offsetX;
            float controlY2 = end.y - offsetY;
            
            path.cubicTo(controlX1, controlY1, controlX2, controlY2, end.x, end.y);
        } else { // Regular forward loop
            path.quadTo(start.x + offsetX, (start.y + end.y) / 2 + offsetY, end.x, end.y);
        }
        
        canvas.drawPath(path, paint);
    }
    
    private void drawErrorConnection(Canvas canvas, Paint paint, PointF start, PointF end) {
        // Error connections are jagged/angular
        Path path = new Path();
        path.moveTo(start.x, start.y);
        
        float midX = (start.x + end.x) / 2;
        float midY = (start.y + end.y) / 2;
        
        // Create angular path
        path.lineTo(midX - 10, midY);
        path.lineTo(midX + 10, midY);
        path.lineTo(end.x, end.y);
        
        canvas.drawPath(path, paint);
    }
    
    private void drawConnectionArrow(Canvas canvas, Paint paint, PointF end, PointF start) {
        // Calculate arrow direction
        float dx = end.x - start.x;
        float dy = end.y - start.y;
        float length = (float) Math.sqrt(dx * dx + dy * dy);
        
        if (length < 1f) return;
        
        // Normalize
        dx /= length;
        dy /= length;
        
        // Arrow properties
        float arrowLength = 15f;
        float arrowWidth = 8f;
        
        // Calculate arrow points
        float angle = (float) Math.atan2(dy, dx);
        float arrowAngle = 0.5f;
        
        float x1 = end.x - arrowLength * (float) Math.cos(angle - arrowAngle);
        float y1 = end.y - arrowLength * (float) Math.sin(angle - arrowAngle);
        
        float x2 = end.x - arrowLength * (float) Math.cos(angle + arrowAngle);
        float y2 = end.y - arrowLength * (float) Math.sin(angle + arrowAngle);
        
        // Draw filled arrow
        Path arrowPath = new Path();
        arrowPath.moveTo(end.x, end.y);
        arrowPath.lineTo(x1, y1);
        arrowPath.lineTo(x2, y2);
        arrowPath.close();
        
        Paint arrowPaint = new Paint(paint);
        arrowPaint.setStyle(Paint.Style.FILL);
        canvas.drawPath(arrowPath, arrowPaint);
    }
    
    private void drawConnectionLabel(Canvas canvas, Paint paint, PointF start, PointF end) {
        float midX = (start.x + end.x) / 2;
        float midY = (start.y + end.y) / 2;
        
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFF424242);
        textPaint.setTextSize(16f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        
        // Draw background for text
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(0xFFFFFFFF);
        bgPaint.setStyle(Paint.Style.FILL);
        
        float textWidth = textPaint.measureText(label);
        float textHeight = textPaint.getTextSize();
        
        canvas.drawRoundRect(
            midX - textWidth/2 - 4,
            midY - textHeight/2 - 2,
            midX + textWidth/2 + 4,
            midY + textHeight/2 + 2,
            4f, 4f, bgPaint
        );
        
        canvas.drawText(label, midX, midY + textHeight/3, textPaint);
    }
    
    private PointF getSourceConnectionPoint() {
        // Bottom center of source node
        return new PointF(
            source.getX() + 75f, // Half of NODE_WIDTH (150)
            source.getY() + 100f  // NODE_HEIGHT
        );
    }
    
    private PointF getTargetConnectionPoint() {
        // Top center of target node
        return new PointF(
            target.getX() + 75f, // Half of NODE_WIDTH (150)
            target.getY()
        );
    }
    
    public boolean containsPoint(float x, float y) {
        PointF start = getSourceConnectionPoint();
        PointF end = getTargetConnectionPoint();
        
        // Check if point is near the connection line
        float distance = distanceToLine(x, y, start.x, start.y, end.x, end.y);
        return distance <= 12f; // Touch tolerance
    }
    
    private float distanceToLine(float px, float py, float x1, float y1, float x2, float y2) {
        float A = px - x1;
        float B = py - y1;
        float C = x2 - x1;
        float D = y2 - y1;
        
        float dot = A * C + B * D;
        float lenSq = C * C + D * D;
        
        if (lenSq == 0) return (float) Math.sqrt(A * A + B * B);
        
        float param = Math.max(0, Math.min(1, dot / lenSq));
        
        float xx = x1 + param * C;
        float yy = y1 + param * D;
        
        float dx = px - xx;
        float dy = py - yy;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    public void updateEndpoints() {
        // Recalculate connection points if nodes moved
        // Implementation depends on node positioning logic
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof VisualConnection)) return false;
        
        VisualConnection other = (VisualConnection) obj;
        return source.equals(other.source) && target.equals(other.target);
    }
    
    @Override
    public int hashCode() {
        return source.hashCode() * 31 + target.hashCode();
    }
    
    // Getters and setters
    public VisualWorkflowNode getSource() { return source; }
    public void setSource(VisualWorkflowNode source) { this.source = source; }
    
    public VisualWorkflowNode getTarget() { return target; }
    public void setTarget(VisualWorkflowNode target) { this.target = target; }
    
    public ConnectionType getType() { return type; }
    public void setType(ConnectionType type) { this.type = type; }
    
    public boolean isSelected() { return isSelected; }
    public void setSelected(boolean selected) { this.isSelected = selected; }
    
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}