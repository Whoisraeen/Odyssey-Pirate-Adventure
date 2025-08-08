package com.odyssey.ui.widgets;

import com.odyssey.ui.*;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A slider widget for selecting numeric values.
 */
public class Slider extends UINode {
    private static final Logger logger = LoggerFactory.getLogger(Slider.class);
    
    // Value properties
    private float value = 0.0f;
    private float minValue = 0.0f;
    private float maxValue = 1.0f;
    private float step = 0.01f;
    
    // Visual properties
    private final Vector4f trackColor = new Vector4f(0.2f, 0.2f, 0.2f, 1.0f);
    private final Vector4f fillColor = new Vector4f(0.4f, 0.6f, 1.0f, 1.0f);
    private final Vector4f thumbColor = new Vector4f(0.8f, 0.8f, 0.8f, 1.0f);
    private final Vector4f thumbHoverColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private final Vector4f thumbPressedColor = new Vector4f(0.6f, 0.6f, 0.6f, 1.0f);
    
    // Layout properties
    private float trackHeight = 4.0f;
    private float thumbSize = 16.0f;
    
    // State
    private boolean dragging = false;
    private boolean thumbHovered = false;
    private Vector2f dragOffset = new Vector2f();
    
    // Event handler
    private ValueChangeListener valueChangeListener;
    
    /**
     * Interface for value change events.
     */
    @FunctionalInterface
    public interface ValueChangeListener {
        void onValueChanged(float oldValue, float newValue);
    }
    
    /**
     * Creates a new slider.
     */
    public Slider() {
        super();
        setSize(200.0f, 20.0f);
        setInteractive(true);
    }
    
    /**
     * Creates a new slider with range.
     */
    public Slider(float minValue, float maxValue) {
        this();
        setRange(minValue, maxValue);
    }
    
    /**
     * Creates a new slider with range and initial value.
     */
    public Slider(float minValue, float maxValue, float value) {
        this(minValue, maxValue);
        setValue(value);
        setFocusable(true); // Sliders are focusable
    }
    
    @Override
    protected void onUpdate(double deltaTime) {
        // No special update logic needed
    }
    
    @Override
    protected void onRender(UIRenderer renderer) {
        Vector2f pos = getComputedPosition();
        Vector2f size = getComputedSize();
        
        // Calculate track bounds
        float trackY = pos.y + (size.y - trackHeight) * 0.5f;
        float trackX = pos.x + thumbSize * 0.5f;
        float trackWidth = size.x - thumbSize;
        
        // Draw track background
        renderer.drawRectangle(trackX, trackY, trackWidth, trackHeight, trackColor);
        
        // Draw filled portion
        float normalizedValue = (value - minValue) / (maxValue - minValue);
        float fillWidth = trackWidth * normalizedValue;
        if (fillWidth > 0) {
            renderer.drawRectangle(trackX, trackY, fillWidth, trackHeight, fillColor);
        }
        
        // Calculate thumb position
        float thumbX = trackX + (trackWidth * normalizedValue) - (thumbSize * 0.5f);
        float thumbY = pos.y + (size.y - thumbSize) * 0.5f;
        
        // Draw thumb
        Vector4f currentThumbColor = thumbColor;
        if (dragging) {
            currentThumbColor = thumbPressedColor;
        } else if (thumbHovered) {
            currentThumbColor = thumbHoverColor;
        }
        
        renderer.drawRectangle(thumbX, thumbY, thumbSize, thumbSize, currentThumbColor);
        
        // Draw thumb border
        Vector4f borderColor = new Vector4f(0.6f, 0.6f, 0.6f, 1.0f);
        float borderWidth = 1.0f;
        
        // Top border
        renderer.drawRectangle(thumbX, thumbY, thumbSize, borderWidth, borderColor);
        // Bottom border
        renderer.drawRectangle(thumbX, thumbY + thumbSize - borderWidth, thumbSize, borderWidth, borderColor);
        // Left border
        renderer.drawRectangle(thumbX, thumbY, borderWidth, thumbSize, borderColor);
        // Right border
        renderer.drawRectangle(thumbX + thumbSize - borderWidth, thumbY, borderWidth, thumbSize, borderColor);
    }
    
    @Override
    protected boolean onInput(UIInputEvent event) {
        Vector2f pos = getComputedPosition();
        Vector2f size = getComputedSize();
        Vector2f mousePos = event.getPosition();
        
        // Calculate track and thumb bounds
        float trackY = pos.y + (size.y - trackHeight) * 0.5f;
        float trackX = pos.x + thumbSize * 0.5f;
        float trackWidth = size.x - thumbSize;
        
        float normalizedValue = (value - minValue) / (maxValue - minValue);
        float thumbX = trackX + (trackWidth * normalizedValue) - (thumbSize * 0.5f);
        float thumbY = pos.y + (size.y - thumbSize) * 0.5f;
        
        switch (event.getType()) {
            case MOUSE_MOVE:
                // Check if mouse is over thumb
                boolean wasThumbHovered = thumbHovered;
                thumbHovered = mousePos.x >= thumbX && mousePos.x <= thumbX + thumbSize &&
                              mousePos.y >= thumbY && mousePos.y <= thumbY + thumbSize;
                
                if (thumbHovered != wasThumbHovered) {
                    if (thumbHovered) {
                        fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_ENTER, 
                            event.getPosition(), event.getButton(), 
                            event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    } else {
                        fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_EXIT, 
                            event.getPosition(), event.getButton(), 
                            event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    }
                }
                
                // Handle dragging
                if (dragging) {
                    updateValueFromMouse(mousePos.x - dragOffset.x, trackX, trackWidth);
                    return true;
                }
                
                return thumbHovered;
                
            case MOUSE_PRESS:
                if (event.getButton() == GLFW_MOUSE_BUTTON_LEFT) {
                    if (thumbHovered) {
                        // Start dragging thumb
                        dragging = true;
                        dragOffset.x = mousePos.x - (thumbX + thumbSize * 0.5f);
                        dragOffset.y = mousePos.y - (thumbY + thumbSize * 0.5f);
                        
                        fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_PRESS, 
                            event.getPosition(), event.getButton(), 
                            event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                        
                        logger.debug("Slider thumb drag started, value: {}", value);
                        return true;
                    } else if (mousePos.x >= trackX && mousePos.x <= trackX + trackWidth &&
                              mousePos.y >= trackY && mousePos.y <= trackY + trackHeight) {
                        // Click on track - jump to position
                        updateValueFromMouse(mousePos.x, trackX, trackWidth);
                        logger.debug("Slider track clicked, value: {}", value);
                        return true;
                    }
                }
                break;
                
            case MOUSE_RELEASE:
                if (event.getButton() == GLFW_MOUSE_BUTTON_LEFT && dragging) {
                    dragging = false;
                    
                    fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_RELEASE, 
                        event.getPosition(), event.getButton(), 
                        event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    
                    logger.debug("Slider thumb drag ended, final value: {}", value);
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    @Override
    protected void onLayout() {
        // Ensure minimum size for proper thumb display
        if (size.x < thumbSize * 2) {
            size.x = thumbSize * 2;
        }
        if (size.y < thumbSize) {
            size.y = thumbSize;
        }
    }
    
    /**
     * Updates the slider value based on mouse position.
     */
    private void updateValueFromMouse(float mouseX, float trackX, float trackWidth) {
        float normalizedPos = (mouseX - trackX) / trackWidth;
        normalizedPos = Math.max(0.0f, Math.min(1.0f, normalizedPos));
        
        float newValue = minValue + (normalizedPos * (maxValue - minValue));
        
        // Apply step
        if (step > 0) {
            newValue = Math.round(newValue / step) * step;
        }
        
        setValue(newValue);
    }
    
    /**
     * Called when mouse exits the slider area.
     */
    public void onMouseExit() {
        if (thumbHovered && !dragging) {
            thumbHovered = false;
            fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_EXIT, 
                new Vector2f(), -1, false, false, false));
        }
    }
    
    // Getters and setters
    public float getValue() { return value; }
    public void setValue(float value) {
        float oldValue = this.value;
        this.value = Math.max(minValue, Math.min(maxValue, value));
        
        if (this.value != oldValue) {
            fireEvent(new UIEvent.ValueChangeEvent(oldValue, this.value));
            if (valueChangeListener != null) {
                valueChangeListener.onValueChanged(oldValue, this.value);
            }
        }
    }
    
    public float getMinValue() { return minValue; }
    public float getMaxValue() { return maxValue; }
    public void setRange(float minValue, float maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
        setValue(value); // Clamp current value to new range
    }
    
    public float getStep() { return step; }
    public void setStep(float step) { this.step = Math.max(0.0f, step); }
    
    public Vector4f getTrackColor() { return new Vector4f(trackColor); }
    public void setTrackColor(float r, float g, float b, float a) { 
        trackColor.set(r, g, b, a); 
    }
    
    public Vector4f getFillColor() { return new Vector4f(fillColor); }
    public void setFillColor(float r, float g, float b, float a) { 
        fillColor.set(r, g, b, a); 
    }
    
    public Vector4f getThumbColor() { return new Vector4f(thumbColor); }
    public void setThumbColor(float r, float g, float b, float a) { 
        thumbColor.set(r, g, b, a); 
    }
    
    public float getTrackHeight() { return trackHeight; }
    public void setTrackHeight(float trackHeight) { 
        this.trackHeight = Math.max(1.0f, trackHeight); 
        markNeedsLayout();
    }
    
    public float getThumbSize() { return thumbSize; }
    public void setThumbSize(float thumbSize) { 
        this.thumbSize = Math.max(4.0f, thumbSize); 
        markNeedsLayout();
    }
    
    public ValueChangeListener getValueChangeListener() { return valueChangeListener; }
    public void setValueChangeListener(ValueChangeListener listener) { 
        this.valueChangeListener = listener; 
    }
}