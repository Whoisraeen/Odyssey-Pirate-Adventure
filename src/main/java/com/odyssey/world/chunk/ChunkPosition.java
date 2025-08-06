package com.odyssey.world.chunk;

import java.util.Objects;

/**
 * Represents a chunk position in the world coordinate system.
 * Chunks are 16x16 block regions that form the basic unit of world storage and generation.
 * 
 * <p>This class is immutable and thread-safe, making it suitable for use as a key
 * in concurrent data structures.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public final class ChunkPosition {
    
    /** The size of a chunk in blocks (16x16). */
    public static final int CHUNK_SIZE = 16;
    
    /** The x-coordinate of this chunk. */
    public final int x;
    
    /** The z-coordinate of this chunk. */
    public final int z;
    
    // Cached hash code for performance
    private final int hashCode;
    
    /**
     * Creates a new chunk position.
     * 
     * @param x the x-coordinate of the chunk
     * @param z the z-coordinate of the chunk
     */
    public ChunkPosition(int x, int z) {
        this.x = x;
        this.z = z;
        this.hashCode = Objects.hash(x, z);
    }
    
    /**
     * Creates a chunk position from world coordinates.
     * 
     * @param worldX the world x-coordinate
     * @param worldZ the world z-coordinate
     * @return the chunk position containing the given world coordinates
     */
    public static ChunkPosition fromWorldPosition(int worldX, int worldZ) {
        return new ChunkPosition(worldX >> 4, worldZ >> 4);
    }
    
    /**
     * Creates a chunk position from world coordinates.
     * 
     * @param worldX the world x-coordinate
     * @param worldZ the world z-coordinate
     * @return the chunk position containing the given world coordinates
     */
    public static ChunkPosition fromWorldPosition(float worldX, float worldZ) {
        return new ChunkPosition((int) Math.floor(worldX) >> 4, (int) Math.floor(worldZ) >> 4);
    }
    
    /**
     * Gets the world x-coordinate of the chunk's origin (northwest corner).
     * 
     * @return the world x-coordinate of the chunk origin
     */
    public int getWorldX() {
        return x << 4;
    }
    
    /**
     * Gets the world z-coordinate of the chunk's origin (northwest corner).
     * 
     * @return the world z-coordinate of the chunk origin
     */
    public int getWorldZ() {
        return z << 4;
    }
    
    /**
     * Gets the world x-coordinate of the chunk's center.
     * 
     * @return the world x-coordinate of the chunk center
     */
    public int getCenterWorldX() {
        return (x << 4) + 8;
    }
    
    /**
     * Gets the world z-coordinate of the chunk's center.
     * 
     * @return the world z-coordinate of the chunk center
     */
    public int getCenterWorldZ() {
        return (z << 4) + 8;
    }
    
    /**
     * Converts world coordinates to local chunk coordinates.
     * 
     * @param worldX the world x-coordinate
     * @return the local x-coordinate within this chunk (0-15)
     */
    public int toLocalX(int worldX) {
        return worldX & 15;
    }
    
    /**
     * Converts world coordinates to local chunk coordinates.
     * 
     * @param worldZ the world z-coordinate
     * @return the local z-coordinate within this chunk (0-15)
     */
    public int toLocalZ(int worldZ) {
        return worldZ & 15;
    }
    
    /**
     * Calculates the squared distance to another chunk position.
     * 
     * @param other the other chunk position
     * @return the squared distance in chunk units
     */
    public int distanceSquared(ChunkPosition other) {
        int dx = this.x - other.x;
        int dz = this.z - other.z;
        return dx * dx + dz * dz;
    }
    
    /**
     * Calculates the Manhattan distance to another chunk position.
     * 
     * @param other the other chunk position
     * @return the Manhattan distance in chunk units
     */
    public int manhattanDistance(ChunkPosition other) {
        return Math.abs(this.x - other.x) + Math.abs(this.z - other.z);
    }
    
    /**
     * Gets the chunk position to the north of this one.
     * 
     * @return the northern neighbor chunk position
     */
    public ChunkPosition north() {
        return new ChunkPosition(x, z - 1);
    }
    
    /**
     * Gets the chunk position to the south of this one.
     * 
     * @return the southern neighbor chunk position
     */
    public ChunkPosition south() {
        return new ChunkPosition(x, z + 1);
    }
    
    /**
     * Gets the chunk position to the east of this one.
     * 
     * @return the eastern neighbor chunk position
     */
    public ChunkPosition east() {
        return new ChunkPosition(x + 1, z);
    }
    
    /**
     * Gets the chunk position to the west of this one.
     * 
     * @return the western neighbor chunk position
     */
    public ChunkPosition west() {
        return new ChunkPosition(x - 1, z);
    }
    
    /**
     * Gets all 8 neighboring chunk positions (including diagonals).
     * 
     * @return an array of 8 neighboring chunk positions
     */
    public ChunkPosition[] getNeighbors() {
        return new ChunkPosition[] {
            new ChunkPosition(x - 1, z - 1), // Northwest
            new ChunkPosition(x, z - 1),     // North
            new ChunkPosition(x + 1, z - 1), // Northeast
            new ChunkPosition(x + 1, z),     // East
            new ChunkPosition(x + 1, z + 1), // Southeast
            new ChunkPosition(x, z + 1),     // South
            new ChunkPosition(x - 1, z + 1), // Southwest
            new ChunkPosition(x - 1, z)      // West
        };
    }
    
    /**
     * Gets the 4 directly adjacent chunk positions (no diagonals).
     * 
     * @return an array of 4 adjacent chunk positions
     */
    public ChunkPosition[] getAdjacentChunks() {
        return new ChunkPosition[] {
            north(), east(), south(), west()
        };
    }
    
    /**
     * Checks if this chunk position is within the specified radius of another position.
     * 
     * @param center the center position
     * @param radius the radius in chunk units
     * @return true if within radius, false otherwise
     */
    public boolean isWithinRadius(ChunkPosition center, int radius) {
        return distanceSquared(center) <= radius * radius;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ChunkPosition that = (ChunkPosition) obj;
        return x == that.x && z == that.z;
    }
    
    @Override
    public int hashCode() {
        return hashCode;
    }
    
    @Override
    public String toString() {
        return String.format("ChunkPosition{x=%d, z=%d}", x, z);
    }
    
    /**
     * Returns a string representation suitable for debugging.
     * 
     * @return detailed string representation
     */
    public String toDetailedString() {
        return String.format("ChunkPosition{x=%d, z=%d, worldX=%d-%d, worldZ=%d-%d}", 
            x, z, getWorldX(), getWorldX() + 15, getWorldZ(), getWorldZ() + 15);
    }
}