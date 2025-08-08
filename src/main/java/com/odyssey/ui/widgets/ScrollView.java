package com.odyssey.ui.widgets;

import com.odyssey.ui.*;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.lwjgl.glfw.GLFW.*;

/**
 * A scrollable container widget that can display content larger than its visible area.
 */
public class ScrollView extends UINode {
    private static final Logger logger = LoggerFactory.getLogger(ScrollView.class);
    
    // Content properties
    private UINode contentNode;
    private final Vector2f scrollOffset = new Vector2f(0.0f, 0.0f);
    private final Vector2f contentSize = new Vector2f(0.0f, 0.0f);
    
    // Scrollbar properties
    private boolean showHorizontalScrollbar = true;
    private boolean showVerticalScrollbar = true;
    private float scrollbarWidth = 16.0f;
    private float scrollbarMinThumbSize = 20.0f;
    
    // Visual properties
    private final Vector4f backgroundColor = new Vector4f(0.95f, 0.95f, 0.95f, 1.0f);
    private final Vector4f scrollbarTrackColor = new Vector4f(0.9f, 0.9f, 0.9f, 1.0f);
    private final Vector4f scrollbarThumbColor = new Vector4f(0.7f, 0.7f, 0.7f, 1.0f);
    private final Vector4f scrollbarThumbHoverColor = new Vector4f(0.6f, 0.6f, 0.6f, 1.0f);
    private final Vector4f scrollbarThumbPressedColor = new Vector4f(0.5f, 0.5f, 0.5f, 1.0f);
    
    // Scrolling behavior
    private float scrollSpeed = 20.0f;
    private boolean smoothScrolling = true;
    private final Vector2f targetScrollOffset = new Vector2f(0.0f, 0.0f);
    private float scrollLerpSpeed = 10.0f;
    
    // State
    private boolean verticalScrollbarHovered = false;
    private boolean horizontalScrollbarHovered = false;
    private boolean verticalScrollbarPressed = false;
    private boolean horizontalScrollbarPressed = false;
    private boolean draggingVerticalThumb = false;
    private boolean draggingHorizontalThumb = false;
    private final Vector2f dragOffset = new Vector2f();
    
    /**
     * Creates a new scroll view.
     */
    public ScrollView() {
        super();
        setSize(300.0f, 200.0f);
        setInteractive(true);
        setClipChildren(true);
        
        // Create default content node
        contentNode = new ContentContainer();
        addChild(contentNode);
    }
    
    /**
     * Creates a new scroll view with content.
     */
    public ScrollView(UINode content) {
        this();
        setContent(content);
    }
    
    @Override
    protected void onUpdate(double deltaTime) {
        // Update content size
        updateContentSize();
        
        // Smooth scrolling
        if (smoothScrolling) {
            float lerpFactor = (float)(scrollLerpSpeed * deltaTime);
            scrollOffset.lerp(targetScrollOffset, lerpFactor);
        } else {
            scrollOffset.set(targetScrollOffset);
        }
        
        // Clamp scroll offset
        clampScrollOffset();
        
        // Update content position
        if (contentNode != null) {
            Vector2f contentPos = getContentPosition();
            contentNode.setPosition(contentPos.x, contentPos.y);
        }
    }
    
    @Override
    protected void onRender(UIRenderer renderer) {
        Vector2f pos = getComputedPosition();
        Vector2f size = getComputedSize();
        
        // Draw background
        renderer.drawRectangle(pos.x, pos.y, size.x, size.y, backgroundColor);
        
        // Calculate viewport size (excluding scrollbars)
        Vector2f viewportSize = getViewportSize();
        
        // Draw scrollbars
        if (needsVerticalScrollbar()) {
            drawVerticalScrollbar(renderer, pos, size, viewportSize);
        }
        
        if (needsHorizontalScrollbar()) {
            drawHorizontalScrollbar(renderer, pos, size, viewportSize);
        }
    }
    
    @Override
    protected boolean onInput(UIInputEvent event) {
        Vector2f pos = getComputedPosition();
        Vector2f size = getComputedSize();
        Vector2f mousePos = event.getPosition();
        Vector2f viewportSize = getViewportSize();
        
        // Check if mouse is within bounds
        boolean inBounds = mousePos.x >= pos.x && mousePos.x <= pos.x + size.x &&
                          mousePos.y >= pos.y && mousePos.y <= pos.y + size.y;
        
        switch (event.getType()) {
            case MOUSE_MOVE:
                if (inBounds) {
                    updateScrollbarHoverState(mousePos, pos, size, viewportSize);
                    
                    // Handle scrollbar dragging
                    if (draggingVerticalThumb) {
                        handleVerticalScrollbarDrag(mousePos, pos, size, viewportSize);
                        return true;
                    }
                    
                    if (draggingHorizontalThumb) {
                        handleHorizontalScrollbarDrag(mousePos, pos, size, viewportSize);
                        return true;
                    }
                }
                break;
                
            case MOUSE_PRESS:
                if (event.getButton() == GLFW_MOUSE_BUTTON_LEFT && inBounds) {
                    // Check scrollbar clicks
                    if (handleScrollbarPress(mousePos, pos, size, viewportSize)) {
                        return true;
                    }
                }
                break;
                
            case MOUSE_RELEASE:
                if (event.getButton() == GLFW_MOUSE_BUTTON_LEFT) {
                    if (draggingVerticalThumb || draggingHorizontalThumb) {
                        draggingVerticalThumb = false;
                        draggingHorizontalThumb = false;
                        verticalScrollbarPressed = false;
                        horizontalScrollbarPressed = false;
                        return true;
                    }
                }
                break;
                
            case MOUSE_SCROLL:
                if (inBounds) {
                    handleMouseScroll(event.getScrollX(), event.getScrollY());
                    return true;
                }
                break;
        }
        
        return false;
    }
    
    @Override
    protected void onLayout() {
        updateContentSize();
        clampScrollOffset();
        
        if (contentNode != null) {
            Vector2f contentPos = getContentPosition();
            contentNode.setPosition(contentPos.x, contentPos.y);
        }
    }
    
    /**
     * Updates the content size based on children.
     */
    private void updateContentSize() {
        if (contentNode == null || contentNode.getChildren().isEmpty()) {
            contentSize.set(0.0f, 0.0f);
            return;
        }
        
        float maxX = 0.0f;
        float maxY = 0.0f;
        
        for (UINode child : contentNode.getChildren()) {
            Vector2f childPos = child.getPosition();
            Vector2f childSize = child.getSize();
            
            maxX = Math.max(maxX, childPos.x + childSize.x);
            maxY = Math.max(maxY, childPos.y + childSize.y);
        }
        
        contentSize.set(maxX, maxY);
    }
    
    /**
     * Gets the viewport size (excluding scrollbars).
     */
    private Vector2f getViewportSize() {
        Vector2f size = getComputedSize();
        float viewportWidth = size.x;
        float viewportHeight = size.y;
        
        if (needsVerticalScrollbar()) {
            viewportWidth -= scrollbarWidth;
        }
        
        if (needsHorizontalScrollbar()) {
            viewportHeight -= scrollbarWidth;
        }
        
        return new Vector2f(viewportWidth, viewportHeight);
    }
    
    /**
     * Gets the content position based on scroll offset.
     */
    private Vector2f getContentPosition() {
        Vector2f pos = getComputedPosition();
        return new Vector2f(pos.x - scrollOffset.x, pos.y - scrollOffset.y);
    }
    
    /**
     * Checks if vertical scrollbar is needed.
     */
    private boolean needsVerticalScrollbar() {
        if (!showVerticalScrollbar) return false;
        Vector2f viewportSize = new Vector2f(getComputedSize());
        if (needsHorizontalScrollbar()) {
            viewportSize.y -= scrollbarWidth;
        }
        return contentSize.y > viewportSize.y;
    }
    
    /**
     * Checks if horizontal scrollbar is needed.
     */
    private boolean needsHorizontalScrollbar() {
        if (!showHorizontalScrollbar) return false;
        Vector2f viewportSize = new Vector2f(getComputedSize());
        if (needsVerticalScrollbar()) {
            viewportSize.x -= scrollbarWidth;
        }
        return contentSize.x > viewportSize.x;
    }
    
    /**
     * Clamps scroll offset to valid range.
     */
    private void clampScrollOffset() {
        Vector2f viewportSize = getViewportSize();
        
        float maxScrollX = Math.max(0.0f, contentSize.x - viewportSize.x);
        float maxScrollY = Math.max(0.0f, contentSize.y - viewportSize.y);
        
        scrollOffset.x = Math.max(0.0f, Math.min(maxScrollX, scrollOffset.x));
        scrollOffset.y = Math.max(0.0f, Math.min(maxScrollY, scrollOffset.y));
        
        targetScrollOffset.x = Math.max(0.0f, Math.min(maxScrollX, targetScrollOffset.x));
        targetScrollOffset.y = Math.max(0.0f, Math.min(maxScrollY, targetScrollOffset.y));
    }
    
    /**
     * Draws the vertical scrollbar.
     */
    private void drawVerticalScrollbar(UIRenderer renderer, Vector2f pos, Vector2f size, Vector2f viewportSize) {
        float scrollbarX = pos.x + viewportSize.x;
        float scrollbarY = pos.y;
        float scrollbarHeight = viewportSize.y;
        
        // Draw track
        renderer.drawRectangle(scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight, scrollbarTrackColor);
        
        // Calculate thumb properties
        float contentHeight = contentSize.y;
        float thumbHeight = Math.max(scrollbarMinThumbSize, 
            (viewportSize.y / contentHeight) * scrollbarHeight);
        
        float maxScrollY = contentHeight - viewportSize.y;
        float thumbY = scrollbarY + (scrollOffset.y / maxScrollY) * (scrollbarHeight - thumbHeight);
        
        // Draw thumb
        Vector4f thumbColor = scrollbarThumbColor;
        if (verticalScrollbarPressed) {
            thumbColor = scrollbarThumbPressedColor;
        } else if (verticalScrollbarHovered) {
            thumbColor = scrollbarThumbHoverColor;
        }
        
        renderer.drawRectangle(scrollbarX, thumbY, scrollbarWidth, thumbHeight, thumbColor);
    }
    
    /**
     * Draws the horizontal scrollbar.
     */
    private void drawHorizontalScrollbar(UIRenderer renderer, Vector2f pos, Vector2f size, Vector2f viewportSize) {
        float scrollbarX = pos.x;
        float scrollbarY = pos.y + viewportSize.y;
        float scrollbarWidth = viewportSize.x;
        
        // Draw track
        renderer.drawRectangle(scrollbarX, scrollbarY, scrollbarWidth, this.scrollbarWidth, scrollbarTrackColor);
        
        // Calculate thumb properties
        float contentWidth = contentSize.x;
        float thumbWidth = Math.max(scrollbarMinThumbSize, 
            (viewportSize.x / contentWidth) * scrollbarWidth);
        
        float maxScrollX = contentWidth - viewportSize.x;
        float thumbX = scrollbarX + (scrollOffset.x / maxScrollX) * (scrollbarWidth - thumbWidth);
        
        // Draw thumb
        Vector4f thumbColor = scrollbarThumbColor;
        if (horizontalScrollbarPressed) {
            thumbColor = scrollbarThumbPressedColor;
        } else if (horizontalScrollbarHovered) {
            thumbColor = scrollbarThumbHoverColor;
        }
        
        renderer.drawRectangle(thumbX, scrollbarY, thumbWidth, this.scrollbarWidth, thumbColor);
    }
    
    /**
     * Updates scrollbar hover state.
     */
    private void updateScrollbarHoverState(Vector2f mousePos, Vector2f pos, Vector2f size, Vector2f viewportSize) {
        verticalScrollbarHovered = false;
        horizontalScrollbarHovered = false;
        
        if (needsVerticalScrollbar()) {
            float scrollbarX = pos.x + viewportSize.x;
            float scrollbarY = pos.y;
            
            verticalScrollbarHovered = mousePos.x >= scrollbarX && 
                                     mousePos.x <= scrollbarX + scrollbarWidth &&
                                     mousePos.y >= scrollbarY && 
                                     mousePos.y <= scrollbarY + viewportSize.y;
        }
        
        if (needsHorizontalScrollbar()) {
            float scrollbarX = pos.x;
            float scrollbarY = pos.y + viewportSize.y;
            
            horizontalScrollbarHovered = mousePos.x >= scrollbarX && 
                                       mousePos.x <= scrollbarX + viewportSize.x &&
                                       mousePos.y >= scrollbarY && 
                                       mousePos.y <= scrollbarY + scrollbarWidth;
        }
    }
    
    /**
     * Handles scrollbar press events.
     */
    private boolean handleScrollbarPress(Vector2f mousePos, Vector2f pos, Vector2f size, Vector2f viewportSize) {
        // Check vertical scrollbar
        if (needsVerticalScrollbar() && verticalScrollbarHovered) {
            float scrollbarX = pos.x + viewportSize.x;
            float scrollbarY = pos.y;
            float scrollbarHeight = viewportSize.y;
            
            // Calculate thumb bounds
            float contentHeight = contentSize.y;
            float thumbHeight = Math.max(scrollbarMinThumbSize, 
                (viewportSize.y / contentHeight) * scrollbarHeight);
            float maxScrollY = contentHeight - viewportSize.y;
            float thumbY = scrollbarY + (scrollOffset.y / maxScrollY) * (scrollbarHeight - thumbHeight);
            
            if (mousePos.y >= thumbY && mousePos.y <= thumbY + thumbHeight) {
                // Start dragging thumb
                draggingVerticalThumb = true;
                verticalScrollbarPressed = true;
                dragOffset.y = mousePos.y - thumbY;
                return true;
            } else {
                // Click on track - jump to position
                float clickRatio = (mousePos.y - scrollbarY) / scrollbarHeight;
                targetScrollOffset.y = clickRatio * (contentHeight - viewportSize.y);
                return true;
            }
        }
        
        // Check horizontal scrollbar
        if (needsHorizontalScrollbar() && horizontalScrollbarHovered) {
            float scrollbarX = pos.x;
            float scrollbarY = pos.y + viewportSize.y;
            float scrollbarWidth = viewportSize.x;
            
            // Calculate thumb bounds
            float contentWidth = contentSize.x;
            float thumbWidth = Math.max(scrollbarMinThumbSize, 
                (viewportSize.x / contentWidth) * scrollbarWidth);
            float maxScrollX = contentWidth - viewportSize.x;
            float thumbX = scrollbarX + (scrollOffset.x / maxScrollX) * (scrollbarWidth - thumbWidth);
            
            if (mousePos.x >= thumbX && mousePos.x <= thumbX + thumbWidth) {
                // Start dragging thumb
                draggingHorizontalThumb = true;
                horizontalScrollbarPressed = true;
                dragOffset.x = mousePos.x - thumbX;
                return true;
            } else {
                // Click on track - jump to position
                float clickRatio = (mousePos.x - scrollbarX) / scrollbarWidth;
                targetScrollOffset.x = clickRatio * (contentWidth - viewportSize.x);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Handles vertical scrollbar dragging.
     */
    private void handleVerticalScrollbarDrag(Vector2f mousePos, Vector2f pos, Vector2f size, Vector2f viewportSize) {
        float scrollbarY = pos.y;
        float scrollbarHeight = viewportSize.y;
        
        float contentHeight = contentSize.y;
        float thumbHeight = Math.max(scrollbarMinThumbSize, 
            (viewportSize.y / contentHeight) * scrollbarHeight);
        
        float thumbY = mousePos.y - dragOffset.y;
        float thumbRatio = (thumbY - scrollbarY) / (scrollbarHeight - thumbHeight);
        
        targetScrollOffset.y = thumbRatio * (contentHeight - viewportSize.y);
    }
    
    /**
     * Handles horizontal scrollbar dragging.
     */
    private void handleHorizontalScrollbarDrag(Vector2f mousePos, Vector2f pos, Vector2f size, Vector2f viewportSize) {
        float scrollbarX = pos.x;
        float scrollbarWidth = viewportSize.x;
        
        float contentWidth = contentSize.x;
        float thumbWidth = Math.max(scrollbarMinThumbSize, 
            (viewportSize.x / contentWidth) * scrollbarWidth);
        
        float thumbX = mousePos.x - dragOffset.x;
        float thumbRatio = (thumbX - scrollbarX) / (scrollbarWidth - thumbWidth);
        
        targetScrollOffset.x = thumbRatio * (contentWidth - viewportSize.x);
    }
    
    /**
     * Handles mouse scroll events.
     */
    private void handleMouseScroll(float scrollX, float scrollY) {
        targetScrollOffset.x -= scrollX * scrollSpeed;
        targetScrollOffset.y -= scrollY * scrollSpeed;
        
        clampScrollOffset();
    }
    
    // Content management
    public UINode getContent() { return contentNode; }
    public void setContent(UINode content) {
        if (contentNode != null) {
            removeChild(contentNode);
        }
        
        contentNode = content;
        if (contentNode != null) {
            addChild(contentNode);
        }
        
        updateContentSize();
    }
    
    public void addContentChild(UINode child) {
        if (contentNode != null) {
            contentNode.addChild(child);
            updateContentSize();
        }
    }
    
    public void removeContentChild(UINode child) {
        if (contentNode != null) {
            contentNode.removeChild(child);
            updateContentSize();
        }
    }
    
    // Scrolling methods
    public void scrollTo(float x, float y) {
        targetScrollOffset.set(x, y);
        clampScrollOffset();
    }
    
    public void scrollBy(float deltaX, float deltaY) {
        targetScrollOffset.add(deltaX, deltaY);
        clampScrollOffset();
    }
    
    public void scrollToTop() { scrollTo(scrollOffset.x, 0.0f); }
    public void scrollToBottom() { 
        Vector2f viewportSize = getViewportSize();
        scrollTo(scrollOffset.x, contentSize.y - viewportSize.y); 
    }
    public void scrollToLeft() { scrollTo(0.0f, scrollOffset.y); }
    public void scrollToRight() { 
        Vector2f viewportSize = getViewportSize();
        scrollTo(contentSize.x - viewportSize.x, scrollOffset.y); 
    }
    
    // Getters and setters
    public Vector2f getScrollOffset() { return new Vector2f(scrollOffset); }
    public Vector2f getContentSize() { return new Vector2f(contentSize); }
    
    public boolean isShowHorizontalScrollbar() { return showHorizontalScrollbar; }
    public void setShowHorizontalScrollbar(boolean show) { 
        this.showHorizontalScrollbar = show; 
        markNeedsLayout();
    }
    
    public boolean isShowVerticalScrollbar() { return showVerticalScrollbar; }
    public void setShowVerticalScrollbar(boolean show) { 
        this.showVerticalScrollbar = show; 
        markNeedsLayout();
    }
    
    public float getScrollbarWidth() { return scrollbarWidth; }
    public void setScrollbarWidth(float width) { 
        this.scrollbarWidth = Math.max(8.0f, width); 
        markNeedsLayout();
    }
    
    public float getScrollSpeed() { return scrollSpeed; }
    public void setScrollSpeed(float speed) { this.scrollSpeed = Math.max(1.0f, speed); }
    
    public boolean isSmoothScrolling() { return smoothScrolling; }
    public void setSmoothScrolling(boolean smooth) { this.smoothScrolling = smooth; }
    
    public float getScrollLerpSpeed() { return scrollLerpSpeed; }
    public void setScrollLerpSpeed(float speed) { this.scrollLerpSpeed = Math.max(0.1f, speed); }
    
    public Vector4f getBackgroundColor() { return new Vector4f(backgroundColor); }
    public void setBackgroundColor(float r, float g, float b, float a) { 
        backgroundColor.set(r, g, b, a); 
    }
}

/**
 * Simple concrete implementation of UINode for use as a content container.
 */
class ContentContainer extends UINode {
    @Override
    protected void onUpdate(double deltaTime) {
        // No update logic needed for content container
    }
    
    @Override
    protected void onRender(UIRenderer renderer) {
        // No rendering needed for content container
    }
    
    @Override
    protected boolean onInput(UIInputEvent event) {
        // No input handling needed for content container
        return false;
    }
    
    @Override
    protected void onLayout() {
        // No layout logic needed for content container
    }
}