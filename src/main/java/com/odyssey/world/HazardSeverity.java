package com.odyssey.world;

/**
 * Enum defining the severity levels of hazards
 */
public enum HazardSeverity {
    
    MINOR("Minor", "A minor hazard with minimal impact", 
          0.8f, 0.9f, 1.1f, 1.05f, 0.95f, 0.02f, 0.1f, 0.05f),
    
    MODERATE("Moderate", "A moderate hazard requiring caution",
             0.6f, 0.8f, 1.3f, 1.15f, 0.85f, 0.05f, 0.2f, 0.1f),
    
    MAJOR("Major", "A major hazard posing significant danger",
          0.4f, 0.6f, 1.6f, 1.3f, 0.7f, 0.1f, 0.4f, 0.2f),
    
    SEVERE("Severe", "A severe hazard with extreme danger",
           0.2f, 0.4f, 2.0f, 1.5f, 0.5f, 0.2f, 0.6f, 0.35f),
    
    CATASTROPHIC("Catastrophic", "A catastrophic hazard threatening all who approach",
                 0.1f, 0.2f, 3.0f, 2.0f, 0.3f, 0.4f, 0.8f, 0.5f);
    
    private final String displayName;
    private final String description;
    private final float visibilityMultiplier;
    private final float speedMultiplier;
    private final float damageMultiplier;
    private final float difficultyMultiplier;
    private final float detectionMultiplier;
    private final float crewCasualtyRisk;
    private final float cargoLossRisk;
    private final float shipDamageRisk;
    
    HazardSeverity(String displayName, String description, float visibilityMultiplier,
                   float speedMultiplier, float damageMultiplier, float difficultyMultiplier,
                   float detectionMultiplier, float crewCasualtyRisk, float cargoLossRisk,
                   float shipDamageRisk) {
        this.displayName = displayName;
        this.description = description;
        this.visibilityMultiplier = visibilityMultiplier;
        this.speedMultiplier = speedMultiplier;
        this.damageMultiplier = damageMultiplier;
        this.difficultyMultiplier = difficultyMultiplier;
        this.detectionMultiplier = detectionMultiplier;
        this.crewCasualtyRisk = crewCasualtyRisk;
        this.cargoLossRisk = cargoLossRisk;
        this.shipDamageRisk = shipDamageRisk;
    }
    
    /**
     * Gets the probability of this severity occurring
     */
    public float getProbability() {
        switch (this) {
            case MINOR:
                return 0.4f;
            case MODERATE:
                return 0.3f;
            case MAJOR:
                return 0.2f;
            case SEVERE:
                return 0.08f;
            case CATASTROPHIC:
                return 0.02f;
            default:
                return 0.1f;
        }
    }
    
    /**
     * Gets the required reputation threshold to handle this severity
     */
    public int getReputationThreshold() {
        switch (this) {
            case MINOR:
                return 0;
            case MODERATE:
                return 100;
            case MAJOR:
                return 300;
            case SEVERE:
                return 600;
            case CATASTROPHIC:
                return 1000;
            default:
                return 0;
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
                return 1.6f;
            case SEVERE:
                return 2.2f;
            case CATASTROPHIC:
                return 4.0f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Gets the crew morale impact of encountering this severity
     */
    public float getCrewMoraleImpact() {
        switch (this) {
            case MINOR:
                return -0.05f;
            case MODERATE:
                return -0.1f;
            case MAJOR:
                return -0.2f;
            case SEVERE:
                return -0.4f;
            case CATASTROPHIC:
                return -0.7f;
            default:
                return 0.0f;
        }
    }
    
    /**
     * Gets the experience multiplier for successfully navigating this severity
     */
    public float getExperienceMultiplier() {
        switch (this) {
            case MINOR:
                return 1.1f;
            case MODERATE:
                return 1.3f;
            case MAJOR:
                return 1.6f;
            case SEVERE:
                return 2.2f;
            case CATASTROPHIC:
                return 3.5f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Gets the news priority for events of this severity
     */
    public NewsPriority getNewsPriority() {
        switch (this) {
            case MINOR:
                return NewsPriority.LOW;
            case MODERATE:
                return NewsPriority.NORMAL;
            case MAJOR:
                return NewsPriority.HIGH;
            case SEVERE:
                return NewsPriority.URGENT;
            case CATASTROPHIC:
                return NewsPriority.BREAKING;
            default:
                return NewsPriority.NORMAL;
        }
    }
    
    /**
     * Gets the recovery time in hours for this severity
     */
    public float getRecoveryTimeHours() {
        switch (this) {
            case MINOR:
                return 0.5f;
            case MODERATE:
                return 2.0f;
            case MAJOR:
                return 6.0f;
            case SEVERE:
                return 24.0f;
            case CATASTROPHIC:
                return 72.0f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * Gets the faction standing change for surviving this severity
     */
    public int getFactionStandingChange() {
        switch (this) {
            case MINOR:
                return 1;
            case MODERATE:
                return 2;
            case MAJOR:
                return 5;
            case SEVERE:
                return 10;
            case CATASTROPHIC:
                return 20;
            default:
                return 0;
        }
    }
    
    /**
     * Checks if this severity requires government intervention
     */
    public boolean requiresGovernmentIntervention() {
        return this == SEVERE || this == CATASTROPHIC;
    }
    
    /**
     * Checks if this severity triggers emergency protocols
     */
    public boolean triggersEmergencyProtocols() {
        return this == MAJOR || this == SEVERE || this == CATASTROPHIC;
    }
    
    /**
     * Gets the recommended crew skill level for this severity
     */
    public int getRecommendedCrewSkillLevel() {
        switch (this) {
            case MINOR:
                return 1;
            case MODERATE:
                return 3;
            case MAJOR:
                return 5;
            case SEVERE:
                return 7;
            case CATASTROPHIC:
                return 9;
            default:
                return 1;
        }
    }
    
    /**
     * Gets the color representation for UI display
     */
    public String getColor() {
        switch (this) {
            case MINOR:
                return "#32CD32"; // Lime green
            case MODERATE:
                return "#FFD700"; // Gold
            case MAJOR:
                return "#FF8C00"; // Dark orange
            case SEVERE:
                return "#FF4500"; // Orange red
            case CATASTROPHIC:
                return "#8B0000"; // Dark red
            default:
                return "#808080"; // Gray
        }
    }
    
    /**
     * Gets the icon representation for UI display
     */
    public String getIcon() {
        switch (this) {
            case MINOR:
                return "⚠️";
            case MODERATE:
                return "⚠️";
            case MAJOR:
                return "🚨";
            case SEVERE:
                return "☠️";
            case CATASTROPHIC:
                return "💀";
            default:
                return "❓";
        }
    }
    
    /**
     * Gets an adjective describing this severity
     */
    public String getAdjective() {
        switch (this) {
            case MINOR:
                return "minor";
            case MODERATE:
                return "moderate";
            case MAJOR:
                return "major";
            case SEVERE:
                return "severe";
            case CATASTROPHIC:
                return "catastrophic";
            default:
                return "unknown";
        }
    }
    
    /**
     * Determines severity based on various factors
     */
    public static HazardSeverity determineSeverity(float randomFactor, HazardType hazardType,
                                                 float regionStability, float economicHealth) {
        // Base probability modified by factors
        float[] probabilities = new float[values().length];
        
        for (int i = 0; i < values().length; i++) {
            HazardSeverity severity = values()[i];
            probabilities[i] = severity.getProbability();
            
            // Modify based on hazard type threat level
            switch (hazardType.getThreatLevel()) {
                case EXTREME:
                    if (severity.ordinal() >= MAJOR.ordinal()) {
                        probabilities[i] *= 2.0f;
                    }
                    break;
                case HIGH:
                    if (severity.ordinal() >= MODERATE.ordinal()) {
                        probabilities[i] *= 1.5f;
                    }
                    break;
                case LOW:
                case MINIMAL:
                    if (severity.ordinal() <= MODERATE.ordinal()) {
                        probabilities[i] *= 1.5f;
                    }
                    break;
            }
            
            // Modify based on region stability (lower stability = higher severity)
            probabilities[i] *= (2.0f - regionStability);
            
            // Modify based on economic health (lower health = higher severity)
            probabilities[i] *= (2.0f - economicHealth);
        }
        
        // Normalize probabilities
        float total = 0.0f;
        for (float prob : probabilities) {
            total += prob;
        }
        
        if (total <= 0.0f) {
            return MINOR; // Fallback
        }
        
        for (int i = 0; i < probabilities.length; i++) {
            probabilities[i] /= total;
        }
        
        // Select based on random factor
        float cumulative = 0.0f;
        for (int i = 0; i < probabilities.length; i++) {
            cumulative += probabilities[i];
            if (randomFactor <= cumulative) {
                return values()[i];
            }
        }
        
        return CATASTROPHIC; // Fallback to highest severity
    }
    
    // Helper enum for news priority
    public enum NewsPriority {
        LOW("Low Priority"),
        NORMAL("Normal Priority"),
        HIGH("High Priority"),
        URGENT("Urgent"),
        BREAKING("Breaking News");
        
        private final String displayName;
        
        NewsPriority(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Gets the duration multiplier for this severity
     */
    public float getDurationMultiplier() {
        switch (this) {
            case MINOR: return 0.8f;
            case MODERATE: return 1.0f;
            case MAJOR: return 1.3f;
            case SEVERE: return 1.6f;
            case CATASTROPHIC: return 2.0f;
            default: return 1.0f;
        }
    }
    
    /**
     * Gets the intensity bonus for this severity
     */
    public float getIntensityBonus() {
        switch (this) {
            case MINOR: return 0.1f;
            case MODERATE: return 0.2f;
            case MAJOR: return 0.3f;
            case SEVERE: return 0.4f;
            case CATASTROPHIC: return 0.5f;
            default: return 0.0f;
        }
    }
    
    /**
     * Gets the effect multiplier for this severity
     */
    public float getEffectMultiplier() {
        return damageMultiplier;
    }
    
    /**
     * Gets the growth multiplier for this severity
     */
    public float getGrowthMultiplier() {
        switch (this) {
            case MINOR: return 0.5f;
            case MODERATE: return 0.8f;
            case MAJOR: return 1.0f;
            case SEVERE: return 1.3f;
            case CATASTROPHIC: return 1.6f;
            default: return 1.0f;
        }
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public float getVisibilityMultiplier() { return visibilityMultiplier; }
    public float getSpeedMultiplier() { return speedMultiplier; }
    public float getDamageMultiplier() { return damageMultiplier; }
    public float getDifficultyMultiplier() { return difficultyMultiplier; }
    public float getDetectionMultiplier() { return detectionMultiplier; }
    public float getCrewCasualtyRisk() { return crewCasualtyRisk; }
    public float getCargoLossRisk() { return cargoLossRisk; }
    public float getShipDamageRisk() { return shipDamageRisk; }
}