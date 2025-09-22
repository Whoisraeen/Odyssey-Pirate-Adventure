package com.odyssey.world;

/**
 * Defines different types of structures that can be found on islands
 */
public enum StructureType {
    // Pirate structures
    PIRATE_CAMP("Pirate Camp", "Temporary pirate encampment", StructureCategory.PIRATE, 0.05f, 0.6f, 0.8f),
    PIRATE_HIDEOUT("Pirate Hideout", "Hidden pirate base", StructureCategory.PIRATE, 0.02f, 0.8f, 0.9f),
    PIRATE_FORT("Pirate Fort", "Fortified pirate stronghold", StructureCategory.PIRATE, 0.008f, 0.9f, 1.0f),
    SMUGGLER_CAVE("Smuggler Cave", "Hidden smuggling operation", StructureCategory.PIRATE, 0.03f, 0.7f, 0.7f),
    
    // Shipwrecks
    SHIPWRECK("Shipwreck", "Wrecked sailing vessel", StructureCategory.WRECK, 0.04f, 0.7f, 0.8f),
    GHOST_SHIP("Ghost Ship", "Supernatural haunted vessel", StructureCategory.WRECK, 0.005f, 1.0f, 0.9f),
    MERCHANT_WRECK("Merchant Wreck", "Sunken merchant vessel", StructureCategory.WRECK, 0.03f, 0.6f, 0.9f),
    WARSHIP_WRECK("Warship Wreck", "Destroyed military vessel", StructureCategory.WRECK, 0.015f, 0.8f, 0.7f),
    
    // Caves and underground
    CAVE("Cave", "Natural cave system", StructureCategory.CAVE, 0.08f, 0.5f, 0.6f),
    TREASURE_CAVE("Treasure Cave", "Cave containing treasure", StructureCategory.CAVE, 0.01f, 0.9f, 1.0f),
    CRYSTAL_CAVE("Crystal Cave", "Cave with precious crystals", StructureCategory.CAVE, 0.005f, 0.95f, 0.8f),
    UNDERGROUND_LAKE("Underground Lake", "Subterranean water body", StructureCategory.CAVE, 0.02f, 0.7f, 0.5f),
    LAVA_CAVE("Lava Cave", "Volcanic cave system", StructureCategory.CAVE, 0.012f, 0.8f, 0.4f),
    
    // Settlements
    ABANDONED_VILLAGE("Abandoned Village", "Deserted settlement", StructureCategory.SETTLEMENT, 0.015f, 0.6f, 0.7f),
    FISHING_VILLAGE("Fishing Village", "Small coastal community", StructureCategory.SETTLEMENT, 0.025f, 0.4f, 0.6f),
    TRADING_POST("Trading Post", "Commercial outpost", StructureCategory.SETTLEMENT, 0.02f, 0.5f, 0.8f),
    NATIVE_VILLAGE("Native Village", "Indigenous settlement", StructureCategory.SETTLEMENT, 0.018f, 0.7f, 0.5f),
    
    // Military structures
    WATCHTOWER("Watchtower", "Coastal observation tower", StructureCategory.MILITARY, 0.03f, 0.5f, 0.4f),
    FORTRESS("Fortress", "Large military fortification", StructureCategory.MILITARY, 0.008f, 0.9f, 0.6f),
    CANNON_BATTERY("Cannon Battery", "Coastal defense position", StructureCategory.MILITARY, 0.02f, 0.6f, 0.5f),
    SIGNAL_TOWER("Signal Tower", "Communication beacon", StructureCategory.MILITARY, 0.025f, 0.4f, 0.3f),
    
    // Religious structures
    TEMPLE("Temple", "Ancient temple ruins", StructureCategory.RELIGIOUS, 0.01f, 0.8f, 0.7f),
    SHRINE("Shrine", "Sacred shrine", StructureCategory.RELIGIOUS, 0.04f, 0.6f, 0.5f),
    MONASTERY("Monastery", "Religious retreat", StructureCategory.RELIGIOUS, 0.005f, 0.9f, 0.4f),
    GRAVEYARD("Graveyard", "Ancient burial ground", StructureCategory.RELIGIOUS, 0.02f, 0.7f, 0.3f),
    
    // Mysterious structures
    ANCIENT_RUINS("Ancient Ruins", "Mysterious ancient structure", StructureCategory.MYSTERIOUS, 0.008f, 0.95f, 0.8f),
    STONE_CIRCLE("Stone Circle", "Mystical stone formation", StructureCategory.MYSTERIOUS, 0.012f, 0.8f, 0.6f),
    OBELISK("Obelisk", "Towering stone monument", StructureCategory.MYSTERIOUS, 0.006f, 0.9f, 0.7f),
    PORTAL_RUINS("Portal Ruins", "Remnants of magical portal", StructureCategory.MYSTERIOUS, 0.002f, 1.0f, 0.9f),
    
    // Utility structures
    LIGHTHOUSE("Lighthouse", "Navigation beacon", StructureCategory.UTILITY, 0.015f, 0.6f, 0.8f),
    DOCK("Dock", "Small harbor structure", StructureCategory.UTILITY, 0.04f, 0.3f, 0.7f),
    WINDMILL("Windmill", "Wind-powered mill", StructureCategory.UTILITY, 0.02f, 0.4f, 0.5f),
    WELL("Well", "Freshwater well", StructureCategory.UTILITY, 0.06f, 0.3f, 0.6f);
    
    private final String displayName;
    private final String description;
    private final StructureCategory category;
    private final float spawnChance;
    private final float rarityValue;
    private final float resourceValue;
    
    StructureType(String displayName, String description, StructureCategory category,
                  float spawnChance, float rarityValue, float resourceValue) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.spawnChance = spawnChance;
        this.rarityValue = rarityValue;
        this.resourceValue = resourceValue;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public StructureCategory getCategory() { return category; }
    public float getSpawnChance() { return spawnChance; }
    public float getRarityValue() { return rarityValue; }
    public float getResourceValue() { return resourceValue; }
    
    // Utility methods
    public boolean isPirate() { return category == StructureCategory.PIRATE; }
    public boolean isWreck() { return category == StructureCategory.WRECK; }
    public boolean isCave() { return category == StructureCategory.CAVE; }
    public boolean isSettlement() { return category == StructureCategory.SETTLEMENT; }
    public boolean isMilitary() { return category == StructureCategory.MILITARY; }
    public boolean isReligious() { return category == StructureCategory.RELIGIOUS; }
    public boolean isMysterious() { return category == StructureCategory.MYSTERIOUS; }
    public boolean isUtility() { return category == StructureCategory.UTILITY; }
    
    public boolean requiresCoast() {
        return this == DOCK || this == LIGHTHOUSE || this == CANNON_BATTERY ||
               isWreck() || this == FISHING_VILLAGE;
    }
    
    public boolean requiresElevation() {
        return this == WATCHTOWER || this == SIGNAL_TOWER || this == LIGHTHOUSE ||
               this == FORTRESS || this == TEMPLE;
    }
    
    public boolean isUnderground() {
        return category == StructureCategory.CAVE || this == SMUGGLER_CAVE;
    }
    
    public boolean isRare() {
        return rarityValue > 0.8f;
    }
    
    public boolean providesResources() {
        return resourceValue > 0.6f;
    }
    
    public boolean isHostile() {
        return isPirate() || this == FORTRESS || this == CANNON_BATTERY;
    }
    
    /**
     * Check if this structure type is underwater
     * @return true if the structure is typically found underwater
     */
    public boolean isUnderwater() {
        return isWreck() || this == UNDERGROUND_LAKE;
    }
    
    /**
     * Get the minimum distance this structure should be from other structures
     * @return minimum distance in world units
     */
    public float getMinimumDistance() {
        switch (getSize()) {
            case LARGE:
                return 200.0f;
            case MEDIUM:
                return 100.0f;
            case SMALL:
            default:
                return 50.0f;
        }
    }
    
    public boolean isSafe() {
        return isSettlement() || isReligious() || isUtility();
    }
    
    /**
     * Gets the exploration reward for discovering this structure
     */
    public int getExplorationReward() {
        return (int) (rarityValue * resourceValue * 200);
    }
    
    /**
     * Gets the minimum distance from other structures
     */
    public float getMinStructureDistance() {
        switch (category) {
            case PIRATE: return 100.0f;
            case WRECK: return 50.0f;
            case CAVE: return 80.0f;
            case SETTLEMENT: return 150.0f;
            case MILITARY: return 120.0f;
            case RELIGIOUS: return 100.0f;
            case MYSTERIOUS: return 200.0f;
            case UTILITY: return 60.0f;
            default: return 80.0f;
        }
    }
    
    /**
     * Gets the size category of this structure
     */
    public StructureSize getSize() {
        if (this == PIRATE_FORT || this == FORTRESS || this == MONASTERY || 
            this == ANCIENT_RUINS || this == PORTAL_RUINS) {
            return StructureSize.LARGE;
        } else if (this == PIRATE_HIDEOUT || this == TREASURE_CAVE || 
                   this == ABANDONED_VILLAGE || this == TEMPLE || this == LIGHTHOUSE) {
            return StructureSize.MEDIUM;
        } else {
            return StructureSize.SMALL;
        }
    }
    
    /**
     * Gets the danger level of this structure
     */
    public DangerLevel getDangerLevel() {
        if (isHostile()) {
            return DangerLevel.HIGH;
        } else if (this == GHOST_SHIP || isMysterious() || this == GRAVEYARD) {
            return DangerLevel.MEDIUM;
        } else if (isSafe()) {
            return DangerLevel.LOW;
        } else {
            return DangerLevel.MEDIUM;
        }
    }
    
    /**
     * Structure categories for organization and spawning rules
     */
    public enum StructureCategory {
        PIRATE("Pirate"),
        WRECK("Wreck"),
        CAVE("Cave"),
        SETTLEMENT("Settlement"),
        MILITARY("Military"),
        RELIGIOUS("Religious"),
        MYSTERIOUS("Mysterious"),
        UTILITY("Utility");
        
        private final String displayName;
        
        StructureCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Structure size categories
     */
    public enum StructureSize {
        SMALL("Small", 10.0f),
        MEDIUM("Medium", 25.0f),
        LARGE("Large", 50.0f);
        
        private final String displayName;
        private final float radius;
        
        StructureSize(String displayName, float radius) {
            this.displayName = displayName;
            this.radius = radius;
        }
        
        public String getDisplayName() { return displayName; }
        public float getRadius() { return radius; }
    }
    
    /**
     * Danger level categories
     */
    public enum DangerLevel {
        LOW("Low", 0.1f),
        MEDIUM("Medium", 0.5f),
        HIGH("High", 0.9f),
        EXTREME("Extreme", 1.0f);
        
        private final String displayName;
        private final float threatLevel;
        
        DangerLevel(String displayName, float threatLevel) {
            this.displayName = displayName;
            this.threatLevel = threatLevel;
        }
        
        public String getDisplayName() { return displayName; }
        public float getThreatLevel() { return threatLevel; }
    }
}