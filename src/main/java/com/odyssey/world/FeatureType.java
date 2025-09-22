package com.odyssey.world;

/**
 * Defines different types of natural features that can be found on islands
 */
public enum FeatureType {
    // Vegetation features
    PALM_TREE("Palm Tree", "Tropical palm tree", FeatureCategory.VEGETATION, 0.3f, 0.2f, 0.4f),
    COCONUT_PALM("Coconut Palm", "Palm tree with coconuts", FeatureCategory.VEGETATION, 0.25f, 0.15f, 0.6f),
    TROPICAL_TREE("Tropical Tree", "Dense tropical tree", FeatureCategory.VEGETATION, 0.4f, 0.3f, 0.5f),
    MANGROVE("Mangrove", "Coastal mangrove tree", FeatureCategory.VEGETATION, 0.2f, 0.4f, 0.3f),
    BAMBOO_GROVE("Bamboo Grove", "Cluster of bamboo", FeatureCategory.VEGETATION, 0.6f, 0.1f, 0.2f),
    FERN_PATCH("Fern Patch", "Tropical ferns", FeatureCategory.VEGETATION, 0.8f, 0.05f, 0.1f),
    
    // Geological features
    ROCK_FORMATION("Rock Formation", "Natural rock outcrop", FeatureCategory.GEOLOGICAL, 0.1f, 0.8f, 0.0f),
    BOULDER("Boulder", "Large stone boulder", FeatureCategory.GEOLOGICAL, 0.05f, 0.6f, 0.0f),
    CLIFF_FACE("Cliff Face", "Steep rock cliff", FeatureCategory.GEOLOGICAL, 0.02f, 1.0f, 0.0f),
    CAVE_ENTRANCE("Cave Entrance", "Opening to underground cave", FeatureCategory.GEOLOGICAL, 0.01f, 0.9f, 0.8f),
    HOT_SPRING("Hot Spring", "Natural hot water spring", FeatureCategory.GEOLOGICAL, 0.005f, 0.7f, 0.9f),
    LAVA_TUBE("Lava Tube", "Volcanic lava tube", FeatureCategory.GEOLOGICAL, 0.008f, 0.8f, 0.7f),
    
    // Water features
    FRESHWATER_SPRING("Freshwater Spring", "Natural spring water", FeatureCategory.WATER, 0.03f, 0.5f, 0.8f),
    WATERFALL("Waterfall", "Cascading waterfall", FeatureCategory.WATER, 0.01f, 0.9f, 0.9f),
    TIDAL_POOL("Tidal Pool", "Coastal tidal pool", FeatureCategory.WATER, 0.15f, 0.2f, 0.3f),
    LAGOON("Lagoon", "Shallow coastal lagoon", FeatureCategory.WATER, 0.02f, 0.3f, 0.6f),
    
    // Marine features
    CORAL_REEF("Coral Reef", "Living coral formation", FeatureCategory.MARINE, 0.1f, 0.4f, 0.7f),
    SEAWEED_BED("Seaweed Bed", "Dense seaweed growth", FeatureCategory.MARINE, 0.2f, 0.1f, 0.2f),
    KELP_FOREST("Kelp Forest", "Underwater kelp forest", FeatureCategory.MARINE, 0.05f, 0.3f, 0.4f),
    OYSTER_BED("Oyster Bed", "Natural oyster colony", FeatureCategory.MARINE, 0.08f, 0.2f, 0.5f),
    
    // Treasure features
    TREASURE_CHEST("Treasure Chest", "Buried treasure chest", FeatureCategory.TREASURE, 0.001f, 0.8f, 1.0f),
    BURIED_CACHE("Buried Cache", "Hidden supply cache", FeatureCategory.TREASURE, 0.005f, 0.6f, 0.8f),
    ANCIENT_RUINS("Ancient Ruins", "Mysterious ancient structure", FeatureCategory.TREASURE, 0.002f, 0.9f, 0.9f),
    SHRINE("Shrine", "Sacred shrine or altar", FeatureCategory.TREASURE, 0.003f, 0.7f, 0.7f),
    
    // Wildlife features
    BIRD_NEST("Bird Nest", "Seabird nesting area", FeatureCategory.WILDLIFE, 0.12f, 0.1f, 0.2f),
    TURTLE_NESTING("Turtle Nesting", "Sea turtle nesting beach", FeatureCategory.WILDLIFE, 0.02f, 0.3f, 0.6f),
    CRAB_COLONY("Crab Colony", "Large crab population", FeatureCategory.WILDLIFE, 0.08f, 0.2f, 0.3f),
    
    // Additional features for existing feature classes
    VOLCANIC_CRATER("Volcanic Crater", "Active or dormant volcanic crater", FeatureCategory.GEOLOGICAL, 0.005f, 0.9f, 0.6f),
    SPRING("Spring", "Natural freshwater spring", FeatureCategory.WATER, 0.03f, 0.5f, 0.8f),
    ORE_DEPOSIT("Ore Deposit", "Natural mineral deposit", FeatureCategory.GEOLOGICAL, 0.02f, 0.7f, 0.9f),
    CAVE("Cave", "Natural underground cave system", FeatureCategory.GEOLOGICAL, 0.01f, 0.8f, 0.7f);
    
    private final String displayName;
    private final String description;
    private final FeatureCategory category;
    private final float spawnChance;
    private final float rarityValue;
    private final float resourceValue;
    
    FeatureType(String displayName, String description, FeatureCategory category,
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
    public FeatureCategory getCategory() { return category; }
    public float getSpawnChance() { return spawnChance; }
    public float getRarityValue() { return rarityValue; }
    public float getResourceValue() { return resourceValue; }
    
    // Utility methods
    public boolean isVegetation() { return category == FeatureCategory.VEGETATION; }
    public boolean isGeological() { return category == FeatureCategory.GEOLOGICAL; }
    public boolean isWater() { return category == FeatureCategory.WATER; }
    public boolean isMarine() { return category == FeatureCategory.MARINE; }
    public boolean isTreasure() { return category == FeatureCategory.TREASURE; }
    public boolean isWildlife() { return category == FeatureCategory.WILDLIFE; }
    
    public boolean requiresWater() {
        return category == FeatureCategory.MARINE || 
               category == FeatureCategory.WATER ||
               this == MANGROVE || this == TIDAL_POOL;
    }
    
    public boolean requiresLand() {
        return !requiresWater() && category != FeatureCategory.MARINE;
    }
    
    public boolean isRare() {
        return rarityValue > 0.7f;
    }
    
    public boolean providesResources() {
        return resourceValue > 0.5f;
    }
    
    /**
     * Gets the exploration reward for discovering this feature
     */
    public int getExplorationReward() {
        return (int) (rarityValue * resourceValue * 100);
    }
    
    /**
     * Gets the minimum distance from other features of the same type
     */
    public float getMinFeatureDistance() {
        switch (category) {
            case TREASURE: return 50.0f;
            case GEOLOGICAL: return 20.0f;
            case WATER: return 30.0f;
            case MARINE: return 15.0f;
            case VEGETATION: return 5.0f;
            case WILDLIFE: return 25.0f;
            default: return 10.0f;
        }
    }
    
    /**
     * Feature categories for organization and spawning rules
     */
    public enum FeatureCategory {
        VEGETATION("Vegetation"),
        GEOLOGICAL("Geological"),
        WATER("Water"),
        MARINE("Marine"),
        TREASURE("Treasure"),
        WILDLIFE("Wildlife");
        
        private final String displayName;
        
        FeatureCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}