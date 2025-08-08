package com.odyssey.world.treasures;

import org.joml.Vector2i;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the visual data for a treasure map, including explored areas,
 * landmarks, and the treasure location marker.
 */
public class MapData {
    private int originX, originZ;
    private int width, height;
    private boolean[][] exploredAreas;
    private Map<Vector2i, TreasureMap.LandmarkType> landmarks;
    private Vector2i treasureLocation;
    private float zoomLevel = 1.0f;
    
    public MapData() {
        this.landmarks = new HashMap<>();
    }
    
    /**
     * Initializes the map data with specified dimensions and origin.
     */
    public void initialize(int originX, int originZ, int width, int height) {
        this.originX = originX;
        this.originZ = originZ;
        this.width = width;
        this.height = height;
        this.exploredAreas = new boolean[width][height];
        this.landmarks.clear();
        this.treasureLocation = null;
    }
    
    /**
     * Reveals an area around the specified coordinates.
     */
    public void revealArea(int worldX, int worldZ, int radius) {
        int mapX = worldX - originX;
        int mapZ = worldZ - originZ;
        
        for (int x = mapX - radius; x <= mapX + radius; x++) {
            for (int z = mapZ - radius; z <= mapZ + radius; z++) {
                if (isValidMapCoordinate(x, z)) {
                    // Check if within circular radius
                    int dx = x - mapX;
                    int dz = z - mapZ;
                    if (dx * dx + dz * dz <= radius * radius) {
                        exploredAreas[x][z] = true;
                    }
                }
            }
        }
    }
    
    /**
     * Marks a landmark at the specified world coordinates.
     */
    public void markLandmark(int worldX, int worldZ, TreasureMap.LandmarkType type) {
        Vector2i mapPos = worldToMapCoordinates(worldX, worldZ);
        if (mapPos != null) {
            landmarks.put(mapPos, type);
        }
    }
    
    /**
     * Marks the treasure location at the specified world coordinates.
     */
    public void markTreasureLocation(int worldX, int worldZ) {
        this.treasureLocation = worldToMapCoordinates(worldX, worldZ);
    }
    
    /**
     * Converts world coordinates to map coordinates.
     */
    private Vector2i worldToMapCoordinates(int worldX, int worldZ) {
        int mapX = worldX - originX;
        int mapZ = worldZ - originZ;
        
        if (isValidMapCoordinate(mapX, mapZ)) {
            return new Vector2i(mapX, mapZ);
        }
        return null;
    }
    
    /**
     * Converts map coordinates to world coordinates.
     */
    public Vector2i mapToWorldCoordinates(int mapX, int mapZ) {
        return new Vector2i(mapX + originX, mapZ + originZ);
    }
    
    /**
     * Checks if the given map coordinates are valid.
     */
    private boolean isValidMapCoordinate(int mapX, int mapZ) {
        return mapX >= 0 && mapX < width && mapZ >= 0 && mapZ < height;
    }
    
    /**
     * Checks if an area at the given map coordinates is explored.
     */
    public boolean isExplored(int mapX, int mapZ) {
        if (!isValidMapCoordinate(mapX, mapZ)) {
            return false;
        }
        return exploredAreas[mapX][mapZ];
    }
    
    /**
     * Gets the landmark at the specified map coordinates.
     */
    public TreasureMap.LandmarkType getLandmarkAt(int mapX, int mapZ) {
        return landmarks.get(new Vector2i(mapX, mapZ));
    }
    
    /**
     * Checks if there's a landmark at the specified map coordinates.
     */
    public boolean hasLandmarkAt(int mapX, int mapZ) {
        return landmarks.containsKey(new Vector2i(mapX, mapZ));
    }
    
    /**
     * Checks if the treasure location is at the specified map coordinates.
     */
    public boolean isTreasureAt(int mapX, int mapZ) {
        return treasureLocation != null && 
               treasureLocation.x == mapX && treasureLocation.y == mapZ;
    }
    
    /**
     * Gets the percentage of the map that has been explored.
     */
    public float getExplorationPercentage() {
        int totalCells = width * height;
        int exploredCells = 0;
        
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                if (exploredAreas[x][z]) {
                    exploredCells++;
                }
            }
        }
        
        return (float) exploredCells / totalCells;
    }
    
    /**
     * Sets the zoom level for map rendering.
     */
    public void setZoomLevel(float zoomLevel) {
        this.zoomLevel = Math.max(0.5f, Math.min(4.0f, zoomLevel)); // Clamp between 0.5x and 4x
    }
    
    /**
     * Zooms in on the map.
     */
    public void zoomIn() {
        setZoomLevel(zoomLevel * 1.5f);
    }
    
    /**
     * Zooms out on the map.
     */
    public void zoomOut() {
        setZoomLevel(zoomLevel / 1.5f);
    }
    
    /**
     * Resets zoom to default level.
     */
    public void resetZoom() {
        this.zoomLevel = 1.0f;
    }
    
    /**
     * Gets the current zoom level.
     */
    public float getZoomLevel() {
        return zoomLevel;
    }
    
    /**
     * Gets the map dimensions.
     */
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    
    /**
     * Gets the map origin in world coordinates.
     */
    public int getOriginX() { return originX; }
    public int getOriginZ() { return originZ; }
    
    /**
     * Gets the treasure location in map coordinates.
     */
    public Vector2i getTreasureLocation() {
        return treasureLocation != null ? new Vector2i(treasureLocation) : null;
    }
    
    /**
     * Gets all landmarks on the map.
     */
    public Map<Vector2i, TreasureMap.LandmarkType> getLandmarks() {
        return new HashMap<>(landmarks);
    }
    
    /**
     * Clears all exploration data (useful for creating blank maps).
     */
    public void clearExploration() {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                exploredAreas[x][z] = false;
            }
        }
    }
    
    /**
     * Reveals the entire map (useful for creative mode or admin commands).
     */
    public void revealAll() {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                exploredAreas[x][z] = true;
            }
        }
    }
}