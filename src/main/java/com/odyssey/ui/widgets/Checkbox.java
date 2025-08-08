package com.odyssey.ui.widgets;

import com.odyssey.ui.*;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A checkbox widget for boolean input.
 */
public class Checkbox extends UINode {
    private static final Logger logger = LoggerFactory.getLogger(Checkbox.class);
    
    // State
    private boolean checked = false;
    private boolean hovered = false;
    private boolean pressed = false;
    
    // Visual properties
    private final Vector4f backgroundColor = new Vector4f(0.9f, 0.9f, 0.9f, 1.0f);
    private final Vector4f hoverColor = new Vector4f(0.95f, 0.95f, 0.95f, 1.0f);
    private final Vector4f pressedColor = new Vector4f(0.8f, 0.8f, 0.8f, 1.0f);
    private final Vector4f borderColor = new Vector4f(0.6f, 0.6f, 0.6f, 1.0f);
    private final Vector4f checkColor = new Vector4f(0.2f, 0.6f, 0.2f, 1.0f);
    private final Vector4f textColor = new Vector4f(0.0f, 0.0f, 0.0f, 1.0f);
    
    // Layout properties
    private float checkboxSize = 16.0f;
    private float textSpacing = 8.0f;
    private String text = "";
    
    // Event handler
    private CheckChangeListener checkChangeListener;
    
    /**
     * Interface for check change events.
     */
    @FunctionalInterface
    public interface CheckChangeListener {
        void onCheckChanged(boolean checked);
    }
    
    /**
     * Creates a new checkbox.
     */
    public Checkbox() {
        super();
        setSize(100.0f, 20.0f);
        setInteractive(true);
    }
    
    /**
     * Creates a new checkbox with text.
     */
    public Checkbox(String text) {
        this();
        setText(text);
        setFocusable(true); // Checkboxes are focusable
    }
    
    /**
     * Creates a new checkbox with text and initial state.
     */
    public Checkbox(String text, boolean checked) {
        this(text);
        setChecked(checked);
    }
    
    @Override
    protected void onUpdate(double deltaTime) {
        // No special update logic needed
    }
    
    @Override
    protected void onRender(UIRenderer renderer) {
        Vector2f pos = getComputedPosition();
        Vector2f size = getComputedSize();
        
        // Calculate checkbox bounds
        float checkboxY = pos.y + (size.y - checkboxSize) * 0.5f;
        
        // Determine background color based on state
        Vector4f bgColor = backgroundColor;
        if (pressed) {
            bgColor = pressedColor;
        } else if (hovered) {
            bgColor = hoverColor;
        }
        
        // Draw checkbox background
        renderer.drawRectangle(pos.x, checkboxY, checkboxSize, checkboxSize, bgColor);
        
        // Draw checkbox border
        float borderWidth = 1.0f;
        
        // Top border
        renderer.drawRectangle(pos.x, checkboxY, checkboxSize, borderWidth, borderColor);
        // Bottom border
        renderer.drawRectangle(pos.x, checkboxY + checkboxSize - borderWidth, checkboxSize, borderWidth, borderColor);
        // Left border
        renderer.drawRectangle(pos.x, checkboxY, borderWidth, checkboxSize, borderColor);
        // Right border
        renderer.drawRectangle(pos.x + checkboxSize - borderWidth, checkboxY, borderWidth, checkboxSize, borderColor);
        
        // Draw check mark if checked
        if (checked) {
            float checkPadding = 3.0f;
            float checkX = pos.x + checkPadding;
            float checkY = checkboxY + checkPadding;
            float checkSize = checkboxSize - (checkPadding * 2);
            
            // Draw simple check mark as filled rectangle (could be improved with actual check mark shape)
            renderer.drawRectangle(checkX, checkY, checkSize, checkSize, checkColor);
        }
        
        // Draw text if present
        if (!text.isEmpty()) {
            float textX = pos.x + checkboxSize + textSpacing;
            float textY = pos.y + (size.y * 0.5f);
            
            // Note: This is a placeholder for text rendering
            // In a real implementation, you'd use a proper text renderer
            float fontSize = 12.0f; // Default font size
            renderer.drawText(text, textX, textY, fontSize, textColor);
        }
    }
    
    @Override
    protected boolean onInput(UIInputEvent event) {
        Vector2f pos = getComputedPosition();
        Vector2f size = getComputedSize();
        Vector2f mousePos = event.getPosition();
        
        // Check if mouse is within bounds
        boolean inBounds = mousePos.x >= pos.x && mousePos.x <= pos.x + size.x &&
                          mousePos.y >= pos.y && mousePos.y <= pos.y + size.y;
        
        switch (event.getType()) {
            case MOUSE_MOVE:
                boolean wasHovered = hovered;
                hovered = inBounds;
                
                if (hovered != wasHovered) {
                    if (hovered) {
                        fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_ENTER, 
                            event.getPosition(), event.getButton(), 
                            event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    } else {
                        fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_EXIT, 
                            event.getPosition(), event.getButton(), 
                            event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    }
                }
                
                return hovered;
                
            case MOUSE_PRESS:
                if (event.getButton() == GLFW_MOUSE_BUTTON_LEFT && inBounds) {
                    pressed = true;
                    
                    fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_PRESS, 
                        event.getPosition(), event.getButton(), 
                        event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    
                    logger.debug("Checkbox pressed: {}", text);
                    return true;
                }
                break;
                
            case MOUSE_RELEASE:
                if (event.getButton() == GLFW_MOUSE_BUTTON_LEFT && pressed) {
                    pressed = false;
                    
                    if (inBounds) {
                        // Toggle checked state
                        setChecked(!checked);
                        
                        fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_CLICK, 
                            event.getPosition(), event.getButton(), 
                            event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                        
                        logger.debug("Checkbox clicked: {} -> {}", text, checked);
                    }
                    
                    fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_RELEASE, 
                        event.getPosition(), event.getButton(), 
                        event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    
                    return true;
                }
                break;
                
            case KEY_PRESS:
                if (isFocused() && event.getKey() == GLFW_KEY_SPACE) {
                    setChecked(!checked);
                    
                    fireEvent(new UIEvent.KeyEvent(UIEvent.UIEventType.KEY_PRESS, 
                        event.getKey(), event.getScancode(), event.getMods()));
                    
                    logger.debug("Checkbox toggled via keyboard: {} -> {}", text, checked);
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    @Override
    protected void onLayout() {
        // Calculate minimum width based on checkbox size and text
        float minWidth = checkboxSize;
        if (!text.isEmpty()) {
            // This is a placeholder calculation - in a real implementation,
            // you'd measure the actual text width
            minWidth += textSpacing + (text.length() * 8); // Rough estimate
        }
        
        if (size.x < minWidth) {
            size.x = minWidth;
        }
        
        // Ensure minimum height for checkbox
        if (size.y < checkboxSize) {
            size.y = checkboxSize;
        }
    }
    
    /**
     * Called when mouse exits the checkbox area.
     */
    public void onMouseExit() {
        if (hovered) {
            hovered = false;
            fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_EXIT, 
                new Vector2f(), -1, false, false, false));
        }
    }
    
    // Getters and setters
    public boolean isChecked() { return checked; }
    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            this.checked = checked;
            
            fireEvent(new UIEvent.ValueChangeEvent(checked ? 0.0f : 1.0f, checked ? 1.0f : 0.0f));
            
            if (checkChangeListener != null) {
                checkChangeListener.onCheckChanged(checked);
            }
        }
    }
    
    public String getText() { return text; }
    public void setText(String text) { 
        this.text = text != null ? text : "";
        markNeedsLayout();
    }
    
    public float getCheckboxSize() { return checkboxSize; }
    public void setCheckboxSize(float checkboxSize) { 
        this.checkboxSize = Math.max(8.0f, checkboxSize); 
        markNeedsLayout();
    }
    
    public float getTextSpacing() { return textSpacing; }
    public void setTextSpacing(float textSpacing) { 
        this.textSpacing = Math.max(0.0f, textSpacing); 
        markNeedsLayout();
    }
    
    public Vector4f getBackgroundColor() { return new Vector4f(backgroundColor); }
    public void setBackgroundColor(float r, float g, float b, float a) { 
        backgroundColor.set(r, g, b, a); 
    }
    
    public Vector4f getHoverColor() { return new Vector4f(hoverColor); }
    public void setHoverColor(float r, float g, float b, float a) { 
        hoverColor.set(r, g, b, a); 
    }
    
    public Vector4f getPressedColor() { return new Vector4f(pressedColor); }
    public void setPressedColor(float r, float g, float b, float a) { 
        pressedColor.set(r, g, b, a); 
    }
    
    public Vector4f getBorderColor() { return new Vector4f(borderColor); }
    public void setBorderColor(float r, float g, float b, float a) { 
        borderColor.set(r, g, b, a); 
    }
    
    public Vector4f getCheckColor() { return new Vector4f(checkColor); }
    public void setCheckColor(float r, float g, float b, float a) { 
        checkColor.set(r, g, b, a); 
    }
    
    public Vector4f getTextColor() { return new Vector4f(textColor); }
    public void setTextColor(float r, float g, float b, float a) { 
        textColor.set(r, g, b, a); 
    }
    
    public CheckChangeListener getCheckChangeListener() { return checkChangeListener; }
    public void setCheckChangeListener(CheckChangeListener listener) { 
        this.checkChangeListener = listener; 
    }
}