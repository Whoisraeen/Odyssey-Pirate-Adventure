package com.odyssey.world;

/**
 * Enum defining different types of ores that can be found in the world.
 * Each ore type has different properties affecting rarity, value, and usage.
 */
public enum OreType {
    
    // Common ores
    IRON("Iron Ore", "Common metallic ore used for tools and weapons", 
         Rarity.COMMON, 10, 2.0f, true, false),
    COPPER("Copper Ore", "Reddish metal ore used for coins and decorations", 
           Rarity.COMMON, 8, 1.5f, true, false),
    TIN("Tin Ore", "Soft metal ore used for alloys and containers", 
        Rarity.COMMON, 6, 1.2f, true, false),
    
    // Uncommon ores
    SILVER("Silver Ore", "Precious metal ore valued by traders", 
           Rarity.UNCOMMON, 25, 3.5f, true, true),
    LEAD("Lead Ore", "Heavy metal ore used for ammunition", 
         Rarity.UNCOMMON, 12, 2.8f, true, false),
    COAL("Coal", "Combustible mineral used for fuel and smelting", 
         Rarity.UNCOMMON, 5, 1.0f, false, false),
    
    // Rare ores
    GOLD("Gold Ore", "Highly valuable precious metal ore", 
         Rarity.RARE, 50, 5.0f, true, true),
    EMERALD("Emerald", "Precious green gemstone", 
            Rarity.RARE, 75, 1.0f, false, true),
    RUBY("Ruby", "Precious red gemstone", 
         Rarity.RARE, 80, 1.0f, false, true),
    SAPPHIRE("Sapphire", "Precious blue gemstone", 
             Rarity.RARE, 70, 1.0f, false, true),
    
    // Very rare ores
    PLATINUM("Platinum Ore", "Extremely valuable white metal ore", 
             Rarity.VERY_RARE, 100, 6.0f, true, true),
    DIAMOND("Diamond", "The hardest and most valuable gemstone", 
            Rarity.VERY_RARE, 150, 1.0f, false, true),
    
    // Legendary ores (pirate-themed)
    CURSED_GOLD("Cursed Gold", "Mystical gold ore with supernatural properties", 
                Rarity.LEGENDARY, 200, 4.0f, true, true),
    KRAKEN_PEARL("Kraken Pearl", "Rare pearl from the depths of the ocean", 
                 Rarity.LEGENDARY, 300, 0.5f, false, true),
    STORM_CRYSTAL("Storm Crystal", "Crystallized lightning with electrical properties", 
                  Rarity.LEGENDARY, 250, 2.0f, false, true),
    SIREN_SCALE("Siren Scale", "Iridescent scale with enchanting properties", 
                Rarity.LEGENDARY, 180, 1.5f, false, true),
    
    // Additional ores for IslandGenerator compatibility
    SULFUR("Sulfur", "Yellow mineral used for gunpowder and alchemy", 
           Rarity.UNCOMMON, 8, 1.0f, false, false),
    OBSIDIAN("Obsidian", "Volcanic glass used for sharp tools", 
             Rarity.UNCOMMON, 15, 3.0f, false, false),
    SALT("Salt", "Essential mineral for food preservation", 
         Rarity.COMMON, 3, 0.5f, false, false),
    GEMS("Gems", "Generic precious stones", 
         Rarity.RARE, 60, 1.5f, false, true),
    LIMESTONE("Limestone", "Sedimentary rock used for construction", 
              Rarity.COMMON, 2, 0.8f, false, false);
    
    public enum Rarity {
        COMMON(0.4f, "Common"),
        UNCOMMON(0.25f, "Uncommon"), 
        RARE(0.1f, "Rare"),
        VERY_RARE(0.03f, "Very Rare"),
        LEGENDARY(0.005f, "Legendary");
        
        private final float spawnChance;
        private final String displayName;
        
        Rarity(float spawnChance, String displayName) {
            this.spawnChance = spawnChance;
            this.displayName = displayName;
        }
        
        public float getSpawnChance() { return spawnChance; }
        public String getDisplayName() { return displayName; }
    }
    
    private final String displayName;
    private final String description;
    private final Rarity rarity;
    private final int baseValue;
    private final float hardness; // Mining difficulty (1.0 = easy, 10.0 = very hard)
    private final boolean isMetal;
    private final boolean isPrecious;
    
    OreType(String displayName, String description, Rarity rarity, int baseValue, 
            float hardness, boolean isMetal, boolean isPrecious) {
        this.displayName = displayName;
        this.description = description;
        this.rarity = rarity;
        this.baseValue = baseValue;
        this.hardness = hardness;
        this.isMetal = isMetal;
        this.isPrecious = isPrecious;
    }
    
    // Getters
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public Rarity getRarity() { return rarity; }
    public int getBaseValue() { return baseValue; }
    public float getHardness() { return hardness; }
    public boolean isMetal() { return isMetal; }
    public boolean isPrecious() { return isPrecious; }
    
    /**
     * Gets the maximum number of deposits for this ore type
     */
    public int getMaxDeposits() {
        switch (rarity) {
            case COMMON: return 5;
            case UNCOMMON: return 3;
            case RARE: return 2;
            case VERY_RARE: return 1;
            case LEGENDARY: return 1;
            default: return 2;
        }
    }
    
    /**
     * Gets the maximum size for ore deposits of this type
     */
    public float getMaxSize() {
        return hardness * 10.0f;
    }
    
    /**
     * Gets the minimum size for ore deposits of this type
     */
    public float getMinSize() {
        return hardness * 2.0f;
    }
    
    /**
     * Get the spawn chance for this ore type
     */
    public float getSpawnChance() {
        return rarity.getSpawnChance();
    }
    
    /**
     * Check if this ore is a gemstone
     */
    public boolean isGemstone() {
        return !isMetal && isPrecious;
    }
    
    /**
     * Check if this ore is common enough to be found regularly
     */
    public boolean isCommon() {
        return rarity == Rarity.COMMON || rarity == Rarity.UNCOMMON;
    }
    
    /**
     * Check if this ore is rare or legendary
     */
    public boolean isRare() {
        return rarity == Rarity.RARE || rarity == Rarity.VERY_RARE || rarity == Rarity.LEGENDARY;
    }
    
    /**
     * Check if this ore has magical or supernatural properties
     */
    public boolean isMagical() {
        return rarity == Rarity.LEGENDARY;
    }
    
    /**
     * Get the mining time multiplier based on hardness
     */
    public float getMiningTimeMultiplier() {
        return hardness;
    }
    
    /**
     * Get the experience points gained from mining this ore
     */
    public int getExperienceReward() {
        return baseValue / 5 + (int)(hardness * 2);
    }
    
    /**
     * Calculate the actual value based on market conditions
     */
    public int calculateValue(float marketMultiplier) {
        return (int)(baseValue * marketMultiplier);
    }
    
    /**
     * Get ores that are commonly found together with this ore
     */
    public OreType[] getAssociatedOres() {
        switch (this) {
            case IRON:
                return new OreType[]{COAL, COPPER};
            case COPPER:
                return new OreType[]{TIN, IRON};
            case SILVER:
                return new OreType[]{LEAD, GOLD};
            case GOLD:
                return new OreType[]{SILVER, PLATINUM};
            case EMERALD:
                return new OreType[]{RUBY, SAPPHIRE};
            case RUBY:
                return new OreType[]{EMERALD, SAPPHIRE};
            case SAPPHIRE:
                return new OreType[]{EMERALD, RUBY};
            case PLATINUM:
                return new OreType[]{GOLD, DIAMOND};
            case DIAMOND:
                return new OreType[]{PLATINUM};
            default:
                return new OreType[0];
        }
    }
    
    /**
     * Get a random ore type based on rarity weights
     */
    public static OreType getRandomOre(java.util.Random random) {
        float roll = random.nextFloat();
        float cumulative = 0.0f;
        
        for (OreType ore : values()) {
            cumulative += ore.getSpawnChance();
            if (roll <= cumulative) {
                return ore;
            }
        }
        
        return IRON; // Fallback to most common ore
    }
    
    /**
     * Get ores suitable for a specific biome type
     */
    public static OreType[] getOresForBiome(BiomeType biome) {
        switch (biome) {
            case MOUNTAIN:
            case VOLCANIC_PEAK:
                return new OreType[]{IRON, COPPER, SILVER, GOLD, COAL, DIAMOND};
            case DEEP_OCEAN:
            case OCEAN:
                return new OreType[]{KRAKEN_PEARL, SIREN_SCALE, SILVER};
            case TROPICAL_FOREST:
            case JUNGLE:
                return new OreType[]{EMERALD, RUBY, GOLD, COPPER};
            case DESERT:
                return new OreType[]{GOLD, SILVER, SAPPHIRE, LEAD};
            case SWAMP:
                return new OreType[]{CURSED_GOLD, STORM_CRYSTAL, IRON, TIN};
            default:
                return new OreType[]{IRON, COPPER, TIN, COAL};
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s (%s) - %d gold", displayName, rarity.displayName, baseValue);
    }
}