package com.odyssey.world.save.stats;

/**
 * Rarity levels for maritime achievements.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public enum AchievementRarity {
    COMMON("Common", "#FFFFFF"),
    UNCOMMON("Uncommon", "#00FF00"),
    RARE("Rare", "#0080FF"),
    EPIC("Epic", "#8000FF"),
    LEGENDARY("Legendary", "#FF8000");
    
    private final String displayName;
    private final String color;
    
    AchievementRarity(String displayName, String color) {
        this.displayName = displayName;
        this.color = color;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getColor() {
        return color;
    }
}