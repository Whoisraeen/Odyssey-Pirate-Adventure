package com.odyssey.world.feature;

import com.odyssey.world.FeatureType;
import org.joml.Vector3f;

/**
 * Base class for all world features (caves, springs, ore deposits, etc.)
 * Features are natural or generated elements that add detail to islands.
 */
public abstract class Feature {
    
    protected final Vector3f position;
    protected final FeatureType type;
    protected boolean discovered;
    protected float resourceValue;
    protected String description;
    protected long creationTime;
    
    public Feature(FeatureType type, float x, float y, float z) {
        this.type = type;
        this.position = new Vector3f(x, y, z);
        this.discovered = false;
        this.resourceValue = type.getResourceValue();
        this.description = type.getDescription();
        this.creationTime = System.currentTimeMillis();
    }
    
    public Feature(FeatureType type, Vector3f position) {
        this(type, position.x, position.y, position.z);
    }
    
    /**
     * Called when a player or entity interacts with this feature
     */
    public abstract void onInteract();
    
    /**
     * Called when this feature is discovered by a player
     */
    public void onDiscover() {
        this.discovered = true;
    }
    
    /**
     * Get the resources that can be extracted from this feature
     */
    public abstract String[] getAvailableResources();
    
    /**
     * Extract resources from this feature
     */
    public abstract boolean extractResource(String resourceType, int amount);
    
    /**
     * Check if this feature can be harvested/mined
     */
    public boolean isHarvestable() {
        return resourceValue > 0;
    }
    
    /**
     * Get the exploration reward for discovering this feature
     */
    public int getExplorationReward() {
        return type.getExplorationReward();
    }
    
    /**
     * Check if this feature requires special tools or conditions to access
     */
    public boolean requiresSpecialAccess() {
        return false;
    }
    
    /**
     * Get the danger level of this feature (0 = safe, 10 = extremely dangerous)
     */
    public int getDangerLevel() {
        return 0;
    }
    
    // Getters and setters
    public Vector3f getPosition() { return new Vector3f(position); }
    public float getX() { return position.x; }
    public float getY() { return position.y; }
    public float getZ() { return position.z; }
    
    public FeatureType getType() { return type; }
    public boolean isDiscovered() { return discovered; }
    public void setDiscovered(boolean discovered) { this.discovered = discovered; }
    
    public float getResourceValue() { return resourceValue; }
    public void setResourceValue(float resourceValue) { this.resourceValue = resourceValue; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    /**
     * Get the distance from this feature to a point
     */
    public float getDistanceTo(float x, float z) {
        float dx = position.x - x;
        float dz = position.z - z;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Get the distance from this feature to another feature
     */
    public float getDistanceTo(Feature other) {
        return position.distance(other.position);
    }
    
    /**
     * Check if this feature is within a certain radius of a point
     */
    public boolean isWithinRadius(float x, float z, float radius) {
        return getDistanceTo(x, z) <= radius;
    }
    
    @Override
    public String toString() {
        return String.format("%s at (%.1f, %.1f, %.1f) - %s", 
                           type.getDisplayName(), position.x, position.y, position.z,
                           discovered ? "Discovered" : "Undiscovered");
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Feature feature = (Feature) obj;
        return type == feature.type && position.equals(feature.position);
    }
    
    @Override
    public int hashCode() {
        int result = position.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }
    
    /**
     * Get the creation time of this feature
     */
    public long getCreationTime() {
        return creationTime;
    }
}