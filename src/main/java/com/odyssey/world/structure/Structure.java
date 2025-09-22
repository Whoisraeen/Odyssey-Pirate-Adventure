package com.odyssey.world.structure;

import org.joml.Vector3f;
import com.odyssey.world.StructureType;

/**
 * Base class for all world structures (pirate camps, shipwrecks, caves, settlements, etc.)
 * Structures are man-made or significant natural formations that provide gameplay elements.
 */
public abstract class Structure {
    
    protected final Vector3f position;
    protected final StructureType type;
    protected boolean discovered;
    protected boolean explored;
    protected float integrity; // 0.0 to 1.0, represents structural condition
    protected String name;
    protected String description;
    
    public Structure(StructureType type, float x, float y, float z) {
        this.type = type;
        this.position = new Vector3f(x, y, z);
        this.discovered = false;
        this.explored = false;
        this.integrity = 1.0f;
        this.name = type.getDisplayName();
        this.description = type.getDescription();
    }
    
    public Structure(StructureType type, Vector3f position) {
        this(type, position.x, position.y, position.z);
    }
    
    /**
     * Called when a player enters this structure
     */
    public abstract void onEnter();
    
    /**
     * Called when a player exits this structure
     */
    public abstract void onExit();
    
    /**
     * Called when this structure is discovered by a player
     */
    public void onDiscover() {
        this.discovered = true;
    }
    
    /**
     * Called when this structure is fully explored
     */
    public void onExplore() {
        this.explored = true;
    }
    
    /**
     * Get the loot that can be found in this structure
     */
    public abstract String[] getAvailableLoot();
    
    /**
     * Attempt to loot this structure
     */
    public abstract boolean loot(String lootType);
    
    /**
     * Check if this structure has been fully looted
     */
    public abstract boolean isLooted();
    
    /**
     * Get the exploration reward for discovering this structure
     */
    public int getExplorationReward() {
        return type.getExplorationReward();
    }
    
    /**
     * Get the danger level of this structure
     */
    public StructureType.DangerLevel getDangerLevel() {
        return type.getDangerLevel();
    }
    
    /**
     * Get the size category of this structure
     */
    public StructureType.StructureSize getSize() {
        return type.getSize();
    }
    
    /**
     * Check if this structure requires special equipment to explore
     */
    public boolean requiresSpecialEquipment() {
        return type.isUnderwater() || getDangerLevel() == StructureType.DangerLevel.EXTREME;
    }
    
    /**
     * Damage the structure (reduces integrity)
     */
    public void damage(float amount) {
        integrity = Math.max(0.0f, integrity - amount);
    }
    
    /**
     * Repair the structure (increases integrity)
     */
    public void repair(float amount) {
        integrity = Math.min(1.0f, integrity + amount);
    }
    
    /**
     * Check if the structure is in ruins (very low integrity)
     */
    public boolean isRuined() {
        return integrity < 0.3f;
    }
    
    // Getters and setters
    public Vector3f getPosition() { return new Vector3f(position); }
    public float getX() { return position.x; }
    public float getY() { return position.y; }
    public float getZ() { return position.z; }
    
    public StructureType getType() { return type; }
    public boolean isDiscovered() { return discovered; }
    public void setDiscovered(boolean discovered) { this.discovered = discovered; }
    
    public boolean isExplored() { return explored; }
    public void setExplored(boolean explored) { this.explored = explored; }
    
    public float getIntegrity() { return integrity; }
    public void setIntegrity(float integrity) { 
        this.integrity = Math.max(0.0f, Math.min(1.0f, integrity)); 
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    /**
     * Get the distance from this structure to a point
     */
    public float getDistanceTo(float x, float z) {
        float dx = position.x - x;
        float dz = position.z - z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Get the distance from this structure to another structure
     */
    public float getDistanceTo(Structure other) {
        return position.distance(other.position);
    }
    
    /**
     * Check if this structure is within a certain radius of a point
     */
    public boolean isWithinRadius(float x, float z, float radius) {
        return getDistanceTo(x, z) <= radius;
    }
    
    /**
     * Get the minimum distance this structure should be from other structures
     */
    public float getMinimumDistance() {
        return type.getMinimumDistance();
    }
    
    @Override
    public String toString() {
        String status = discovered ? (explored ? "Explored" : "Discovered") : "Undiscovered";
        return String.format("%s '%s' at (%.1f, %.1f, %.1f) - %s (%.0f%% integrity)", 
                           type.getDisplayName(), name, position.x, position.y, position.z,
                           status, integrity * 100);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Structure structure = (Structure) obj;
        return type == structure.type && position.equals(structure.position);
    }
    
    @Override
    public int hashCode() {
        int result = position.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
}