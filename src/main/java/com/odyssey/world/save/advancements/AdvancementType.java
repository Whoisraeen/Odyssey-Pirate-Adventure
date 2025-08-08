package com.odyssey.world.save.advancements;

/**
 * Types of maritime advancements.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public enum AdvancementType {
    NAVIGATION("Navigation & Exploration"),
    COMBAT("Combat & Battles"),
    TREASURE("Treasure & Economy"),
    SHIPBUILDING("Shipbuilding & Engineering"),
    FISHING("Fishing & Marine Life"),
    SURVIVAL("Survival & Weather"),
    SOCIAL("Social & Multiplayer"),
    STORY("Story & Quests"),
    CHALLENGE("Challenges & Achievements");
    
    private final String displayName;
    
    AdvancementType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}