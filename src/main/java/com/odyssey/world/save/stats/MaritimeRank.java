package com.odyssey.world.save.stats;

/**
 * Maritime ranks based on player experience and achievements.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public enum MaritimeRank {
    LANDLUBBER("Landlubber", 0, "A newcomer to the seas"),
    APPRENTICE_SAILOR("Apprentice Sailor", 5, "Learning the ropes of maritime life"),
    EXPERIENCED_MARINER("Experienced Mariner", 10, "A seasoned sailor with some experience"),
    SEASONED_SAILOR("Seasoned Sailor", 20, "A skilled sailor who knows the waters well"),
    MASTER_NAVIGATOR("Master Navigator", 30, "An expert in navigation and seamanship"),
    LEGENDARY_CAPTAIN("Legendary Captain", 50, "A legendary figure of the high seas");
    
    private final String title;
    private final int requiredLevel;
    private final String description;
    
    MaritimeRank(String title, int requiredLevel, String description) {
        this.title = title;
        this.requiredLevel = requiredLevel;
        this.description = description;
    }
    
    public String getTitle() {
        return title;
    }
    
    public int getRequiredLevel() {
        return requiredLevel;
    }
    
    public String getDescription() {
        return description;
    }
}