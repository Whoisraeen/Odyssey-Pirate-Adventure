package com.odyssey.world;

import java.util.Arrays;
import java.util.List;

/**
 * Defines different types of trade routes with their characteristics
 */
public enum TradeRouteType {
    
    BASIC("Basic Trade", 0.15f, 0.0f, 0.9f, 0.6f, 
          Arrays.asList("food", "water", "wood", "cotton")),
    
    LUXURY("Luxury Goods", 0.35f, 0.1f, 0.8f, 0.4f,
           Arrays.asList("silk", "spices", "gems", "fine_wine")),
    
    MILITARY("Military Supplies", 0.25f, 0.2f, 0.7f, 0.3f,
             Arrays.asList("weapons", "gunpowder", "armor", "cannons")),
    
    EXOTIC("Exotic Trade", 0.45f, 0.3f, 0.6f, 0.2f,
           Arrays.asList("rare_spices", "precious_metals", "artifacts", "exotic_animals")),
    
    SMUGGLING("Smuggling Route", 0.60f, 0.4f, 0.4f, 0.1f,
              Arrays.asList("contraband", "stolen_goods", "illegal_weapons", "forbidden_books")),
    
    PIRATE("Pirate Route", 0.40f, 0.5f, 0.3f, 0.2f,
           Arrays.asList("plunder", "stolen_treasure", "captured_goods", "black_market_items")),
    
    MERCHANT("Merchant Marine", 0.20f, 0.1f, 0.8f, 0.7f,
             Arrays.asList("manufactured_goods", "textiles", "tools", "household_items")),
    
    COLONIAL("Colonial Supply", 0.18f, 0.15f, 0.75f, 0.5f,
             Arrays.asList("colonial_goods", "raw_materials", "plantation_products", "settlers_supplies")),
    
    TREASURE("Treasure Route", 0.80f, 0.6f, 0.2f, 0.05f,
             Arrays.asList("gold", "silver", "precious_gems", "ancient_artifacts")),
    
    DIPLOMATIC("Diplomatic Mission", 0.10f, 0.05f, 0.95f, 0.8f,
               Arrays.asList("diplomatic_pouches", "gifts", "treaties", "official_documents"));
    
    private final String displayName;
    private final float baseProfitMargin;
    private final float difficultyModifier;
    private final float safetyModifier;
    private final float baseTraffic;
    private final List<String> specialGoods;
    
    TradeRouteType(String displayName, float baseProfitMargin, float difficultyModifier, 
                   float safetyModifier, float baseTraffic, List<String> specialGoods) {
        this.displayName = displayName;
        this.baseProfitMargin = baseProfitMargin;
        this.difficultyModifier = difficultyModifier;
        this.safetyModifier = safetyModifier;
        this.baseTraffic = baseTraffic;
        this.specialGoods = specialGoods;
    }
    
    /**
     * Gets the rarity of this route type (0.0 = common, 1.0 = extremely rare)
     */
    public float getRarity() {
        switch (this) {
            case BASIC: return 0.0f;
            case MERCHANT: return 0.1f;
            case COLONIAL: return 0.2f;
            case LUXURY: return 0.3f;
            case MILITARY: return 0.4f;
            case DIPLOMATIC: return 0.5f;
            case EXOTIC: return 0.6f;
            case PIRATE: return 0.7f;
            case SMUGGLING: return 0.8f;
            case TREASURE: return 0.9f;
            default: return 0.5f;
        }
    }
    
    /**
     * Gets the minimum reputation required to access this route type
     */
    public int getMinimumReputation() {
        switch (this) {
            case BASIC: return 0;
            case MERCHANT: return 100;
            case COLONIAL: return 200;
            case LUXURY: return 300;
            case MILITARY: return 400;
            case DIPLOMATIC: return 500;
            case EXOTIC: return 600;
            case PIRATE: return -200; // Negative reputation (infamy)
            case SMUGGLING: return -100;
            case TREASURE: return 800;
            default: return 0;
        }
    }
    
    /**
     * Gets the faction alignment required for this route type
     */
    public String getRequiredFaction() {
        switch (this) {
            case MILITARY: return "Navy";
            case DIPLOMATIC: return "Government";
            case PIRATE: return "Pirates";
            case SMUGGLING: return "Outlaws";
            case COLONIAL: return "Colonial_Powers";
            default: return "Neutral";
        }
    }
    
    /**
     * Gets the time of day preference for this route type
     */
    public String getTimePreference() {
        switch (this) {
            case SMUGGLING: return "Night";
            case PIRATE: return "Dawn/Dusk";
            case TREASURE: return "Night";
            case DIPLOMATIC: return "Day";
            default: return "Any";
        }
    }
    
    /**
     * Gets the weather preference for this route type
     */
    public String getWeatherPreference() {
        switch (this) {
            case SMUGGLING: return "Stormy";
            case PIRATE: return "Foggy";
            case LUXURY: return "Clear";
            case DIPLOMATIC: return "Clear";
            default: return "Any";
        }
    }
    
    /**
     * Gets the seasonal modifier for this route type
     */
    public float getSeasonalModifier(String season) {
        switch (this) {
            case LUXURY:
                return season.equals("Winter") ? 1.3f : 1.0f; // Higher demand in winter
            case COLONIAL:
                return season.equals("Spring") ? 1.2f : 1.0f; // Planting season
            case PIRATE:
                return season.equals("Summer") ? 1.2f : 0.8f; // More active in summer
            case TREASURE:
                return season.equals("Autumn") ? 1.1f : 1.0f; // Harvest of treasures
            default:
                return 1.0f;
        }
    }
    
    /**
     * Gets the crew morale modifier for this route type
     */
    public float getCrewMoraleModifier() {
        switch (this) {
            case TREASURE: return 1.5f; // Crew excited about treasure
            case LUXURY: return 1.2f; // Comfortable conditions
            case PIRATE: return 1.3f; // Thrilling for pirates
            case SMUGGLING: return 0.8f; // Nervous crew
            case MILITARY: return 1.1f; // Disciplined
            case DIPLOMATIC: return 1.0f; // Professional
            default: return 1.0f;
        }
    }
    
    /**
     * Gets the experience gain modifier for this route type
     */
    public float getExperienceModifier() {
        switch (this) {
            case TREASURE: return 2.0f; // High risk, high reward
            case EXOTIC: return 1.8f; // Learning about new cultures
            case SMUGGLING: return 1.6f; // Dangerous skills
            case PIRATE: return 1.5f; // Combat experience
            case MILITARY: return 1.3f; // Disciplined training
            case DIPLOMATIC: return 1.2f; // Social skills
            case LUXURY: return 1.1f; // Quality experience
            default: return 1.0f;
        }
    }
    
    /**
     * Gets the insurance cost multiplier for this route type
     */
    public float getInsuranceCostMultiplier() {
        switch (this) {
            case TREASURE: return 5.0f; // Extremely high risk
            case SMUGGLING: return 4.0f; // Illegal activities
            case PIRATE: return 3.5f; // Combat likely
            case EXOTIC: return 2.5f; // Unknown dangers
            case MILITARY: return 2.0f; // War zones
            case LUXURY: return 1.5f; // Valuable cargo
            case DIPLOMATIC: return 0.8f; // Protected status
            default: return 1.0f;
        }
    }
    
    /**
     * Gets the patrol encounter chance for this route type
     */
    public float getPatrolEncounterChance() {
        switch (this) {
            case SMUGGLING: return 0.8f; // Heavily patrolled
            case PIRATE: return 0.7f; // Navy hunting pirates
            case TREASURE: return 0.6f; // Attracts attention
            case MILITARY: return 0.3f; // Escorted
            case DIPLOMATIC: return 0.1f; // Safe passage
            default: return 0.4f;
        }
    }
    
    /**
     * Gets the pirate encounter chance for this route type
     */
    public float getPirateEncounterChance() {
        switch (this) {
            case TREASURE: return 0.9f; // Pirates love treasure
            case LUXURY: return 0.7f; // Valuable targets
            case EXOTIC: return 0.6f; // Rare goods attract pirates
            case MERCHANT: return 0.5f; // Standard risk
            case MILITARY: return 0.2f; // Well defended
            case PIRATE: return 0.1f; // Pirates don't attack pirates
            case DIPLOMATIC: return 0.1f; // Protected
            default: return 0.4f;
        }
    }
    
    /**
     * Checks if this route type is legal in the given region
     */
    public boolean isLegalInRegion(String region) {
        switch (this) {
            case SMUGGLING: return false; // Illegal everywhere
            case PIRATE: return region.equals("Pirate_Territory");
            case TREASURE: return !region.equals("Naval_Territory");
            default: return true;
        }
    }
    
    /**
     * Gets the description of this route type
     */
    public String getDescription() {
        switch (this) {
            case BASIC: return "Standard trade routes carrying everyday goods between settlements.";
            case LUXURY: return "High-value routes transporting luxury goods for wealthy clients.";
            case MILITARY: return "Dangerous routes supplying military outposts and fortifications.";
            case EXOTIC: return "Rare trade routes dealing in exotic goods from distant lands.";
            case SMUGGLING: return "Illegal routes avoiding customs and law enforcement.";
            case PIRATE: return "Lawless routes controlled by pirate factions.";
            case MERCHANT: return "Established commercial routes used by merchant guilds.";
            case COLONIAL: return "Supply routes supporting colonial expansion and settlements.";
            case TREASURE: return "Legendary routes rumored to lead to great treasures.";
            case DIPLOMATIC: return "Official routes used for diplomatic missions and state business.";
            default: return "Unknown trade route type.";
        }
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public float getBaseProfitMargin() { return baseProfitMargin; }
    public float getDifficultyModifier() { return difficultyModifier; }
    public float getSafetyModifier() { return safetyModifier; }
    public float getBaseTraffic() { return baseTraffic; }
    public List<String> getSpecialGoods() { return specialGoods; }
    
    @Override
    public String toString() {
        return displayName;
    }
}