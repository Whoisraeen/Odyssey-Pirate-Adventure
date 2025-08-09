package com.odyssey.ui;

import org.joml.Vector4f;

/**
 * A simple text node for rendering text in the UI system.
 * Integrates with the existing UIRenderer and FontRenderer.
 */
public class TextNode {
    private String text;
    private float x;
    private float y;
    private float size;
    private Vector4f color;
    private boolean visible;
    
    /**
     * Creates a new TextNode with default white color.
     */
    public TextNode(String text, float x, float y, float size) {
        this(text, x, y, size, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
    }
    
    /**
     * Creates a new TextNode with specified color.
     */
    public TextNode(String text, float x, float y, float size, Vector4f color) {
        this.text = text != null ? text : "";
        this.x = x;
        this.y = y;
        this.size = size;
        this.color = new Vector4f(color);
        this.visible = true;
    }
    
    /**
     * Renders this text node using the provided UIRenderer.
     */
    public void render(UIRenderer renderer) {
        if (visible && text != null && !text.isEmpty()) {
            renderer.drawText(text, x, y, size, color);
        }
    }
    
    /**
     * Gets the text content.
     */
    public String getText() {
        return text;
    }
    
    /**
     * Sets the text content.
     */
    public void setText(String text) {
        this.text = text != null ? text : "";
    }
    
    /**
     * Gets the X position.
     */
    public float getX() {
        return x;
    }
    
    /**
     * Sets the X position.
     */
    public void setX(float x) {
        this.x = x;
    }
    
    /**
     * Gets the Y position.
     */
    public float getY() {
        return y;
    }
    
    /**
     * Sets the Y position.
     */
    public void setY(float y) {
        this.y = y;
    }
    
    /**
     * Sets both X and Y position.
     */
    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }
    
    /**
     * Gets the font size.
     */
    public float getSize() {
        return size;
    }
    
    /**
     * Sets the font size.
     */
    public void setSize(float size) {
        this.size = size;
    }
    
    /**
     * Gets the text color.
     */
    public Vector4f getColor() {
        return new Vector4f(color);
    }
    
    /**
     * Sets the text color.
     */
    public void setColor(Vector4f color) {
        this.color.set(color);
    }
    
    /**
     * Sets the text color using RGBA values.
     */
    public void setColor(float r, float g, float b, float a) {
        this.color.set(r, g, b, a);
    }
    
    /**
     * Gets the visibility state.
     */
    public boolean isVisible() {
        return visible;
    }
    
    /**
     * Sets the visibility state.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    /**
     * Calculates the width of this text node using the provided UIRenderer.
     */
    public float getTextWidth(UIRenderer renderer) {
        if (text == null || text.isEmpty()) {
            return 0.0f;
        }
        return renderer.getTextWidth(text, size);
    }
    
    /**
     * Calculates the height of this text node using the provided UIRenderer.
     */
    public float getTextHeight(UIRenderer renderer) {
        return renderer.getLineHeight(size);
    }
}