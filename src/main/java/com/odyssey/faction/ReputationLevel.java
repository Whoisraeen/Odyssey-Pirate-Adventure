package com.odyssey.faction;

/**
 * Defines different levels of reputation a player can have with factions.
 * Each level provides different benefits, penalties, and access to faction-specific content.
 */
public enum ReputationLevel {
    
    HOSTILE("Hostile", "Kill on sight - faction actively hunts the player",
            -1.0f, 0.0f, 0.0f, 2.0f, 0.0f, false, true),
    
    UNFRIENDLY("Unfriendly", "Distrusted - limited interactions and higher prices",
               -0.5f, 0.5f, 1.5f, 1.5f, 0.1f, false, false),
    
    NEUTRAL("Neutral", "Unknown - standard interactions and prices",
            0.0f, 1.0f, 1.0f, 1.0f, 0.3f, true, false),
    
    FRIENDLY("Friendly", "Trusted - better prices and some special services",
             0.3f, 1.2f, 0.8f, 0.8f, 0.6f, true, false),
    
    ALLIED("Allied", "Highly regarded - significant benefits and exclusive access",
           0.6f, 1.5f, 0.6f, 0.5f, 0.8f, true, false),
    
    REVERED("Revered", "Legendary status - maximum benefits and unique opportunities",
            1.0f, 2.0f, 0.4f, 0.2f, 1.0f, true, false);
    
    private final String displayName;
    private final String description;
    private final float tradeBonus; // Bonus/penalty to trade prices
    private final float questRewardMultiplier; // Multiplier for quest rewards
    private final float serviceCostMultiplier; // Multiplier for service costs
    private final float hostilityChance; // Chance of hostile encounters
    private final float informationAccess; // Access to faction information (0-1)
    private final boolean canTrade; // Whether player can trade with faction
    private final boolean attackOnSight; // Whether faction attacks player immediately
    
    ReputationLevel(String displayName, String description, float tradeBonus,
                   float questRewardMultiplier, float serviceCostMultiplier,
                   float hostilityChance, float informationAccess,
                   boolean canTrade, boolean attackOnSight) {
        this.displayName = displayName;
        this.description = description;
        this.tradeBonus = tradeBonus;
        this.questRewardMultiplier = questRewardMultiplier;
        this.serviceCostMultiplier = serviceCostMultiplier;
        this.hostilityChance = hostilityChance;
        this.informationAccess = informationAccess;
        this.canTrade = canTrade;
        this.attackOnSight = attackOnSight;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public float getTradeBonus() { return tradeBonus; }
    public float getQuestRewardMultiplier() { return questRewardMultiplier; }
    public float getServiceCostMultiplier() { return serviceCostMultiplier; }
    public float getHostilityChance() { return hostilityChance; }
    public float getInformationAccess() { return informationAccess; }
    public boolean canTrade() { return canTrade; }
    public boolean isAttackOnSight() { return attackOnSight; }
    
    /**
     * Gets the color associated with this reputation level for UI display
     */
    public String getColor() {
        switch (this) {
            case HOSTILE: return "#FF0000"; // Red
            case UNFRIENDLY: return "#FF8000"; // Orange
            case NEUTRAL: return "#FFFF00"; // Yellow
            case FRIENDLY: return "#80FF00"; // Light Green
            case ALLIED: return "#00FF00"; // Green
            case REVERED: return "#00FFFF"; // Cyan
            default: return "#FFFFFF"; // White
        }
    }
    
    /**
     * Gets the reputation level from a reputation value
     */
    public static ReputationLevel fromReputation(int reputation, FactionType faction) {
        if (reputation <= faction.getReputationThreshold(HOSTILE)) {
            return HOSTILE;
        } else if (reputation <= faction.getReputationThreshold(UNFRIENDLY)) {
            return UNFRIENDLY;
        } else if (reputation < faction.getReputationThreshold(FRIENDLY)) {
            return NEUTRAL;
        } else if (reputation < faction.getReputationThreshold(ALLIED)) {
            return FRIENDLY;
        } else if (reputation < faction.getReputationThreshold(REVERED)) {
            return ALLIED;
        } else {
            return REVERED;
        }
    }
    
    /**
     * Gets special abilities or services available at this reputation level
     */
    public String[] getAvailableServices() {
        switch (this) {
            case HOSTILE:
                return new String[]{};
            case UNFRIENDLY:
                return new String[]{"Basic Trade"};
            case NEUTRAL:
                return new String[]{"Standard Trade", "Basic Information", "Simple Quests"};
            case FRIENDLY:
                return new String[]{"Enhanced Trade", "Detailed Information", "Standard Quests", "Ship Repairs"};
            case ALLIED:
                return new String[]{"Premium Trade", "Insider Information", "Important Quests", 
                                  "Advanced Services", "Safe Harbor"};
            case REVERED:
                return new String[]{"Exclusive Trade", "Secret Information", "Legendary Quests",
                                  "Unique Services", "Faction Fleet Support", "Special Equipment"};
            default:
                return new String[]{};
        }
    }
    
    /**
     * Gets the minimum reputation change required to reach the next level
     */
    public int getReputationToNextLevel(int currentReputation, FactionType faction) {
        switch (this) {
            case HOSTILE:
                return faction.getReputationThreshold(UNFRIENDLY) - currentReputation;
            case UNFRIENDLY:
                return faction.getReputationThreshold(NEUTRAL) - currentReputation;
            case NEUTRAL:
                return faction.getReputationThreshold(FRIENDLY) - currentReputation;
            case FRIENDLY:
                return faction.getReputationThreshold(ALLIED) - currentReputation;
            case ALLIED:
                return faction.getReputationThreshold(REVERED) - currentReputation;
            case REVERED:
                return 0; // Already at maximum
            default:
                return 0;
        }
    }
    
    /**
     * Checks if this reputation level allows access to a specific service
     */
    public boolean hasAccess(String service) {
        String[] services = getAvailableServices();
        for (String availableService : services) {
            if (availableService.equalsIgnoreCase(service)) {
                return true;
            }
        }
        return false;
    }
}