package com.odyssey.world;

import com.odyssey.world.BiomeType;
import com.odyssey.world.Island;
import com.odyssey.world.navigation.NavigationHazard;
import org.joml.Vector2f;
import org.joml.Vector3f;
import java.util.*;

/**
 * Represents a chunk of the world containing terrain, biomes, and features
 */
public class WorldChunk {
    
    private final int chunkX;
    private final int chunkZ;
    private final int chunkSize;
    
    // Terrain data
    private final float[][] heightMap;
    private final BiomeType[][] biomeMap;
    private final float[][] temperatureMap;
    private final float[][] humidityMap;
    
    // Features
    private final List<Island> islands;
    private final List<NavigationHazard> hazards;
    private final Map<Vector2f, Float> depthMap;
    
    // Ocean features
    private final float[][] currentStrengthMap;
    private final Vector2f[][] currentDirectionMap;
    private final float[][] waveHeightMap;
    
    // Generation state
    private boolean terrainGenerated = false;
    private boolean biomesGenerated = false;
    private boolean featuresGenerated = false;
    
    public WorldChunk(int chunkX, int chunkZ, int chunkSize) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.chunkSize = chunkSize;
        
        // Initialize terrain data
        this.heightMap = new float[chunkSize][chunkSize];
        this.biomeMap = new BiomeType[chunkSize][chunkSize];
        this.temperatureMap = new float[chunkSize][chunkSize];
        this.humidityMap = new float[chunkSize][chunkSize];
        
        // Initialize features
        this.islands = new ArrayList<>();
        this.hazards = new ArrayList<>();
        this.depthMap = new HashMap<>();
        
        // Initialize ocean features
        this.currentStrengthMap = new float[chunkSize][chunkSize];
        this.currentDirectionMap = new Vector2f[chunkSize][chunkSize];
        this.waveHeightMap = new float[chunkSize][chunkSize];
        
        // Initialize current directions
        for (int x = 0; x < chunkSize; x++) {
            for (int z = 0; z < chunkSize; z++) {
                currentDirectionMap[x][z] = new Vector2f(0, 0);
            }
        }
    }
    
    /**
     * Gets the height at local chunk coordinates
     */
    public float getHeightAt(float localX, float localZ) {
        if (localX < 0 || localX >= chunkSize || localZ < 0 || localZ >= chunkSize) {
            return 0; // Default height for out of bounds
        }
        
        // Bilinear interpolation for smooth height values
        int x1 = (int) Math.floor(localX);
        int z1 = (int) Math.floor(localZ);
        int x2 = Math.min(x1 + 1, chunkSize - 1);
        int z2 = Math.min(z1 + 1, chunkSize - 1);
        
        float fx = localX - x1;
        float fz = localZ - z1;
        
        float h11 = heightMap[x1][z1];
        float h12 = heightMap[x1][z2];
        float h21 = heightMap[x2][z1];
        float h22 = heightMap[x2][z2];
        
        float h1 = h11 * (1 - fx) + h21 * fx;
        float h2 = h12 * (1 - fx) + h22 * fx;
        
        return h1 * (1 - fz) + h2 * fz;
    }
    
    /**
     * Gets the biome at local chunk coordinates
     */
    public BiomeType getBiomeAt(float localX, float localZ) {
        if (localX < 0 || localX >= chunkSize || localZ < 0 || localZ >= chunkSize) {
            return BiomeType.OCEAN; // Default biome for out of bounds
        }
        
        int x = (int) Math.floor(localX);
        int z = (int) Math.floor(localZ);
        
        x = Math.max(0, Math.min(x, chunkSize - 1));
        z = Math.max(0, Math.min(z, chunkSize - 1));
        
        return biomeMap[x][z];
    }
    
    /**
     * Gets the temperature at local chunk coordinates
     */
    public float getTemperatureAt(float localX, float localZ) {
        if (localX < 0 || localX >= chunkSize || localZ < 0 || localZ >= chunkSize) {
            return 20.0f; // Default temperature
        }
        
        int x = (int) Math.floor(localX);
        int z = (int) Math.floor(localZ);
        
        x = Math.max(0, Math.min(x, chunkSize - 1));
        z = Math.max(0, Math.min(z, chunkSize - 1));
        
        return temperatureMap[x][z];
    }
    
    /**
     * Gets the humidity at local chunk coordinates
     */
    public float getHumidityAt(float localX, float localZ) {
        if (localX < 0 || localX >= chunkSize || localZ < 0 || localZ >= chunkSize) {
            return 0.5f; // Default humidity
        }
        
        int x = (int) Math.floor(localX);
        int z = (int) Math.floor(localZ);
        
        x = Math.max(0, Math.min(x, chunkSize - 1));
        z = Math.max(0, Math.min(z, chunkSize - 1));
        
        return humidityMap[x][z];
    }
    
    /**
     * Gets the ocean current at local chunk coordinates
     */
    public Vector2f getCurrentAt(float localX, float localZ) {
        if (localX < 0 || localX >= chunkSize || localZ < 0 || localZ >= chunkSize) {
            return new Vector2f(0, 0);
        }
        
        int x = (int) Math.floor(localX);
        int z = (int) Math.floor(localZ);
        
        x = Math.max(0, Math.min(x, chunkSize - 1));
        z = Math.max(0, Math.min(z, chunkSize - 1));
        
        Vector2f direction = currentDirectionMap[x][z];
        float strength = currentStrengthMap[x][z];
        
        return new Vector2f(direction.x * strength, direction.y * strength);
    }
    
    /**
     * Gets the wave height at local chunk coordinates
     */
    public float getWaveHeightAt(float localX, float localZ) {
        if (localX < 0 || localX >= chunkSize || localZ < 0 || localZ >= chunkSize) {
            return 0;
        }
        
        int x = (int) Math.floor(localX);
        int z = (int) Math.floor(localZ);
        
        x = Math.max(0, Math.min(x, chunkSize - 1));
        z = Math.max(0, Math.min(z, chunkSize - 1));
        
        return waveHeightMap[x][z];
    }
    
    /**
     * Gets the water depth at a position (negative values indicate above sea level)
     */
    public float getDepthAt(float localX, float localZ, float seaLevel) {
        float height = getHeightAt(localX, localZ);
        return seaLevel - height;
    }
    
    /**
     * Checks if a position is underwater
     */
    public boolean isUnderwater(float localX, float localZ, float seaLevel) {
        return getHeightAt(localX, localZ) < seaLevel;
    }
    
    /**
     * Adds an island to this chunk
     */
    public void addIsland(Island island) {
        islands.add(island);
    }
    
    /**
     * Adds a navigation hazard to this chunk
     */
    public void addHazard(NavigationHazard hazard) {
        hazards.add(hazard);
    }
    
    /**
     * Sets height at specific coordinates
     */
    public void setHeight(int x, int z, float height) {
        if (x >= 0 && x < chunkSize && z >= 0 && z < chunkSize) {
            heightMap[x][z] = height;
        }
    }
    
    /**
     * Sets biome at specific coordinates
     */
    public void setBiome(int x, int z, BiomeType biome) {
        if (x >= 0 && x < chunkSize && z >= 0 && z < chunkSize) {
            biomeMap[x][z] = biome;
        }
    }
    
    /**
     * Sets temperature at specific coordinates
     */
    public void setTemperature(int x, int z, float temperature) {
        if (x >= 0 && x < chunkSize && z >= 0 && z < chunkSize) {
            temperatureMap[x][z] = temperature;
        }
    }
    
    /**
     * Sets humidity at specific coordinates
     */
    public void setHumidity(int x, int z, float humidity) {
        if (x >= 0 && x < chunkSize && z >= 0 && z < chunkSize) {
            humidityMap[x][z] = humidity;
        }
    }
    
    /**
     * Sets ocean current at specific coordinates
     */
    public void setCurrent(int x, int z, Vector2f direction, float strength) {
        if (x >= 0 && x < chunkSize && z >= 0 && z < chunkSize) {
            currentDirectionMap[x][z] = direction.normalize();
            currentStrengthMap[x][z] = strength;
        }
    }
    
    /**
     * Sets wave height at specific coordinates
     */
    public void setWaveHeight(int x, int z, float waveHeight) {
        if (x >= 0 && x < chunkSize && z >= 0 && z < chunkSize) {
            waveHeightMap[x][z] = waveHeight;
        }
    }
    
    // Getters
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public int getChunkSize() { return chunkSize; }
    
    public List<Island> getIslands() { return new ArrayList<>(islands); }
    public List<NavigationHazard> getHazards() { return new ArrayList<>(hazards); }
    
    public boolean isTerrainGenerated() { return terrainGenerated; }
    public boolean isBiomesGenerated() { return biomesGenerated; }
    public boolean isFeaturesGenerated() { return featuresGenerated; }
    
    public void setTerrainGenerated(boolean terrainGenerated) { this.terrainGenerated = terrainGenerated; }
    public void setBiomesGenerated(boolean biomesGenerated) { this.biomesGenerated = biomesGenerated; }
    public void setFeaturesGenerated(boolean featuresGenerated) { this.featuresGenerated = featuresGenerated; }
    
    /**
     * Gets world coordinates for this chunk
     */
    public Vector2f getWorldPosition() {
        return new Vector2f(chunkX * chunkSize, chunkZ * chunkSize);
    }
    
    /**
     * Converts local coordinates to world coordinates
     */
    public Vector2f localToWorld(float localX, float localZ) {
        return new Vector2f(
            chunkX * chunkSize + localX,
            chunkZ * chunkSize + localZ
        );
    }
    
    /**
     * Converts world coordinates to local coordinates
     */
    public Vector2f worldToLocal(float worldX, float worldZ) {
        return new Vector2f(
            worldX - (chunkX * chunkSize),
            worldZ - (chunkZ * chunkSize)
        );
    }
}