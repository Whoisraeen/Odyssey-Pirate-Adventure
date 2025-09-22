package com.odyssey.world;

import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * Represents a single block/voxel in the world.
 * Each block has a type, position, and various properties that affect gameplay.
 */
public class Block {
    
    /**
     * Enumeration of all block types in the game
     */
    public enum BlockType {
        AIR(0, "Air", false, false, 0.0f, new Vector3f(0.0f)),
        STONE(1, "Stone", true, true, 1.5f, new Vector3f(0.5f, 0.5f, 0.5f)),
        DIRT(2, "Dirt", true, true, 0.5f, new Vector3f(0.4f, 0.3f, 0.2f)),
        GRASS(3, "Grass", true, true, 0.6f, new Vector3f(0.3f, 0.8f, 0.2f)),
        SAND(4, "Sand", true, true, 0.5f, new Vector3f(0.9f, 0.8f, 0.6f)),
        WATER(5, "Water", false, false, 0.0f, new Vector3f(0.2f, 0.4f, 0.8f)),
        WOOD(6, "Wood", true, true, 1.0f, new Vector3f(0.6f, 0.4f, 0.2f)),
        LEAVES(7, "Leaves", true, false, 0.2f, new Vector3f(0.2f, 0.6f, 0.2f)),
        COBBLESTONE(8, "Cobblestone", true, true, 2.0f, new Vector3f(0.4f, 0.4f, 0.4f)),
        PLANKS(9, "Planks", true, true, 1.0f, new Vector3f(0.7f, 0.5f, 0.3f)),
        COAL_ORE(10, "Coal Ore", true, true, 3.0f, new Vector3f(0.3f, 0.3f, 0.3f)),
        IRON_ORE(11, "Iron Ore", true, true, 3.0f, new Vector3f(0.7f, 0.6f, 0.5f)),
        GOLD_ORE(12, "Gold Ore", true, true, 3.0f, new Vector3f(0.9f, 0.8f, 0.2f)),
        TREASURE_CHEST(13, "Treasure Chest", true, true, 2.5f, new Vector3f(0.6f, 0.4f, 0.2f)),
        PALM_WOOD(14, "Palm Wood", true, true, 1.0f, new Vector3f(0.8f, 0.6f, 0.4f)),
        PALM_LEAVES(15, "Palm Leaves", true, false, 0.2f, new Vector3f(0.4f, 0.8f, 0.3f)),
        CORAL(16, "Coral", true, false, 0.5f, new Vector3f(0.9f, 0.3f, 0.4f)),
        SEAWEED(17, "Seaweed", false, false, 0.1f, new Vector3f(0.2f, 0.6f, 0.3f)),
        TORCH(18, "Torch", false, false, 0.1f, new Vector3f(1.0f, 0.8f, 0.2f)),
        LANTERN(19, "Lantern", true, false, 1.5f, new Vector3f(1.0f, 0.9f, 0.3f)),
        GLOWSTONE(20, "Glowstone", true, false, 0.3f, new Vector3f(1.0f, 1.0f, 0.8f)),
        LAVA(21, "Lava", false, false, 0.0f, new Vector3f(1.0f, 0.3f, 0.0f)),
        FIRE(22, "Fire", false, false, 0.0f, new Vector3f(1.0f, 0.2f, 0.0f));
        
        private final int id;
        private final String name;
        private final boolean solid;
        private final boolean opaque;
        private final float hardness;
        private final Vector3f color;
        
        BlockType(int id, String name, boolean solid, boolean opaque, float hardness, Vector3f color) {
            this.id = id;
            this.name = name;
            this.solid = solid;
            this.opaque = opaque;
            this.hardness = hardness;
            this.color = new Vector3f(color);
        }
        
        public int getId() { return id; }
        public String getName() { return name; }
        public boolean isSolid() { return solid; }
        public boolean isOpaque() { return opaque; }
        public float getHardness() { return hardness; }
        public Vector3f getColor() { return new Vector3f(color); }
        
        public static BlockType fromId(int id) {
            for (BlockType type : values()) {
                if (type.id == id) return type;
            }
            return AIR;
        }
        
        public boolean isTransparent() { return !opaque; }
        public boolean isLiquid() { return this == WATER || this == SEAWEED; }
        public boolean isOre() { return this == COAL_ORE || this == IRON_ORE || this == GOLD_ORE; }
        public boolean isVegetation() { return this == LEAVES || this == PALM_LEAVES || this == SEAWEED; }
        public boolean canBreak() { return this != AIR && this != WATER; }
        
        /**
         * Gets the light absorption level of this block type (0-15)
         */
        public int getLightAbsorption() {
            if (isOpaque()) return 15;
            if (this == WATER) return 2;
            if (this == LEAVES || this == PALM_LEAVES) return 1;
            return 0;
        }
        
        /**
         * Gets the light emission level of this block type (0-15)
         */
        public int getLightLevel() {
            switch (this) {
                case TORCH: return 14;
                case LANTERN: return 15;
                case GLOWSTONE: return 15;
                case LAVA: return 15;
                case FIRE: return 15;
                default: return 0;
            }
        }
    }
    
    private BlockType type;
    private Vector3i position;
    private byte metadata; // For additional block data (rotation, growth stage, etc.)
    private float health; // Current health of the block
    
    /**
     * Creates a new block with the specified type at the given position
     */
    public Block(BlockType type, Vector3i position) {
        this.type = type;
        this.position = new Vector3i(position);
        this.metadata = 0;
        this.health = type.getHardness();
    }
    
    /**
     * Creates a new block with the specified type, position, and metadata
     */
    public Block(BlockType type, Vector3i position, byte metadata) {
        this.type = type;
        this.position = new Vector3i(position);
        this.metadata = metadata;
        this.health = type.getHardness();
    }
    
    /**
     * Creates an air block at the specified position
     */
    public static Block createAir(Vector3i position) {
        return new Block(BlockType.AIR, position);
    }
    
    /**
     * Creates a water block at the specified position
     */
    public static Block createWater(Vector3i position) {
        return new Block(BlockType.WATER, position);
    }
    
    // Getters and setters
    public BlockType getType() { return type; }
    public void setType(BlockType type) { 
        this.type = type; 
        this.health = type.getHardness();
    }
    
    public Vector3i getPosition() { return new Vector3i(position); }
    public void setPosition(Vector3i position) { this.position.set(position); }
    
    public byte getMetadata() { return metadata; }
    public void setMetadata(byte metadata) { this.metadata = metadata; }
    
    public float getHealth() { return health; }
    public void setHealth(float health) { this.health = Math.max(0, health); }
    
    // Utility methods
    public boolean isAir() { return type == BlockType.AIR; }
    public boolean isSolid() { return type.isSolid(); }
    public boolean isOpaque() { return type.isOpaque(); }
    public boolean isTransparent() { return type.isTransparent(); }
    public boolean isLiquid() { return type.isLiquid(); }
    public boolean canBreak() { return type.canBreak(); }
    
    /**
     * Damages the block by the specified amount
     * @param damage Amount of damage to deal
     * @return true if the block was destroyed, false otherwise
     */
    public boolean damage(float damage) {
        if (!canBreak()) return false;
        
        health -= damage;
        return health <= 0;
    }
    
    /**
     * Repairs the block to full health
     */
    public void repair() {
        health = type.getHardness();
    }
    
    /**
     * Gets the break progress as a percentage (0.0 to 1.0)
     */
    public float getBreakProgress() {
        if (!canBreak() || type.getHardness() <= 0) return 0.0f;
        return Math.max(0.0f, 1.0f - (health / type.getHardness()));
    }
    
    /**
     * Checks if this block should render a face adjacent to another block
     */
    public boolean shouldRenderFaceAgainst(Block other) {
        if (other == null || other.isAir()) return true;
        if (this.isTransparent() && other.isTransparent() && this.type != other.type) return true;
        return !other.isOpaque();
    }
    
    /**
     * Gets the light level emitted by this block (0-15)
     */
    public int getLightLevel() {
        switch (type) {
            case TREASURE_CHEST: return 3;
            case CORAL: return 2;
            case TORCH: return 14;
            case LANTERN: return 15;
            case GLOWSTONE: return 15;
            case LAVA: return 15;
            case FIRE: return 15;
            default: return 0;
        }
    }
    
    /**
     * Gets the light absorption level of this block (0-15)
     */
    public int getLightAbsorption() {
        if (isOpaque()) return 15;
        if (type == BlockType.WATER) return 2;
        if (type == BlockType.LEAVES || type == BlockType.PALM_LEAVES) return 1;
        return 0;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Block block = (Block) obj;
        return type == block.type && 
               position.equals(block.position) && 
               metadata == block.metadata;
    }
    
    @Override
    public int hashCode() {
        return type.hashCode() * 31 + position.hashCode() * 7 + metadata;
    }
    
    @Override
    public String toString() {
        return String.format("Block{type=%s, pos=(%d,%d,%d), meta=%d, health=%.1f}", 
                           type.name(), position.x, position.y, position.z, metadata, health);
    }
    
    /**
     * Creates a copy of this block
     */
    public Block copy() {
        Block copy = new Block(type, position, metadata);
        copy.health = this.health;
        return copy;
    }
}