package com.odyssey.world;

/**
 * Enum defining the severity levels of trade events
 */
public enum TradeEventSeverity {
    
    MINOR("Minor", "Small-scale disruption with limited impact", 0.8f, 0.5f, 1.2f, 0.9f),
    MODERATE("Moderate", "Noticeable disruption affecting regional trade", 1.0f, 1.0f, 1.5f, 1.0f),
    MAJOR("Major", "Significant disruption with widespread effects", 1.3f, 1.5f, 2.0f, 1.2f),
    CRITICAL("Critical", "Severe disruption causing major economic impact", 1.6f, 2.0f, 3.0f, 1.5f),
    CATASTROPHIC("Catastrophic", "Devastating event with long-lasting consequences", 2.0f, 3.0f, 5.0f, 2.0f);
    
    private final String displayName;
    private final String description;
    private final float effectMultiplier;
    private final float durationMultiplier;
    private final float economicImpactMultiplier;
    private final float radiusMultiplier;
    
    TradeEventSeverity(String displayName, String description, float effectMultiplier, 
                       float durationMultiplier, float economicImpactMultiplier, float radiusMultiplier) {
        this.displayName = displayName;
        this.description = description;
        this.effectMultiplier = effectMultiplier;
        this.durationMultiplier = durationMultiplier;
        this.economicImpactMultiplier = economicImpactMultiplier;
        this.radiusMultiplier = radiusMultiplier;
    }
    
    /**
     * Gets the probability of this severity occurring
     */
    public float getProbability() {
        switch (this) {
            case MINOR:
                return 0.40f; // 40% chance
            case MODERATE:
                return 0.35f; // 35% chance
            case MAJOR:
                return 0.15f; // 15% chance
            case CRITICAL:
                return 0.08f; // 8% chance
            case CATASTROPHIC:
                return 0.02f; // 2% chance
            default:
                return 0.35f;
        }
    }
    
    /**
     * Gets the reputation threshold required for advance warning
     */
    public int getWarningReputationThreshold() {
        switch (this) {
            case MINOR:
                return 10;
            case MODERATE:
                return 20;
            case MAJOR:
                return 35;
            case CRITICAL:
                return 50;
            case CATASTROPHIC:
                return 70;
            default:
                return 20;
        }
    }
    
    /**
     * Gets the insurance cost multiplier for this severity
     */
    public float getInsuranceCostMultiplier() {
        switch (this) {
            case MINOR:
                return 1.1f;
            case MODERATE:
                return 1.3f;
            case MAJOR:
                return 1.8f;
            case CRITICAL:
                return 2.5f;
            case CATASTROPHIC:
                return 4.0f;
            default:
                return 1.3f;
        }
    }
    
    /**
     * Gets the crew morale impact
     */
    public float getCrewMoraleImpact() {
        switch (this) {
            case MINOR:
                return -0.05f; // -5% morale
            case MODERATE:
                return -0.10f; // -10% morale
            case MAJOR:
                return -0.20f; // -20% morale
            case CRITICAL:
                return -0.35f; // -35% morale
            case CATASTROPHIC:
                return -0.50f; // -50% morale
            default:
                return -0.10f;
        }
    }
    
    /**
     * Gets the experience gain multiplier for surviving this event
     */
    public float getExperienceMultiplier() {
        switch (this) {
            case MINOR:
                return 1.1f;
            case MODERATE:
                return 1.2f;
            case MAJOR:
                return 1.5f;
            case CRITICAL:
                return 2.0f;
            case CATASTROPHIC:
                return 3.0f;
            default:
                return 1.2f;
        }
    }
    
    /**
     * Gets the news priority level
     */
    public NewsPriority getNewsPriority() {
        switch (this) {
            case MINOR:
                return NewsPriority.LOCAL;
            case MODERATE:
                return NewsPriority.REGIONAL;
            case MAJOR:
                return NewsPriority.NATIONAL;
            case CRITICAL:
                return NewsPriority.INTERNATIONAL;
            case CATASTROPHIC:
                return NewsPriority.BREAKING;
            default:
                return NewsPriority.REGIONAL;
        }
    }
    
    /**
     * Gets the recovery time multiplier
     */
    public float getRecoveryTimeMultiplier() {
        switch (this) {
            case MINOR:
                return 0.5f;
            case MODERATE:
                return 1.0f;
            case MAJOR:
                return 2.0f;
            case CRITICAL:
                return 4.0f;
            case CATASTROPHIC:
                return 8.0f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Gets the faction standing change multiplier
     */
    public float getFactionStandingMultiplier() {
        switch (this) {
            case MINOR:
                return 0.5f;
            case MODERATE:
                return 1.0f;
            case MAJOR:
                return 1.5f;
            case CRITICAL:
                return 2.0f;
            case CATASTROPHIC:
                return 3.0f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Checks if this severity requires government intervention
     */
    public boolean requiresGovernmentIntervention() {
        return this == CRITICAL || this == CATASTROPHIC;
    }
    
    /**
     * Checks if this severity triggers emergency protocols
     */
    public boolean triggersEmergencyProtocols() {
        return this == MAJOR || this == CRITICAL || this == CATASTROPHIC;
    }
    
    /**
     * Gets the minimum crew skill level recommended to handle this severity
     */
    public int getRecommendedCrewSkillLevel() {
        switch (this) {
            case MINOR:
                return 1;
            case MODERATE:
                return 3;
            case MAJOR:
                return 5;
            case CRITICAL:
                return 7;
            case CATASTROPHIC:
                return 9;
            default:
                return 3;
        }
    }
    
    /**
     * Gets the ship damage risk factor
     */
    public float getShipDamageRisk() {
        switch (this) {
            case MINOR:
                return 0.05f; // 5% chance of damage
            case MODERATE:
                return 0.15f; // 15% chance of damage
            case MAJOR:
                return 0.30f; // 30% chance of damage
            case CRITICAL:
                return 0.50f; // 50% chance of damage
            case CATASTROPHIC:
                return 0.75f; // 75% chance of damage
            default:
                return 0.15f;
        }
    }
    
    /**
     * Gets the cargo loss risk factor
     */
    public float getCargoLossRisk() {
        switch (this) {
            case MINOR:
                return 0.02f; // 2% chance of cargo loss
            case MODERATE:
                return 0.08f; // 8% chance of cargo loss
            case MAJOR:
                return 0.20f; // 20% chance of cargo loss
            case CRITICAL:
                return 0.40f; // 40% chance of cargo loss
            case CATASTROPHIC:
                return 0.65f; // 65% chance of cargo loss
            default:
                return 0.08f;
        }
    }
    
    /**
     * Gets the crew casualty risk factor
     */
    public float getCrewCasualtyRisk() {
        switch (this) {
            case MINOR:
                return 0.01f; // 1% chance of casualties
            case MODERATE:
                return 0.03f; // 3% chance of casualties
            case MAJOR:
                return 0.10f; // 10% chance of casualties
            case CRITICAL:
                return 0.25f; // 25% chance of casualties
            case CATASTROPHIC:
                return 0.45f; // 45% chance of casualties
            default:
                return 0.03f;
        }
    }
    
    /**
     * Gets the color representation for UI display
     */
    public String getColor() {
        switch (this) {
            case MINOR:
                return "#90EE90"; // Light green
            case MODERATE:
                return "#FFD700"; // Gold
            case MAJOR:
                return "#FF8C00"; // Dark orange
            case CRITICAL:
                return "#FF4500"; // Orange red
            case CATASTROPHIC:
                return "#8B0000"; // Dark red
            default:
                return "#FFD700";
        }
    }
    
    /**
     * Gets the icon representation for UI display
     */
    public String getIcon() {
        switch (this) {
            case MINOR:
                return "⚠️"; // Warning sign
            case MODERATE:
                return "⚡"; // Lightning bolt
            case MAJOR:
                return "🔥"; // Fire
            case CRITICAL:
                return "💀"; // Skull
            case CATASTROPHIC:
                return "☠️"; // Skull and crossbones
            default:
                return "⚡";
        }
    }
    
    /**
     * Gets a descriptive adjective for this severity
     */
    public String getAdjective() {
        switch (this) {
            case MINOR:
                return "manageable";
            case MODERATE:
                return "concerning";
            case MAJOR:
                return "dangerous";
            case CRITICAL:
                return "devastating";
            case CATASTROPHIC:
                return "apocalyptic";
            default:
                return "concerning";
        }
    }
    
    /**
     * Determines severity based on multiple factors
     */
    public static TradeEventSeverity determineSeverity(float randomFactor, TradeEventType eventType, 
                                                      float regionStability, float economicHealth) {
        // Base probabilities
        float[] baseProbabilities = {
            MINOR.getProbability(),
            MODERATE.getProbability(),
            MAJOR.getProbability(),
            CRITICAL.getProbability(),
            CATASTROPHIC.getProbability()
        };
        
        // Adjust probabilities based on event type
        switch (eventType.getDangerLevel()) {
            case EXTREME:
                // More likely to be severe
                baseProbabilities[0] *= 0.5f; // Minor less likely
                baseProbabilities[1] *= 0.7f; // Moderate less likely
                baseProbabilities[2] *= 1.3f; // Major more likely
                baseProbabilities[3] *= 1.8f; // Critical more likely
                baseProbabilities[4] *= 2.5f; // Catastrophic more likely
                break;
            case HIGH:
                baseProbabilities[0] *= 0.7f;
                baseProbabilities[1] *= 0.9f;
                baseProbabilities[2] *= 1.2f;
                baseProbabilities[3] *= 1.4f;
                baseProbabilities[4] *= 1.6f;
                break;
            case NONE:
                // Positive events are usually minor to moderate
                baseProbabilities[0] *= 1.5f;
                baseProbabilities[1] *= 1.2f;
                baseProbabilities[2] *= 0.5f;
                baseProbabilities[3] *= 0.1f;
                baseProbabilities[4] *= 0.05f;
                break;
        }
        
        // Adjust based on regional stability
        float stabilityFactor = 2.0f - regionStability; // Lower stability = higher severity
        for (int i = 2; i < baseProbabilities.length; i++) { // Affect major+ severities
            baseProbabilities[i] *= stabilityFactor;
        }
        
        // Adjust based on economic health
        float economicFactor = 2.0f - economicHealth; // Poor economy = higher severity
        for (int i = 1; i < baseProbabilities.length; i++) { // Affect moderate+ severities
            baseProbabilities[i] *= economicFactor;
        }
        
        // Normalize probabilities
        float total = 0;
        for (float prob : baseProbabilities) {
            total += prob;
        }
        for (int i = 0; i < baseProbabilities.length; i++) {
            baseProbabilities[i] /= total;
        }
        
        // Select severity based on random factor
        float cumulative = 0;
        TradeEventSeverity[] severities = values();
        for (int i = 0; i < severities.length; i++) {
            cumulative += baseProbabilities[i];
            if (randomFactor <= cumulative) {
                return severities[i];
            }
        }
        
        return MODERATE; // Fallback
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public float getEffectMultiplier() { return effectMultiplier; }
    public float getDurationMultiplier() { return durationMultiplier; }
    public float getEconomicImpactMultiplier() { return economicImpactMultiplier; }
    public float getRadiusMultiplier() { return radiusMultiplier; }
    
    // Helper enum
    public enum NewsPriority {
        LOCAL("Local News"),
        REGIONAL("Regional Alert"),
        NATIONAL("National News"),
        INTERNATIONAL("International Alert"),
        BREAKING("Breaking News");
        
        private final String displayName;
        NewsPriority(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
}