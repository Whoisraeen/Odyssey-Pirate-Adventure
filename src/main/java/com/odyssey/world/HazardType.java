package com.odyssey.world;

import com.odyssey.world.weather.WeatherCondition;

/**
 * Enum defining different types of hazards with their properties and behaviors
 */
public enum HazardType {
    
    // Natural hazards
    STORM_SYSTEM("Storm System", "A dangerous weather system with high winds and lightning", 
                 0.6f, 0.4f, 0.7f, 0.8f, false, true, 3600000L, // 1 hour
                 FalloffPattern.GRADUAL, IntensityPattern.FLUCTUATING, 5.0f, 50.0f, 100.0f, 
                 0.1f, 2.0f, true, 0.8f, 0.6f),
    
    WHIRLPOOL("Whirlpool", "A spinning vortex of water that can trap ships",
              0.3f, 0.8f, 0.9f, 0.7f, false, false, -1L, // Permanent
              FalloffPattern.SHARP, IntensityPattern.CONSTANT, 10.0f, 30.0f, 80.0f,
              0.0f, 5.0f, false, 0.2f, 0.9f),
    
    FOG_BANK("Fog Bank", "Dense fog that severely reduces visibility",
             0.9f, 0.2f, 0.1f, 0.6f, false, true, 7200000L, // 2 hours
             FalloffPattern.GRADUAL, IntensityPattern.DECLINING, 0.0f, 100.0f, 500.0f,
             0.0f, 0.0f, true, 0.9f, 0.3f),
    
    ICE_FIELD("Ice Field", "Floating ice that can damage ship hulls",
              0.4f, 0.6f, 0.5f, 0.7f, false, false, -1L, // Permanent (seasonal)
              FalloffPattern.MODERATE, IntensityPattern.CONSTANT, 0.0f, 200.0f, 1000.0f,
              0.0f, 0.0f, true, 0.4f, 0.8f),
    
    REEF("Coral Reef", "Shallow coral formations that can wreck ships",
         0.1f, 0.3f, 0.8f, 0.9f, true, false, -1L, // Permanent
         FalloffPattern.SHARP, IntensityPattern.CONSTANT, 0.0f, 50.0f, 200.0f,
         0.0f, 0.0f, false, 0.0f, 0.9f),
    
    SHALLOW_WATERS("Shallow Waters", "Areas too shallow for deep-draft vessels",
                   0.0f, 0.4f, 0.3f, 0.8f, true, false, -1L, // Permanent
                   FalloffPattern.GRADUAL, IntensityPattern.CONSTANT, 0.0f, 100.0f, 500.0f,
                   0.0f, 0.0f, false, 0.0f, 0.7f),
    
    // Supernatural hazards
    SIREN_WATERS("Siren Waters", "Enchanted waters where sirens lure sailors to their doom",
                 0.2f, 0.1f, 0.4f, 0.9f, false, false, -1L, // Permanent
                 FalloffPattern.MODERATE, IntensityPattern.FLUCTUATING, 0.0f, 80.0f, 200.0f,
                 0.0f, 0.0f, false, 0.3f, 0.8f),
    
    CURSED_WATERS("Cursed Waters", "Waters tainted by dark magic that bring misfortune",
                  0.3f, 0.2f, 0.3f, 0.8f, false, false, -1L, // Permanent
                  FalloffPattern.GRADUAL, IntensityPattern.CONSTANT, 0.0f, 60.0f, 150.0f,
                  0.0f, 0.0f, false, 0.4f, 0.9f),
    
    GHOST_SHIP("Ghost Ship", "Spectral vessel that haunts these waters",
               0.4f, 0.3f, 0.6f, 0.7f, false, true, 1800000L, // 30 minutes
               FalloffPattern.MODERATE, IntensityPattern.PEAK_MIDDLE, 2.0f, 40.0f, 80.0f,
               0.0f, 0.0f, false, 0.5f, 0.8f),
    
    // Human hazards
    PIRATE_AMBUSH("Pirate Ambush", "Pirates lying in wait to attack merchant vessels",
                  0.1f, 0.2f, 0.8f, 0.6f, false, true, 10800000L, // 3 hours
                  FalloffPattern.SHARP, IntensityPattern.GROWING, 1.0f, 30.0f, 60.0f,
                  0.0f, 0.0f, false, 0.2f, 0.9f),
    
    NAVAL_PATROL("Naval Patrol", "Military vessels enforcing maritime law",
                 0.0f, 0.1f, 0.3f, 0.4f, false, true, 7200000L, // 2 hours
                 FalloffPattern.MODERATE, IntensityPattern.CONSTANT, 3.0f, 50.0f, 100.0f,
                 0.0f, 0.0f, false, 0.1f, 0.5f),
    
    SMUGGLER_HIDEOUT("Smuggler Hideout", "Hidden base used by smugglers and criminals",
                     0.2f, 0.1f, 0.4f, 0.5f, false, false, -1L, // Permanent
                     FalloffPattern.SHARP, IntensityPattern.CONSTANT, 0.0f, 20.0f, 50.0f,
                     0.0f, 0.0f, false, 0.3f, 0.7f),
    
    // Environmental hazards
    TOXIC_ALGAE("Toxic Algae Bloom", "Poisonous algae that contaminates water and air",
                0.5f, 0.3f, 0.2f, 0.6f, false, true, 14400000L, // 4 hours
                FalloffPattern.GRADUAL, IntensityPattern.DECLINING, 0.0f, 150.0f, 400.0f,
                0.0f, 0.0f, true, 0.7f, 0.4f),
    
    VOLCANIC_ACTIVITY("Volcanic Activity", "Underwater volcanic vents creating dangerous conditions",
                      0.4f, 0.5f, 0.6f, 0.8f, false, false, -1L, // Permanent
                      FalloffPattern.SHARP, IntensityPattern.FLUCTUATING, 0.0f, 40.0f, 120.0f,
                      0.0f, 0.0f, false, 0.3f, 0.9f),
    
    DEAD_ZONE("Dead Zone", "Area devoid of marine life with stagnant, lifeless water",
              0.6f, 0.4f, 0.1f, 0.7f, false, false, -1L, // Permanent
              FalloffPattern.MODERATE, IntensityPattern.CONSTANT, 0.0f, 200.0f, 800.0f,
              0.0f, 0.0f, true, 0.8f, 0.3f);
    
    private final String displayName;
    private final String description;
    private final float baseVisibilityReduction;
    private final float baseSpeedReduction;
    private final float baseDamageRisk;
    private final float baseNavigationDifficulty;
    private final boolean isPermanent;
    private final boolean isMoving;
    private final long baseDuration; // In milliseconds, -1 for permanent
    private final FalloffPattern falloffPattern;
    private final IntensityPattern intensityPattern;
    private final float movementSpeed;
    private final float minRadius;
    private final float maxRadius;
    private final float rotationSpeed;
    private final float growthRate;
    private final boolean weatherSensitive;
    private final float weatherSensitivity;
    private final float detectionDifficulty;
    
    HazardType(String displayName, String description, float baseVisibilityReduction,
               float baseSpeedReduction, float baseDamageRisk, float baseNavigationDifficulty,
               boolean isPermanent, boolean isMoving, long baseDuration,
               FalloffPattern falloffPattern, IntensityPattern intensityPattern,
               float movementSpeed, float minRadius, float maxRadius,
               float rotationSpeed, float growthRate, boolean weatherSensitive,
               float weatherSensitivity, float detectionDifficulty) {
        this.displayName = displayName;
        this.description = description;
        this.baseVisibilityReduction = baseVisibilityReduction;
        this.baseSpeedReduction = baseSpeedReduction;
        this.baseDamageRisk = baseDamageRisk;
        this.baseNavigationDifficulty = baseNavigationDifficulty;
        this.isPermanent = isPermanent;
        this.isMoving = isMoving;
        this.baseDuration = baseDuration;
        this.falloffPattern = falloffPattern;
        this.intensityPattern = intensityPattern;
        this.movementSpeed = movementSpeed;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
        this.rotationSpeed = rotationSpeed;
        this.growthRate = growthRate;
        this.weatherSensitive = weatherSensitive;
        this.weatherSensitivity = weatherSensitivity;
        this.detectionDifficulty = detectionDifficulty;
    }
    
    /**
     * Gets the rarity of this hazard type
     */
    public HazardRarity getRarity() {
        switch (this) {
            case GHOST_SHIP:
            case SIREN_WATERS:
            case CURSED_WATERS:
                return HazardRarity.LEGENDARY;
            case VOLCANIC_ACTIVITY:
            case TOXIC_ALGAE:
            case PIRATE_AMBUSH:
                return HazardRarity.RARE;
            case WHIRLPOOL:
            case ICE_FIELD:
            case SMUGGLER_HIDEOUT:
            case DEAD_ZONE:
                return HazardRarity.UNCOMMON;
            default:
                return HazardRarity.COMMON;
        }
    }
    
    /**
     * Gets the threat level of this hazard type
     */
    public ThreatLevel getThreatLevel() {
        float totalThreat = baseDamageRisk + baseNavigationDifficulty + baseSpeedReduction;
        
        if (totalThreat >= 2.0f) return ThreatLevel.EXTREME;
        if (totalThreat >= 1.5f) return ThreatLevel.HIGH;
        if (totalThreat >= 1.0f) return ThreatLevel.MODERATE;
        if (totalThreat >= 0.5f) return ThreatLevel.LOW;
        return ThreatLevel.MINIMAL;
    }
    
    /**
     * Checks if this hazard type is visually detectable
     */
    public boolean isVisuallyDetectable() {
        switch (this) {
            case SIREN_WATERS:
            case CURSED_WATERS:
            case GHOST_SHIP:
                return false; // Supernatural hazards may not be visible
            case SHALLOW_WATERS:
            case REEF:
                return true; // Always visible in good conditions
            default:
                return baseVisibilityReduction < 0.8f; // Visible if not too obscuring
        }
    }
    
    /**
     * Checks if this hazard type is detectable by sonar
     */
    public boolean isSonarDetectable() {
        switch (this) {
            case REEF:
            case SHALLOW_WATERS:
            case ICE_FIELD:
            case WHIRLPOOL:
                return true; // Physical features show up on sonar
            case FOG_BANK:
            case STORM_SYSTEM:
            case TOXIC_ALGAE:
                return false; // Weather/atmospheric phenomena
            case SIREN_WATERS:
            case CURSED_WATERS:
            case GHOST_SHIP:
                return false; // Supernatural phenomena
            default:
                return true;
        }
    }
    
    /**
     * Checks if this hazard type is detectable by magical means
     */
    public boolean isMagicallyDetectable() {
        switch (this) {
            case SIREN_WATERS:
            case CURSED_WATERS:
            case GHOST_SHIP:
                return true; // Supernatural hazards are magically detectable
            case VOLCANIC_ACTIVITY:
            case DEAD_ZONE:
                return true; // Strong magical signatures
            default:
                return false;
        }
    }
    
    /**
     * Gets the required magic level to detect this hazard
     */
    public int getRequiredMagicLevel() {
        if (!isMagicallyDetectable()) return Integer.MAX_VALUE;
        
        switch (this) {
            case GHOST_SHIP:
            case CURSED_WATERS:
                return 8;
            case SIREN_WATERS:
                return 6;
            case VOLCANIC_ACTIVITY:
                return 4;
            case DEAD_ZONE:
                return 3;
            default:
                return 5;
        }
    }
    
    /**
     * Gets the required experience level to detect this hazard by experience
     */
    public int getRequiredExperienceLevel() {
        switch (getRarity()) {
            case LEGENDARY:
                return 9;
            case RARE:
                return 7;
            case UNCOMMON:
                return 5;
            case COMMON:
                return 3;
            default:
                return 5;
        }
    }
    
    /**
     * Checks if this hazard can interact with another hazard type
     */
    public boolean canInteractWith(HazardType other) {
        // Storm systems can enhance other hazards
        if (this == STORM_SYSTEM) {
            return other == WHIRLPOOL || other == FOG_BANK || other == TOXIC_ALGAE;
        }
        
        // Fog can conceal other hazards
        if (this == FOG_BANK) {
            return other == PIRATE_AMBUSH || other == REEF || other == SHALLOW_WATERS;
        }
        
        // Supernatural hazards can interact
        if (isSupernatural() && other.isSupernatural()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Checks if this is a supernatural hazard
     */
    public boolean isSupernatural() {
        switch (this) {
            case SIREN_WATERS:
            case CURSED_WATERS:
            case GHOST_SHIP:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Checks if this is a natural hazard
     */
    public boolean isNatural() {
        switch (this) {
            case STORM_SYSTEM:
            case WHIRLPOOL:
            case FOG_BANK:
            case ICE_FIELD:
            case REEF:
            case SHALLOW_WATERS:
            case TOXIC_ALGAE:
            case VOLCANIC_ACTIVITY:
            case DEAD_ZONE:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Checks if this is a human-made hazard
     */
    public boolean isHumanMade() {
        switch (this) {
            case PIRATE_AMBUSH:
            case NAVAL_PATROL:
            case SMUGGLER_HIDEOUT:
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Gets the formation probability based on environmental conditions
     */
    public float getFormationProbability(float depth, float temperature, float salinity, 
                                       WeatherCondition weather, float humanActivity) {
        float probability = 0.1f; // Base probability
        
        switch (this) {
            case STORM_SYSTEM:
                if (weather != null && weather.getPressure() < 1000.0f) {
                    probability *= 3.0f;
                }
                break;
                
            case WHIRLPOOL:
                if (depth > 100.0f && Math.abs(temperature - 15.0f) < 5.0f) {
                    probability *= 2.0f;
                }
                break;
                
            case FOG_BANK:
                if (weather != null && weather.getHumidity() > 0.8f && weather.getWindSpeed() < 10.0f) {
                    probability *= 4.0f;
                }
                break;
                
            case ICE_FIELD:
                if (temperature < 2.0f) {
                    probability *= 5.0f;
                } else {
                    probability = 0.0f;
                }
                break;
                
            case REEF:
                if (depth < 50.0f && temperature > 20.0f) {
                    probability *= 2.0f;
                }
                break;
                
            case PIRATE_AMBUSH:
                probability *= humanActivity * 2.0f;
                break;
                
            case TOXIC_ALGAE:
                if (temperature > 25.0f && salinity < 30.0f) {
                    probability *= 3.0f;
                }
                break;
        }
        
        return Math.min(1.0f, probability * getRarity().getFormationMultiplier());
    }
    
    /**
     * Gets the warning message for this hazard type
     */
    public String getWarningMessage() {
        switch (this) {
            case STORM_SYSTEM:
                return "WARNING: Severe weather system detected ahead. {severity} storm conditions expected.";
            case WHIRLPOOL:
                return "DANGER: {severity} whirlpool '{name}' detected. Maintain safe distance.";
            case FOG_BANK:
                return "CAUTION: Dense fog ahead reducing visibility. Navigate with extreme care.";
            case ICE_FIELD:
                return "WARNING: Ice field detected. Risk of hull damage from ice collision.";
            case REEF:
                return "NAVIGATION HAZARD: Coral reef '{name}' ahead. Shallow water danger.";
            case PIRATE_AMBUSH:
                return "SECURITY ALERT: Suspicious activity detected. Possible pirate presence.";
            case SIREN_WATERS:
                return "SUPERNATURAL WARNING: Enchanted waters ahead. Crew may be affected by siren song.";
            case GHOST_SHIP:
                return "PARANORMAL ALERT: Spectral vessel sighted. Approach with extreme caution.";
            default:
                return "HAZARD ALERT: {severity} {name} detected ahead. Exercise caution.";
        }
    }
    
    /**
     * Gets the color representation for UI display
     */
    public String getColor() {
        switch (getThreatLevel()) {
            case EXTREME:
                return "#8B0000"; // Dark red
            case HIGH:
                return "#FF4500"; // Orange red
            case MODERATE:
                return "#FFD700"; // Gold
            case LOW:
                return "#32CD32"; // Lime green
            case MINIMAL:
                return "#87CEEB"; // Sky blue
            default:
                return "#808080"; // Gray
        }
    }
    
    /**
     * Gets the icon representation for UI display
     */
    public String getIcon() {
        switch (this) {
            case STORM_SYSTEM:
                return "⛈️";
            case WHIRLPOOL:
                return "🌀";
            case FOG_BANK:
                return "🌫️";
            case ICE_FIELD:
                return "🧊";
            case REEF:
                return "🪸";
            case SHALLOW_WATERS:
                return "🏖️";
            case SIREN_WATERS:
                return "🧜‍♀️";
            case CURSED_WATERS:
                return "☠️";
            case GHOST_SHIP:
                return "👻";
            case PIRATE_AMBUSH:
                return "🏴‍☠️";
            case NAVAL_PATROL:
                return "⚓";
            case SMUGGLER_HIDEOUT:
                return "🕳️";
            case TOXIC_ALGAE:
                return "☣️";
            case VOLCANIC_ACTIVITY:
                return "🌋";
            case DEAD_ZONE:
                return "💀";
            default:
                return "⚠️";
        }
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public float getBaseVisibilityReduction() { return baseVisibilityReduction; }
    public float getBaseSpeedReduction() { return baseSpeedReduction; }
    public float getBaseDamageRisk() { return baseDamageRisk; }
    public float getBaseNavigationDifficulty() { return baseNavigationDifficulty; }
    public boolean isPermanent() { return isPermanent; }
    public boolean isMoving() { return isMoving; }
    public long getBaseDuration() { return baseDuration; }
    public FalloffPattern getFalloffPattern() { return falloffPattern; }
    public IntensityPattern getIntensityPattern() { return intensityPattern; }
    public float getMovementSpeed() { return movementSpeed; }
    public float getMinRadius() { return minRadius; }
    public float getMaxRadius() { return maxRadius; }
    public float getRotationSpeed() { return rotationSpeed; }
    public float getGrowthRate() { return growthRate; }
    public boolean isWeatherSensitive() { return weatherSensitive; }
    public float getWeatherSensitivity() { return weatherSensitivity; }
    public float getDetectionDifficulty() { return detectionDifficulty; }
    
    // Helper enums
    public enum FalloffPattern {
        SHARP {
            @Override
            public float calculateFalloff(float normalizedDistance) {
                return Math.max(0.0f, 1.0f - normalizedDistance * normalizedDistance);
            }
        },
        MODERATE {
            @Override
            public float calculateFalloff(float normalizedDistance) {
                return Math.max(0.0f, 1.0f - normalizedDistance);
            }
        },
        GRADUAL {
            @Override
            public float calculateFalloff(float normalizedDistance) {
                return Math.max(0.0f, 1.0f - (float) Math.sqrt(normalizedDistance));
            }
        };
        
        public abstract float calculateFalloff(float normalizedDistance);
    }
    
    public enum IntensityPattern {
        CONSTANT, GROWING, DECLINING, PEAK_MIDDLE, FLUCTUATING
    }
    
    public enum HazardRarity {
        COMMON("Common", 1.0f),
        UNCOMMON("Uncommon", 0.5f),
        RARE("Rare", 0.2f),
        LEGENDARY("Legendary", 0.05f);
        
        private final String displayName;
        private final float formationMultiplier;
        
        HazardRarity(String displayName, float formationMultiplier) {
            this.displayName = displayName;
            this.formationMultiplier = formationMultiplier;
        }
        
        public String getDisplayName() { return displayName; }
        public float getFormationMultiplier() { return formationMultiplier; }
    }
    
    public enum ThreatLevel {
        MINIMAL("Minimal Threat"),
        LOW("Low Threat"),
        MODERATE("Moderate Threat"),
        HIGH("High Threat"),
        EXTREME("Extreme Threat");
        
        private final String displayName;
        
        ThreatLevel(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
}