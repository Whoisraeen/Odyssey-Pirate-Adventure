package com.odyssey.world;

import com.odyssey.world.weather.WeatherCondition;
import com.odyssey.world.weather.PrecipitationType;

/**
 * Enum defining different types of events that can affect trade routes
 */
public enum TradeEventType {
    
    PIRATE_RAID("Pirate Raid", "Pirates attacking merchant vessels", 0.15f, 24 * 3, true),
    NAVAL_BLOCKADE("Naval Blockade", "Military blockade restricting trade", 0.08f, 24 * 7, true),
    STORM_DAMAGE("Storm Damage", "Weather damage to ports and shipping", 0.20f, 24 * 2, false),
    PLAGUE_OUTBREAK("Plague Outbreak", "Disease outbreak causing quarantine", 0.05f, 24 * 14, true),
    RESOURCE_SHORTAGE("Resource Shortage", "Critical shortage of essential goods", 0.12f, 24 * 5, false),
    MARKET_CRASH("Market Crash", "Economic collapse affecting trade", 0.06f, 24 * 10, false),
    DIPLOMATIC_CRISIS("Diplomatic Crisis", "Political tensions disrupting trade", 0.10f, 24 * 8, true),
    DISCOVERY("New Discovery", "Discovery of new trade opportunities", 0.08f, 24 * 12, false),
    FESTIVAL("Local Festival", "Celebration increasing demand", 0.25f, 24 * 1, false),
    WAR("Military Conflict", "Active warfare making region dangerous", 0.03f, 24 * 30, true);
    
    private final String displayName;
    private final String description;
    private final float baseFrequency; // Probability per day
    private final int baseDurationHours;
    private final boolean requiresTrigger; // Whether event needs specific conditions
    
    TradeEventType(String displayName, String description, float baseFrequency, 
                   int baseDurationHours, boolean requiresTrigger) {
        this.displayName = displayName;
        this.description = description;
        this.baseFrequency = baseFrequency;
        this.baseDurationHours = baseDurationHours;
        this.requiresTrigger = requiresTrigger;
    }
    
    /**
     * Gets the rarity level of this event type
     */
    public EventRarity getRarity() {
        if (baseFrequency >= 0.20f) return EventRarity.COMMON;
        if (baseFrequency >= 0.10f) return EventRarity.UNCOMMON;
        if (baseFrequency >= 0.05f) return EventRarity.RARE;
        return EventRarity.VERY_RARE;
    }
    
    /**
     * Gets the danger level of this event type
     */
    public DangerLevel getDangerLevel() {
        switch (this) {
            case WAR:
            case PLAGUE_OUTBREAK:
                return DangerLevel.EXTREME;
            case PIRATE_RAID:
            case NAVAL_BLOCKADE:
                return DangerLevel.HIGH;
            case STORM_DAMAGE:
            case DIPLOMATIC_CRISIS:
            case RESOURCE_SHORTAGE:
                return DangerLevel.MODERATE;
            case MARKET_CRASH:
                return DangerLevel.LOW;
            case DISCOVERY:
            case FESTIVAL:
                return DangerLevel.NONE;
            default:
                return DangerLevel.LOW;
        }
    }
    
    /**
     * Gets the economic impact level
     */
    public EconomicImpact getEconomicImpact() {
        switch (this) {
            case MARKET_CRASH:
            case WAR:
            case PLAGUE_OUTBREAK:
                return EconomicImpact.MAJOR;
            case NAVAL_BLOCKADE:
            case RESOURCE_SHORTAGE:
            case DIPLOMATIC_CRISIS:
                return EconomicImpact.SIGNIFICANT;
            case PIRATE_RAID:
            case STORM_DAMAGE:
                return EconomicImpact.MODERATE;
            case DISCOVERY:
            case FESTIVAL:
                return EconomicImpact.POSITIVE;
            default:
                return EconomicImpact.MINOR;
        }
    }
    
    /**
     * Checks if this event type can occur in the given season
     */
    public boolean canOccurInSeason(Season season) {
        switch (this) {
            case STORM_DAMAGE:
                return season == Season.AUTUMN || season == Season.WINTER;
            case PLAGUE_OUTBREAK:
                return season == Season.SUMMER || season == Season.AUTUMN;
            case FESTIVAL:
                return season == Season.SPRING || season == Season.SUMMER;
            case PIRATE_RAID:
                return season == Season.SPRING || season == Season.SUMMER || season == Season.AUTUMN;
            default:
                return true; // Most events can occur any time
        }
    }
    
    /**
     * Gets the preferred weather conditions for this event
     */
    public WeatherCondition getPreferredWeather() {
        switch (this) {
            case STORM_DAMAGE:
                return new WeatherCondition(15.0f, 0.9f, 950.0f, 0.3f, 
                                          PrecipitationType.HEAVY_RAIN, 0.8f, 25.0f, 180.0f);
            case PIRATE_RAID:
                return new WeatherCondition(25.0f, 0.4f, 1015.0f, 0.8f, 
                                          PrecipitationType.NONE, 0.2f, 8.0f, 90.0f);
            case FESTIVAL:
                return new WeatherCondition(22.0f, 0.5f, 1020.0f, 0.9f, 
                                          PrecipitationType.NONE, 0.1f, 5.0f, 45.0f);
            default:
                return null; // No specific weather preference
        }
    }
    
    /**
     * Gets the formation probability based on current conditions
     */
    public float getFormationProbability(WeatherCondition weather, Season season, 
                                       float regionStability, float economicHealth) {
        float probability = baseFrequency;
        
        // Season modifier
        if (!canOccurInSeason(season)) {
            probability *= 0.1f;
        }
        
        // Weather modifier
        WeatherCondition preferred = getPreferredWeather();
        if (preferred != null && weather != null) {
            float weatherMatch = calculateWeatherMatch(weather, preferred);
            probability *= (0.5f + weatherMatch * 0.5f);
        }
        
        // Regional stability modifier
        switch (this) {
            case PIRATE_RAID:
            case WAR:
            case DIPLOMATIC_CRISIS:
                probability *= (2.0f - regionStability); // More likely in unstable regions
                break;
            case DISCOVERY:
            case FESTIVAL:
                probability *= regionStability; // More likely in stable regions
                break;
        }
        
        // Economic health modifier
        switch (this) {
            case MARKET_CRASH:
            case RESOURCE_SHORTAGE:
                probability *= (2.0f - economicHealth); // More likely in poor economies
                break;
            case DISCOVERY:
            case FESTIVAL:
                probability *= economicHealth; // More likely in healthy economies
                break;
        }
        
        return Math.max(0.0f, Math.min(1.0f, probability));
    }
    
    private float calculateWeatherMatch(WeatherCondition current, WeatherCondition preferred) {
        float tempMatch = 1.0f - Math.abs(current.getTemperature() - preferred.getTemperature()) / 30.0f;
        float humidityMatch = 1.0f - Math.abs(current.getHumidity() - preferred.getHumidity());
        float precipMatch = current.getPrecipitationType() == preferred.getPrecipitationType() ? 1.0f : 0.5f;
        
        return (tempMatch + humidityMatch + precipMatch) / 3.0f;
    }
    
    /**
     * Gets the minimum reputation required to receive advance warning
     */
    public int getWarningReputationThreshold() {
        switch (this) {
            case PIRATE_RAID:
                return 25; // Need some naval contacts
            case NAVAL_BLOCKADE:
                return 40; // Need good military connections
            case DIPLOMATIC_CRISIS:
                return 50; // Need political connections
            case WAR:
                return 60; // Need high-level intelligence
            case PLAGUE_OUTBREAK:
                return 30; // Medical/port authority contacts
            default:
                return 20; // General merchant network
        }
    }
    
    /**
     * Gets the advance warning time in hours
     */
    public int getAdvanceWarningHours() {
        switch (this) {
            case STORM_DAMAGE:
                return 12; // Weather can be predicted
            case NAVAL_BLOCKADE:
                return 24; // Military operations take time to organize
            case DIPLOMATIC_CRISIS:
                return 48; // Political tensions build over time
            case PLAGUE_OUTBREAK:
                return 6; // Disease spreads quickly but has early signs
            case FESTIVAL:
                return 72; // Festivals are planned in advance
            case WAR:
                return 36; // Military buildup is visible
            default:
                return 3; // Most events have little warning
        }
    }
    
    /**
     * Gets the typical radius of effect in nautical miles
     */
    public float getTypicalRadius() {
        switch (this) {
            case WAR:
                return 200.0f; // Large military operations
            case PLAGUE_OUTBREAK:
                return 150.0f; // Quarantine zones are extensive
            case NAVAL_BLOCKADE:
                return 100.0f; // Blockades cover shipping lanes
            case DIPLOMATIC_CRISIS:
                return 120.0f; // Affects entire regions
            case STORM_DAMAGE:
                return 80.0f; // Weather systems are large
            case MARKET_CRASH:
                return 90.0f; // Economic effects spread
            case RESOURCE_SHORTAGE:
                return 70.0f; // Affects supply networks
            case PIRATE_RAID:
                return 50.0f; // Localized attacks
            case DISCOVERY:
                return 60.0f; // New opportunities attract traders
            case FESTIVAL:
                return 30.0f; // Local celebrations
            default:
                return 50.0f;
        }
    }
    
    /**
     * Checks if this event type can stack with another
     */
    public boolean canStackWith(TradeEventType other) {
        // Some events are mutually exclusive
        if (this == FESTIVAL && other == WAR) return false;
        if (this == WAR && other == FESTIVAL) return false;
        if (this == DISCOVERY && other == MARKET_CRASH) return false;
        if (this == MARKET_CRASH && other == DISCOVERY) return false;
        
        // Most events can coexist
        return true;
    }
    
    /**
     * Gets the faction most likely to be involved in this event
     */
    public String getPrimaryFaction() {
        switch (this) {
            case PIRATE_RAID:
                return "Pirates";
            case NAVAL_BLOCKADE:
            case WAR:
                return "Navy";
            case DIPLOMATIC_CRISIS:
                return "Government";
            case PLAGUE_OUTBREAK:
                return "Medical_Authority";
            case MARKET_CRASH:
            case RESOURCE_SHORTAGE:
                return "Merchants_Guild";
            case DISCOVERY:
                return "Explorers_Guild";
            case FESTIVAL:
                return "Local_Authorities";
            default:
                return null;
        }
    }
    
    /**
     * Gets the color representation for UI display
     */
    public String getColor() {
        switch (getDangerLevel()) {
            case EXTREME:
                return "#8B0000"; // Dark red
            case HIGH:
                return "#FF4500"; // Orange red
            case MODERATE:
                return "#FFD700"; // Gold
            case LOW:
                return "#32CD32"; // Lime green
            case NONE:
                return "#00CED1"; // Dark turquoise
            default:
                return "#808080"; // Gray
        }
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public float getBaseFrequency() { return baseFrequency; }
    public int getBaseDurationHours() { return baseDurationHours; }
    public boolean requiresTrigger() { return requiresTrigger; }
    
    // Helper enums
    public enum EventRarity {
        COMMON("Common"),
        UNCOMMON("Uncommon"),
        RARE("Rare"),
        VERY_RARE("Very Rare");
        
        private final String displayName;
        EventRarity(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    public enum DangerLevel {
        NONE("Safe"),
        LOW("Low Risk"),
        MODERATE("Moderate Risk"),
        HIGH("High Risk"),
        EXTREME("Extreme Risk");
        
        private final String displayName;
        DangerLevel(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    public enum EconomicImpact {
        MAJOR("Major Impact"),
        SIGNIFICANT("Significant Impact"),
        MODERATE("Moderate Impact"),
        MINOR("Minor Impact"),
        POSITIVE("Positive Impact");
        
        private final String displayName;
        EconomicImpact(String displayName) { this.displayName = displayName; }
        public String getDisplayName() { return displayName; }
    }
    
    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }
}