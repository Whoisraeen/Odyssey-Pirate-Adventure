package com.odyssey.world;

/**
 * Defines the operational status of trade routes
 */
public enum TradeRouteStatus {
    
    ACTIVE("Active", "Route is operational and safe for travel"),
    INACTIVE("Inactive", "Route is temporarily closed"),
    DANGEROUS("Dangerous", "Route has elevated risk levels"),
    BLOCKED("Blocked", "Route is impassable due to obstacles"),
    CONTESTED("Contested", "Route is disputed by multiple factions"),
    SEASONAL("Seasonal", "Route is only available during certain seasons"),
    ABANDONED("Abandoned", "Route has been permanently closed"),
    UNDER_CONSTRUCTION("Under Construction", "Route is being established or repaired"),
    PATROL_HEAVY("Heavy Patrol", "Route is under increased military surveillance"),
    PIRATE_INFESTED("Pirate Infested", "Route is controlled by hostile pirates");
    
    private final String displayName;
    private final String description;
    
    TradeRouteStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    /**
     * Checks if the route is safe for civilian travel
     */
    public boolean isSafeForCivilians() {
        switch (this) {
            case ACTIVE:
            case PATROL_HEAVY:
                return true;
            case SEASONAL:
            case UNDER_CONSTRUCTION:
                return false; // Conditional safety
            default:
                return false;
        }
    }
    
    /**
     * Checks if the route allows trade activities
     */
    public boolean allowsTrade() {
        switch (this) {
            case ACTIVE:
            case DANGEROUS:
            case CONTESTED:
            case SEASONAL:
            case PATROL_HEAVY:
            case PIRATE_INFESTED:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets the risk multiplier for this status
     */
    public float getRiskMultiplier() {
        switch (this) {
            case ACTIVE: return 1.0f;
            case DANGEROUS: return 1.5f;
            case CONTESTED: return 1.8f;
            case SEASONAL: return 1.2f;
            case PATROL_HEAVY: return 0.8f;
            case PIRATE_INFESTED: return 2.5f;
            case BLOCKED: return 10.0f; // Extremely risky to attempt
            case ABANDONED: return 3.0f;
            case UNDER_CONSTRUCTION: return 1.3f;
            case INACTIVE: return 2.0f;
            default: return 1.0f;
        }
    }
    
    /**
     * Gets the cost multiplier for using this route
     */
    public float getCostMultiplier() {
        switch (this) {
            case ACTIVE: return 1.0f;
            case DANGEROUS: return 1.3f;
            case CONTESTED: return 1.6f;
            case SEASONAL: return 0.9f; // Cheaper when available
            case PATROL_HEAVY: return 1.1f; // Taxes and inspections
            case PIRATE_INFESTED: return 1.8f; // Bribes and protection
            case BLOCKED: return 5.0f; // Alternative routes needed
            case ABANDONED: return 2.0f; // No infrastructure
            case UNDER_CONSTRUCTION: return 1.4f; // Delays and detours
            case INACTIVE: return 3.0f; // Unofficial passage
            default: return 1.0f;
        }
    }
    
    /**
     * Gets the time delay multiplier for this status
     */
    public float getTimeDelayMultiplier() {
        switch (this) {
            case ACTIVE: return 1.0f;
            case DANGEROUS: return 1.2f; // Cautious travel
            case CONTESTED: return 1.4f; // Avoiding conflicts
            case SEASONAL: return 1.0f;
            case PATROL_HEAVY: return 1.3f; // Inspections and checkpoints
            case PIRATE_INFESTED: return 1.5f; // Evasive maneuvers
            case BLOCKED: return 3.0f; // Long detours
            case ABANDONED: return 1.8f; // No navigation aids
            case UNDER_CONSTRUCTION: return 2.0f; // Construction delays
            case INACTIVE: return 1.6f; // Unofficial routes
            default: return 1.0f;
        }
    }
    
    /**
     * Gets the reputation change for successfully using this route
     */
    public int getReputationChange() {
        switch (this) {
            case DANGEROUS: return 5; // Brave trader
            case CONTESTED: return 8; // Skilled navigator
            case PIRATE_INFESTED: return 10; // Fearless captain
            case BLOCKED: return 15; // Legendary achievement
            case ABANDONED: return 12; // Explorer
            default: return 1; // Standard trade
        }
    }
    
    /**
     * Gets the faction standing change for using this route
     */
    public int getFactionStandingChange(String faction) {
        switch (this) {
            case PATROL_HEAVY:
                return faction.equals("Navy") ? 2 : 0;
            case PIRATE_INFESTED:
                return faction.equals("Pirates") ? 3 : (faction.equals("Navy") ? -2 : 0);
            case CONTESTED:
                return faction.equals("Merchants") ? 1 : 0;
            default:
                return 0;
        }
    }
    
    /**
     * Checks if this status can transition to another status
     */
    public boolean canTransitionTo(TradeRouteStatus newStatus) {
        switch (this) {
            case ACTIVE:
                return newStatus != ABANDONED && newStatus != UNDER_CONSTRUCTION;
            case INACTIVE:
                return newStatus == ACTIVE || newStatus == ABANDONED || newStatus == UNDER_CONSTRUCTION;
            case DANGEROUS:
                return newStatus == ACTIVE || newStatus == PIRATE_INFESTED || newStatus == BLOCKED;
            case BLOCKED:
                return newStatus == UNDER_CONSTRUCTION || newStatus == ABANDONED;
            case CONTESTED:
                return newStatus == ACTIVE || newStatus == DANGEROUS || newStatus == PIRATE_INFESTED;
            case SEASONAL:
                return newStatus == ACTIVE || newStatus == INACTIVE;
            case ABANDONED:
                return newStatus == UNDER_CONSTRUCTION; // Only way back
            case UNDER_CONSTRUCTION:
                return newStatus == ACTIVE || newStatus == ABANDONED;
            case PATROL_HEAVY:
                return newStatus == ACTIVE || newStatus == CONTESTED;
            case PIRATE_INFESTED:
                return newStatus == CONTESTED || newStatus == DANGEROUS || newStatus == BLOCKED;
            default:
                return true;
        }
    }
    
    /**
     * Gets the color representation for UI display
     */
    public String getColor() {
        switch (this) {
            case ACTIVE: return "#00FF00"; // Green
            case INACTIVE: return "#808080"; // Gray
            case DANGEROUS: return "#FFA500"; // Orange
            case BLOCKED: return "#FF0000"; // Red
            case CONTESTED: return "#FFFF00"; // Yellow
            case SEASONAL: return "#00FFFF"; // Cyan
            case ABANDONED: return "#800080"; // Purple
            case UNDER_CONSTRUCTION: return "#0000FF"; // Blue
            case PATROL_HEAVY: return "#000080"; // Navy
            case PIRATE_INFESTED: return "#8B0000"; // Dark Red
            default: return "#FFFFFF"; // White
        }
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    
    @Override
    public String toString() {
        return displayName;
    }
}