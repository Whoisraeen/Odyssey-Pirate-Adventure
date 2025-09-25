package com.odyssey.faction;

/**
 * Defines the major factions in the Odyssey world.
 * Each faction has unique characteristics, goals, and relationships with other factions.
 */
public enum FactionType {
    
    // Major Naval Powers
    ROYAL_NAVY("Royal Navy", "The official naval force maintaining order on the seas",
               FactionCategory.MILITARY, FactionAlignment.LAWFUL_GOOD, 1000, 500,
               new String[]{"Naval Base", "Port Town", "Colonial Settlement"},
               new String[]{"Piracy", "Smuggling", "Rebellion"}),
    
    IMPERIAL_FLEET("Imperial Fleet", "Elite naval forces of the great empire",
                   FactionCategory.MILITARY, FactionAlignment.LAWFUL_NEUTRAL, 800, 600,
                   new String[]{"Imperial Port", "Fortress", "Trading Hub"},
                   new String[]{"Piracy", "Contraband", "Espionage"}),
    
    // Pirate Factions
    CRIMSON_CORSAIRS("Crimson Corsairs", "Notorious pirate confederation known for their red sails",
                     FactionCategory.PIRATE, FactionAlignment.CHAOTIC_NEUTRAL, 600, 300,
                     new String[]{"Pirate Haven", "Hidden Cove", "Smuggler's Den"},
                     new String[]{"Naval Authority", "Merchant Exploitation", "Treasure Hunting"}),
    
    BLACKWATER_BROTHERHOOD("Blackwater Brotherhood", "Ruthless pirates who show no mercy",
                           FactionCategory.PIRATE, FactionAlignment.CHAOTIC_EVIL, 400, 200,
                           new String[]{"Pirate Stronghold", "Lawless Port", "Raider Camp"},
                           new String[]{"Murder", "Slavery", "Destruction"}),
    
    STORM_RIDERS("Storm Riders", "Elite pirates who sail through the worst weather",
                 FactionCategory.PIRATE, FactionAlignment.CHAOTIC_NEUTRAL, 300, 400,
                 new String[]{"Storm Harbor", "Weather Station", "Cliff Base"},
                 new String[]{"Weather Mastery", "Naval Combat", "Freedom"}),
    
    // Merchant Guilds
    FREE_TRADERS_GUILD("Free Traders Guild", "Independent merchants promoting free trade",
                       FactionCategory.MERCHANT, FactionAlignment.NEUTRAL_GOOD, 1200, 800,
                       new String[]{"Trading Post", "Market Square", "Merchant Quarter"},
                       new String[]{"Fair Trade", "Economic Growth", "Safe Passage"}),
    
    GOLDEN_COMPASS_COMPANY("Golden Compass Company", "Wealthy trading corporation",
                           FactionCategory.MERCHANT, FactionAlignment.LAWFUL_NEUTRAL, 1500, 1000,
                           new String[]{"Corporate Office", "Luxury Market", "Banking Center"},
                           new String[]{"Profit", "Monopoly", "Expansion"}),
    
    EASTERN_SPICE_CONSORTIUM("Eastern Spice Consortium", "Exotic goods traders from distant lands",
                             FactionCategory.MERCHANT, FactionAlignment.TRUE_NEUTRAL, 800, 600,
                             new String[]{"Spice Market", "Exotic Bazaar", "Cultural Exchange"},
                             new String[]{"Rare Goods", "Cultural Exchange", "Exploration"}),
    
    // Islander Communities
    ISLANDER_VILLAGERS("Islander Villagers", "Peaceful native communities living on remote islands",
                       FactionCategory.CIVILIAN, FactionAlignment.NEUTRAL_GOOD, 400, 200,
                       new String[]{"Native Village", "Fishing Village", "Sacred Grove"},
                       new String[]{"Harmony", "Tradition", "Protection of Home"}),
    
    REEF_GUARDIANS("Reef Guardians", "Island warriors protecting their coral territories",
                   FactionCategory.CIVILIAN, FactionAlignment.LAWFUL_GOOD, 300, 400,
                   new String[]{"Coral Fortress", "Sacred Reef", "Guardian Outpost"},
                   new String[]{"Environmental Protection", "Ancestral Duty", "Marine Life"}),
    
    // Specialized Groups
    TREASURE_HUNTERS_SOCIETY("Treasure Hunters Society", "Professional treasure seekers and archaeologists",
                             FactionCategory.EXPLORER, FactionAlignment.CHAOTIC_GOOD, 500, 300,
                             new String[]{"Explorer's Lodge", "Ancient Ruins", "Research Station"},
                             new String[]{"Discovery", "Knowledge", "Adventure"}),
    
    SMUGGLERS_NETWORK("Smugglers Network", "Underground network of contraband traders",
                      FactionCategory.CRIMINAL, FactionAlignment.CHAOTIC_NEUTRAL, 400, 500,
                      new String[]{"Hidden Cache", "Secret Tunnel", "Black Market"},
                      new String[]{"Profit", "Secrecy", "Avoiding Authority"}),
    
    MYSTIC_ORDER("Mystic Order", "Mysterious group studying supernatural phenomena",
                 FactionCategory.MYSTICAL, FactionAlignment.TRUE_NEUTRAL, 200, 600,
                 new String[]{"Ancient Temple", "Mystical Grove", "Observatory"},
                 new String[]{"Knowledge", "Balance", "Supernatural Understanding"}),
    
    // Government Bodies
    COLONIAL_ADMINISTRATION("Colonial Administration", "Government officials managing colonial territories",
                            FactionCategory.GOVERNMENT, FactionAlignment.LAWFUL_NEUTRAL, 800, 400,
                            new String[]{"Government House", "Colonial Office", "Administrative Center"},
                            new String[]{"Order", "Taxation", "Colonial Expansion"}),
    
    HARBOR_AUTHORITY("Harbor Authority", "Officials managing ports and maritime regulations",
                     FactionCategory.GOVERNMENT, FactionAlignment.LAWFUL_GOOD, 600, 300,
                     new String[]{"Harbor Master Office", "Customs House", "Port Authority"},
                     new String[]{"Maritime Law", "Trade Regulation", "Port Safety"});
    
    private final String displayName;
    private final String description;
    private final FactionCategory category;
    private final FactionAlignment alignment;
    private final int maxReputation;
    private final int minReputation;
    private final String[] territories;
    private final String[] interests;
    
    FactionType(String displayName, String description, FactionCategory category, 
                FactionAlignment alignment, int maxReputation, int minReputation,
                String[] territories, String[] interests) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.alignment = alignment;
        this.maxReputation = maxReputation;
        this.minReputation = -minReputation; // Negative for minimum
        this.territories = territories;
        this.interests = interests;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public FactionCategory getCategory() { return category; }
    public FactionAlignment getAlignment() { return alignment; }
    public int getMaxReputation() { return maxReputation; }
    public int getMinReputation() { return minReputation; }
    public String[] getTerritories() { return territories; }
    public String[] getInterests() { return interests; }
    
    /**
     * Gets the base relationship modifier with another faction
     */
    public float getBaseRelationship(FactionType other) {
        if (this == other) return 1.0f;
        
        // Same category factions are generally neutral to friendly
        if (this.category == other.category) {
            return 0.2f;
        }
        
        // Specific faction relationships
        switch (this) {
            case ROYAL_NAVY:
            case IMPERIAL_FLEET:
                if (other.category == FactionCategory.PIRATE) return -0.8f;
                if (other.category == FactionCategory.CRIMINAL) return -0.6f;
                if (other.category == FactionCategory.MERCHANT) return 0.4f;
                break;
                
            case CRIMSON_CORSAIRS:
            case BLACKWATER_BROTHERHOOD:
            case STORM_RIDERS:
                if (other.category == FactionCategory.MILITARY) return -0.8f;
                if (other.category == FactionCategory.MERCHANT) return -0.4f;
                if (other == SMUGGLERS_NETWORK) return 0.6f;
                break;
                
            case FREE_TRADERS_GUILD:
            case GOLDEN_COMPASS_COMPANY:
                if (other.category == FactionCategory.PIRATE) return -0.6f;
                if (other.category == FactionCategory.MILITARY) return 0.3f;
                break;
                
            case ISLANDER_VILLAGERS:
            case REEF_GUARDIANS:
                if (other.category == FactionCategory.PIRATE) return -0.5f;
                if (other == TREASURE_HUNTERS_SOCIETY) return -0.3f;
                break;
        }
        
        return 0.0f; // Neutral by default
    }
    
    /**
     * Gets the reputation threshold for different relationship levels
     */
    public int getReputationThreshold(ReputationLevel level) {
        switch (level) {
            case HOSTILE: return minReputation;
            case UNFRIENDLY: return minReputation / 2;
            case NEUTRAL: return 0;
            case FRIENDLY: return maxReputation / 3;
            case ALLIED: return (maxReputation * 2) / 3;
            case REVERED: return maxReputation;
            default: return 0;
        }
    }
    
    /**
     * Faction categories for grouping related factions
     */
    public enum FactionCategory {
        MILITARY("Military"),
        PIRATE("Pirate"),
        MERCHANT("Merchant"),
        CIVILIAN("Civilian"),
        CRIMINAL("Criminal"),
        EXPLORER("Explorer"),
        MYSTICAL("Mystical"),
        GOVERNMENT("Government");
        
        private final String displayName;
        
        FactionCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
    
    /**
     * Faction alignments affecting behavior and relationships
     */
    public enum FactionAlignment {
        LAWFUL_GOOD("Lawful Good"),
        NEUTRAL_GOOD("Neutral Good"),
        CHAOTIC_GOOD("Chaotic Good"),
        LAWFUL_NEUTRAL("Lawful Neutral"),
        TRUE_NEUTRAL("True Neutral"),
        CHAOTIC_NEUTRAL("Chaotic Neutral"),
        LAWFUL_EVIL("Lawful Evil"),
        NEUTRAL_EVIL("Neutral Evil"),
        CHAOTIC_EVIL("Chaotic Evil");
        
        private final String displayName;
        
        FactionAlignment(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() { return displayName; }
    }
}