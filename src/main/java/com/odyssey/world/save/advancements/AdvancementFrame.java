package com.odyssey.world.save.advancements;

/**
 * Frame types for advancement display.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public enum AdvancementFrame {
    TASK("Task", "#FFFFFF"),
    GOAL("Goal", "#00FF00"),
    CHALLENGE("Challenge", "#FF8000");
    
    private final String displayName;
    private final String color;
    
    AdvancementFrame(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
}