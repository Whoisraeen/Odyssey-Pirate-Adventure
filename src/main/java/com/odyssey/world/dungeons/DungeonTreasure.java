package com.odyssey.world.dungeons;

import org.joml.Vector3i;

/**
 * Represents a treasure found within a dungeon.
 * Treasures can be of different types and values, providing rewards for exploration.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public class DungeonTreasure {
    private final String treasureId;
    private final Vector3i position;
    private final TreasureType type;
    private final int value;
    private final String description;
    private boolean collected;
    
    /**
     * Creates a new dungeon treasure.
     * 
     * @param treasureId Unique identifier for this treasure
     * @param position Position of the treasure in the dungeon
     * @param type Type of treasure
     * @param value Monetary value of the treasure
     * @param description Description of the treasure
     */
    public DungeonTreasure(String treasureId, Vector3i position, TreasureType type, int value, String description) {
        this.treasureId = treasureId;
        this.position = new Vector3i(position);
        this.type = type;
        this.value = value;
        this.description = description;
        this.collected = false;
    }
    
    /**
     * Creates a new dungeon treasure with default description.
     * 
     * @param treasureId Unique identifier for this treasure
     * @param position Position of the treasure in the dungeon
     * @param type Type of treasure
     * @param value Monetary value of the treasure
     */
    public DungeonTreasure(String treasureId, Vector3i position, TreasureType type, int value) {
        this(treasureId, position, type, value, type.getDefaultDescription());
    }
    
    // Getters
    public String getTreasureId() { return treasureId; }
    public Vector3i getPosition() { return new Vector3i(position); }
    public TreasureType getType() { return type; }
    public int getValue() { return value; }
    public String getDescription() { return description; }
    public boolean isCollected() { return collected; }
    
    /**
     * Marks this treasure as collected.
     */
    public void collect() {
        this.collected = true;
    }
    
    /**
     * Resets the treasure to uncollected state.
     */
    public void reset() {
        this.collected = false;
    }
    
    /**
     * Gets the display name for this treasure.
     */
    public String getDisplayName() {
        return type.getDisplayName() + " (" + value + " gold)";
    }
    
    @Override
    public String toString() {
        return "DungeonTreasure{" +
                "id='" + treasureId + '\'' +
                ", position=" + position +
                ", type=" + type +
                ", value=" + value +
                ", collected=" + collected +
                '}';
    }
    
    /**
     * Types of treasures that can be found in dungeons.
     */
    public enum TreasureType {
        MINOR("Minor Treasure", "A small cache of coins and gems", 50, 200),
        MAJOR("Major Treasure", "A substantial hoard of valuable items", 200, 1000),
        LEGENDARY("Legendary Treasure", "An incredibly rare and valuable artifact", 1000, 5000),
        GOLD_COINS("Gold Coins", "A pile of gleaming gold coins", 25, 150),
        PRECIOUS_GEMS("Precious Gems", "A collection of rare gemstones", 100, 500),
        ANCIENT_ARTIFACT("Ancient Artifact", "A mysterious relic from ages past", 300, 1500),
        WEAPON_CACHE("Weapon Cache", "A hidden stash of fine weapons", 150, 800),
        SPELL_SCROLL("Spell Scroll", "Ancient magical knowledge preserved on parchment", 75, 400),
        RARE_MATERIALS("Rare Materials", "Uncommon crafting materials and components", 40, 250);
        
        private final String displayName;
        private final String defaultDescription;
        private final int minValue;
        private final int maxValue;
        
        TreasureType(String displayName, String defaultDescription, int minValue, int maxValue) {
            this.displayName = displayName;
            this.defaultDescription = defaultDescription;
            this.minValue = minValue;
            this.maxValue = maxValue;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDefaultDescription() { return defaultDescription; }
        public int getMinValue() { return minValue; }
        public int getMaxValue() { return maxValue; }
        
        /**
         * Generates a random value for this treasure type.
         */
        public int generateRandomValue(java.util.Random random) {
            return minValue + random.nextInt(maxValue - minValue + 1);
        }
        
        /**
         * Checks if this treasure type is considered rare.
         */
        public boolean isRare() {
            return this == LEGENDARY || this == ANCIENT_ARTIFACT;
        }
        
        /**
         * Checks if this treasure type is magical in nature.
         */
        public boolean isMagical() {
            return this == SPELL_SCROLL || this == ANCIENT_ARTIFACT || this == LEGENDARY;
        }
    }
}