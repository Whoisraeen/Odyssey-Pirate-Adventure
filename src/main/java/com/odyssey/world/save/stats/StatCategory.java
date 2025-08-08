package com.odyssey.world.save.stats;

/**
 * Categories for organizing statistics in The Odyssey.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public enum StatCategory {
    NAVIGATION("Navigation & Travel"),
    COMBAT("Combat & Battles"),
    EXPLORATION("Exploration & Discovery"),
    ECONOMY("Economy & Trade"),
    SHIPBUILDING("Shipbuilding & Engineering"),
    MARINE_LIFE("Marine Life & Fishing"),
    SURVIVAL("Survival & Weather"),
    SOCIAL("Social & Multiplayer"),
    CRAFTING("Crafting & Resources"),
    GENERAL("General Statistics");
    
    private final String displayName;
    
    StatCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}