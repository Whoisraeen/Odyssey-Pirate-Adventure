package com.odyssey.ui;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Base class for all UI elements in the scene graph.
 * Provides hierarchical structure, positioning, and event handling.
 */
public abstract class UINode {
    private static final Logger logger = LoggerFactory.getLogger(UINode.class);
    
    // Hierarchy
    protected UINode parent;
    protected final List<UINode> children = new CopyOnWriteArrayList<>();
    
    // Transform properties
    protected final Vector2f position = new Vector2f();
    protected final Vector2f size = new Vector2f();
    protected final Vector2f anchor = new Vector2f(0.0f, 0.0f); // 0,0 = top-left, 1,1 = bottom-right
    protected final Vector2f pivot = new Vector2f(0.0f, 0.0f);
    
    // Visual properties
    protected boolean visible = true;
    protected float opacity = 1.0f;
    protected final Vector4f backgroundColor = new Vector4f(0.0f, 0.0f, 0.0f, 0.0f);
    
    // Layout properties
    protected boolean needsLayout = true;
    protected final Vector2f computedPosition = new Vector2f();
    protected final Vector2f computedSize = new Vector2f();
    
    // Event handling
    protected boolean interactive = true;
    protected boolean enabled = true;
    protected boolean focusable = false;
    protected boolean focused = false;
    protected boolean hovered = false;
    protected boolean pressed = false;
    
    // Event listeners
    protected final List<UIEventListener> eventListeners = new ArrayList<>();
    
    /**
     * Creates a new UI node.
     */
    public UINode() {
        logger.debug("Created UI node: {}", getClass().getSimpleName());
    }
    
    /**
     * Adds a child node to this node.
     */
    public void addChild(UINode child) {
        if (child == null) {
            throw new IllegalArgumentException("Child cannot be null");
        }
        
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        
        children.add(child);
        child.parent = this;
        child.markNeedsLayout();
        markNeedsLayout();
        
        logger.debug("Added child {} to {}", child.getClass().getSimpleName(), getClass().getSimpleName());
    }
    
    /**
     * Removes a child node from this node.
     */
    public void removeChild(UINode child) {
        if (children.remove(child)) {
            child.parent = null;
            markNeedsLayout();
            logger.debug("Removed child {} from {}", child.getClass().getSimpleName(), getClass().getSimpleName());
        }
    }
    
    /**
     * Removes all children from this node.
     */
    public void removeAllChildren() {
        for (UINode child : children) {
            child.parent = null;
        }
        children.clear();
        markNeedsLayout();
        logger.debug("Removed all children from {}", getClass().getSimpleName());
    }
    
    /**
     * Updates the node and all its children.
     */
    public void update(double deltaTime) {
        if (!visible) return;
        
        // Update layout if needed
        if (needsLayout) {
            updateLayout();
            needsLayout = false;
        }
        
        // Update this node
        onUpdate(deltaTime);
        
        // Update children
        for (UINode child : children) {
            child.update(deltaTime);
        }
    }
    
    /**
     * Renders the node and all its children.
     */
    public void render(UIRenderer renderer) {
        if (!visible || opacity <= 0.0f) return;
        
        // Render this node
        onRender(renderer);
        
        // Render children
        for (UINode child : children) {
            child.render(renderer);
        }
    }
    
    /**
     * Handles input events for this node and its children.
     */
    public boolean handleInput(UIInputEvent event) {
        if (!visible || !interactive) return false;
        
        // Check children first (front to back)
        for (int i = children.size() - 1; i >= 0; i--) {
            UINode child = children.get(i);
            if (child.handleInput(event)) {
                return true; // Event consumed by child
            }
        }
        
        // Check if event is within this node's bounds
        if (containsPoint(event.getPosition())) {
            return onInput(event);
        }
        
        return false;
    }
    
    /**
     * Checks if a point is within this node's bounds.
     */
    public boolean containsPoint(Vector2f point) {
        return point.x >= computedPosition.x && 
               point.x <= computedPosition.x + computedSize.x &&
               point.y >= computedPosition.y && 
               point.y <= computedPosition.y + computedSize.y;
    }
    
    /**
     * Marks this node as needing layout recalculation.
     */
    public void markNeedsLayout() {
        needsLayout = true;
        
        // Propagate to children
        for (UINode child : children) {
            child.markNeedsLayout();
        }
    }
    
    /**
     * Updates the layout of this node.
     */
    protected void updateLayout() {
        // Calculate computed position based on parent and anchor
        if (parent != null) {
            Vector2f parentPos = parent.getComputedPosition();
            Vector2f parentSize = parent.getComputedSize();
            
            computedPosition.x = parentPos.x + (parentSize.x * anchor.x) + position.x - (size.x * pivot.x);
            computedPosition.y = parentPos.y + (parentSize.y * anchor.y) + position.y - (size.y * pivot.y);
        } else {
            computedPosition.x = position.x - (size.x * pivot.x);
            computedPosition.y = position.y - (size.y * pivot.y);
        }
        
        // Copy size (can be overridden by subclasses for auto-sizing)
        computedSize.set(size);
        
        // Allow subclasses to perform custom layout
        onLayout();
    }
    
    /**
     * Adds an event listener to this node.
     */
    public void addEventListener(UIEventListener listener) {
        eventListeners.add(listener);
    }
    
    /**
     * Removes an event listener from this node.
     */
    public void removeEventListener(UIEventListener listener) {
        eventListeners.remove(listener);
    }
    
    /**
     * Fires an event to all listeners.
     */
    protected void fireEvent(UIEvent event) {
        for (UIEventListener listener : eventListeners) {
            listener.onEvent(event);
        }
    }
    
    // Abstract methods for subclasses
    protected abstract void onUpdate(double deltaTime);
    protected abstract void onRender(UIRenderer renderer);
    protected abstract boolean onInput(UIInputEvent event);
    protected abstract void onLayout();
    
    /**
     * Called when the mouse exits this node.
     */
    public void onMouseExit() {
        hovered = false;
    }
    
    // Getters and setters
    public Vector2f getPosition() { return new Vector2f(position); }
    public void setPosition(float x, float y) { 
        position.set(x, y); 
        markNeedsLayout(); 
    }
    
    public Vector2f getSize() { return new Vector2f(size); }
    public void setSize(float width, float height) { 
        size.set(width, height); 
        markNeedsLayout(); 
    }
    
    public Vector2f getAnchor() { return new Vector2f(anchor); }
    public void setAnchor(float x, float y) { 
        anchor.set(x, y); 
        markNeedsLayout(); 
    }
    
    public Vector2f getPivot() { return new Vector2f(pivot); }
    public void setPivot(float x, float y) { 
        pivot.set(x, y); 
        markNeedsLayout(); 
    }
    
    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }
    
    public float getOpacity() { return opacity; }
    public void setOpacity(float opacity) { this.opacity = Math.max(0.0f, Math.min(1.0f, opacity)); }
    
    public Vector4f getBackgroundColor() { return new Vector4f(backgroundColor); }
    public void setBackgroundColor(float r, float g, float b, float a) { 
        backgroundColor.set(r, g, b, a); 
    }
    
    public boolean isInteractive() { return interactive; }
    public void setInteractive(boolean interactive) { this.interactive = interactive; }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isFocusable() { return focusable; }
    public void setFocusable(boolean focusable) { this.focusable = focusable; }
    
    public boolean isFocused() { return focused; }
    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    /**
     * Requests focus for this node.
     */
    public void requestFocus() {
        // This would typically be handled by the UIManager
        // For now, just set focused to true
        setFocused(true);
    }

    /**
     * Releases focus from this node.
     */
    public void releaseFocus() {
        setFocused(false);
    }

    /**
     * Sets whether this node clips its children to its bounds.
     */
    public void setClipChildren(boolean clipChildren) {
        // This would be used for rendering optimization
        // For now, just store the value (could add a field if needed)
    }
    
    public boolean isHovered() { return hovered; }
    public boolean isPressed() { return pressed; }
    
    public Vector2f getComputedPosition() { return new Vector2f(computedPosition); }
    public Vector2f getComputedSize() { return new Vector2f(computedSize); }
    
    public UINode getParent() { return parent; }
    public List<UINode> getChildren() { return new ArrayList<>(children); }
}