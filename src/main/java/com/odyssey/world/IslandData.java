package com.odyssey.world;

/**
 * Data class containing metadata about an island at specific coordinates.
 * Used for caching and quick lookups during world generation.
 */
public class IslandData {
    
    private final float worldX;
    private final float worldZ;
    private final IslandType type;
    private final boolean hasIsland;
    
    // Additional metadata
    private float radius;
    private float maxHeight;
    private int estimatedSize;
    private long generationTime;
    
    /**
     * Creates island data for coordinates with an island
     */
    public IslandData(float worldX, float worldZ, IslandType type, boolean hasIsland) {
        this.worldX = worldX;
        this.worldZ = worldZ;
        this.type = type;
        this.hasIsland = hasIsland;
        this.generationTime = System.currentTimeMillis();
        
        if (hasIsland && type != null) {
            this.radius = type.getRadius();
            this.maxHeight = type.getMaxHeight();
            this.estimatedSize = (int) (radius * 2);
        } else {
            this.radius = 0;
            this.maxHeight = 0;
            this.estimatedSize = 0;
        }
    }
    
    /**
     * Creates island data for coordinates without an island
     */
    public static IslandData noIsland(float worldX, float worldZ) {
        return new IslandData(worldX, worldZ, null, false);
    }
    
    /**
     * Creates island data for coordinates with an island
     */
    public static IslandData withIsland(float worldX, float worldZ, IslandType type) {
        return new IslandData(worldX, worldZ, type, true);
    }
    
    // Getters
    public float getWorldX() { return worldX; }
    public float getWorldZ() { return worldZ; }
    public IslandType getType() { return type; }
    public boolean hasIsland() { return hasIsland; }
    public float getRadius() { return radius; }
    public float getMaxHeight() { return maxHeight; }
    public int getEstimatedSize() { return estimatedSize; }
    public long getGenerationTime() { return generationTime; }
    
    /**
     * Check if a world coordinate is within this island's bounds
     */
    public boolean containsPoint(float x, float z) {
        if (!hasIsland) return false;
        
        float dx = x - worldX;
        float dz = z - worldZ;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        
        return distance <= radius;
    }
    
    /**
     * Get the distance from a point to the island center
     */
    public float getDistanceToCenter(float x, float z) {
        float dx = x - worldX;
        float dz = z - worldZ;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Check if this island overlaps with another island's area
     */
    public boolean overlaps(IslandData other) {
        if (!this.hasIsland || !other.hasIsland) return false;
        
        float distance = getDistanceToCenter(other.worldX, other.worldZ);
        return distance < (this.radius + other.radius);
    }
    
    @Override
    public String toString() {
        if (hasIsland) {
            return String.format("IslandData{pos=(%.1f, %.1f), type=%s, radius=%.1f}", 
                               worldX, worldZ, type, radius);
        } else {
            return String.format("IslandData{pos=(%.1f, %.1f), no island}", worldX, worldZ);
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        IslandData that = (IslandData) obj;
        return Float.compare(that.worldX, worldX) == 0 &&
               Float.compare(that.worldZ, worldZ) == 0 &&
               hasIsland == that.hasIsland &&
               type == that.type;
    }
    
    @Override
    public int hashCode() {
        int result = Float.floatToIntBits(worldX);
        result = 31 * result + Float.floatToIntBits(worldZ);
        result = 31 * result + (hasIsland ? 1 : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}