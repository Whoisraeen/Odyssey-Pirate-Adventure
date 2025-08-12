package com.odyssey.ui;

import org.joml.Vector2f;
import org.joml.Vector4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


import static org.lwjgl.opengl.GL11.*;

/**
 * Radial UI system for console-style interaction.
 * Provides circular menus that can be navigated with gamepad sticks or mouse.
 */
public class RadialUI extends UINode {
    private static final Logger logger = LoggerFactory.getLogger(RadialUI.class);
    
    private final List<RadialItem> items = new ArrayList<>();
    private int selectedIndex = -1;
    private boolean isVisible = false;
    private float radius = 100.0f;
    private float innerRadius = 30.0f;
    private Vector2f center = new Vector2f();
    private Vector4f backgroundColor = new Vector4f(0.0f, 0.0f, 0.0f, 0.7f);
    private Vector4f selectedColor = new Vector4f(0.2f, 0.6f, 1.0f, 0.8f);
    private Vector4f normalColor = new Vector4f(0.3f, 0.3f, 0.3f, 0.6f);
    private Vector4f textColor = new Vector4f(1.0f, 1.0f, 1.0f, 1.0f);
    
    // Animation
    private float animationTime = 0.0f;
    private float animationDuration = 0.3f;
    private boolean animatingIn = false;
    private boolean animatingOut = false;
    
    /**
     * Represents an item in the radial menu.
     */
    public static class RadialItem {
        private final String text;
        private final String icon;
        private final Runnable action;
        private final Vector4f color;
        private boolean enabled = true;
        
        public RadialItem(String text, String icon, Runnable action) {
            this(text, icon, action, new Vector4f(1.0f, 1.0f, 1.0f, 1.0f));
        }
        
        public RadialItem(String text, String icon, Runnable action, Vector4f color) {
            this.text = text;
            this.icon = icon;
            this.action = action;
            this.color = color;
        }
        
        public String getText() { return text; }
        public String getIcon() { return icon; }
        public Runnable getAction() { return action; }
        public Vector4f getColor() { return color; }
        public boolean isEnabled() { return enabled; }
        
        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
        
        public void execute() {
            if (enabled && action != null) {
                action.run();
            }
        }
    }
    
    public RadialUI() {
        super();
        setSize(200, 200);
    }
    
    /**
     * Adds an item to the radial menu.
     */
    public void addItem(String text, String icon, Runnable action) {
        items.add(new RadialItem(text, icon, action));
        updateRadialLayout();
    }
    
    /**
     * Adds an item with custom color to the radial menu.
     */
    public void addItem(String text, String icon, Runnable action, Vector4f color) {
        items.add(new RadialItem(text, icon, action, color));
        updateRadialLayout();
    }
    
    /**
     * Removes all items from the radial menu.
     */
    public void clearItems() {
        items.clear();
        selectedIndex = -1;
        updateRadialLayout();
    }
    
    /**
     * Shows the radial menu with animation.
     */
    public void show() {
        if (!isVisible) {
            isVisible = true;
            animatingIn = true;
            animatingOut = false;
            animationTime = 0.0f;
            selectedIndex = -1;
            logger.debug("Showing radial UI with {} items", items.size());
        }
    }
    
    /**
     * Hides the radial menu with animation.
     */
    public void hide() {
        if (isVisible) {
            animatingOut = true;
            animatingIn = false;
            animationTime = 0.0f;
            logger.debug("Hiding radial UI");
        }
    }
    
    /**
     * Sets the center position of the radial menu.
     */
    public void setCenter(Vector2f center) {
        this.center.set(center);
        setPosition(center.x - size.x / 2, center.y - size.y / 2);
    }
    
    /**
     * Sets the radius of the radial menu.
     */
    public void setRadius(float radius) {
        this.radius = radius;
        updateRadialLayout();
    }
    
    /**
     * Sets the inner radius (dead zone) of the radial menu.
     */
    public void setInnerRadius(float innerRadius) {
        this.innerRadius = innerRadius;
    }
    
    /**
     * Updates selection based on input direction.
     */
    public void updateSelection(Vector2f inputDirection) {
        if (!isVisible || items.isEmpty()) {
            return;
        }
        
        float magnitude = inputDirection.length();
        if (magnitude < 0.3f) {
            selectedIndex = -1;
            return;
        }
        
        // Calculate angle from input direction
        float angle = (float) Math.atan2(inputDirection.y, inputDirection.x);
        if (angle < 0) {
            angle += 2 * Math.PI;
        }
        
        // Convert to item index
        float anglePerItem = (2 * (float) Math.PI) / items.size();
        int index = Math.round(angle / anglePerItem) % items.size();
        
        if (index != selectedIndex) {
            selectedIndex = index;
            logger.debug("Selected radial item: {} ({})", index, 
                        index >= 0 && index < items.size() ? items.get(index).getText() : "none");
        }
    }
    
    /**
     * Executes the currently selected item.
     */
    public void executeSelected() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            RadialItem item = items.get(selectedIndex);
            if (item.isEnabled()) {
                logger.info("Executing radial item: {}", item.getText());
                item.execute();
                hide();
            }
        }
    }
    
    /**
     * Gets the currently selected item.
     */
    public RadialItem getSelectedItem() {
        if (selectedIndex >= 0 && selectedIndex < items.size()) {
            return items.get(selectedIndex);
        }
        return null;
    }
    
    @Override
    protected void onUpdate(double deltaTime) {
        if (animatingIn || animatingOut) {
            animationTime += deltaTime;
            
            if (animationTime >= animationDuration) {
                animationTime = animationDuration;
                
                if (animatingOut) {
                    isVisible = false;
                    animatingOut = false;
                }
                
                animatingIn = false;
            }
        }
    }
    
    @Override
    protected void onRender(UIRenderer renderer) {
        if (!isVisible && !animatingOut) {
            return;
        }
        
        float alpha = calculateAlpha();
        if (alpha <= 0.0f) {
            return;
        }
        
        Vector2f worldCenter = new Vector2f(position.x + size.x / 2, position.y + size.y / 2);
        
        // Draw background circle
        Vector4f bgColor = new Vector4f(backgroundColor).mul(alpha);
        drawCircle(renderer, worldCenter, radius, bgColor, 32);
        
        // Draw inner dead zone
        Vector4f innerColor = new Vector4f(0.1f, 0.1f, 0.1f, 0.5f * alpha);
        drawCircle(renderer, worldCenter, innerRadius, innerColor, 16);
        
        // Draw items
        if (!items.isEmpty()) {
            float anglePerItem = (2 * (float) Math.PI) / items.size();
            
            for (int i = 0; i < items.size(); i++) {
                RadialItem item = items.get(i);
                float angle = i * anglePerItem - (float) Math.PI / 2; // Start at top
                
                // Calculate item position
                float itemRadius = (radius + innerRadius) / 2;
                Vector2f itemPos = new Vector2f(
                    worldCenter.x + (float) Math.cos(angle) * itemRadius,
                    worldCenter.y + (float) Math.sin(angle) * itemRadius
                );
                
                // Draw item background
                Vector4f itemColor = (i == selectedIndex) ? 
                    new Vector4f(selectedColor).mul(alpha) : 
                    new Vector4f(normalColor).mul(alpha);
                
                if (!item.isEnabled()) {
                    itemColor.mul(0.5f);
                }
                
                drawCircle(renderer, itemPos, 20.0f, itemColor, 16);
                
                // Draw item text
                Vector4f itemTextColor = new Vector4f(item.getColor()).mul(alpha);
                if (!item.isEnabled()) {
                    itemTextColor.mul(0.5f);
                }
                
                float textWidth = renderer.getTextWidth(item.getText(), 12.0f);
                renderer.drawText(item.getText(), 
                    itemPos.x - textWidth / 2, 
                    itemPos.y - 6.0f, 
                    12.0f, 
                    itemTextColor);
                
                // Draw selection indicator
                if (i == selectedIndex) {
                    Vector4f indicatorColor = new Vector4f(1.0f, 1.0f, 1.0f, alpha);
                    drawCircleOutline(renderer, itemPos, 25.0f, indicatorColor, 2.0f, 16);
                }
            }
        }
        
        // Draw center dot
        Vector4f centerColor = new Vector4f(0.8f, 0.8f, 0.8f, alpha);
        drawCircle(renderer, worldCenter, 3.0f, centerColor, 8);
    }
    
    @Override
    protected boolean onInput(UIInputEvent event) {
        if (!isVisible) {
            return false;
        }
        
        if (event.getType() == UIInputEvent.InputType.MOUSE_MOVE) {
            // Update selection based on mouse position
            Vector2f worldCenter = new Vector2f(position.x + size.x / 2, position.y + size.y / 2);
            Vector2f mousePos = event.getPosition();
            Vector2f direction = new Vector2f(mousePos).sub(worldCenter);
            
            if (direction.length() > innerRadius) {
                direction.normalize();
                updateSelection(direction);
            } else {
                selectedIndex = -1;
            }
        } else if (event.getType() == UIInputEvent.InputType.MOUSE_PRESS && 
                   event.getButton() == 0) { // Left click
            executeSelected();
            return true;
        }
        
        return false;
    }
    
    @Override
    protected void onLayout() {
        // Radial UI doesn't need traditional layout
        updateRadialLayout();
    }
    
    private void updateRadialLayout() {
        // Update size based on radius
        float diameter = radius * 2;
        setSize(diameter, diameter);
    }
    
    private float calculateAlpha() {
        if (!animatingIn && !animatingOut) {
            return isVisible ? 1.0f : 0.0f;
        }
        
        float progress = animationTime / animationDuration;
        
        if (animatingIn) {
            return easeOutCubic(progress);
        } else if (animatingOut) {
            return 1.0f - easeOutCubic(progress);
        }
        
        return 0.0f;
    }
    
    private float easeOutCubic(float t) {
        return 1 - (float) Math.pow(1 - t, 3);
    }
    
    private void drawCircle(UIRenderer renderer, Vector2f center, float radius, Vector4f color, int segments) {
        // Draw circle as triangle fan
        float angleStep = (2 * (float) Math.PI) / segments;
        
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) * angleStep;
            
            Vector2f p1 = new Vector2f(
                center.x + (float) Math.cos(angle1) * radius,
                center.y + (float) Math.sin(angle1) * radius
            );
            Vector2f p2 = new Vector2f(
                center.x + (float) Math.cos(angle2) * radius,
                center.y + (float) Math.sin(angle2) * radius
            );
            
            // Draw triangle (center, p1, p2)
            drawTriangle(renderer, center, p1, p2, color);
        }
    }
    
    private void drawCircleOutline(UIRenderer renderer, Vector2f center, float radius, Vector4f color, float thickness, int segments) {
        float angleStep = (2 * (float) Math.PI) / segments;
        float innerRadius = radius - thickness / 2;
        float outerRadius = radius + thickness / 2;
        
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = (i + 1) * angleStep;
            
            Vector2f inner1 = new Vector2f(
                center.x + (float) Math.cos(angle1) * innerRadius,
                center.y + (float) Math.sin(angle1) * innerRadius
            );
            Vector2f outer1 = new Vector2f(
                center.x + (float) Math.cos(angle1) * outerRadius,
                center.y + (float) Math.sin(angle1) * outerRadius
            );
            Vector2f inner2 = new Vector2f(
                center.x + (float) Math.cos(angle2) * innerRadius,
                center.y + (float) Math.sin(angle2) * innerRadius
            );
            Vector2f outer2 = new Vector2f(
                center.x + (float) Math.cos(angle2) * outerRadius,
                center.y + (float) Math.sin(angle2) * outerRadius
            );
            
            // Draw quad as two triangles
            drawTriangle(renderer, inner1, outer1, inner2, color);
            drawTriangle(renderer, outer1, outer2, inner2, color);
        }
    }
    
    private void drawTriangle(UIRenderer renderer, Vector2f p1, Vector2f p2, Vector2f p3, Vector4f color) {
        // Calculate triangle bounds for rectangle approximation
        float minX = Math.min(Math.min(p1.x, p2.x), p3.x);
        float maxX = Math.max(Math.max(p1.x, p2.x), p3.x);
        float minY = Math.min(Math.min(p1.y, p2.y), p3.y);
        float maxY = Math.max(Math.max(p1.y, p2.y), p3.y);
        
        // Draw as rectangle (simplified for now)
        renderer.drawRectangle(minX, minY, maxX - minX, maxY - minY, color);
    }
    
    public boolean isVisible() {
        return isVisible;
    }
    
    public int getItemCount() {
        return items.size();
    }
    
    public RadialItem getItem(int index) {
        if (index >= 0 && index < items.size()) {
            return items.get(index);
        }
        return null;
    }
}