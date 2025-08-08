package com.odyssey.world.dungeons;

import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents a corridor connecting rooms in a dungeon.
 */
public class DungeonCorridor {
    private final String corridorId;
    private final Vector3i start;
    private final Vector3i end;
    private final int width;
    private final List<Vector3i> path;
    
    public DungeonCorridor(String corridorId, Vector3i start, Vector3i end, int width) {
        this.corridorId = corridorId;
        this.start = new Vector3i(start);
        this.end = new Vector3i(end);
        this.width = Math.max(1, width);
        this.path = generatePath();
    }
    
    /**
     * Generates the path from start to end position.
     */
    private List<Vector3i> generatePath() {
        List<Vector3i> pathPoints = new ArrayList<>();
        
        // Simple L-shaped path (could be enhanced with more complex pathfinding)
        Vector3i current = new Vector3i(start);
        pathPoints.add(new Vector3i(current));
        
        // Move horizontally first (X direction)
        while (current.x != end.x) {
            current.x += Integer.compare(end.x, current.x);
            pathPoints.add(new Vector3i(current));
        }
        
        // Then move vertically (Y direction)
        while (current.y != end.y) {
            current.y += Integer.compare(end.y, current.y);
            pathPoints.add(new Vector3i(current));
        }
        
        // Finally move in Z direction
        while (current.z != end.z) {
            current.z += Integer.compare(end.z, current.z);
            pathPoints.add(new Vector3i(current));
        }
        
        return pathPoints;
    }
    
    /**
     * Gets a random position along this corridor.
     */
    public Vector3i getRandomPosition(Random random) {
        if (path.isEmpty()) {
            return new Vector3i(start);
        }
        
        Vector3i pathPoint = path.get(random.nextInt(path.size()));
        
        // Add some random offset within the corridor width
        int offsetX = random.nextInt(width) - width / 2;
        int offsetZ = random.nextInt(width) - width / 2;
        
        return new Vector3i(
            pathPoint.x + offsetX,
            pathPoint.y,
            pathPoint.z + offsetZ
        );
    }
    
    /**
     * Gets the length of this corridor.
     */
    public int getLength() {
        return path.size();
    }
    
    /**
     * Gets the Manhattan distance between start and end.
     */
    public int getManhattanDistance() {
        return Math.abs(end.x - start.x) + Math.abs(end.y - start.y) + Math.abs(end.z - start.z);
    }
    
    /**
     * Gets the Euclidean distance between start and end.
     */
    public double getEuclideanDistance() {
        int dx = end.x - start.x;
        int dy = end.y - start.y;
        int dz = end.z - start.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * Checks if a position is part of this corridor.
     */
    public boolean contains(Vector3i position) {
        for (Vector3i pathPoint : path) {
            // Check if position is within corridor width of any path point
            int dx = Math.abs(position.x - pathPoint.x);
            int dz = Math.abs(position.z - pathPoint.z);
            int maxOffset = width / 2;
            
            if (dx <= maxOffset && dz <= maxOffset && position.y == pathPoint.y) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Gets all positions that make up this corridor.
     */
    public List<Vector3i> getAllPositions() {
        List<Vector3i> allPositions = new ArrayList<>();
        int halfWidth = width / 2;
        
        for (Vector3i pathPoint : path) {
            for (int x = pathPoint.x - halfWidth; x <= pathPoint.x + halfWidth; x++) {
                for (int z = pathPoint.z - halfWidth; z <= pathPoint.z + halfWidth; z++) {
                    allPositions.add(new Vector3i(x, pathPoint.y, z));
                }
            }
        }
        
        return allPositions;
    }
    
    /**
     * Gets the midpoint of this corridor.
     */
    public Vector3i getMidpoint() {
        if (path.isEmpty()) {
            return new Vector3i(
                (start.x + end.x) / 2,
                (start.y + end.y) / 2,
                (start.z + end.z) / 2
            );
        }
        
        return new Vector3i(path.get(path.size() / 2));
    }
    
    /**
     * Checks if this corridor intersects with another corridor.
     */
    public boolean intersects(DungeonCorridor other) {
        for (Vector3i pos1 : this.getAllPositions()) {
            for (Vector3i pos2 : other.getAllPositions()) {
                if (pos1.equals(pos2)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // Getters
    public String getCorridorId() { return corridorId; }
    public Vector3i getStart() { return new Vector3i(start); }
    public Vector3i getEnd() { return new Vector3i(end); }
    public int getWidth() { return width; }
    public List<Vector3i> getPath() { return new ArrayList<>(path); }
    
    @Override
    public String toString() {
        return String.format("DungeonCorridor{id='%s', start=(%d,%d,%d), end=(%d,%d,%d), width=%d, length=%d}", 
                           corridorId, start.x, start.y, start.z, end.x, end.y, end.z, width, getLength());
    }
}