package com.odyssey.ui.widgets;

import com.odyssey.ui.*;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A clickable button widget.
 */
public class Button extends UINode {
    private static final Logger logger = LoggerFactory.getLogger(Button.class);
    
    // Visual states
    private final Vector4f normalColor = new Vector4f(0.3f, 0.3f, 0.3f, 1.0f);
    private final Vector4f hoverColor = new Vector4f(0.4f, 0.4f, 0.4f, 1.0f);
    private final Vector4f pressedColor = new Vector4f(0.2f, 0.2f, 0.2f, 1.0f);
    private final Vector4f disabledColor = new Vector4f(0.15f, 0.15f, 0.15f, 0.5f);
    
    // Text properties
    private String text = "";
    private final Vector4f textColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    private final Vector4f disabledTextColor = new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
    private float fontSize = 16.0f;
    
    // State
    private boolean enabled = true;
    private Runnable clickHandler;
    
    /**
     * Creates a new button.
     */
    public Button() {
        super();
        setSize(100.0f, 30.0f);
        setInteractive(true);
    }
    
    /**
     * Creates a new button with text.
     */
    public Button(String text) {
        this();
        setText(text);
        this.focusable = true; // Buttons are focusable
    }
    
    /**
     * Creates a new button with text and click handler.
     */
    public Button(String text, Runnable clickHandler) {
        this(text);
        setClickHandler(clickHandler);
    }
    
    @Override
    protected void onUpdate(double deltaTime) {
        // Update visual state based on interaction
        if (!enabled) {
            setBackgroundColor(disabledColor.x, disabledColor.y, disabledColor.z, disabledColor.w);
        } else if (pressed) {
            setBackgroundColor(pressedColor.x, pressedColor.y, pressedColor.z, pressedColor.w);
        } else if (hovered) {
            setBackgroundColor(hoverColor.x, hoverColor.y, hoverColor.z, hoverColor.w);
        } else {
            setBackgroundColor(normalColor.x, normalColor.y, normalColor.z, normalColor.w);
        }
    }
    
    @Override
    protected void onRender(UIRenderer renderer) {
        Vector2f pos = getComputedPosition();
        Vector2f size = getComputedSize();
        Vector4f bgColor = getBackgroundColor();
        
        // Draw button background
        renderer.drawRectangle(pos.x, pos.y, size.x, size.y, bgColor);
        
        // Draw button border
        Vector4f borderColor = new Vector4f(0.6f, 0.6f, 0.6f, 1.0f);
        float borderWidth = 1.0f;
        
        // Top border
        renderer.drawRectangle(pos.x, pos.y, size.x, borderWidth, borderColor);
        // Bottom border
        renderer.drawRectangle(pos.x, pos.y + size.y - borderWidth, size.x, borderWidth, borderColor);
        // Left border
        renderer.drawRectangle(pos.x, pos.y, borderWidth, size.y, borderColor);
        // Right border
        renderer.drawRectangle(pos.x + size.x - borderWidth, pos.y, borderWidth, size.y, borderColor);
        
        // Draw text
        if (!text.isEmpty()) {
            Vector4f currentTextColor = enabled ? textColor : disabledTextColor;
            
            // Calculate text position (centered)
            float textWidth = text.length() * fontSize * 0.6f;
            float textHeight = fontSize;
            float textX = pos.x + (size.x - textWidth) * 0.5f;
            float textY = pos.y + (size.y - textHeight) * 0.5f;
            
            renderer.drawText(text, textX, textY, fontSize, currentTextColor);
        }
    }
    
    @Override
    protected boolean onInput(UIInputEvent event) {
        if (!enabled) return false;
        
        switch (event.getType()) {
            case MOUSE_MOVE:
                if (!hovered) {
                    hovered = true;
                    fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_ENTER, 
                        event.getPosition(), event.getButton(), 
                        event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    logger.debug("Button '{}' mouse enter", text);
                }
                return true;
                
            case MOUSE_PRESS:
                if (event.getButton() == GLFW_MOUSE_BUTTON_LEFT) {
                    pressed = true;
                    fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_PRESS, 
                        event.getPosition(), event.getButton(), 
                        event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    logger.debug("Button '{}' mouse press", text);
                    return true;
                }
                break;
                
            case MOUSE_RELEASE:
                if (event.getButton() == GLFW_MOUSE_BUTTON_LEFT && pressed) {
                    pressed = false;
                    
                    // Fire click event
                    fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_CLICK, 
                        event.getPosition(), event.getButton(), 
                        event.isShiftDown(), event.isCtrlDown(), event.isAltDown()));
                    
                    // Execute click handler
                    if (clickHandler != null) {
                        clickHandler.run();
                    }
                    
                    logger.debug("Button '{}' clicked", text);
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    @Override
    protected void onLayout() {
        // Auto-size based on text if size is not explicitly set
        if (text != null && !text.isEmpty()) {
            float textWidth = text.length() * fontSize * 0.6f;
            float textHeight = fontSize;
            
            // Add padding
            float padding = 16.0f;
            float minWidth = textWidth + padding;
            float minHeight = textHeight + padding;
            
            if (size.x < minWidth) {
                size.x = minWidth;
            }
            if (size.y < minHeight) {
                size.y = minHeight;
            }
        }
    }
    
    /**
     * Called when mouse exits the button area.
     */
    public void onMouseExit() {
        if (hovered) {
            hovered = false;
            pressed = false;
            fireEvent(new UIEvent.MouseEvent(UIEvent.UIEventType.MOUSE_EXIT, 
                new Vector2f(), -1, false, false, false));
            logger.debug("Button '{}' mouse exit", text);
        }
    }
    
    // Getters and setters
    public String getText() { return text; }
    public void setText(String text) { 
        this.text = text != null ? text : ""; 
        markNeedsLayout();
    }
    
    public Vector4f getTextColor() { return new Vector4f(textColor); }
    public void setTextColor(float r, float g, float b, float a) { 
        textColor.set(r, g, b, a); 
    }
    
    public float getFontSize() { return fontSize; }
    public void setFontSize(float fontSize) { 
        this.fontSize = fontSize; 
        markNeedsLayout();
    }
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { 
        this.enabled = enabled; 
        setInteractive(enabled);
        if (!enabled) {
            hovered = false;
            pressed = false;
        }
    }
    
    public Runnable getClickHandler() { return clickHandler; }
    public void setClickHandler(Runnable clickHandler) { this.clickHandler = clickHandler; }
    
    public Vector4f getNormalColor() { return new Vector4f(normalColor); }
    public void setNormalColor(float r, float g, float b, float a) { 
        normalColor.set(r, g, b, a); 
    }
    
    public Vector4f getHoverColor() { return new Vector4f(hoverColor); }
    public void setHoverColor(float r, float g, float b, float a) { 
        hoverColor.set(r, g, b, a); 
    }
    
    public Vector4f getPressedColor() { return new Vector4f(pressedColor); }
    public void setPressedColor(float r, float g, float b, float a) { 
        pressedColor.set(r, g, b, a); 
    }
}