package com.odyssey.world.save.region;

import java.util.Objects;

/**
 * Represents a region position in the world coordinate system.
 * Regions are 32x32 chunk areas that form the basic unit of file storage.
 * 
 * <p>This class is immutable and thread-safe, making it suitable for use as a key
 * in concurrent data structures.
 * 
 * @author The Odyssey Team
 * @since 1.0.0
 */
public final class RegionPosition {
    
    /** The X coordinate of the region. */
    public final int x;
    
    /** The Z coordinate of the region. */
    public final int z;
    
    /**
     * Creates a new region position.
     * 
     * @param x the X coordinate
     * @param z the Z coordinate
     */
    public RegionPosition(int x, int z) {
        this.x = x;
        this.z = z;
    }
    
    /**
     * Creates a region position from a string representation.
     * Expected format: "x,z" (e.g., "5,-3")
     * 
     * @param str the string representation
     * @return the region position
     * @throws IllegalArgumentException if the string format is invalid
     */
    public static RegionPosition fromString(String str) {
        if (str == null || str.trim().isEmpty()) {
            throw new IllegalArgumentException("Region position string cannot be null or empty");
        }
        
        String[] parts = str.trim().split(",");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid region position format: " + str);
        }
        
        try {
            int x = Integer.parseInt(parts[0].trim());
            int z = Integer.parseInt(parts[1].trim());
            return new RegionPosition(x, z);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid region position coordinates: " + str, e);
        }
    }
    
    /**
     * Gets the region file name for this position.
     * 
     * @return the region file name (e.g., "r.5.-3.oreg")
     */
    public String getFileName() {
        return String.format("r.%d.%d.oreg", x, z);
    }
    
    /**
     * Calculates the distance to another region position.
     * 
     * @param other the other region position
     * @return the Euclidean distance
     */
    public double distanceTo(RegionPosition other) {
        if (other == null) {
            return Double.POSITIVE_INFINITY;
        }
        
        double dx = this.x - other.x;
        double dz = this.z - other.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
    
    /**
     * Calculates the Manhattan distance to another region position.
     * 
     * @param other the other region position
     * @return the Manhattan distance
     */
    public int manhattanDistanceTo(RegionPosition other) {
        if (other == null) {
            return Integer.MAX_VALUE;
        }
        
        return Math.abs(this.x - other.x) + Math.abs(this.z - other.z);
    }
    
    /**
     * Gets the neighboring region positions (8 directions).
     * 
     * @return an array of 8 neighboring region positions
     */
    public RegionPosition[] getNeighbors() {
        return new RegionPosition[] {
            new RegionPosition(x - 1, z - 1), // NW
            new RegionPosition(x,     z - 1), // N
            new RegionPosition(x + 1, z - 1), // NE
            new RegionPosition(x - 1, z),     // W
            new RegionPosition(x + 1, z),     // E
            new RegionPosition(x - 1, z + 1), // SW
            new RegionPosition(x,     z + 1), // S
            new RegionPosition(x + 1, z + 1)  // SE
        };
    }
    
    /**
     * Gets the neighboring region positions (4 cardinal directions).
     * 
     * @return an array of 4 neighboring region positions
     */
    public RegionPosition[] getCardinalNeighbors() {
        return new RegionPosition[] {
            new RegionPosition(x,     z - 1), // N
            new RegionPosition(x - 1, z),     // W
            new RegionPosition(x + 1, z),     // E
            new RegionPosition(x,     z + 1)  // S
        };
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        RegionPosition that = (RegionPosition) obj;
        return x == that.x && z == that.z;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(x, z);
    }
    
    @Override
    public String toString() {
        return String.format("RegionPosition{x=%d, z=%d}", x, z);
    }
    
    /**
     * Returns a compact string representation suitable for file names.
     * 
     * @return compact string representation (e.g., "5,-3")
     */
    public String toCompactString() {
        return x + "," + z;
    }
}