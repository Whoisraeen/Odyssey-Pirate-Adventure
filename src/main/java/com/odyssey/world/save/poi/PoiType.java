package com.odyssey.world.save.poi;

/**
 * Defines different types of Points of Interest in the maritime world.
 * Each POI type has specific properties and behaviors for gameplay mechanics.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public enum PoiType {
    // Maritime POIs
    PORT("port", "Trading Port", 64, true, true),
    HARBOR("harbor", "Natural Harbor", 32, false, true),
    LIGHTHOUSE("lighthouse", "Lighthouse", 128, false, false),
    SHIPWRECK("shipwreck", "Shipwreck", 16, false, false),
    TREASURE_ISLAND("treasure_island", "Treasure Island", 48, false, false),
    PIRATE_HIDEOUT("pirate_hideout", "Pirate Hideout", 32, true, true),
    
    // Trading POIs
    TRADING_POST("trading_post", "Trading Post", 24, true, false),
    MARKET("market", "Market", 32, true, false),
    WAREHOUSE("warehouse", "Warehouse", 16, true, false),
    
    // Resource POIs
    FISHING_SPOT("fishing_spot", "Fishing Spot", 16, false, false),
    KELP_FOREST("kelp_forest", "Kelp Forest", 24, false, false),
    CORAL_REEF("coral_reef", "Coral Reef", 20, false, false),
    DEEP_TRENCH("deep_trench", "Deep Ocean Trench", 32, false, false),
    
    // Settlement POIs
    VILLAGE("village", "Coastal Village", 48, true, true),
    TOWN("town", "Port Town", 64, true, true),
    CITY("city", "Maritime City", 96, true, true),
    OUTPOST("outpost", "Remote Outpost", 24, true, false),
    
    // Navigation POIs
    BUOY("buoy", "Navigation Buoy", 8, false, false),
    BEACON("beacon", "Signal Beacon", 16, false, false),
    LANDMARK("landmark", "Natural Landmark", 32, false, false),
    
    // Danger POIs
    WHIRLPOOL("whirlpool", "Whirlpool", 24, false, false),
    STORM_CENTER("storm_center", "Storm Center", 48, false, false),
    REEF_HAZARD("reef_hazard", "Dangerous Reef", 16, false, false),
    KRAKEN_LAIR("kraken_lair", "Kraken Lair", 64, false, false),
    
    // Special POIs
    ANCIENT_RUINS("ancient_ruins", "Ancient Ruins", 40, false, false),
    MYSTICAL_SITE("mystical_site", "Mystical Site", 32, false, false),
    PORTAL("portal", "Dimensional Portal", 16, false, false),
    
    // Player-created POIs
    PLAYER_BASE("player_base", "Player Base", 32, true, true),
    PLAYER_DOCK("player_dock", "Player Dock", 16, true, false),
    PLAYER_MARKER("player_marker", "Player Marker", 8, false, false);
    
    private final String id;
    private final String displayName;
    private final int detectionRange;
    private final boolean hasInventory;
    private final boolean canSpawnNpcs;
    
    /**
     * Creates a POI type with the specified properties.
     * 
     * @param id the unique identifier
     * @param displayName the display name
     * @param detectionRange the detection range in blocks
     * @param hasInventory whether this POI type has an inventory
     * @param canSpawnNpcs whether this POI type can spawn NPCs
     */
    PoiType(String id, String displayName, int detectionRange, boolean hasInventory, boolean canSpawnNpcs) {
        this.id = id;
        this.displayName = displayName;
        this.detectionRange = detectionRange;
        this.hasInventory = hasInventory;
        this.canSpawnNpcs = canSpawnNpcs;
    }
    
    /**
     * Gets the unique identifier for this POI type.
     * 
     * @return the POI type ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the display name for this POI type.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the detection range for this POI type.
     * 
     * @return the detection range in blocks
     */
    public int getDetectionRange() {
        return detectionRange;
    }
    
    /**
     * Checks if this POI type has an inventory.
     * 
     * @return true if this POI type has an inventory
     */
    public boolean hasInventory() {
        return hasInventory;
    }
    
    /**
     * Checks if this POI type can spawn NPCs.
     * 
     * @return true if this POI type can spawn NPCs
     */
    public boolean canSpawnNpcs() {
        return canSpawnNpcs;
    }
    
    /**
     * Checks if this is a maritime-specific POI.
     * 
     * @return true if this is a maritime POI
     */
    public boolean isMaritime() {
        return this == PORT || this == HARBOR || this == LIGHTHOUSE || 
               this == SHIPWRECK || this == TREASURE_ISLAND || this == PIRATE_HIDEOUT ||
               this == FISHING_SPOT || this == KELP_FOREST || this == CORAL_REEF ||
               this == DEEP_TRENCH || this == WHIRLPOOL || this == KRAKEN_LAIR;
    }
    
    /**
     * Checks if this is a trading-related POI.
     * 
     * @return true if this is a trading POI
     */
    public boolean isTrading() {
        return this == PORT || this == TRADING_POST || this == MARKET || 
               this == WAREHOUSE || hasInventory;
    }
    
    /**
     * Checks if this is a dangerous POI.
     * 
     * @return true if this is a dangerous POI
     */
    public boolean isDangerous() {
        return this == WHIRLPOOL || this == STORM_CENTER || this == REEF_HAZARD || 
               this == KRAKEN_LAIR || this == PIRATE_HIDEOUT;
    }
    
    /**
     * Checks if this is a navigation aid POI.
     * 
     * @return true if this is a navigation POI
     */
    public boolean isNavigation() {
        return this == LIGHTHOUSE || this == BUOY || this == BEACON || this == LANDMARK;
    }
    
    /**
     * Checks if this is a player-created POI.
     * 
     * @return true if this is a player-created POI
     */
    public boolean isPlayerCreated() {
        return this == PLAYER_BASE || this == PLAYER_DOCK || this == PLAYER_MARKER;
    }
    
    /**
     * Gets a POI type by its ID.
     * 
     * @param id the POI type ID
     * @return the POI type, or null if not found
     */
    public static PoiType fromId(String id) {
        for (PoiType type : values()) {
            if (type.getId().equals(id)) {
                return type;
            }
        }
        return null;
    }
    
    /**
     * Gets all maritime POI types.
     * 
     * @return array of maritime POI types
     */
    public static PoiType[] getMaritimeTypes() {
        return new PoiType[] {
            PORT, HARBOR, LIGHTHOUSE, SHIPWRECK, TREASURE_ISLAND, PIRATE_HIDEOUT,
            FISHING_SPOT, KELP_FOREST, CORAL_REEF, DEEP_TRENCH, WHIRLPOOL, KRAKEN_LAIR
        };
    }
    
    /**
     * Gets all trading POI types.
     * 
     * @return array of trading POI types
     */
    public static PoiType[] getTradingTypes() {
        return new PoiType[] {
            PORT, TRADING_POST, MARKET, WAREHOUSE, VILLAGE, TOWN, CITY
        };
    }
    
    /**
     * Gets all dangerous POI types.
     * 
     * @return array of dangerous POI types
     */
    public static PoiType[] getDangerousTypes() {
        return new PoiType[] {
            WHIRLPOOL, STORM_CENTER, REEF_HAZARD, KRAKEN_LAIR, PIRATE_HIDEOUT
        };
    }
}